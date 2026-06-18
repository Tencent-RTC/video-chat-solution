package io.trtc.uikit.videochat.page.call

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import io.trtc.uikit.videochat.R
import io.trtc.uikit.videochat.common.utils.ImageResourceCache
import io.trtc.uikit.videochat.page.call.controls.SingleCallControlsView
import io.trtc.tuikit.atomicxcore.api.call.CallParticipantInfo
import io.trtc.tuikit.atomicxcore.api.call.CallStore
import io.trtc.tuikit.atomicxcore.api.view.CallCoreView
import io.trtc.tuikit.atomicxcore.api.view.CallLayoutTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private data class ParticipantAvatarInfo(
        var originalUrl: String,
        var cachedPath: String?
    )
    private var coreView: CallCoreView? = null
    private var scope: CoroutineScope? = null
    private val imageResourceCache = ImageResourceCache(context)
    private var layoutFunction: FrameLayout? = null
    private val participantAvatarInfoMap: MutableMap<String, ParticipantAvatarInfo> = mutableMapOf()

    init {
        initView()
    }

    internal fun setLayoutTemplate(template: CallLayoutTemplate) {
        val isPipView = template == CallLayoutTemplate.Pip
        layoutFunction?.visibility = if (isPipView) GONE else VISIBLE
        coreView?.setLayoutTemplate(template)
    }

    internal fun showFunctionView(show: Boolean) {
        layoutFunction?.visibility = if (show) VISIBLE else GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addFunctionLayout()
        val newScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope = newScope
        newScope.launch {
            CallStore.shared.observerState.allParticipants.collect { allParticipants ->
                updateParticipantsAvatars(allParticipants)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
        coreView?.removeAllViews()
    }

    private fun updateParticipantsAvatars(participants: Collection<CallParticipantInfo>) {
        val removedAny = removeStaleParticipants(participants)
        val toUpdate = findAvatarsToUpdate(participants)
        if (toUpdate.isEmpty()) {
            if (removedAny) applyAvatarMap()
            return
        }
        cacheAvatarsAndApply(toUpdate)
    }

    private fun removeStaleParticipants(participants: Collection<CallParticipantInfo>): Boolean {
        val currentIds = participants.map { it.id }.toSet()
        val removed = participantAvatarInfoMap.keys.filter { it !in currentIds }
        removed.forEach { participantAvatarInfoMap.remove(it) }
        return removed.isNotEmpty()
    }

    private fun findAvatarsToUpdate(participants: Collection<CallParticipantInfo>): List<Pair<String, String>> {
        val toUpdate = mutableListOf<Pair<String, String>>()
        for (p in participants) {
            val url = p.avatarURL ?: ""
            val existing = participantAvatarInfoMap[p.id]
            if (existing?.originalUrl != url) {
                toUpdate.add(p.id to url)
                participantAvatarInfoMap[p.id] = ParticipantAvatarInfo(originalUrl = url, cachedPath = existing?.cachedPath)
            }
        }
        return toUpdate
    }

    private fun cacheAvatarsAndApply(toUpdate: List<Pair<String, String>>) {
        val completed = AtomicInteger(0)
        val total = toUpdate.size
        for ((id, url) in toUpdate) {
            imageResourceCache.cacheNetworkImage(url) { cachedPath ->
                onAvatarCached(id, cachedPath)
                if (completed.incrementAndGet() == total) applyAvatarMap()
            }
        }
    }

    private fun onAvatarCached(participantId: String, cachedPath: String?) {
        synchronized(participantAvatarInfoMap) {
            val info = participantAvatarInfoMap[participantId] ?: return
            info.cachedPath = when {
                cachedPath != null -> File(cachedPath).absolutePath
                else -> imageResourceCache.getDefaultAvatarPath(R.drawable.videochat_ic_default_avatar)
            }
            if (info.cachedPath == null) participantAvatarInfoMap.remove(participantId)
        }
    }

    private fun applyAvatarMap() {
        coreView?.setParticipantAvatars(buildAvatarPathMap())
    }

    private fun buildAvatarPathMap(): Map<String, String> {
        return participantAvatarInfoMap
            .filter { it.value.cachedPath != null }
            .mapValues { it.value.cachedPath!! }
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.videochat_view_video_call, this, true)
        layoutFunction = findViewById(R.id.rl_layout_function)
        coreView = CallCoreView(context)
        val layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        this.addView(coreView, 0, layoutParams)
    }

    private fun addFunctionLayout() {
        val controlsView = SingleCallControlsView(context)
        layoutFunction?.addView(controlsView)
    }

}