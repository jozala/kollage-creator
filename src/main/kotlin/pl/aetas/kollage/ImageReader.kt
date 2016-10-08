package pl.aetas.kollage

import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import javax.imageio.ImageIO

object ImageReader {

    val ACCEPTED_FILES_MATCHER: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.{jpg,tiff}")


    fun size(path: Path): Size {
        val imageStream = ImageIO.createImageInputStream(path.toFile())

        val readers = ImageIO.getImageReaders(imageStream)

        if (readers.hasNext()) {
            val reader = readers.next()
            try {
                reader.input = imageStream
                return Size(reader.getWidth(0), reader.getHeight(0))
            } finally {
                reader.dispose()
            }
        }
        throw IllegalStateException("Cannot read size from image on given path: $path")
    }

    fun photosPathsFromDirectory(dirPath: Path): List<Path> {
        return Files.newDirectoryStream(dirPath)
                .filter { ACCEPTED_FILES_MATCHER.matches(it) }
                .toList()
    }

    fun save(image: BufferedImage, path: Path) {
        ImageIO.write(image, "TIFF", path.toFile())
    }
}