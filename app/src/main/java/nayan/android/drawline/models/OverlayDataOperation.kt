package co.nayan.c3_specialist.models

import nayan.android.drawline.models.OverlayData

data class OverlayDataOperation(
    val overlayDataList: MutableList<OverlayData>, val isDeleteOperation: Boolean) {
}