package pl.aetas.kollage

import spock.lang.Specification

class ImageWrapperTest extends Specification {

  def "should change image width and height when resizing to size with less width than before but the same height"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.resize(50, 100)
    then:
    wrapper.size() == new Size(50, 50)
  }

  def "should change image size to given height and scaled width when target height is changed more than width"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.resize(90, 80)
    then:
    wrapper.size() == new Size(80, 80)
  }

  def "should change image size to given width and scaled height when target width is changed more than height"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.resize(80, 90)
    then:
    wrapper.size() == new Size(80, 80)
  }

  def "should throw exception when trying to downsize image and give width higher than original"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.resize(101, 99)
    wrapper.size()
    then:
    thrown(IllegalArgumentException)
  }

  def "should throw exception when trying to downsize image and give height higher than original"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.resize(90, 101)
    wrapper.size()
    then:
    thrown(IllegalArgumentException)
  }

  def "should ceil height value when resizing and result is not an integer"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 101))
    when:
    wrapper.resize(50, 101)
    then:
    wrapper.size() == new Size(50, 51)
  }

  def "should ceil width value when resizing and result is not an integer"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(101, 100))
    when:
    wrapper.resize(101, 50)
    then:
    wrapper.size() == new Size(51, 50)
  }

  def "should apply operations in order from first added to lately added"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.resize(80, 90)
    wrapper.resize(80, 80)
    wrapper.resize(70, 80)
    then:
    wrapper.size() == new Size(70, 70)
  }

  def "should apply all downsize operation before returning buffered image when operations has been added"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    wrapper.resize(90, 90)
    wrapper.resize(80, 90)
    wrapper.resize(80, 70)
    when:
    def bufferedImage = wrapper.bufferedImage()
    then:
    bufferedImage.width == 70
    bufferedImage.height == 70
  }

  def "should change image height only when cropping image"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.crop(22)
    then:
    wrapper.size() == new Size(100, 78)
  }

  def "should throw exception when cropping image with pixelsToCrop > height"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    when:
    wrapper.crop(101)
    then:
    thrown(AssertionError)
  }

  def "should apply all crop operation before returning buffered image when operations has been added"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    wrapper.crop(10)
    wrapper.crop(12)
    wrapper.crop(28)
    when:
    def bufferedImage = wrapper.bufferedImage()
    then:
    bufferedImage.width == 100
    bufferedImage.height == 100 - 10 - 12 - 28
  }

  def "should apply all operations (crop + downsize) before returning buffered image when operations has been added"() {
    given:
    ImageWrapper wrapper = new ImageWrapper(new MockImage(100, 100))
    wrapper.crop(10)
    wrapper.resize(100, 80)
    wrapper.crop(10)
    wrapper.resize(85, 60)

    when:
    def bufferedImage = wrapper.bufferedImage()
    then:
    bufferedImage.width == Math.ceil(100 * (8d/9) * (6/7)).intValue()
    bufferedImage.height == 60
  }


}
