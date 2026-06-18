package io.trtc.uikit.videochat.common.widget.button

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.utils.widget.ImageFilterView
import io.trtc.uikit.videochat.R

class ControlButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    val imageView: ImageFilterView
    val textView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.videochat_function_view_video_item, this, true)
        imageView = findViewById(R.id.iv_function)
        textView = findViewById(R.id.tv_function)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ControlButton, 0, 0)
            val iconRes = typedArray.getResourceId(R.styleable.ControlButton_cbIcon, 0)
            val text = typedArray.getString(R.styleable.ControlButton_cbText)
            val iconSize = typedArray.getDimensionPixelSize(R.styleable.ControlButton_cbIconSize, 0)
            val iconBg = typedArray.getResourceId(R.styleable.ControlButton_cbIconBackground, 0)
            val iconPad = typedArray.getDimensionPixelSize(R.styleable.ControlButton_cbIconPadding, 0)
            if (iconRes != 0) {
                imageView.setImageResource(iconRes)
            }
            textView.text = text ?: ""
            if (iconSize > 0) {
                imageView.layoutParams = imageView.layoutParams.apply {
                    width = iconSize
                    height = iconSize
                }
            }
            if (iconBg != 0) {
                imageView.setBackgroundResource(iconBg)
            }
            if (iconPad > 0) {
                imageView.setPadding(iconPad, iconPad, iconPad, iconPad)
            }
            typedArray.recycle()
        }
    }
}