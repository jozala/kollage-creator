package pl.aetas.kollage

import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths


val CHOOSE_TWO_COLUMNS_PROBABILITY = 0.2;


class Canvas(val columns: List<Column>, val size: Size) {

    fun addPhoto(image: Image) {

        shortestColumn().fit(ImageWrapper(image))

        // extend to neighbouring column if height is similar
    }

    private fun shortestColumn() = columns.minBy { it.height() }!!

    fun alignBottomLine() {
        throw UnsupportedOperationException("not implemented")
    }

    fun draw(): BufferedImage {
        val collageImage = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB) // TODO - can add height (+300) to see bottom line clearly
        val graphics = collageImage.createGraphics()
        columns.flatMap { it.contentWithPosition() }
                .filterIsInstance<Pair<ImageWrapper, Position>>()
                .forEach { pair ->
                    val (image, position) = pair
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

}

interface Wrapper {

    fun size(): Size
    fun width(): Int = size().width
    fun height(): Int = size().height
}

class PlaceholderWrapper: Wrapper {
    override fun size(): Size {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
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
        // TODO - add cropping here when it will be implemented as ImageOperation
        val simplifiedDownsizeOperation = DownsizeOperation(width, height)
        return simplifiedDownsizeOperation.apply(image.bufferedImage())
    }

    fun resize(targetWidth: Int, targetHeight: Int) {
        operations.add(DownsizeOperation(targetWidth, targetHeight))
    }

    interface ImageOperation {
        fun simulate(currentSize: Size): Size
        fun apply(bufferedImage: BufferedImage): BufferedImage
    }

    class DownsizeOperation(val targetMaxWidth: Int, val targetMaxHeight: Int): ImageOperation {

        override fun apply(bufferedImage: BufferedImage): BufferedImage {
            val (width, height) = simulate(Size(bufferedImage.width, bufferedImage.height))
            return Scalr.resize(bufferedImage, width, height)
        }

        override fun simulate(currentSize: Size): Size {

            if (currentSize.width < targetMaxWidth) throw IllegalArgumentException("Cannot downsize image to width higher than original")
            if (currentSize.height < targetMaxHeight) throw IllegalArgumentException("Cannot downsize image to height higher than original")

            val widthRatio = targetMaxWidth.toDouble() / currentSize.width
            val heightRatio = targetMaxHeight.toDouble() / currentSize.height

            if (widthRatio < heightRatio) {
                return Size(targetMaxWidth, Math.ceil(currentSize.height * widthRatio).toInt())
            }

            return Size(Math.ceil(currentSize.width * heightRatio).toInt(), targetMaxHeight)

        }

    }
}

data class Size(val width: Int, val height: Int) {
    fun ratio() = width.toDouble() / height
}

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
        return Canvas(columns, canvasSize)
    }
}

