package pl.aetas.kollage

import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths

class LazyImageTest extends Specification {

  static String tempDirName = 'temp_' + UUID.randomUUID().toString().substring(0, 5)

  void setup() {
    new File(tempDirName).mkdir()
  }

  void cleanup() {
    new File(tempDirName).deleteDir()
  }

  def "should read image size"() {
    given:
    createImageFile(Paths.get("$tempDirName/testImage1.tiff"), 300, 200)
    def image = new LazyImage(Paths.get("$tempDirName/testImage1.tiff"))
    when:
    def size = image.size
    then:
    size == new Size(300, 200)
  }

  def "should read image size only once and then reuse value"() {
    given:
    createImageFile(Paths.get("$tempDirName/testImage1.tiff"), 55, 123)
    def image = new LazyImage(Paths.get("$tempDirName/testImage1.tiff"))
    when:
    image.size
    new File(tempDirName).deleteDir()
    def size = image.size
    then:
    size == new Size(55, 123)
  }

  private def createImageFile(Path path, int width, int height) {
    def image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    ImageIO.write(image, 'tiff', path.toFile())
  }
}
