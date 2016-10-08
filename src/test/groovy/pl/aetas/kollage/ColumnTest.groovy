package pl.aetas.kollage

import spock.lang.Specification

class ColumnTest extends Specification {

  def "should return all images with position when there are multiple images in column"() {
    given:
    def column = new Column(100, 50)
    column.fit(new ImageWrapper(new MockImage(50, 30)))
    column.fit(new ImageWrapper(new MockImage(50, 70)))
    column.fit(new ImageWrapper(new MockImage(50, 20)))
    column.fit(new ImageWrapper(new MockImage(50, 10)))
    when:
    def content = column.contentWithPosition()
    then:
    content[0].first.height() == 30
    content[0].second == new Position(100, 0)
    content[1].first.height() == 70
    content[1].second == new Position(100, 30)
    content[2].first.height() == 20
    content[2].second == new Position(100, 100)
    content[3].first.height() == 10
    content[3].second == new Position(100, 120)

  }
}
