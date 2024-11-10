package nayan.android.drawline.models

import android.graphics.Paint
import nayan.android.drawline.DrawType
import java.util.*

data class OverlayData(
    val points: MutableList<List<Float>>,
    val type: String = DrawType.LINE,
    var paint: Paint = Paint(),
    var shouldFixViewPoints: Boolean = false,
    var isSelected: Boolean = false,
    var isDeleted: Boolean = false,
    var isAIGenerated: Boolean = false
) {

    val id = UUID.randomUUID()!!


    fun fixViewPoints(bitmapWidth: Float?, bitmapHeight: Float?): OverlayData {
        if (!shouldFixViewPoints || bitmapWidth == null || bitmapHeight == null) {
            return this
        }
        points.sortBy { pair -> pair.last() }
        for (index in 0 until points.size) {
            val xResult = points[index].first() / bitmapWidth
            val yResult = points[index].last() / bitmapHeight
            points.removeAt(index)
            points.add(index, listOf(xResult, yResult))
        }
        shouldFixViewPoints = false
        return this
    }

    fun isValidLine(): Boolean {
        return points.size >= 2
    }
}