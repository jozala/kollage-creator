package pl.aetas.kollage.creator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode

val CHOOSE_TWO_COLUMNS_PROBABILITY = 0.2;
val SMALL_DIFF_PX = 10;

data class Size(val width: Int, val height: Int) {
    fun area(): Double = width.toDouble() * height
    fun resize(targetMaxWidth: Int, targetMaxHeight: Int): Size {
        var newSize = this
        if (width > targetMaxWidth) {
            val newWidth = targetMaxWidth
            val newHeight = height * (newWidth.toDouble() / width)
            newSize = Size(newWidth, newHeight.toInt())
        }
        if (newSize.height > targetMaxHeight) {
            val newHeight = targetMaxHeight
            val newWidth = width * (newHeight.toDouble() / height)
            newSize = Size(newWidth.toInt(), newHeight)
        }
        return newSize
    }
}

data class Position(val x: Int, val y: Int)

class Canvas(columnsNumber: Int, val size: Size) {


    private val columns: Array<Column> = Array(columnsNumber, { Column(size.width / columnsNumber) })

    val logger: Logger = LoggerFactory.getLogger(Canvas::class.java)
    init {
        logger.debug("column width: ${size.width / columnsNumber}")
    }

    fun addImage(image: Image) {
        val shortestColumn = shortestColumn()
        val shortestColumnIndex = columns.indexOf(shortestColumn)
        val similarWithPlusOne = areNeighbouringColumnsSimilarHeight(shortestColumnIndex, shortestColumnIndex + 1)
        val similarWithMinusOne = areNeighbouringColumnsSimilarHeight(shortestColumnIndex, shortestColumnIndex - 1)

        fun joinColumnsByProbability() = Math.random() < CHOOSE_TWO_COLUMNS_PROBABILITY

        if (similarWithMinusOne && joinColumnsByProbability()) {
            alignColumns(shortestColumnIndex, shortestColumnIndex-1)
            val neighbourColumn = columns[shortestColumnIndex - 1]
            insertIntoBothColumns(image, neighbourColumn, shortestColumn)
        } else if (similarWithPlusOne && joinColumnsByProbability()) {
            alignColumns(shortestColumnIndex, shortestColumnIndex+1)
            val neighbourColumn = columns[shortestColumnIndex + 1]
            insertIntoBothColumns(image, shortestColumn, neighbourColumn)
        } else {
            shortestColumn.fit(Wrapper(image))
        }
    }

    private fun insertIntoBothColumns(image: Image, leftColumn: Column, rightColumn: Column) {
        val realWrapper = Wrapper(image)
        val linkedWrapper = Wrapper(realWrapper.size())
        linkedWrapper.linkedWrapper = realWrapper
        realWrapper.linkedWrapper = linkedWrapper
        leftColumn.fitIntoMultiple(realWrapper, 2) // TODO possible improvements to not do resize twice. We need to add
        rightColumn.fitIntoMultiple(linkedWrapper, 2) // content, but not need to resize it from both wrappers
    }

    private fun shortestColumn(): Column = columns.minBy { it.height() }!!

    private fun areNeighbouringColumnsSimilarHeight(col1Index: Int, col2Index: Int): Boolean {

        if (col1Index < -1 || col2Index < -1) throw IllegalStateException("Not possible!!")
        if (col1Index < 0 || col2Index < 0) return false
        if (col1Index > columns.size-1 || col2Index > columns.size-1) return false

        val diff = Math.abs(columns[col1Index].height() - columns[col2Index].height())
        return diff <= SMALL_DIFF_PX
    }

