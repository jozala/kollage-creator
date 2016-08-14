package pl.aetas.kollage.creator

import org.imgscalr.Scalr
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

interface Image {

    var size: Size

    var image: BufferedImage?

    fun resize(maxTargetWidth: Int, maxTargetHeight: Int) {
        val newSize = size.resize(maxTargetWidth, maxTargetHeight)
        val newImage = Scalr.resize(image, newSize.width, newSize.height)
        image!!.flush()
        size = newSize
        image = newImage
    }

    fun load() {}
    fun unload() {}

    fun crop(targetWidth: Int, targetHeight: Int) {
        val newImage = Scalr.crop(image, targetWidth, targetHeight)
        image!!.flush()
        size = Size(targetWidth, targetHeight)
        image = newImage
    }
}

class MockImage(override var size: Size, color: Color?, text: String) : Image {

    override var image: BufferedImage? = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);

    init {
        val graphics = image!!.createGraphics()
        graphics.paint = color ?: Color.WHITE//Color ( random255(), random255(), random255() );
        graphics.fillRect ( 0, 0, image!!.width, image!!.height);

        graphics.paint = Color.BLACK
        graphics.drawRect(0, 0, image!!.width, image!!.height)

        graphics.drawString(text, 20, 20)
    }

    private fun random255() = (Math.random()*255).toInt()
}

class PhotoImage(var imagePath: Path) : Image {

    override var image: BufferedImage? = null
    override var size = Size(0,0)

    init {
        load()
        size = Size(image!!.width, image!!.height)
        unload()
    }

    override fun load() {
        image = ImageIO.read(imagePath.toFile())
    }

    override fun unload() {
        image?.flush()
        image = null
    }

    private fun save() {
        val newPath = Paths.get("/Users/mariusz/tmp/temp_photos/${imagePath.fileName}")
        ImageIO.write(image, "JPEG", newPath.toFile())
        imagePath = newPath
    }

    override fun resize(maxTargetWidth: Int, maxTargetHeight: Int) {
        load()
        super.resize(maxTargetWidth, maxTargetHeight)
        save()
        unload()
    }

    override fun crop(targetWidth: Int, targetHeight: Int) {
        load()
        super.crop(targetWidth, targetHeight)
        save()
        unload()
    }


}