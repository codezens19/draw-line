package nayan.android.drawline

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.github.chrisbanes.photoview.PhotoViewAttacher

class OverlayViewAttacher(imageView: ImageView, listener: OnOverlayDrawListener) :
    PhotoViewAttacher(imageView) {

    private var multiTouch = false

    private var isEditModeEnabled: Boolean = false
    private var isSelectModeEnabled: Boolean = false

    private val onPencilDrawListener = listener

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, ev: MotionEvent): Boolean {

        if (isEditModeEnabled) {
            when (ev.pointerCount) {
                1 -> {
                    when (ev.action) {

                        MotionEvent.ACTION_UP -> {
                            if (multiTouch) {
                                onPencilDrawListener.discardStroke()
                                multiTouch = false
                            } else {
                                onPencilDrawListener.closeStroke()
                            }
                        }

                        MotionEvent.ACTION_DOWN -> {
                            if (!multiTouch && isSelectModeEnabled) {
                                sendEventToListener(ev)
                            } else {
                                super.onTouch(v, ev)
                            }
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (!multiTouch) {
                                sendEventToListener(ev)
                            } else {
                                super.onTouch(v, ev)
                            }
                        }
                    }
                }
            }
        } else {
            super.onTouch(v, ev)
        }
        return true
    }

    private fun sendEventToListener(ev: MotionEvent) {
        val displayRect = displayRect
        if (displayRect != null) {
            val x = ev.x
            val y = ev.y
            if (displayRect.contains(x, y)) {
                val xResult = (x - displayRect.left) / displayRect.width()
                val yResult = (y - displayRect.top) / displayRect.height()
                onPencilDrawListener.touchPoints(xResult, yResult)
            }
        }
    }

    fun setEditModeEnabled(isEnabled: Boolean) {
        isEditModeEnabled = isEnabled
    }

    fun setSelectModeEnabled(isEnabled: Boolean) {
        isSelectModeEnabled = isEnabled
    }
}
