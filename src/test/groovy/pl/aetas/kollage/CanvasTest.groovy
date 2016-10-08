package pl.aetas.kollage

import spock.lang.Specification

import java.awt.Color

class CanvasTest extends Specification {

  def "should add photo to first found column when all columns are empty"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100))
    def image = new MockImage(15, 20)
    when:
    canvas.addPhoto(image)
    then:
    canvas.columns[0].content.size == 1
  }

  def "should add photo to column with lowest sum of photos height when all photos do not have to be scaled"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100))
    def images = [new MockImage(10, 20),
                  new MockImage(10, 15),
                  new MockImage(10, 20),
                  new MockImage(10, 20)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    canvas.columns[1].content.size() == 2
  }

  def "should add photo to column with lowest sum of photos height scaled when some photos have to be scaled because they are too wide"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100))
    def images = [new MockImage(10, 20),
                  new MockImage(10, 15),
                  new MockImage(20, 20),
                  new MockImage(10, 20)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    canvas.columns[2].content.size() == 2
  }

  def "should draw black buffered image of the size of canvas when no photo added"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100))
    when:
    def bufferedImage = canvas.draw()
    then:
    bufferedImage.width == 30
    bufferedImage.height == 100
    bufferedImage.getRGB(0, 0) == Color.BLACK.getRGB()
  }

  def "should draw buffered image containing photo added to canvas when any photo has been added"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 30)], new Size(30, 100))
    canvas.addPhoto(new MockImage(30, 80))
    when:
    def bufferedImage = canvas.draw()
    then:
    bufferedImage.getRGB(0, 0) == Color.WHITE.getRGB()
    bufferedImage.getRGB(29, 79) == Color.WHITE.getRGB()
    bufferedImage.getRGB(29, 80) == Color.BLACK.getRGB()
  }
}




