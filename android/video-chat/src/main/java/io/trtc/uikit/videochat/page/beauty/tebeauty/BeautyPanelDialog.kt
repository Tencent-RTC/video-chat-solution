package io.trtc.uikit.videochat.page.beauty.tebeauty

import android.content.Context
import io.trtc.uikit.videochat.common.widget.panel.VideoChatBottomPanel

class BeautyPanelDialog(context: Context) : VideoChatBottomPanel(context) {

    init {
        setContent(TEBeautyView(context))
    }
}
