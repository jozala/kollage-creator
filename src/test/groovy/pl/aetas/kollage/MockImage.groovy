package pl.aetas.kollage

import java.awt.Color
import java.awt.image.BufferedImage

class MockImage implements Image {

  final Size size;
  final Color color

  MockImage(int width, int height, Color color = Color.WHITE) {
    this.color = color
    this.size = new Size(width, height);
  }

  int height() {
    return size.height
  }

  int width() {
    return size.width
  }

  @Override
  BufferedImage bufferedImage() {
    def image = new BufferedImage(width(), height(), BufferedImage.TYPE_INT_RGB)
    def graphics = image.createGraphics()
    graphics.paint = color;
    graphics.fillRect(0, 0, width(), height());
    return image
  }
}
