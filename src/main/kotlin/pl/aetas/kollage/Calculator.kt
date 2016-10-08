package pl.aetas.kollage

object Calculator {

    fun numberOfColumns(photosSizes: List<Size>, canvasSize: Size): Int {
        val canvasArea = canvasSize.height * canvasSize.width
        val allFullImagesArea: Long = photosSizes.map { it.width.toLong() * it.height }.sum()

        if (canvasArea > allFullImagesArea) {
            throw IllegalArgumentException("Photos are too small to create canvas ($canvasArea > $allFullImagesArea)")
        }

        val averageImageArea = canvasArea.toDouble() / photosSizes.size
        // stupid way to have almost right number of columns -
        // we start with it but it will later retry with lower number if it is too high
        val averageImageAreaAfterExtendingSome = averageImageArea * (1 - CHOOSE_TWO_COLUMNS_PROBABILITY)
        val averageExpectedImageWidth = Math.sqrt(3.0 / 2 * averageImageAreaAfterExtendingSome)
        val columnsNumber = Math.ceil(canvasSize.width / averageExpectedImageWidth).toInt()

        return columnsNumber
    }
}