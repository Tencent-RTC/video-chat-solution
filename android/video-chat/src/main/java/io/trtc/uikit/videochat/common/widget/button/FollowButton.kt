package io.trtc.uikit.videochat.common.widget.button

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tencent.qcloud.tuicore.util.ScreenUtil.dip2px
import io.trtc.uikit.videochat.R
import io.trtc.uikit.videochat.common.Theme

/**
 * 自绘心形图标，支持空心/实心两种状态：
 * - 未关注：紫色描边空心心形
 * - 已关注：粉红色实心心形
 *
 * 使用方式：
 * ```kotlin
 * val heart = FollowButton(context).apply {
 *     layoutParams = LayoutParams(dp(32), dp(32))
 * }
 * heart.setFilled(true)  // 切换为已关注
 * ```
 */
class FollowButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isFilled = false

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.PURPLE
        style = Paint.Style.STROKE
        strokeWidth = dip2px(2f).toFloat()
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.videochat_follow_btn_fill)
        style = Paint.Style.FILL
    }

    private val path = Path()

    fun setFilled(filled: Boolean) {
        if (isFilled == filled) return
        isFilled = filled
        invalidate()
    }

    fun isFilled(): Boolean = isFilled

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = w * 0.06f

        path.reset()
        val cx = w / 2f
        val top = padding
        val bottom = h - padding

        path.moveTo(cx, bottom * 0.88f)
        // 左半心
        path.cubicTo(
            padding - w * 0.02f, h * 0.52f,
            padding + w * 0.02f, top,
            cx, top + h * 0.18f
        )
        // 右半心
        path.cubicTo(
            w - padding - w * 0.02f, top,
            w - padding + w * 0.02f, h * 0.52f,
            cx, bottom * 0.88f
        )
        path.close()

        if (isFilled) {
            canvas.drawPath(path, fillPaint)
        } else {
            canvas.drawPath(path, strokePaint)
        }
    }
}
