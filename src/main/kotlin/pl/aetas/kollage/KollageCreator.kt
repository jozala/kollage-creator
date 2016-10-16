package pl.aetas.kollage

import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*


val CHOOSE_TWO_COLUMNS_PROBABILITY = 0.2F;


class Canvas(val columns: List<Column>, val size: Size,
             val chooseTwoColumnsProbability: Float, val similarHeightMaxDifferencePx: Int) {

    val LOGGER = LoggerFactory.getLogger(Canvas::class.java)


    fun addPhoto(image: Image) {

        val shortestColumn = shortestColumn()
        val shortestColumnIndex = columns.indexOf(shortestColumn)
        val columnOnTheLeft  = if (shortestColumn != columns.first()) columns[shortestColumnIndex - 1] else null
        val columnOnTheRight = if (shortestColumn != columns.last())  columns[shortestColumnIndex + 1] else null
        val extendToNeighbouringColumnByProbability = Random().nextFloat() < chooseTwoColumnsProbability
        val imageWrapper = ImageWrapper(image)

        if (similarHeightColumns(shortestColumn, columnOnTheLeft) && extendToNeighbouringColumnByProbability) {
            alignColumns(shortestColumn, columnOnTheLeft!!)
            shortestColumn.addPlaceholder(imageWrapper)
            columnOnTheLeft.fitExtended(imageWrapper)
        } else if (similarHeightColumns(shortestColumn, columnOnTheRight) && extendToNeighbouringColumnByProbability) {
            alignColumns(shortestColumn, columnOnTheRight!!)
            shortestColumn.fitExtended(imageWrapper)
            columnOnTheRight.addPlaceholder(imageWrapper)
        } else {
            shortestColumn.fit(imageWrapper)
        }
    }

    private fun alignColumns(shorterColumn: Column, longerColumn: Column) {
        val diffInPx = longerColumn.height() - shorterColumn.height()
        assert(diffInPx >= 0, { "Incorrect parameters " +
                "(shorterColumn height: ${shorterColumn.height()}, longerColumn height: ${longerColumn.height()})" })

        if (diffInPx != 0) {
            LOGGER.debug("aligning diff: $diffInPx px")
            longerColumn.crop(diffInPx)
        }
    }

    private fun similarHeightColumns(column1: Column, column2: Column?): Boolean {
        if (column2 == null) return false
        val columnHeight = column1.height()
        val neighbourHeight = column2.height()
        val heightDifference = neighbourHeight - columnHeight
        return Math.abs(heightDifference) <= similarHeightMaxDifferencePx
    }

    private fun shortestColumn() = columns.minBy { it.height() }!!

    fun alignBottomLine() {
        // TODO 3 continue LATER here - check previous TODOs first
        throw UnsupportedOperationException("not implemented")
    }

    fun draw(): BufferedImage {
        val collageImage = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB) // TODO - can add height (+300) to see bottom line clearly
        val graphics = collageImage.createGraphics()
        columns.flatMap { it.contentWithPosition() }
                .filter { it.first is ImageWrapper }
                .forEach { pair ->
                    val (wrapper, position) = pair
                    val image = wrapper as ImageWrapper
                    graphics.drawImage(image.bufferedImage(), position.x, position.y, null)
                }
        return collageImage
    }
}

class Column(val x: Int, val width: Int) {

    private val content: MutableList<Wrapper> = mutableListOf()

    fun fit(wrapper: ImageWrapper) {
        wrapper.resize(width, wrapper.height())
        content.add(wrapper)
    }

    fun fitExtended(wrapper: ImageWrapper) {
        wrapper.resize(width * 2, wrapper.height())
        content.add(wrapper)
    }

    fun addPlaceholder(wrapper: ImageWrapper) {
        content.add(PlaceholderWrapper(wrapper, width))
    }

    fun height() = content.sumBy { it.height() }

    fun contentWithPosition(): List<Pair<Wrapper, Position>> {
        return content.fold(emptyList<Pair<Wrapper, Position>>(), { currentList, image ->
                    currentList + Pair(image, Position(x, currentList.sumBy { it.first.height() }))
                }
        )
    }

    override fun toString(): String {
        return "Column(x=$x, width=$width)"
    }

    fun crop(pixelsToCrop: Int) {
        content.last().crop(pixelsToCrop)
    }

}

interface Wrapper {

    fun size(): Size
    fun width(): Int = size().width
    fun height(): Int = size().height
    fun crop(pixelsToCrop: Int)
}

class PlaceholderWrapper(private val linkedWrapper: ImageWrapper, private val width: Int): Wrapper {
    override fun size() = Size(width, linkedWrapper.size().height)
    override fun crop(pixelsToCrop: Int) { linkedWrapper.crop(pixelsToCrop) }
}

class ImageWrapper(private val image: Image): Wrapper {

    private val originalSize: Size = image.size
    private val operations: MutableList<ImageOperation> = mutableListOf()

    override fun size(): Size {
        return operations.fold(originalSize, { size, operation -> operation.simulate(size) })
    }

