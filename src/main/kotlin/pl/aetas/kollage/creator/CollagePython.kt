package pl.aetas.kollage.creator

class Photo(val filename: String, val width: Int, val height: Int, val orientation: Int = 0 ) {

    fun ratio() = height.toDouble() / width
}



open class Cell(val parents: List<PColumn>, val photo: Photo) {

    var extent: CellExtent? = null
    var h: Int = (w() * wanted_ratio()).toInt()

    /*
    def __repr__(self):
        end = "]"
        if self.extent is not None:
            end = "--"
        return "[%d %d%s" % (self.w, self.h, end)
    */

    fun __repr__(): String {
        var end = "]"
        if (extent != null) {
            end = "--"
        }

        return "[${w()} $h$end"
    }

//    @property def x(self): return self.parents[0].x
    val x: Int = parents[0].x

  /*
    @property
    def y(self):
        """Returns the cell's y coordinate
        It assumes that the cell is in a single column, so it is the previous
        cell's y + h.
        """
        prev = None
        for c in self.parents[0].cells:
            if self is c:
                if prev:
                    return prev.y + prev.h
                return 0
            prev = c
*/
    fun y(): Int {
        var prev: Cell? = null
        for (cell in parents[0].cells) {
            if (this === cell) {
                if (prev != null) {
                    return prev.y() + prev.h
                }
                return 0
            }
            prev = cell
        }
      throw IllegalStateException("Should not be here probably")
    }

//    @property def w(self): return sum(c.w for c in self.parents)
    fun w(): Int = parents.map { it.w() }.sum()

//    @property def ratio(self): return self.h / self.w
    fun ratio(): Double = h.toDouble() / w()

//    @property def wanted_ratio(self): return self.photo.ratio
    fun wanted_ratio(): Double = photo.ratio()
/*
    def scale(self, alpha):
        self.h *= alpha
*/
    fun scale(alpha: Double) {
        h = (h * alpha).toInt()
    }

  /*  def is_extended(self):
        return hasattr(self, 'extent') and self.extent is not None
*/
    fun isExtended(): Boolean = extent != null

/*
    def is_extension(self):
        return isinstance(self, CellExtent)
*/
    open fun isExtension() = false

    /*def content_coords(self):
        """Returns the coordinates of the contained image
        These are computed in order not to loose space, so the content area
        will always be greater than the cell itself. It is the space taken by
        the contained image if it wasn't cropped.
        """
        # If the contained image is too thick to fit
        if self.wanted_ratio < self.ratio:
            h = self.h
            w = self.h / self.wanted_ratio
            y = self.y
            x = self.x - (w - self.w) / 2.0
        # If the contained image is too tall to fit
        elif self.wanted_ratio > self.ratio:
            w = self.w
            h = self.w * self.wanted_ratio
            x = self.x
            y = self.y - (h - self.h) / 2.0
        else:
            w = self.w
            h = self.h
            x = self.x
            y = self.y
        return x, y, w, h*/

    fun contentCoords(): Coords {

        // If the contained image is too thick to fit
        if (wanted_ratio() < ratio()) {
            var _w = h.toDouble() / wanted_ratio()
            var _x = x - (_w - w()) / 2.0
            return Coords(_x.toInt(), y(), _w.toInt(), h)
        } else if (wanted_ratio() > ratio()) {
            var _h = w() * wanted_ratio()
            var _y = y() - (_h - h) / 2.0
            return Coords(x, _y.toInt(), w(), _h.toInt())
        }
        return Coords(x, y(), w(), h)

    }
/*
    def top_neighbor(self):
        """Returns the cell above this one"""
        prev = None
        for c in self.parents[0].cells:
            if self is c:
                return prev
            prev = c

    def bottom_neighbor(self):
        """Returns the cell below this one"""
        prev = None
        for c in reversed(self.parents[0].cells):
            if self is c:
                return prev
            prev = c*/

}
class CellExtent(parents: List<PColumn>, photo: Photo): Cell(parents, photo) {
    override fun isExtension() = true
}

class PColumn(val cells: List<Cell>) {
    fun w(): Int = 0

    /*
     @property
    def x(self):
        x = 0
        for c in self.parent.cols:
            if self is c:
                break
            x += c.w
        return x*/

    val x = 0
}

data class Coords(val x: Int, val y: Int, val w: Int, val h: Int)