    private fun alignColumns(col1Index: Int, col2Index: Int) {

        val shorterColumn = listOf(columns[col1Index], columns[col2Index]).minBy { it.height() }!!
        val longerColumn = listOf(columns[col1Index], columns[col2Index]).maxBy { it.height() }!!
        val diffInPx = longerColumn.height() - shorterColumn.height()

        if (diffInPx != 0) {
            logger.debug("aligning diff: $diffInPx px")
            val lastInColumn = longerColumn.content.last()
            lastInColumn.crop(lastInColumn.size().width, lastInColumn.size().height-diffInPx)
            // TODO here we can improve the algorithm not to crop last in column but to crop a little bit of each image
            // in the column (but maybe only until linkedWrapper is found to not break anything else)
        }
    }

    fun getImagesWithPosition(): List<Pair<Wrapper, Position>> {
        val images: MutableList<Pair<Wrapper, Position>> = arrayListOf()

        var currentX = 0
        var currentY = 0
        columns.forEach { column ->
            column.content.forEach { wrapper ->
                if (!wrapper.isPlaceholder()) {
                    images.add(Pair(wrapper, Position(currentX, currentY)))
                }
                currentY += wrapper.size().height
            }
            currentX += column.width
            currentY = 0
        }

        return images;
    }

    fun alignBottom() {
        logger.debug("Shortest column length is ${shortestColumn().height()} and canvas is ${size.height}")

        val finalHeight = size.height
        val sortedColumns = columns.sortedByDescending { it.height() }

        if (columns.any { it.height() < finalHeight }) {
            throw IllegalStateException("Height of some columns is less than expected - not possible to adjust length by cropping")
        }

        logger.debug("column ${columns.indexOf(sortedColumns.first())} is the highest one")
        logger.debug("column ${columns.indexOf(sortedColumns.last())} is the shortest one")



        columns.forEach { column ->
            val overallDiff = column.height() - finalHeight // 1050 - 1000 = 50 //    1030 - 1000 = 30
            val averageDiff: Int = overallDiff / column.content.filter { !it.locked }.size // 50 / 12 = 4      30 / 8 = 3
            val leftoverDiff: Int = overallDiff % column.content.filter { !it.locked }.size // 50 % 12 = 2     30 % 8 = 6

            val colIndex = columns.indexOf(column)
            val groups: MutableList<Group> = arrayListOf()
            groups.add(Group(0))

            var previousLocked: Wrapper? = null
            column.content.forEach { wrapper ->
                if (!wrapper.locked) {
                    groups.last().wrappers.add(wrapper)
                } else {
                    var thisLockedY = 0
                    if (colIndex > 0 && columns[colIndex-1].content.contains(wrapper.linkedWrapper)) {
                        thisLockedY = columns[colIndex-1].wrapperY(wrapper.linkedWrapper!!)
                    } else if (colIndex < columns.size-1 && columns[colIndex+1].content.contains(wrapper.linkedWrapper)) {
                        thisLockedY = columns[colIndex+1].wrapperY(wrapper.linkedWrapper!!)
                    } else {
                        throw IllegalStateException("None of the neighbours contain wrapper")
                    }
                    val thisLockedHeight = wrapper.size().height

                    var previousLockedY = 0;
                    if (colIndex > 0 && columns[colIndex-1].content.contains(previousLocked?.linkedWrapper)) {
                        previousLockedY = columns[colIndex-1].wrapperY(previousLocked!!.linkedWrapper!!)
                    } else if (colIndex < columns.size-1 && columns[colIndex+1].content.contains(previousLocked?.linkedWrapper)) {
                        previousLockedY = columns[colIndex+1].wrapperY(previousLocked!!.linkedWrapper!!)
                    }
                    val previousLockedHeight = previousLocked?.size()?.height ?: 0 

                    groups.last().height = thisLockedY - (previousLockedY + previousLockedHeight)
                    logger.debug("Group height is ${groups.last().height} and all wrappers are ${groups.last().wrappers.sumBy { it.size().height }}")
                    if (groups.last().height > groups.last().wrappers.sumBy { it.size().height }) {
                        val gapSize = groups.last().height - groups.last().wrappers.sumBy { it.size().height }
                        if (gapSize > 5) {
                            throw IllegalStateException("group height cannot be higher than sum of wrappers: $gapSize")
                        } else {
                            logger.warn("Unfortunate setting with locked wrappers from two sides - for now we have to live with that: $gapSize")
                        }
                    }

                    groups.add(Group(thisLockedY + thisLockedHeight))
                    previousLocked = wrapper
                }
            }
            groups.last().height = this.size.height - groups.last().y
            logger.debug("Group height is ${groups.last().height} and all wrappers are ${groups.last().wrappers.sumBy { it.size().height }}")
            if (groups.last().height > groups.last().wrappers.sumBy { it.size().height }) {
                val gapSize = groups.last().height - groups.last().wrappers.sumBy { it.size().height }
                throw IllegalStateException("group height cannot be higher than sum of wrappers: $gapSize. Probably number of column is too high.")
            }

            groups.filter { it.wrappers.isNotEmpty() }.forEach { group ->
                val groupOverallDiff = group.wrappers.sumBy { it.size().height } - group.height
                val groupAverageDiff: Int = groupOverallDiff / group.wrappers.size
                val groupLeftoverDiff: Int = groupOverallDiff % group.wrappers.size


                logger.debug("Group overall diff: ${group.wrappers.sumBy { it.size().height }} - ${group.height} = $groupOverallDiff")
                logger.debug("Group average diff = $groupAverageDiff")
                logger.debug("Group leftover diff = $groupLeftoverDiff")

                if(groupAverageDiff != 0) {
                    group.wrappers.forEach { image: Wrapper ->
                        val imageSize = image.size()
                        image.crop(imageSize.width, imageSize.height - groupAverageDiff)
                    }
                }

                val highestImage = group.wrappers.maxBy { it.size().height }!!

                val highestImageSize = highestImage.size()


                highestImage.crop(highestImageSize.width, highestImageSize.height - groupLeftoverDiff)

            }


//            if(averageDiff > 0) {
//                column.content.forEach { image: Wrapper ->
//                    val imageSize = image.size()
//                    image.crop(imageSize.width, imageSize.height - averageDiff)
//                }
//            }
//
//            val highestImage = column.content.filter { it.linkedWrapper == null }.maxBy { it.size().height }!!
//
//            val highestImageSize = highestImage.size()
//            highestImage.crop(highestImageSize.width, highestImageSize.height - leftoverDiff)

            column.content.forEach { it.lock() }

            logger.debug("column aligned to height ${column.height()}")

        }

    }


}

