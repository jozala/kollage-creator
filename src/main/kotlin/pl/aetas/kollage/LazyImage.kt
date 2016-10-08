package pl.aetas.kollage

import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO

interface Image {
    val size: Size
    fun height() = size.height
    fun width() = size.width
    fun bufferedImage(): BufferedImage
}

class LazyImage(val path: Path): Image{


    override val size: Size by lazy { ImageReader.size(path) }
    override fun bufferedImage(): BufferedImage {
        return ImageIO.read(path.toFile())
    }

}