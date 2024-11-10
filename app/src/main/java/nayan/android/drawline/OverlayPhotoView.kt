package nayan.android.drawline

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import nayan.android.drawline.models.OverlayData
import co.nayan.c3_specialist.models.OverlayDataOperation
import com.github.chrisbanes.photoview.PhotoView
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt


class OverlayPhotoView(context: Context, attrs: AttributeSet) : PhotoView(context, attrs) {

    private var strokeWidth: Float = 10F
    private var lineDrawnListener: LineDrawnListener? = null
    var isLineInEditMode: Boolean = false
    var lineCurvePoint: List<Float> = listOf()

    // Default paint instance
    private val selectedPaint = Paint()
    private val endPointsPaint = Paint()

    private var bitmapWidth: Float? = null
    private var bitmapHeight: Float? = null

    // Data list for all drawings
    val overlayDataList: MutableList<OverlayData> = mutableListOf()

    // For maintaining user operations for UNDO feature
    val overlayDataOperations: MutableList<OverlayDataOperation> = mutableListOf()

    // Current active draw data
    private var stroke = OverlayData(mutableListOf(), DrawType.LINE, paintGenerator())

    // Last UNDO operation
    private var lastUndoOperation: OverlayDataOperation? = null

    private val onLineDrawListener = object : OnOverlayDrawListener {
        override fun discardStroke() {
        }

        override fun touchPoints(x: Float, y: Float) {
            var shouldAddPoint = true
            if (stroke.type == DrawType.LINE && stroke.points.size in listOf(2, 3)) {
                if (isLineInEditMode) {
                    val touchPoint = listOf(x, y)
                    val thresholdX = 50 / overlayViewAttacher.displayRect.width()
                    val thresholdY = 50 / overlayViewAttacher.displayRect.height()

                    if (calculateDistance(touchPoint, stroke.points[1]) < thresholdX ||
                        calculateDistance(touchPoint, stroke.points[1]) < thresholdY
                    ) {
                        stroke.points.removeAt(1)
                    } else if (calculateDistance(touchPoint, stroke.points[0]) < thresholdX ||
                        calculateDistance(touchPoint, stroke.points[0]) < thresholdY
                    ) {
                        stroke.points[0] = stroke.points[1]
                        stroke.points.removeAt(1)
                    } else {
                        lineCurvePoint = listOf(x, y)
                        shouldAddPoint = false
                    }
                } else {
                    // If Line tool is selected then just remove the second point and add the latest one later
                    stroke.points.removeAt(1)
                }
                invalidate()
            } else if (stroke.type == DrawType.SELECT) {
                // If Select tool is selected then just remove all points and add the latest one later
                stroke.points.clear()
            }

            if (shouldAddPoint) {
                // Adding points to the current active draw data
                stroke.points.add(listOf(x, y))
            }

            // If the Select tool is selected then do not invalidate the image
            if (stroke.type != DrawType.SELECT) {
                invalidate()
            }
        }

        override fun closeStroke() {
            if (stroke.type == DrawType.SELECT) {
                selectStroke()
                stroke = OverlayData(mutableListOf(), stroke.type, paintGenerator())
            } else {
                when (stroke.type) {
                    // Add current drawn line to data list only if the line is a valid Line
                    DrawType.LINE -> if (stroke.isValidLine()) {
                        isLineInEditMode = true
                        val x1 = stroke.points.first().first()
                        val x2 = stroke.points.last().first()
                        val y1 = stroke.points.first().last()
                        val y2 = stroke.points.last().last()
                        if (lineCurvePoint.isEmpty()) {
                            lineCurvePoint = listOf((x1 + x2) / 2, (y1 + y2) / 2)
                        }
                        lineDrawnListener?.onLineDrawn()
                    }
                    else -> {
                        if (stroke.points.isNotEmpty()) {
                            overlayDataList.add(stroke)
                            overlayDataOperations.add(
                                OverlayDataOperation(
                                    mutableListOf(stroke),
                                    false
                                )
                            )
                        }
                        // Clear all drawn points from current active draw data
                        stroke = OverlayData(mutableListOf(), stroke.type, paintGenerator())
                    }
                }
            }
        }
    }

