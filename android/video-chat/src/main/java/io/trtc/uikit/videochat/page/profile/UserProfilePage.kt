package io.trtc.uikit.videochat.page.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMUserFullInfo
import com.tencent.qcloud.tuicore.TUIConstants
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.qcloud.tuicore.util.ScreenUtil.dip2px
import io.trtc.uikit.videochat.R
import io.trtc.uikit.videochat.databinding.VideochatActivityUserProfileBinding
import io.trtc.uikit.videochat.manager.VideoCallStore
import io.trtc.uikit.videochat.manager.UserInfoStore
import io.trtc.uikit.videochat.manager.UserInfoStore.FollowInfoCallback
import io.trtc.tuikit.atomicxcore.api.call.CallMediaType
import kotlinx.coroutines.launch

class UserProfilePage : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "user_id"
    }

    private lateinit var binding: VideochatActivityUserProfileBinding

    private val userId: String by lazy { intent.getStringExtra(EXTRA_USER_ID) ?: "" }
    private var userFullInfo: V2TIMUserFullInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (userId.isEmpty()) { finish(); return }

        binding = VideochatActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadData()
        observeFollowState()
    }

    private fun setupUI() = with(binding) {
        val avatarOverhang = dip2px(68f) / 3
        topSpacer.layoutParams.height = resources.displayMetrics.heightPixels / 4 - avatarOverhang

        bottomBarView.apply {
            onChatClick = { startChatActivity() }
            onCallClick = { startVideoCallActivity() }
            onFollowClick = { toggleFollow() }
        }
    }

    private fun loadData() {
        UserInfoStore.shared.getUserProfile(userId, onResult = { info ->
            if (isFinishing || isDestroyed) return@getUserProfile
            if (info == null) return@getUserProfile
            userFullInfo = info
            with(binding) {
                backgroundView.setBackgroundUrl(info.faceUrl)
                userInfoView.bind(info, showSignature = true)
                profileMomentsView.loadPhotos(List(6) { info.faceUrl })
            }
        })

        UserInfoStore.shared.getUserFollowInfo(userId, object : FollowInfoCallback {
            override fun onFailure(code: Int, desc: String) {}
            override fun onSuccess(followingCount: Long, followersCount: Long) {
                if (isFinishing || isDestroyed) return
                binding.userInfoView.setStats(
                    following = followingCount,
                    followers = followersCount,
                    likes = 0
                )
            }
        })
    }

    private fun observeFollowState() {
        lifecycleScope.launch {
            UserInfoStore.shared.selfFollowingUsers.collect { followingSet ->
                binding.bottomBarView.setFollowed(followingSet.contains(userId))
            }
        }
    }

    private fun startChatActivity() {
        TUICore.startActivity("TUIC2CChatActivity", Bundle().apply {
            putInt(TUIConstants.TUIChat.CHAT_TYPE, V2TIMConversation.V2TIM_C2C)
            putString(TUIConstants.TUIChat.CHAT_ID, userId)
        })
    }

    private fun startVideoCallActivity() {
        VideoCallStore.shared.calls(
            userIdList = listOf(userId),
            mediaType = CallMediaType.Video,
            params = null,
            completion = null
        )
    }

    private fun toggleFollow() {
        val ids = listOf(userId)
        if (UserInfoStore.shared.isFollowing(userId)) {
            UserInfoStore.shared.unFellowUsers(ids, onError = {
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, R.string.videochat_unfollow_failed, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            UserInfoStore.shared.fellowUser(ids, onError = {
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, R.string.videochat_follow_failed, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
