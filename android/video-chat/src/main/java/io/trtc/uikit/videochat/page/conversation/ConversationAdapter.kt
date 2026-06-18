package io.trtc.uikit.videochat.page.conversation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qcloud.tuikit.tuiconversation.classicui.widget.ConversationListAdapter
import io.trtc.uikit.videochat.R

/**
 * 继承 ConversationListAdapter，重写普通会话 ViewHolder 为自定义卡片布局。
 * 特殊类型（搜索头、加载中、空态）交回父类处理。
 */
internal class ConversationAdapter : ConversationListAdapter() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val isNormalConversation = viewType != ITEM_TYPE_HEADER_SEARCH
                && viewType != ITEM_TYPE_FOOTER_LOADING
                && viewType != ITEM_TYPE_NULL_DATA

        if (isNormalConversation) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.videochat_conversation_item, parent, false)
            val holder = ConversationHolder(view)
            holder.setAdapter(this)
            return holder
        }
        return super.onCreateViewHolder(parent, viewType)
    }
}