class Group(var y: Int) {

    var height = 0
    val wrappers: MutableList<Wrapper> = arrayListOf()
}

class Column(val width: Int) {
    val content: MutableList<Wrapper> = arrayListOf()

    val logger: Logger = LoggerFactory.getLogger(Column::class.java)


    fun fit(image: Wrapper) {
        image.resize(width, image.size().height)
        content.add(image)
    }

    fun fitIntoMultiple(image: Wrapper, columnsNumber: Int) {
        image.resize(width*columnsNumber, image.size().height)
        content.add(image)
    }
    fun height(): Int {
        if (content.isEmpty()) return 0
        return content.last().y() + content.last().size().height // FIXME content.sumBy { it.size().height }
        // more safe this way as there might be empty spaces between images // may be empty space really??
    }


    fun alignHeight(targetHeight: Int) {
        class Group(var y: Int) {

            var height = 0
            val wrappers: MutableList<Wrapper> = arrayListOf()
        }

        val groups: MutableList<Group> = arrayListOf()
        groups.add(Group(0))

        for (wrapper in content) {
            if (!wrapper.isPlaceholder()) {
                groups.last().wrappers.add(wrapper)
            } else {
                groups.last().height = wrapper.y() - groups.last().y
                groups.add(Group(wrapper.y() + wrapper.size().height))
            }
        }
        groups.last().height = targetHeight - groups.last().y

        for (group in groups) {
            if (group.wrappers.size == 0) continue
            var alpha = group.height.toDouble() / group.wrappers.map { it.size().height }.sum()
            for (wrapperInGroup in group.wrappers) {
                wrapperInGroup.crop(wrapperInGroup.size().width, (wrapperInGroup.size().height * alpha).toInt())
            }
        }

    }

