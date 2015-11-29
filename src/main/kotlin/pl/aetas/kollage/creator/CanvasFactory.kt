package pl.aetas.kollage.creator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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

class Canvas(columnsNumber: Int, size: Size) {

    private val CHOOSE_TWO_COLUMNS_PROBABILITY = 0.1;
    private val SMALL_DIFF_PX = 0;


    private val columns: Array<Column> = Array(columnsNumber, { Column(size.width / columnsNumber) })

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
            insertIntoBothColumns(image, neighbourColumn, shortestColumn)
        } else {
            shortestColumn.fit(Wrapper(image))
        }
    }

    private fun insertIntoBothColumns(image: Image, neighbourColumn: Column, shortestColumn: Column) {
        val realWrapper = Wrapper(image)
        val linkedWrapper = Wrapper(realWrapper.size())
        linkedWrapper.linkedWrapper = realWrapper
        realWrapper.linkedWrapper = linkedWrapper
        shortestColumn.fitIntoMultiple(realWrapper, 2)
        neighbourColumn.fitIntoMultiple(linkedWrapper, 2)
    }

    private fun shortestColumn(): Column = columns.minBy { it.length() }!!

    private fun areNeighbouringColumnsSimilarHeight(col1Index: Int, col2Index: Int): Boolean {

        if (col1Index < 0 || col2Index < 0) return false;
        if (col1Index > columns.size-1 || col2Index > columns.size-1) return false;

        val diff = Math.abs(columns[col1Index].length() - columns[col2Index].length())
        return diff <= SMALL_DIFF_PX
    }

    private fun alignColumns(col1Index: Int, col2Index: Int) {

        val shorterColumn = listOf(columns[col1Index], columns[col2Index]).minBy { it.length() }!!
        val longerColumn = listOf(columns[col1Index], columns[col2Index]).maxBy { it.length() }!!
        val diffInPx = longerColumn.length() - shorterColumn.length()

        if (diffInPx != 0) {
            val lastInColumn = longerColumn.content.last()
            lastInColumn.resize(lastInColumn.size().width, lastInColumn.size().height-diffInPx)
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
        println("NOT done yet, because of the lazy dev.")
    }


}

class Column(val width: Int) {
    val content: MutableList<Wrapper> = arrayListOf()

    fun fit(image: Wrapper) {
        image.resize(width, image.size().height)
        content.add(image)
    }

    fun fitIntoMultiple(image: Wrapper, columnsNumber: Int) {
        image.resize(width*columnsNumber, image.size().height)
        content.add(image)
    }
    fun length() = content.sumBy { it.size().width }
}

class Wrapper(private var size: Size) {

    var image: Image? = null
    var linkedWrapper: Wrapper? = null
    private val actionsToApply: MutableList<ImageAction> = arrayListOf()

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
        actionsToApply.add(Resize(targetWidth, targetHeight))
    }
}

interface ImageAction {
    fun simulate(currentSize: Size): Wrapper
    fun apply(wrapper: Wrapper)
}

class Crop(x: Int, y: Int, width: Int, height: Int): ImageAction {
    override fun simulate(currentSize: Size): Wrapper {
        TODO()
    }

    override fun apply(wrapper: Wrapper) {
        throw UnsupportedOperationException()
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

    val images: Collection<Image> = (1..365).map { MockImage(Size(15*11, 10*11)) }
    val canvasSize: Size = Size(70*11, 100*11)

    val canvasCreator = CanvasFactory()
    val canvas = canvasCreator.create(images, canvasSize)

    images.forEach { canvas.addImage(it) }
    canvas.alignBottom()

    val imagesWithPosition: List<Pair<Wrapper, Position>> = canvas.getImagesWithPosition()
    val collageImage = BufferedImage(canvasSize.width, canvasSize.height, BufferedImage.TYPE_INT_RGB)
    val graphics = collageImage.createGraphics()
    imagesWithPosition.forEach {
        val (wrapper, position) = it
        wrapper.applyActions()
        graphics.drawImage(wrapper.image?.image, position.x, position.y, null)
    }

    ImageIO.write(collageImage, "jpg", File("result.png"));

}

class CanvasFactory {

    val logger: Logger = LoggerFactory.getLogger(CanvasFactory::class.java)

    fun create(images: Collection<Image>, canvasSize: Size): Canvas {
        val canvasArea = canvasSize.area()
        val averageExpectedImageArea = canvasArea / images.size
        val averageExpectedImageWidth = Math.sqrt(3.0 / 2 * averageExpectedImageArea.toDouble())
        val averageExpectedImageHeight = averageExpectedImageWidth * 2.0 / 3
        val columnsNumber = Math.floor(canvasSize.width / averageExpectedImageWidth).toInt()

        logger.debug("Canvas area: $canvasArea")
        logger.debug("averageExpectedImageArea: $averageExpectedImageArea")
        logger.debug("averageExpectedImageWidth: $averageExpectedImageWidth")
        logger.debug("averageExpectedImageHeight: $averageExpectedImageHeight")
        logger.debug("columnsNumber: $columnsNumber")
        logger.debug("rows number: ${images.size / columnsNumber}")

        return Canvas(columnsNumber, canvasSize)
    }
}

