package io.trtc.uikit.videochat.page.call.controls

import android.content.Context
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.tencent.liteav.base.Log
import io.trtc.tuikit.atomicxcore.api.call.CallStore
import io.trtc.tuikit.atomicxcore.api.call.CallParticipantStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SingleCallControlsView(context: Context) : ConstraintLayout(context) {
    private var scope: CoroutineScope? = null
    private var functionLayout: RelativeLayout? = null
    private var callStatus = CallParticipantStatus.None

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateLayout()
        registerSelfObserver()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
    }

    private fun registerSelfObserver() {
        val newScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope = newScope
        newScope.launch {
            CallStore.shared.observerState.selfInfo.collect { selfInfo ->
                if (callStatus != selfInfo.status && selfInfo.status == CallParticipantStatus.Accept) {
                    callStatus = selfInfo.status
                    updateLayout()
                }
            }
        }
    }

    private fun updateLayout() {
        val selfStatus = CallStore.shared.observerState.selfInfo.value.status
        when {
            selfStatus == CallParticipantStatus.Waiting && !selfIsCaller() -> {
                functionLayout = CalleeWaitingView(context)
            }

            selfStatus == CallParticipantStatus.Waiting && selfIsCaller() -> {
                functionLayout = CallerWaitingAndAcceptView(context)
            }

            selfStatus == CallParticipantStatus.Accept -> {
                functionLayout = CallerWaitingAndAcceptView(context)
            }
        }

        if (functionLayout == null) {
            Log.e("SingleCallControlsView", "functionLayout == null")
        } else {
            removeAllViews()
            addView(functionLayout)
        }
    }

    private fun selfIsCaller(): Boolean {
        val selfId = CallStore.shared.observerState.selfInfo.value.id
        val callerId = CallStore.shared.observerState.activeCall.value.inviterId
        return selfId == callerId
    }
}