    fun Wrapper.y(): Int {
        var result = 0
        for (wrapperInColumn in this@Column.content) {
            if(wrapperInColumn === this) {
                return result;
            } else {
                result += wrapperInColumn.size().height
            }
        }

        throw IllegalStateException("Should never get to this point")
    }

    fun wrapperY(wrapper: Wrapper): Int {
        var result = 0
        for (wrapperInColumn in this.content) {
            if(wrapperInColumn === wrapper) {
                return result;
            } else {
                result += wrapperInColumn.size().height
            }
        }

        throw IllegalStateException("Should never get to this point")
    }

}



class Wrapper(private var size: Size) {

    val logger: Logger = LoggerFactory.getLogger(Wrapper::class.java)

    var image: Image? = null
    var linkedWrapper: Wrapper? = null
    private val actionsToApply: MutableList<ImageAction> = arrayListOf()
    var locked = false

    constructor(image: Image) : this(image.size) {
        this.image = image
    }

    fun size(): Size {
        var actualWrapper = this;
        actionsToApply.forEach {
            actualWrapper = it.simulate(actualWrapper.size)
        }
        return actualWrapper.size
    }

    fun isPlaceholder() = (image == null)

    fun applyActions() {
        if (isPlaceholder()) {
            throw IllegalAccessError("it should never be run for placeholder")
        }
        actionsToApply.forEach { it.apply(this) }
    }

    fun resize(targetWidth: Int, targetHeight: Int) {
        if (locked) {
            logger.debug("This wrapper will not be resized as it is locked")
            return
        }
        actionsToApply.add(Resize(targetWidth, targetHeight))
        linkedWrapper?.actionsToApply?.add(Resize(targetWidth, targetHeight))
    }

    fun crop(targetWidth: Int, targetHeight: Int) {
        if (locked) {
            logger.debug("This wrapper will not be cropped as it is locked")
            return
        }
        if (targetWidth > this.size().width || targetHeight > this.size().height) {
            val gapSize = listOf(targetWidth - size().width, targetHeight - this.size().height).max()!!
            if (gapSize > 5) {
                throw IllegalArgumentException("Cannot crop image [${size().width} x ${size().height}] to [$targetWidth x $targetHeight]")
            } else {
                logger.warn("No crop because of unfortunate gap with: $gapSize px")
                return
            }
        }
        actionsToApply.add(Crop(targetWidth, targetHeight))
        linkedWrapper?.actionsToApply?.add(Crop(targetWidth, targetHeight))
    }

    fun lock() {
        locked = true
        linkedWrapper?.locked = true
    }
}

interface ImageAction {
    fun simulate(currentSize: Size): Wrapper
    fun apply(wrapper: Wrapper)
}

class Crop(val targetWidth: Int, val targetHeight: Int): ImageAction {
    override fun simulate(currentSize: Size): Wrapper {
        return Wrapper(Size(targetWidth, targetHeight))
    }

    override fun apply(wrapper: Wrapper) {
        wrapper.image!!.crop(targetWidth, targetHeight)
    }
}

class Resize(val targetWidth: Int, val targetHeight: Int): ImageAction {

    override fun simulate(currentSize: Size): Wrapper {
        return Wrapper(currentSize.resize(targetWidth, targetHeight))
    }

    override fun apply(wrapper: Wrapper) {
        val image = wrapper.image!!
        image.resize(targetWidth, targetHeight)
    }
}

