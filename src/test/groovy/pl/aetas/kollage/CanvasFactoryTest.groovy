package pl.aetas.kollage

import spock.lang.Specification
import spock.lang.Subject

class CanvasFactoryTest extends Specification {

  Calculator calculator = new Calculator()

  @Subject
  CanvasFactory canvasFactory = new CanvasFactory(calculator)

  def "should equally position columns on the canvas"() {
    given:
    def sizes = (0..100).collect { new Size(10, 10) }
    when:
    def canvas = canvasFactory.create(sizes, new Size(100, 100))
    then:
    canvas.columns.collect { it.x } == (0..90).step(10)
  }

  def "should use ceiling of column width if calculated column width is not an integer"() {
    given:
    def sizes = (0..365).collect { new Size(1080, 1920) }
    when:
    def canvas = canvasFactory.create(sizes, new Size(1080, 1920))
    then:
    canvas.columns.every { it.width == 78}
  }
}
