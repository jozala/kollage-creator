package pl.aetas.kollage.creator

import org.imgscalr.Scalr
import java.awt.Color
import java.awt.image.BufferedImage

interface Image {
    val size: Size

    var image: BufferedImage

    fun resize(maxTargetWidth: Int, maxTargetHeight: Int) {
        val newImage = Scalr.resize(image, maxTargetWidth, maxTargetHeight)
        image.flush()
        image = newImage
    }
}

class MockImage(override val size: Size) : Image {

    override var image: BufferedImage = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);

    init {
        val graphics = image.createGraphics()
        graphics.paint = Color ( random255(), random255(), random255() );
        graphics.fillRect ( 0, 0, image.width, image.height);
    }

    private fun random255() = (Math.random()*255).toInt()
}

class PhotoImage(override var image: BufferedImage) : Image {

    override val size = Size(image.width, image.height)
}