fun main(args: Array<String>) {
    val logger: Logger = LoggerFactory.getLogger("MAIN")


    val horizontalImages: Collection<Image> = (1..315).map { MockImage(Size(15*18, 10*18), null, "HORIZONTAL") }
    logger.debug("horizontal image size: ${15*18} x ${10*18}")

    val verticalImages: Collection<Image> = (1..50).map {MockImage(Size(10*18, 15*18), Color(255,255,255), "VERTICAL")}
    logger.debug("vertical image size: ${10*18} x ${15*18}")


    val allFiles = Files.newDirectoryStream(Paths.get("/Users/mariusz/Desktop/daily_photos_all")).toList()
    val allImages: List<PhotoImage> = allFiles.filter { it.fileName.toString().contains("jpg") }.map { PhotoImage(it) }
//    val allImages = horizontalImages + verticalImages
    Collections.shuffle(allImages)

    val canvasSize: Size = Size(30000, 44000)

    val canvasCreator = CanvasFactory()
    val canvas = canvasCreator.create(allImages, canvasSize)

    allImages.forEach { canvas.addImage(it) }
    canvas.alignBottom()

    val uniqueSizes: MutableSet<Size> = hashSetOf() // log only

    val imagesWithPosition: List<Pair<Wrapper, Position>> = canvas.getImagesWithPosition()
        val collageImage = BufferedImage(canvasSize.width, canvasSize.height + 300, BufferedImage.TYPE_INT_RGB) // FIXME - this (+300) is only to see bottom line clearly
    val graphics = collageImage.createGraphics()
    imagesWithPosition.forEach {
        val (wrapper, position) = it
        wrapper.applyActions()
//        uniqueSizes.add(Size(wrapper.image!!.image.width, wrapper.image!!.image.height)) // log only
        wrapper.image!!.load()
        graphics.drawImage(wrapper.image?.image, position.x, position.y, null)
        wrapper.image!!.unload()
    }

    logger.debug("UniqueSizes: $uniqueSizes")

//    ImageIO.write(collageImage, "jpg", File("result.png"));

    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    writer.output = ImageIO.createImageOutputStream(File("result2.jpg"))
    val param = writer.defaultWriteParam

    val metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(collageImage), param)
    val root = metadata.getAsTree(metadata.getNativeMetadataFormatName()) as IIOMetadataNode
    val jfif = root.getElementsByTagName("app0JFIF").item(0) as IIOMetadataNode

    jfif.setAttribute("resUnits", "1")
    jfif.setAttribute("Xdensity", "300")
    jfif.setAttribute("Ydensity", "300")

    metadata.mergeTree(metadata.nativeMetadataFormatName, root)

    writer.write(null, IIOImage(collageImage, null, metadata), param)
}

class CanvasFactory {

    val logger: Logger = LoggerFactory.getLogger(CanvasFactory::class.java)

    fun create(images: Collection<Image>, canvasSize: Size): Canvas {
        val canvasArea = canvasSize.area()
        val averageExpectedNormalImageArea = canvasArea.toDouble() / images.size
        val averageExpectedImaginatedImageArea = averageExpectedNormalImageArea * (1.0 - 0.2) // stupid way to have almost right height of columns
        val averageExpectedImageWidth = Math.sqrt(3.0 / 2 * averageExpectedImaginatedImageArea)
        val averageExpectedImageHeight = averageExpectedImageWidth * 2.0 / 3
        val columnsNumber = Math.ceil(canvasSize.width / averageExpectedImageWidth).toInt()

        logger.debug("Canvas size: $canvasSize")
        logger.debug("Canvas area: $canvasArea")
        logger.debug("averageExpectedNormalImageArea: $averageExpectedNormalImageArea")
        logger.debug("averageExpectedImageWidth: $averageExpectedImageWidth")
        logger.debug("averageExpectedImageHeight: $averageExpectedImageHeight")
        logger.debug("columnsNumber: $columnsNumber")
        logger.debug("rows number: ${images.size / columnsNumber}")

        return Canvas(columnsNumber, canvasSize)
    }
}

