package nayan.android.drawline

interface OnOverlayDrawListener {
    fun touchPoints(x: Float, y: Float)
    fun closeStroke()
    fun discardStroke()
}
