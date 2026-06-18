package io.trtc.uikit.videochat.common.widget.avatar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.tencent.qcloud.tuicore.util.ScreenUtil.dip2px
import io.trtc.uikit.videochat.R
import io.trtc.uikit.videochat.common.Theme

/**
 * 通用头像组件 — 支持圆角、渐变边框（三层套叠）、在线绿点 badge。
 *
 * 渐变圈实现方式：三层 View 叠加（渐变圆 + 白色间隙圆 + 头像），
 * 完全在组件 bounds 之内，不会被外层 CardView 等裁剪。
 *
 * 使用方式：
 * - XML 中直接设置 layout_width/layout_height 控制大小
 * - [loadAvatar] 加载网络图片
 * - [showBorder] 开启/关闭粉紫渐变边框
 * - [showBadge] 开启/关闭在线绿点
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val gradientRingView: View
    private val whiteRingView: View

    private var cornerRadiusPx: Int = dip2px(34f)
    private var borderWidthPx: Int = dip2px(2f)
    private val whiteGapPx: Int = dip2px(1f)

    private var _showBorder: Boolean = false
    var showBorder: Boolean
        get() = _showBorder
        set(value) {
            _showBorder = value
            gradientRingView.visibility = if (value) View.VISIBLE else View.INVISIBLE
            whiteRingView.visibility = if (value) View.VISIBLE else View.INVISIBLE
            updateImageMargin()
        }

    private var _showBadge: Boolean = false
    var showBadge: Boolean
        get() = _showBadge
        set(value) {
            _showBadge = value
            invalidate()
        }

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.videochat_avatar_badge_green)
    }

    private val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.TEXT_WHITE
        style = Paint.Style.FILL
    }

    private val badgeRadiusPx = dip2px(4f).toFloat()
    private val badgeStrokeRadiusPx = badgeRadiusPx + dip2px(1.5f).toFloat()

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        // 解析 XML 属性
        var initShowBorder = false
        var initShowBadge = false
        if (attrs != null) {
            val typed = context.obtainStyledAttributes(attrs, R.styleable.AvatarView)
            cornerRadiusPx = typed.getDimensionPixelSize(
                R.styleable.AvatarView_avatarCornerRadius, dip2px(34f)
            )
            initShowBorder = typed.getBoolean(R.styleable.AvatarView_avatarShowBorder, false)
            initShowBadge = typed.getBoolean(R.styleable.AvatarView_avatarShowBadge, false)
            borderWidthPx = typed.getDimensionPixelSize(
                R.styleable.AvatarView_avatarBorderWidth, dip2px(2f)
            )
            typed.recycle()
        }

        _showBorder = initShowBorder
        _showBadge = initShowBadge

        // 第一层：渐变圈（占满整个 View）
        gradientRingView = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            background = createGradientBackground()
            visibility = if (initShowBorder) View.VISIBLE else View.INVISIBLE
        }
        addView(gradientRingView)

        // 第二层：白色圈（居中，比外圈每边缩进 borderWidth — 位置由 onLayout 控制）
        whiteRingView = View(context).apply {
            layoutParams = LayoutParams(0, 0) // 尺寸由 onMeasure/onLayout 控制
            background = createWhiteBackground()
            visibility = if (initShowBorder) View.VISIBLE else View.INVISIBLE
        }
        addView(whiteRingView)

        // 第三层：头像 ImageView（位置由 onLayout 控制）
        val imageInset = if (initShowBorder) borderWidthPx + whiteGapPx else 0
        imageView = ImageView(context).apply {
            layoutParams = LayoutParams(0, 0) // 尺寸由 onMeasure/onLayout 控制
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = RoundOutlineProvider(computeImageCornerRadius(imageInset))
            setImageResource(R.drawable.videochat_ic_default_avatar)
        }
        addView(imageView)
    }

    // ─────────────────────────────────────────────────────
    // 对外 API
    // ─────────────────────────────────────────────────────

    fun loadAvatar(url: String?) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.videochat_ic_default_avatar)
            return
        }
        if (isCircular()) {
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.videochat_ic_default_avatar)
                .error(R.drawable.videochat_ic_default_avatar)
                .circleCrop()
                .into(imageView)
        } else {
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.videochat_ic_default_avatar)
                .error(R.drawable.videochat_ic_default_avatar)
                .transform(CenterCrop(), RoundedCorners(cornerRadiusPx))
                .into(imageView)
        }
    }

    fun setCornerRadius(radiusPx: Int) {
        cornerRadiusPx = radiusPx
        gradientRingView.background = createGradientBackground()
        whiteRingView.background = createWhiteBackground()
        val imageInset = if (_showBorder) borderWidthPx + whiteGapPx else 0
        imageView.outlineProvider = RoundOutlineProvider(computeImageCornerRadius(imageInset))
        imageView.clipToOutline = true
        invalidate()
    }

    // ─────────────────────────────────────────────────────
    // 内部实现
    // ─────────────────────────────────────────────────────

    private fun isCircular(): Boolean {
        // cornerRadius >= 20dp 认为是圆形模式
        return cornerRadiusPx >= dip2px(20f)
    }

    private fun computeImageCornerRadius(inset: Int): Float {
        return if (isCircular()) {
            // 圆形：半径足够大即可（outlineProvider 会裁成椭圆/圆）
            cornerRadiusPx.toFloat()
        } else {
            (cornerRadiusPx - inset).coerceAtLeast(0).toFloat()
        }
    }

    private fun createGradientBackground(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Theme.PINK, Theme.PURPLE)
        ).apply {
            if (isCircular()) {
                shape = GradientDrawable.OVAL
            } else {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = this@AvatarView.cornerRadiusPx.toFloat()
            }
        }
    }

    private fun createWhiteBackground(): GradientDrawable {
        return GradientDrawable().apply {
            if (isCircular()) {
                shape = GradientDrawable.OVAL
            } else {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = (cornerRadiusPx - borderWidthPx).coerceAtLeast(0).toFloat()
            }
            setColor(ContextCompat.getColor(context, R.color.videochat_color_white))
        }
    }

    private fun updateImageMargin() {
        val inset = if (_showBorder) borderWidthPx + whiteGapPx else 0
        imageView.outlineProvider = RoundOutlineProvider(computeImageCornerRadius(inset))
        // 触发重新 measure + layout
        requestLayout()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawBadge(canvas)
    }

    private fun drawBadge(canvas: Canvas) {
        if (!_showBadge) return
        val cx = width - badgeStrokeRadiusPx - dip2px(1f)
        val cy = badgeStrokeRadiusPx + dip2px(1f)
        canvas.drawCircle(cx, cy, badgeStrokeRadiusPx, badgeStrokePaint)
        canvas.drawCircle(cx, cy, badgeRadiusPx, badgePaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = dip2px(68f)
        val w = resolveSize(defaultSize, widthMeasureSpec)
        val h = resolveSize(defaultSize, heightMeasureSpec)
        setMeasuredDimension(w, h)

        // 渐变圈：占满整个 View
        gradientRingView.measure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )

        // 白色圈：每边缩进 borderWidth
        val whiteSize = (w - borderWidthPx * 2).coerceAtLeast(0)
        whiteRingView.measure(
            MeasureSpec.makeMeasureSpec(whiteSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(whiteSize, MeasureSpec.EXACTLY)
        )

        // 头像：每边缩进 borderWidth + whiteGap
        val imageInset = if (_showBorder) borderWidthPx + whiteGapPx else 0
        val imageSize = (w - imageInset * 2).coerceAtLeast(0)
        imageView.measure(
            MeasureSpec.makeMeasureSpec(imageSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(imageSize, MeasureSpec.EXACTLY)
        )

        Log.d(TAG, "onMeasure: self=$w x $h, whiteSize=$whiteSize, imageSize=$imageSize, showBorder=$_showBorder")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val w = right - left
        val h = bottom - top

        // 渐变圈：占满
        gradientRingView.layout(0, 0, w, h)

        // 白色圈：居中缩进 borderWidth
        val whiteInset = borderWidthPx
        whiteRingView.layout(whiteInset, whiteInset, w - whiteInset, h - whiteInset)

        // 头像：居中缩进 borderWidth + whiteGap（不显示 border 时缩进=0）
        val imageInset = if (_showBorder) borderWidthPx + whiteGapPx else 0
        imageView.layout(imageInset, imageInset, w - imageInset, h - imageInset)

        Log.d(TAG, "onLayout: self=[$w x $h]")
        Log.d(TAG, "  gradientRing: [${gradientRingView.left},${gradientRingView.top},${gradientRingView.right},${gradientRingView.bottom}] size=${gradientRingView.width}x${gradientRingView.height}")
        Log.d(TAG, "  whiteRing: [${whiteRingView.left},${whiteRingView.top},${whiteRingView.right},${whiteRingView.bottom}] size=${whiteRingView.width}x${whiteRingView.height}")
        Log.d(TAG, "  imageView: [${imageView.left},${imageView.top},${imageView.right},${imageView.bottom}] size=${imageView.width}x${imageView.height}")
    }

    companion object {
        private const val TAG = "AvatarView"
    }
}
