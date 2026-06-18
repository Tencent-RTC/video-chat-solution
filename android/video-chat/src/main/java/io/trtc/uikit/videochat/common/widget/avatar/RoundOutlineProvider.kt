package io.trtc.uikit.videochat.common.widget.avatar

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

/**
 * 圆角裁切 OutlineProvider — 让 View 按指定圆角半径裁切内容。
 */
class RoundOutlineProvider(private val radiusPx: Float) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
    }
}
