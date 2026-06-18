package io.trtc.uikit.videochat.page.beauty

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback
import io.trtc.uikit.videochat.page.beauty.tebeauty.TEBeautyManager
import io.trtc.uikit.videochat.page.beauty.tebeauty.BeautyPanelDialog
import java.lang.ref.WeakReference

object BeautyIntegration {
    private var dialogWeakRef = WeakReference<BeautyPanelDialog?>(null)

    fun isSupportTEBeauty(): Boolean = TEBeautyManager.isSupportTEBeauty()

    fun setupVideoProcessor() {
        TEBeautyManager.setCustomVideoProcess()
    }

    @JvmStatic
    fun showBeautyDialog(context: Context) {
        if (TEBeautyManager.isSupportTEBeauty()) {
            TEBeautyManager.checkBeautyResource(context, object : TUIServiceCallback() {
                override fun onServiceCallback(code: Int, message: String?, bundle: Bundle?) {
                    if (code == 0) {
                        realShowBeautyDialog(context)
                    } else {
                        Log.e("BeautyIntegration", "check beauty resource failed:$code,message:$message")
                    }
                }
            })
        } else {
            Log.e("BeautyIntegration", "check isSupportTEBeauty is false")
        }
    }

    @JvmStatic
    fun resetBeauty() {
        TEBeautyManager.clearBeautyParams()
    }

    private fun realShowBeautyDialog(context: Context) {
        val dialog = BeautyPanelDialog(context)
        dialogWeakRef = WeakReference(dialog)
        dialog.setOnDismissListener {
            TEBeautyManager.exportParam()
        }
        dialog.show()
    }
}
