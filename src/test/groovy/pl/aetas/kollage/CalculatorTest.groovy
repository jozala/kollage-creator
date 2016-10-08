package pl.aetas.kollage

import spock.lang.Specification

class CalculatorTest extends Specification {

  def "should calculate more than 10 columns when canvas 100x100 and 150 photos 10x10"() {
    when:
    def photosSizes = (1..150).collect { new Size(10, 10)}
    def numberOfColumns = new Calculator().numberOfColumns(photosSizes, new Size(100, 100))
    then:
    numberOfColumns > 10
  }

  def "should calculate less than 10 columns when canvas 100x100 and 50 photos 10x10"() {
    when:
    def photosSizes = (1..50).collect { new Size(100, 100)}
    def numberOfColumns = new Calculator().numberOfColumns(photosSizes, new Size(100, 100))
    then:
    numberOfColumns < 10
  }

  def "should throw exception when maximum area of photos is less than canvas area"() {
    given:
    def photosSizes = (1..99).collect { new Size(10, 10)}
    when:
    new Calculator().numberOfColumns(photosSizes, new Size(100, 100))
    then:
    thrown(IllegalArgumentException)
  }
}
