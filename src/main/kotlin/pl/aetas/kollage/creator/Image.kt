package pl.aetas.kollage.creator

import org.imgscalr.Scalr
import java.awt.Color
import java.awt.image.BufferedImage

interface Image {

    var size: Size

    var image: BufferedImage

    fun resize(maxTargetWidth: Int, maxTargetHeight: Int) {
        val newSize = size.resize(maxTargetWidth, maxTargetHeight)
        val newImage = Scalr.resize(image, newSize.width, newSize.height)
        image.flush()
        size = newSize
        image = newImage
    }

    fun crop(targetWidth: Int, targetHeight: Int) {
        val newImage = Scalr.crop(image, targetWidth, targetHeight)
        image.flush()
        size = Size(targetWidth, targetHeight)
        image = newImage
    }
}

class MockImage(override var size: Size, color: Color?, text: String) : Image {

    override var image: BufferedImage = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);

    init {
        val graphics = image.createGraphics()
        graphics.paint = color ?: Color.WHITE//Color ( random255(), random255(), random255() );
        graphics.fillRect ( 0, 0, image.width, image.height);

        graphics.paint = Color.BLACK
        graphics.drawRect(0, 0, image.width, image.height)

        graphics.drawString(text, 20, 20)
    }

    private fun random255() = (Math.random()*255).toInt()
}

class PhotoImage(override var image: BufferedImage) : Image {

    override var size = Size(image.width, image.height)
}