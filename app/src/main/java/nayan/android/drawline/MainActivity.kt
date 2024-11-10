package nayan.android.drawline

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nayan.android.drawline.utils.gone
import nayan.android.drawline.utils.visible
import timber.log.Timber
import java.net.SocketException
import java.util.concurrent.ExecutionException

class MainActivity : AppCompatActivity() {

    private lateinit var originalBitmap: Bitmap
    private var inEditMode = false
    private val lineDrawListener = object : OverlayPhotoView.LineDrawnListener {
        override fun onLineDrawn() {
            lineControlLL.visible()
            editControlLL.gone()
            editModeActions.gone()
            correctBtn.gone()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        populateImage()

        strokeWidthSlider.setOnSeekBarChangeListener(seekChangeListener)
        overlayPhotoView.setLineDrawnListener(lineDrawListener)

        redoBtn.setOnClickListener { overlayPhotoView.redo() }
        undoBtn.setOnClickListener { overlayPhotoView.undo() }

        selectButton.setOnClickListener {
            selectCurrentButton(it)
            overlayPhotoView.setStrokeMode(DrawType.SELECT)
            editModeActions.gone()
            correctBtn.gone()
            selectModeActions.visible()
        }

        lineButton.setOnClickListener {
            selectCurrentButton(it)
            overlayPhotoView.setStrokeMode(DrawType.LINE)
        }


        deleteButton.setOnClickListener {
            overlayPhotoView.deleteSelectedStrokes()
        }

        cancelSelectModeButton.setOnClickListener {
            editModeActions.visible()
            selectModeActions.gone()
            correctBtn.visible()
            selectCurrentButton(null)
            overlayPhotoView.cancelSelection()
        }

        finalButton.setOnClickListener {
            overlayPhotoView.closeCurrentLineStroke()
            editModeActions.visible()
            lineControlLL.gone()
            editControlLL.visible()
            correctBtn.visible()
        }

        cancelLineButton.setOnClickListener {
            overlayPhotoView.clearCurrentLineStroke()
            editModeActions.visible()
            lineControlLL.gone()
            editControlLL.visible()
            correctBtn.visible()
        }

        correctBtn.setOnClickListener {
            saveJudgment()
            overlayPhotoView.reset()
        }

    }

    private fun selectCurrentButton(view: View?) {
        val isSelected = view?.isSelected

        selectButton.isSelected = false
        lineButton.isSelected = false

        inEditMode = (isSelected ?: true).not()

        if (view?.id != R.id.selectButton) {
            view?.isSelected = inEditMode
        }

        toggleEditMode(inEditMode)
    }

    private fun toggleEditMode(inEditMode: Boolean) {
        if (inEditMode) {
            editControlLL.visible()
            strokeWidthSlider.visible()
        } else {
            editControlLL.gone()
            strokeWidthSlider.gone()
        }
        overlayPhotoView.setEditModeEnabled(inEditMode)
    }

    private fun saveJudgment() {

    }

    private fun populateImage() {
        lifecycleScope.launch {
            try {
                originalBitmap = getOriginalBitmapFromUrl()
                overlayPhotoView.setImageBitmap(originalBitmap)
                overlayPhotoView.overlayViewAttacher.update()
            } catch (e: SocketException) {
                Timber.d(e)
            } catch (e: ExecutionException) {
                Timber.d(e)
            }
        }
    }

    private suspend fun getOriginalBitmapFromUrl(): Bitmap =
        withContext(Dispatchers.IO) {
            Glide.with(this@MainActivity)
                .asBitmap()
                .load(Constants.IMAGE_URL)
                .submit().get()
        }


    private val seekChangeListener = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(p0: SeekBar?, width: Int, p2: Boolean) {
            overlayPhotoView.setStrokeWidth(width)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}

        override fun onStopTrackingTouch(p0: SeekBar?) {}
    }
}
