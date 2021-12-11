package pl.aetas.kollage

import pl.aetas.kollage.Canvas
import spock.lang.Ignore
import spock.lang.Specification

import java.awt.*

class CanvasTest extends Specification {

  def "should add photo to first found column when all columns are empty"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 0F, 0)
    def image = new MockImage(15, 20)
    when:
    canvas.addPhoto(image)
    then:
    canvas.columns[0].content.size == 1
  }

  def "should add photo to column with lowest sum of photos height when all photos do not have to be scaled"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 0F, 0)
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
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 0F, 0)
    def images = [new MockImage(10, 20),
                  new MockImage(10, 15),
                  new MockImage(20, 20),
                  new MockImage(10, 20)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    canvas.columns[2].content.size() == 2
  }

  @Ignore('There is 300px height added just for developing')
  def "should draw black buffered image of the size of canvas when no photo added"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 0F, 0)
    when:
    def bufferedImage = canvas.draw()
    then:
    bufferedImage.width == 30
    bufferedImage.height == 100
    bufferedImage.getRGB(0, 0) == Color.BLACK.getRGB()
  }

  def "should draw buffered image containing photo added to canvas when any photo has been added"() {
    given:
    Canvas canvas = new Canvas([new Column(0, 30)], new Size(30, 100), 0F, 0)
    canvas.addPhoto(new MockImage(30, 80))
    when:
    def bufferedImage = canvas.draw()
    then:
    bufferedImage.getRGB(0, 0) == Color.WHITE.getRGB()
    bufferedImage.getRGB(29, 79) == Color.WHITE.getRGB()
    bufferedImage.getRGB(29, 80) == Color.BLACK.getRGB()
  }

  /**
   * 1. image 1 is extended
   * 2. image 2 has similar height to extended image 1, but it is shorter
   * 3. column 3 become shortest column
   * 4. image 3 should be extended and pushed into column 2 and 3
   *  -----------------------
   * |               |       |
   * |       1       |   2   |
   * |               |=======|
   * |===============        |
   * |       |               |
   * |       |       3       |
   * |       |               |
   *  -----------------------
   *
   */
  def "should add placeholder to shortest column and photo to column on left when shortest column and one on left are of similar height"() {
    given:
    def similarHeightMaxDifferencePx = 10
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 1F, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    canvas.columns[0].content.size() == 1
    canvas.columns[1].content.size() == 2
    canvas.columns[1].content.last() instanceof ImageWrapper
    canvas.columns[2].content.size() == 2
    canvas.columns[2].content.last() instanceof PlaceholderWrapper
  }

  def "should align shortest column and column on left when adding extended photo to both columns"() {
    given:
    def similarHeightMaxDifferencePx = 10
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 1F, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    def wrapper1 = canvas.columns[1].contentWithPosition()*.component1().first()
    def wrapper2 = canvas.columns[2].contentWithPosition()*.component1().first()
    wrapper1.height() == 39
    wrapper1.height() == wrapper2.height()
    canvas.columns[1].height() == canvas.columns[2].height()
  }

  def "should extend photo so it's taking two columns when adding extended photo to both columns (left)"() {
    given:
    def similarHeightMaxDifferencePx = 10
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 1F, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    def image = canvas.columns[1].contentWithPosition()*.component1().last()
    image.width() == 20
    image.height() == 40
  }

  /**
   *  -----------------------
   * |               |       |
   * |       1       |   2   |
   * |               |=======|
   * |===============        |
   * |   4   |               |
   * |=======|       3       |
   * |        ===============|
   * |               |       |
   * |       5       |       |
   * |               |       |
   *  -----------------------
   */
  def "should align shortest column and column on right when adding extended photo to both columns"() {
    given:
    def similarHeightMaxDifferencePx = 10
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 1F, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    def wrapper3 = canvas.columns[2].contentWithPosition()*.component1()[1]
    def wrapper4 = canvas.columns[1].contentWithPosition()*.component1()[1]
    wrapper3.height() == 39
    wrapper3.height() == wrapper4.height()
    canvas.columns[0].height() == canvas.columns[1].height()
  }

  def "should add placeholder to shortest column and photo to shortest column when shortest column and one on right are of similar height"() {
    given:
    def similarHeightMaxDifferencePx = 10
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 1F, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    canvas.columns[0].content.size() == 3
    canvas.columns[1].content.size() == 3
    canvas.columns[0].content.last() instanceof ImageWrapper
    canvas.columns[2].content.size() == 2
    canvas.columns[1].content.last() instanceof PlaceholderWrapper
  }

  def "should extend photo so it's taking two columns when adding extended photo to both columns (right)"() {
    given:
    def similarHeightMaxDifferencePx = 10
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 100), 1F, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40),
                  new MockImage(20, 78),
                  new MockImage(20, 40)]
    when:
    images.forEach { canvas.addPhoto(it) }
    then:
    def image = canvas.columns[0].contentWithPosition()*.component1().last()
    image.width() == 20
    image.height() == 40
  }

  def "should align bottom line to height of canvas"() {
    given:
    def similarHeightMaxDifferencePx = 0
    def extendingProbabilityZero = 0F
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 19), extendingProbabilityZero, similarHeightMaxDifferencePx)
    def images = [new MockImage(10, 10), new MockImage(10, 10),  new MockImage(10, 10),
                  new MockImage(10, 15), new MockImage(10, 10), new MockImage(10, 12)]
    images.forEach { canvas.addPhoto(it) }
    when:
    canvas.alignBottomLine()
    then:
    canvas.columns*.height().every { it == 19 }
  }

  def "should crop all photos on column when aliging bottom on column without any extended images"() {
    given:
    def similarHeightMaxDifferencePx = 0
    def extendingProbabilityZero = 0F
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 20), extendingProbabilityZero, similarHeightMaxDifferencePx)
    def images = [new MockImage(10, 10), new MockImage(10, 10),  new MockImage(10, 10),
                  new MockImage(10, 16), new MockImage(10, 10), new MockImage(10, 12)]
    images.forEach { canvas.addPhoto(it) }
    when:
    canvas.alignBottomLine()
    then:
    canvas.columns[0].contentWithPosition()*.component1().first().height() == 7
    canvas.columns[0].contentWithPosition()*.component1().last().height() == 13
    canvas.columns[2].contentWithPosition()*.component1().first().height() == 9
    canvas.columns[2].contentWithPosition()*.component1().last().height() == 11
  }


  /**
   *  -----------------------
   * |               |       |
   * |       1       |   2   |
   * |               |=======|
   * |===============        |
   * |   4   |               |
   * |       |       3       |
   * |======= ===============|
   * |               |       |
   * |       5       |   6   |
   * |               |=======|
   *  -----------------------
   */
  def "should adjust photos height on aligning bottom when one of the image is extended"() {
    given:
    def similarHeightMaxDifferencePx = 1
    def extendingProbabilityOne = 1F
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 92), extendingProbabilityOne, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40), new MockImage(20, 78),
                  new MockImage(20, 39), new MockImage(20, 78),
                  new MockImage(20, 40), new MockImage(20, 40) ]
    images.forEach { canvas.addPhoto(it) }
    when:
    canvas.alignBottomLine()
    then:
    canvas.columns[1].content.collect { it.height() } == [27, 37, 28]
    canvas.columns[2].content.collect { it.height() } == [37, 37, 18]
  }

  def "should adjust photos height on aligning bottom when some photo (3) is shorter than space between photos above (1) and below (5) it"() {
    given:
    def similarHeightMaxDifferencePx = 1
    def extendingProbabilityOne = 1F
    Canvas canvas = new Canvas([new Column(0, 10), new Column(10, 10), new Column(20, 10)], new Size(30, 92), extendingProbabilityOne, similarHeightMaxDifferencePx)
    def images = [new MockImage(20, 40), new MockImage(20, 78),
                  new MockImage(20, 39), new MockImage(20, 78),
                  new MockImage(20, 40), new MockImage(20, 40) ]
    images.forEach { canvas.addPhoto(it) }
    when:
    canvas.alignBottomLine()
    then:
    canvas.columns[0].content.collect { it.height() } == [27, 37, 28]
  }
}




