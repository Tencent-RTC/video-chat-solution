package io.trtc.uikit.demo.manager

import android.util.Log
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMUserFullInfo
import com.tencent.imsdk.v2.V2TIMUserInfoResult
import com.tencent.imsdk.v2.V2TIMValueCallback
import com.tencent.imsdk.v2.V2TIMConversationListener
import io.trtc.tuikit.atomicxcore.api.call.CallEndReason
import io.trtc.tuikit.atomicxcore.api.call.CallListener
import io.trtc.tuikit.atomicxcore.api.call.CallMediaType
import io.trtc.tuikit.atomicxcore.api.call.CallStore
import io.trtc.tuikit.atomicxcore.api.login.LoginStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 社交数据管理器，封装用户数据获取和 IM SDK 交互。
 */
class SocialManager(private val bioFallback: String) {

    companion object {
        private const val TAG = "SocialManager"

        /**
         * 允许通话的最大时长（秒）。
         * 修改此值可调整每次通话的最长时间。
         */
        const val MAX_CALL_DURATION_SECONDS = 120

        /**
         * 距通话结束多少秒时弹出充值提示。
         * 修改此值可调整提示弹出的时机。
         */
        const val TIMEOUT_WARNING_THRESHOLD_SECONDS = 100
    }

    /**
     * 通话计时回调，app 层实现此接口来展示充值弹窗或执行超时逻辑。
     */
    interface CallTimerCallback {
        /** 距离通话超时还剩 [remainingSeconds] 秒，应展示充值提示 */
        fun onTimeoutWarning(remainingSeconds: Int)
        /** 通话时长已达上限，即将自动挂断 */
        fun onCallTimeout()
    }

    var callTimerCallback: CallTimerCallback? = null

    private var subscribeStateJob: Job? = null
    private var hasShownWarning = false

    private val callStatusObserver = object : CallListener() {
        override fun onCallEnded(callId: String, mediaType: CallMediaType, reason: CallEndReason, userId: String) {
            resetTimerState()
        }
    }

    init {
        registerObserver()
    }

    fun reset() {
        subscribeStateJob?.cancel()
        CallStore.shared.removeListener(callStatusObserver)
        callTimerCallback = null
    }

    private fun registerObserver() {
        CallStore.shared.addListener(callStatusObserver)
        subscribeStateJob = CoroutineScope(Dispatchers.Main).launch {
            CallStore.shared.observerState.activeCall.collect {
                val inviterId = it.inviterId
                val loginUserId = LoginStore.shared.loginState.loginUserInfo.value?.userID
                val selfIsCaller = inviterId == loginUserId
                if (it.callId.isEmpty() && !selfIsCaller) {
                    return@collect
                }
                val callDuration = it.duration
                handleCallDurationUpdate(callDuration.toInt())
            }
        }
    }

    private fun handleCallDurationUpdate(durationSeconds: Int) {
        if (!selfIsCaller()) {
            return
        }

        val remaining = MAX_CALL_DURATION_SECONDS - durationSeconds
        // 到达超时阈值，弹出充值提示（仅弹一次）
        if (remaining <= TIMEOUT_WARNING_THRESHOLD_SECONDS && !hasShownWarning) {
            hasShownWarning = true
            Log.i(TAG, "Call timeout warning: ${remaining}s remaining")
            callTimerCallback?.onTimeoutWarning(remaining)
        }

        // 时间耗尽，自动挂断
        if (remaining <= 0) {
            Log.i(TAG, "Call timeout, auto hangup")
            callTimerCallback?.onCallTimeout()
        }
    }

    private fun resetTimerState() {
        hasShownWarning = false
        Log.i(TAG, "Call ended, timer state reset")
    }

    interface UsersCallback {
        fun onResult(users: List<V2TIMUserFullInfo>)
        fun onError()
    }

    interface UnreadCountObserver {
        fun onCountChanged(totalUnread: Long)
    }

