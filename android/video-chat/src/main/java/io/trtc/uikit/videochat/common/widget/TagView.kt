package io.trtc.uikit.videochat.common.widget

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tencent.qcloud.tuicore.util.ScreenUtil.dip2px
import io.trtc.uikit.videochat.R
import io.trtc.uikit.videochat.common.Theme

/**
 * 统一样式标签组件，支持两种视觉风格。
 *
 * 用法：
 * ```
 * val tag = TagView(context, "旅行")                       // 默认紫色描边
 * val overlay = TagView(context, "北京", TagView.Style.OVERLAY)  // 白字半透明
 * container.addView(tag, tag.defaultLayoutParams())
 * ```
 */
class TagView(
    context: Context,
    label: String,
    style: Style = Style.OUTLINED
) : TextView(context) {

    enum class Style {
        /** 用户主页 / 设置页 — 紫色描边标签（原尺寸） */
        OUTLINED,
        /** 列表布局 — 浅紫小号标签 */
        SMALL_OUTLINED,
        /** 卡片布局 — 毛玻璃白底深色字 */
        FROSTED,
        /** 旧样式保留兼容 — 半透明黑底白字 */
        OVERLAY
    }

    init {
        text = label
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        maxWidth = dip2px(120f)
        when (style) {
            Style.OUTLINED -> applyOutlined()
            Style.SMALL_OUTLINED -> applySmallOutlined()
            Style.FROSTED -> applyFrosted()
            Style.OVERLAY -> applyOverlay()
        }
    }

    private fun applyOutlined() {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTextColor(Theme.DEEP_PURPLE)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dip2px(12f).toFloat()
            setColor(Theme.BG_TAG_PURPLE)
            setStroke(dip2px(1f), Theme.PURPLE)
        }
        setPadding(dip2px(10f), dip2px(4f), dip2px(10f), dip2px(4f))
    }

    /** 列表布局用：更浅背景 + 浅紫描边 + 中紫文字 + 小尺寸 */
    private fun applySmallOutlined() {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setTextColor(Theme.TEXT_TAG_LIGHT)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dip2px(10f).toFloat()
            setColor(Theme.BG_TAG_PURPLE_LIGHT)
            setStroke(dip2px(1f), Theme.STROKE_TAG_LIGHT)
        }
        setPadding(dip2px(8f), dip2px(3f), dip2px(8f), dip2px(3f))
    }

    /** 卡片布局用：半透明白底 + 白字（同未关注按钮风格） */
    private fun applyFrosted() {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
        setTextColor(Theme.TEXT_WHITE)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dip2px(8f).toFloat()
            setColor(Theme.BG_TAG_FROSTED)
        }
        setPadding(dip2px(7f), dip2px(3f), dip2px(7f), dip2px(3f))
    }

    private fun applyOverlay() {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
        setTextColor(ContextCompat.getColor(context, R.color.videochat_color_white))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dip2px(8f).toFloat()
            setColor(ContextCompat.getColor(context, R.color.videochat_tag_overlay_bg))
        }
        setPadding(dip2px(6f), dip2px(2f), dip2px(6f), dip2px(2f))
    }

    /** 带间距的默认 LayoutParams，直接传给 addView 即可 */
    fun defaultLayoutParams(): MarginLayoutParams {
        return MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            marginEnd = dip2px(6f)
            bottomMargin = dip2px(6f)
        }
    }
}
