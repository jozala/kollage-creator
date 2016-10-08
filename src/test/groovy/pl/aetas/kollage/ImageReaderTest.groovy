package pl.aetas.kollage

import spock.lang.Specification
import spock.lang.Subject

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths

class ImageReaderTest extends Specification {

  @Subject
  ImageReader imageReader = new ImageReader()

  static String tempDirName = 'temp_' + UUID.randomUUID().toString().substring(0, 5)


  void setup() {
    new File(tempDirName).mkdir()
  }

  void cleanup() {
    new File(tempDirName).deleteDir()
  }

  def "should read width and height from image"() {
    given:
    createImageFile(Paths.get("$tempDirName/testImage1.tiff"), 300, 200)
    when:
    def size = imageReader.size(Paths.get("$tempDirName/testImage1.tiff"))
    then:
    size == new Size(300, 200)
  }

  def "should get list of paths with names of files from directory"() {
    given:
    def paths = (1..3).collect { Paths.get("$tempDirName/testImage_${it}.tiff")}
    paths.each { createImageFile(it, 1, 1) }
    when:
    def filesFromDir = imageReader.photosPathsFromDirectory(Paths.get(tempDirName))
    then:
    filesFromDir.toSet() == paths.toSet()
  }

  def "should filter out files which are not JPG or TIFF when returning list of files"() {
    given:
    def tiffPaths = (1..3).collect { Paths.get("$tempDirName/testFile_${it}.tiff")}
    def jpgPaths = (4..6).collect { Paths.get("$tempDirName/testFile_${it}.jpg")}
    def txtPaths = (7..9).collect { Paths.get("$tempDirName/testFile_${it}.txt")}
    (tiffPaths + jpgPaths + txtPaths).each { createImageFile(it, 1, 1) }
    when:
    def filesFromDir = imageReader.photosPathsFromDirectory(Paths.get(tempDirName))
    then:
    filesFromDir.toSet() == (tiffPaths + jpgPaths).toSet()
  }

  def "should save ImageBuffer to TIFF file on hard drive"() {
    given:
    BufferedImage image = new BufferedImage(10, 12, BufferedImage.TYPE_INT_RGB)
    when:
    imageReader.save(image, Paths.get(tempDirName, 'output1.tiff'))
    then:
    imageReader.size(Paths.get(tempDirName, 'output1.tiff')) == new Size(10, 12)
  }

  private def createImageFile(Path path, int width, int height) {
    def image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    ImageIO.write(image, 'tiff', path.toFile())
  }
}