    val overlayViewAttacher: OverlayViewAttacher =
        OverlayViewAttacher(this, onLineDrawListener)


    private fun selectStroke() {
        // If Select tool is selected and the point selected is out of view bounds then do not execute further
        if (stroke.points.isEmpty()) {
            return
        }
        var found = false

        // For Line drawing data
        val thresholdAreaX = 5 / overlayViewAttacher.displayRect.width()
        val thresholdAreaY = 5 / overlayViewAttacher.displayRect.height()

        // Check if the currently taped coordinates are contained under a threshold limit of any drawn points
        for (data in overlayDataList) {
            when (data.type) {
                DrawType.LINE -> {
                    for (index in 0 until data.points.lastIndex) {
                        // Touch coordinates
                        val touchX = stroke.points[0].first()
                        val touchY = stroke.points[0].last()
                        // Point 1 coordinates
                        val x1 = data.points[index].first()
                        val y1 = data.points[index].last()
                        // Point 2 coordinates
                        val x2 = data.points[index + 1].first()
                        val y2 = data.points[index + 1].last()

                        // Find Area of triangle formed by given three points
                        val area =
                            abs((touchX * (y1 - y2) + x1 * (y2 - touchY) + x2 * (touchY - y1)) / 2)

                        // If the angle at touch point is an obtuse angle and
                        // area of triangle formed is under the threshold limit,
                        // then the current line is found to be selected
                        if (calculateAngle(
                                stroke.points[0],
                                data.points[index],
                                data.points[index + 1]
                            ) > 90 &&
                            (area in -thresholdAreaX..thresholdAreaX ||
                                    area in -thresholdAreaY..thresholdAreaY)
                        ) {
                            data.isSelected = data.isSelected.not()
                            found = true
                        }
                    }
                }
            }
            // If a selected line is found then just break the loop
            if (found) {
                break
            }
        }
        // If found a drawing data invalidate the view
        if (found) {
            invalidate()
        }
    }

    init {
        selectedPaint.style = Paint.Style.STROKE
        selectedPaint.color = ContextCompat.getColor(context, R.color.selectedOverlay)
        selectedPaint.strokeWidth = 10f
        endPointsPaint.style = Paint.Style.STROKE
        endPointsPaint.color = ContextCompat.getColor(context, R.color.endPointsOverlay)
        endPointsPaint.strokeWidth = 10f
    }

    fun setStrokeWidth(stroke_width: Int) {
        strokeWidth = stroke_width.toFloat()
        for (stroke in overlayDataList) {
            stroke.paint.strokeWidth = strokeWidth
        }
        invalidate()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        bitmapWidth = bm?.width?.toFloat()
        bitmapHeight = bm?.height?.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (stroke.points.size > 0) {
            if (isLineInEditMode) {
                drawCircleAtLineEnds(stroke, canvas, endPointsPaint, lineCurvePoint)
            }
            if (stroke.type == DrawType.LINE && lineCurvePoint.isNotEmpty()) {
                val points = stroke.fixViewPoints(bitmapWidth, bitmapHeight).points
                val linePoints = mutableListOf(points.first(), lineCurvePoint, points.last())
                canvas.drawPath(
                    drawCurveLine(linePoints),
                    if (stroke.isSelected) selectedPaint else stroke.paint
                )
            } else {
                canvas.drawPath(
                    drawStroke(stroke.fixViewPoints(bitmapWidth, bitmapHeight).points),
                    if (stroke.isSelected) selectedPaint else stroke.paint
                )
            }

        }

        for (stroke in overlayDataList) {
            // Only draw if the data is not deleted
            if (!stroke.isDeleted) {
                if (stroke.type == DrawType.LINE) {
                    canvas.drawPath(
                        drawCurveLine(stroke.fixViewPoints(bitmapWidth, bitmapHeight).points),
                        if (stroke.isSelected) selectedPaint else stroke.paint
                    )
                } else {
                    canvas.drawPath(
                        drawStroke(stroke.fixViewPoints(bitmapWidth, bitmapHeight).points),
                        if (stroke.isSelected) selectedPaint else stroke.paint
                    )
                }
            }
        }
    }