    private val mockRecommendedUserID = listOf(
        "VideoChatlinxiaoyu", "VideoChatsu_menghan", "VideoChatchenkexin",
        "VideoChatzhaoxinyi", "VideoChatjiangsiqi", "VideoChatzhouyaqi"
    )

    // ── 附近用户 ──────────────────────────────────────────

    fun fetchNearbyUsers(callback: UsersCallback) {
        V2TIMManager.getInstance().getUsersInfo(
            mockRecommendedUserID,
            object : V2TIMValueCallback<List<V2TIMUserFullInfo>> {
                override fun onSuccess(infoList: List<V2TIMUserFullInfo>?) {
                    val list = infoList ?: emptyList()
                    Log.i(TAG, "fetchNearbyUsers success, count: ${list.size}")
                    callback.onResult(list)
                }
                override fun onError(code: Int, desc: String?) {
                    Log.e(TAG, "fetchNearbyUsers failed: code=$code, desc=$desc")
                    callback.onError()
                }
            }
        )
    }

    // ── 关注列表 ──────────────────────────────────────────

    fun fetchFollowingList(callback: UsersCallback) {
        V2TIMManager.getFriendshipManager().getMyFollowingList(
            "",
            object : V2TIMValueCallback<V2TIMUserInfoResult> {
                override fun onSuccess(result: V2TIMUserInfoResult) {
                    val list = result.userFullInfoList
                    if (list.isNullOrEmpty()) {
                        Log.i(TAG, "fetchFollowingList: empty")
                        callback.onResult(emptyList())
                        return
                    }
                    val ids = mutableListOf<String>()
                    for (info in list) {
                        val id = info.userID ?: continue
                        ids.add(id)
                    }
                    V2TIMManager.getInstance().getUsersInfo(
                        ids,
                        object : V2TIMValueCallback<List<V2TIMUserFullInfo>> {
                            override fun onSuccess(infoList: List<V2TIMUserFullInfo>?) {
                                val users = infoList ?: emptyList()
                                Log.i(TAG, "fetchFollowingList success, count=${users.size}")
                                callback.onResult(users)
                            }
                            override fun onError(code: Int, desc: String?) {
                                Log.e(TAG, "fetchFollowingList getUsersInfo failed: $desc")
                                callback.onError()
                            }
                        }
                    )
                }
                override fun onError(code: Int, desc: String?) {
                    Log.e(TAG, "fetchFollowingList failed: $desc")
                    callback.onError()
                }
            }
        )
    }

    // ── 未读消息数 ────────────────────────────────────────

    private var unreadObserver: UnreadCountObserver? = null
    private val conversationListener = object : V2TIMConversationListener() {
        override fun onTotalUnreadMessageCountChanged(totalUnread: Long) {
            Log.d(TAG, "onTotalUnreadMessageCountChanged: $totalUnread")
            unreadObserver?.onCountChanged(totalUnread)
        }
    }

    fun startObservingUnreadCount(observer: UnreadCountObserver) {
        unreadObserver = observer
        V2TIMManager.getConversationManager().addConversationListener(conversationListener)
        V2TIMManager.getConversationManager().getTotalUnreadMessageCount(object : V2TIMValueCallback<Long> {
            override fun onSuccess(count: Long?) {
                Log.d(TAG, "getTotalUnreadMessageCount: $count")
                observer.onCountChanged(count ?: 0)
            }
            override fun onError(code: Int, desc: String?) {
                Log.e(TAG, "getTotalUnreadMessageCount failed: $desc")
            }
        })
    }

    fun stopObservingUnreadCount() {
        V2TIMManager.getConversationManager().removeConversationListener(conversationListener)
        unreadObserver = null
    }

    private fun selfIsCaller(): Boolean {
        val selfId = LoginStore.shared.loginState.loginUserInfo.value?.userID
        val inviterId = CallStore.shared.observerState.activeCall.value.inviterId
        return selfId == inviterId
    }
}
