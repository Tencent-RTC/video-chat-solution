package io.trtc.uikit.demo.topup

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.trtc.uikit.demo.R

/**
 * 充值金币选项网格 Adapter。
 * 支持单选切换，选中项高亮粉紫渐变描边 + 星星闪动动画。
 */
class CoinOptionAdapter(
    private val options: List<CoinOption>,
    private var selectedPosition: Int,
    private val onItemSelected: (Int) -> Unit,
) : RecyclerView.Adapter<CoinOptionAdapter.ViewHolder>() {

    companion object {
        // ============================================================
        // ✨ 星星动画可调参数 —— 在这里集中控制所有动画表现
        // ============================================================

        /** 选中态：星星字号 (sp) */
        const val SPARKLE_SELECTED_TEXT_SIZE = 14f

        /** 未选中态：星星字号 (sp) */
        const val SPARKLE_UNSELECTED_TEXT_SIZE = 12f

        /** 选中态：星星颜色（亮金色） */
        const val SPARKLE_SELECTED_COLOR = 0xFFFFB800.toInt()

        /** 未选中态：星星颜色（淡金色） */
        const val SPARKLE_UNSELECTED_COLOR = 0xFFFFB800.toInt()

        /** 选中态：缩放最小值 */
        const val SPARKLE_SELECTED_SCALE_MIN = 0.8f

        /** 选中态：缩放最大值 */
        const val SPARKLE_SELECTED_SCALE_MAX = 1.6f

        /** 未选中态：缩放最小值 */
        const val SPARKLE_UNSELECTED_SCALE_MIN = 0.7f

        /** 未选中态：缩放最大值 */
        const val SPARKLE_UNSELECTED_SCALE_MAX = 1.3f

        /** 选中态：透明度最小值 */
        const val SPARKLE_SELECTED_ALPHA_MIN = 0.6f

        /** 选中态：透明度最大值 */
        const val SPARKLE_SELECTED_ALPHA_MAX = 1f

        /** 未选中态：透明度最小值 */
        const val SPARKLE_UNSELECTED_ALPHA_MIN = 0.4f

        /** 未选中态：透明度最大值 */
        const val SPARKLE_UNSELECTED_ALPHA_MAX = 0.9f

        /** 选中态：动画周期 (ms) */
        const val SPARKLE_SELECTED_DURATION_MS = 1200L

        /** 未选中态：动画周期 (ms) */
        const val SPARKLE_UNSELECTED_DURATION_MS = 1800L

        /** 选中态：是否启用旋转动画 */
        const val SPARKLE_SELECTED_ROTATION_ENABLED = true

        /** 选中态：旋转角度（一个周期内旋转多少度） */
        const val SPARKLE_SELECTED_ROTATION_DEGREES = 360f
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.item_root)
        val tvAmount: TextView = itemView.findViewById(R.id.tv_coin_amount)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_coin_desc)
        val tvSparkle: TextView = itemView.findViewById(R.id.tv_sparkle)
        var sparkleAnimator: AnimatorSet? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_item_topup_coin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.tvAmount.text = "${option.amount}"
        holder.tvDesc.text = option.description

        val isSelected = position == selectedPosition
        holder.root.isSelected = isSelected

        // 选中态：金币数量文字使用粉紫渐变
        if (isSelected) {
            applyGradientText(holder.tvAmount)
        } else {
            holder.tvAmount.paint.shader = null
            holder.tvAmount.setTextColor(0xFF1F2937.toInt())
        }

        // 所有项都有闪动星星，选中项星星更大更醒目
        startSparkleAnimation(holder, isSelected)

        holder.root.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onItemSelected(selectedPosition)
        }
    }

    override fun getItemCount(): Int = options.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.sparkleAnimator?.cancel()
        holder.sparkleAnimator = null
    }

    private fun applyGradientText(tv: TextView) {
        tv.post {
            val width = tv.paint.measureText(tv.text.toString())
            tv.paint.shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(0xFFFF2D78.toInt(), 0xFFB44FFF.toInt()),
                null, Shader.TileMode.CLAMP
            )
            tv.invalidate()
        }
    }

    /**
     * 所有金币项都有闪动星星动画。
     * - 未选中：缩放 + 透明度闪烁
     * - 选中：更大幅度缩放 + 全透明度 + 旋转，醒目突出
     *
     * 动画参数均可在 companion object 中调整。
     */
    private fun startSparkleAnimation(holder: ViewHolder, isSelected: Boolean) {
        holder.tvSparkle.visibility = View.VISIBLE
        holder.sparkleAnimator?.cancel()

        val scaleMin = if (isSelected) SPARKLE_SELECTED_SCALE_MIN else SPARKLE_UNSELECTED_SCALE_MIN
        val scaleMax = if (isSelected) SPARKLE_SELECTED_SCALE_MAX else SPARKLE_UNSELECTED_SCALE_MAX
        val alphaMin = if (isSelected) SPARKLE_SELECTED_ALPHA_MIN else SPARKLE_UNSELECTED_ALPHA_MIN
        val alphaMax = if (isSelected) SPARKLE_SELECTED_ALPHA_MAX else SPARKLE_UNSELECTED_ALPHA_MAX
        val duration = if (isSelected) SPARKLE_SELECTED_DURATION_MS else SPARKLE_UNSELECTED_DURATION_MS

        holder.tvSparkle.setTextColor(if (isSelected) SPARKLE_SELECTED_COLOR else SPARKLE_UNSELECTED_COLOR)
        holder.tvSparkle.textSize = if (isSelected) SPARKLE_SELECTED_TEXT_SIZE else SPARKLE_UNSELECTED_TEXT_SIZE

        val scaleX = ObjectAnimator.ofFloat(holder.tvSparkle, View.SCALE_X, scaleMin, scaleMax, scaleMin).apply {
            repeatCount = ObjectAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(holder.tvSparkle, View.SCALE_Y, scaleMin, scaleMax, scaleMin).apply {
            repeatCount = ObjectAnimator.INFINITE
        }
        val alpha = ObjectAnimator.ofFloat(holder.tvSparkle, View.ALPHA, alphaMin, alphaMax, alphaMin).apply {
            repeatCount = ObjectAnimator.INFINITE
        }

        val animators = mutableListOf(scaleX, scaleY, alpha)

        if (isSelected && SPARKLE_SELECTED_ROTATION_ENABLED) {
            val rotation = ObjectAnimator.ofFloat(
                holder.tvSparkle, View.ROTATION, 0f, SPARKLE_SELECTED_ROTATION_DEGREES
            ).apply {
                repeatCount = ObjectAnimator.INFINITE
            }
            animators.add(rotation)
        } else {
            holder.tvSparkle.rotation = 0f
        }

        holder.sparkleAnimator = AnimatorSet().apply {
            playTogether(animators.toList())
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