    private fun paintGenerator(): Paint {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.color = getRandomHexCode()
        paint.strokeWidth = strokeWidth
        return paint
    }

    private fun getRandomHexCode(): Int {
        val random = Random()
        val randNum = random.nextInt(0xffffff + 1)
        return Color.parseColor(String.format("#%06x", randNum))
    }


    /*****
     * This method converts a points array into Path object (Stroke)
     * @param points : an array of all the points constituting a stroke
     * @return Path object
     */
    private fun drawStroke(points: MutableList<List<Float>>): Path {
        val stroke = Path()
        val rect = overlayViewAttacher.displayRect

        if (points.isEmpty() || rect == null) {
            return stroke
        }
        stroke.moveTo(
            rect.left + rect.width() * points[0].first(),
            rect.top + rect.height() * points[0].last()
        )
        for (i in 1 until points.size) {
            stroke.lineTo(
                rect.left + rect.width() * points[i].first(),
                rect.top + rect.height() * points[i].last()
            )
        }
        return stroke
    }

    private fun drawCurveLine(points: MutableList<List<Float>>): Path {
        val stroke = Path()
        val rect = overlayViewAttacher.displayRect

        if (points.isEmpty() || rect == null) {
            return stroke
        }
        stroke.moveTo(
            rect.left + rect.width() * points[0].first(),
            rect.top + rect.height() * points[0].last()
        )
        stroke.cubicTo(
            rect.left + rect.width() * points[0].first(),
            rect.top + rect.height() * points[0].last(),
            rect.left + rect.width() * points[1].first(),
            rect.top + rect.height() * points[1].last(),
            rect.left + rect.width() * points[2].first(),
            rect.top + rect.height() * points[2].last()
        )

        return stroke
    }

    private fun drawCircleAtLineEnds(
        overlayLineData: OverlayData,
        canvas: Canvas,
        paint: Paint,
        lineCurvePoint: List<Float>
    ) {
        val rect = overlayViewAttacher.displayRect

        if (overlayLineData.points.isEmpty() || rect == null || overlayLineData.isValidLine().not()) {
            return
        }
        canvas.drawCircle(
            rect.left + rect.width() * overlayLineData.points[0].first(),
            rect.top + rect.height() * overlayLineData.points[0].last(),
            10f,
            paint
        )
        canvas.drawCircle(
            rect.left + rect.width() * overlayLineData.points[1].first(),
            rect.top + rect.height() * overlayLineData.points[1].last(),
            10f,
            paint
        )
        canvas.drawCircle(
            rect.left + rect.width() * lineCurvePoint.first(),
            rect.top + rect.height() * lineCurvePoint.last(),
            10f,
            paint
        )
    }

    /****
     * Redraw a recent removed stroke
     */
    fun redo() {
        if (lastUndoOperation != null) {
            if (lastUndoOperation?.isDeleteOperation == true) {
                val ids = lastUndoOperation?.overlayDataList?.map { overlayData -> overlayData.id }
                if (ids != null) {
                    for (data in overlayDataList) {
                        if (data.id in ids) {
                            data.isDeleted = true
                        }
                    }
                }
            } else {
                lastUndoOperation?.overlayDataList?.first()?.let { overlayDataList.add(it) }
            }
            lastUndoOperation?.let { overlayDataOperations.add(it) }
            lastUndoOperation = null
            invalidate()
        }
    }