    fun bufferedImage(): BufferedImage {
        val (width, height) = size()
        /**
         * This is simplified version of applying all changes. Thanks to that we do not need to apply each operation on buffered image.
         * However, each new operation type need to be implemented here as well.
         */

        val simplifiedDownsizeOperation = DownsizeOperation(width, originalSize.height)
        val bufferedImageAfterDownsize = simplifiedDownsizeOperation.apply(image.bufferedImage())
        val bufferedImageAfterCrop = CropOperation(bufferedImageAfterDownsize.height - height).apply(bufferedImageAfterDownsize)

        return bufferedImageAfterCrop;
    }

    fun resize(targetWidth: Int, targetHeight: Int) {
        operations.add(DownsizeOperation(targetWidth, targetHeight))
    }

    override fun crop(pixelsToCrop: Int) {
        assert(pixelsToCrop < size().height, {"PixelsToCrop is higher than actual height of image ($pixelsToCrop < ${size().height}"})
        operations.add(CropOperation(pixelsToCrop))
    }

    interface ImageOperation {
        fun simulate(currentSize: Size): Size
        fun apply(bufferedImage: BufferedImage): BufferedImage
    }

    class DownsizeOperation(val targetMaxWidth: Int, val targetMaxHeight: Int): ImageOperation {

        val LOGGER = LoggerFactory.getLogger(DownsizeOperation::class.java)

        override fun apply(bufferedImage: BufferedImage): BufferedImage {
            val (width, height) = simulate(Size(bufferedImage.width, bufferedImage.height))
            return Scalr.resize(bufferedImage, width, height)
        }

        override fun simulate(currentSize: Size): Size {

            if (currentSize.width < targetMaxWidth) throw IllegalArgumentException("Cannot downsize image to width higher than original")
            if (currentSize.height < targetMaxHeight) throw IllegalArgumentException("Cannot downsize image to height higher than original")

            val widthRatio = targetMaxWidth.toDouble() / currentSize.width
            val heightRatio = targetMaxHeight.toDouble() / currentSize.height

            if (widthRatio <= heightRatio) {
                return Size(targetMaxWidth, Math.ceil(currentSize.height * widthRatio).toInt())
            }

            LOGGER.warn("Resizing by height - unusual operation - check if it is really needed")
            return Size(Math.ceil(currentSize.width * heightRatio).toInt(), targetMaxHeight)

        }

    }

    class CropOperation(val pixelsToCrop: Int): ImageOperation {

        override fun simulate(currentSize: Size): Size {
            if (currentSize.height < pixelsToCrop) {
                throw IllegalArgumentException("Pixels to crop ($pixelsToCrop) is higher than image height (${currentSize.height})")
            }
            return Size(currentSize.width, currentSize.height - pixelsToCrop)
        }

        override fun apply(bufferedImage: BufferedImage): BufferedImage {
            val (width, height) = simulate(Size(bufferedImage.width, bufferedImage.height))
            return Scalr.crop(bufferedImage, width, height)
        }

    }
}

data class Size(val width: Int, val height: Int)

data class Position (val x: Int, val y: Int)


fun main(args: Array<String>) {

    /* Input parameters */
    val canvasSize = Size(1080, 1920);
    val directoryPath = "/Users/mariusz/Desktop/daily_photos_all"
    val outputPath = "/Users/mariusz/Desktop/result.tiff"
    /* End of input parameters */

    val LOGGER = LoggerFactory.getLogger("MAIN")

    val photosPaths: List<Path> = ImageReader.photosPathsFromDirectory(Paths.get(directoryPath))
    LOGGER.info("Number of photos found in directory ${photosPaths.size}.")
    LOGGER.debug("List of files found: \n ${photosPaths.map { it.fileName }.joinToString("\n")}")

    val photos = photosPaths.map(::LazyImage)
    val photosSizes = photos.map { it.size }
    LOGGER.debug("Unique photos sizes found: [${photosSizes.toSet().joinToString(", ")}].")

    val canvas = CanvasFactory(Calculator).create(photosSizes, canvasSize)
    LOGGER.info("Canvas of size $canvasSize created split into ${canvas.columns.size} columns.")

    photos.forEachIndexed { photoIndex, photo ->
        LOGGER.debug("Adding photo $photoIndex of ${photosPaths.size}")
        canvas.addPhoto(photo)
    }

    try {
        LOGGER.info("Starting process of aligning bottom line...")
        canvas.alignBottomLine()
    } catch (e: Exception) {
        // nothing :)))
        // TODO change this
    }
    LOGGER.info("Creating collage image from canvas...")
    val collage: BufferedImage = canvas.draw()

    LOGGER.info("Saving image on hard drive...")
    ImageReader.save(collage, Paths.get(outputPath))

}

class CanvasFactory(private val calculator: Calculator) {

    val LOGGER = LoggerFactory.getLogger(CanvasFactory::class.java)

    fun create(photosSizes: List<Size>, canvasSize: Size): Canvas {
        val numberOfColumns = calculator.numberOfColumns(photosSizes, canvasSize)
        val columnWidth = Math.ceil(canvasSize.width.toDouble() / numberOfColumns).toInt()
        val columns = (0..(canvasSize.width-1) step columnWidth).map { Column(it, columnWidth) }
        LOGGER.debug("Created columns: " + columns)
        return Canvas(columns, canvasSize, CHOOSE_TWO_COLUMNS_PROBABILITY, 10)
    }
}