    /****
     * Clear a recent stroke from bitmap canvas
     */
    fun undo() {
        if (overlayDataOperations.size > 0) {
            val index = overlayDataOperations.lastIndex
            lastUndoOperation = overlayDataOperations[index]
            overlayDataOperations.removeAt(index)

            if (lastUndoOperation?.isDeleteOperation == true) {
                val ids = lastUndoOperation?.overlayDataList?.map { overlayData -> overlayData.id }
                if (ids != null) {
                    for (data in overlayDataList) {
                        if (data.id in ids) {
                            data.isDeleted = false
                        }
                    }
                }
            } else {
                val id = lastUndoOperation?.overlayDataList?.first()?.id
                overlayDataList.removeAt(overlayDataList.indexOfFirst { overlayData -> overlayData.id == id })
            }
            invalidate()
        }
    }

    fun reset() {
        overlayDataList.clear()
        lastUndoOperation = null
        overlayDataOperations.clear()
    }

    fun setStrokeMode(drawType: String) {
        overlayViewAttacher.setSelectModeEnabled(drawType == DrawType.SELECT)
        stroke = OverlayData(mutableListOf(), drawType, paintGenerator())
    }

    private fun lengthSquare(p1: List<Float>, p2: List<Float>): Float {
        val xDiff = p1.first() - p2.first()
        val yDiff = p1.last() - p2.last()
        return xDiff * xDiff + yDiff * yDiff
    }

    private fun calculateDistance(tapPoint: List<Float>, point: List<Float>): Float {
        val tx = tapPoint.first()
        val ty = tapPoint.last()
        val x = point.first()
        val y = point.last()

        val dx = tx - x
        val dy = ty - y
        //equation constants

        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateAngle(
        tapPoint: List<Float>,
        linePoint1: List<Float>,
        linePoint2: List<Float>
    ): Float {
        // Square of lengths be a2, b2, c2
        val a2 = lengthSquare(linePoint1, linePoint2)
        val b2 = lengthSquare(tapPoint, linePoint2)
        val c2 = lengthSquare(tapPoint, linePoint1)

        // length of sides be b, c
        val b = sqrt(b2)
        val c = sqrt(c2)

        // From Cosine law
        var alpha = acos((b2 + c2 - a2) / (2f * b * c))

        // Converting to degree
        alpha = (alpha * 180 / PI).toFloat()

        return alpha
    }

    fun deleteSelectedStrokes() {
        val deletedStrokes: MutableList<OverlayData> = mutableListOf()
        for (overlayData in overlayDataList) {
            if (overlayData.isSelected) {
                overlayData.isDeleted = true
                overlayData.isSelected = false
                deletedStrokes.add(overlayData)
            }
        }
        if (deletedStrokes.isNotEmpty()) {
            overlayDataOperations.add(OverlayDataOperation(deletedStrokes, true))
        }
        invalidate()
    }

    fun cancelSelection() {
        for (overlayData in overlayDataList) {
            overlayData.isSelected = false
        }
        invalidate()
    }

    fun closeCurrentLineStroke() {
        if (stroke.type == DrawType.LINE && lineCurvePoint.isNotEmpty()) {
            val points = stroke.fixViewPoints(bitmapWidth, bitmapHeight).points
            val linePoints = listOf(points.first(), lineCurvePoint, points.last())
            stroke.points.clear()
            stroke.points.addAll(linePoints)
            lineCurvePoint = listOf()
        }
        overlayDataList.add(stroke)
        overlayDataOperations.add(OverlayDataOperation(mutableListOf(stroke), false))
        // Clear all drawn points from current active draw data
        stroke = OverlayData(mutableListOf(), stroke.type, paintGenerator())
        isLineInEditMode = false
        invalidate()
    }

    fun clearCurrentLineStroke() {
        stroke = OverlayData(mutableListOf(), stroke.type, paintGenerator())
        lineCurvePoint = listOf()
        isLineInEditMode = false
        invalidate()
    }

    fun setEditModeEnabled(inEditMode: Boolean) {
        overlayViewAttacher.setEditModeEnabled(inEditMode)
    }

    fun setLineDrawnListener(lineDrawnListener: LineDrawnListener) {
        this.lineDrawnListener = lineDrawnListener
    }

    interface LineDrawnListener {
        fun onLineDrawn()
    }
}
