package io.trtc.uikit.demo.topup

import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.trtc.uikit.videochat.common.widget.toast.VideoChatToast
import io.trtc.uikit.demo.R

/**
 * 通话中余额不足时弹出的充值底部弹窗。
 *
 * 使用方式：
 * ```
 * TopUpBottomSheetFragment.newInstance().show(supportFragmentManager, "topup")
 * ```
 */
class TopUpBottomSheetFragment : BottomSheetDialogFragment() {

    private var selectedPosition = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            bottomSheet?.let {
                BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.app_fragment_topup_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCoinGrid(view)
        setupPayButton(view)
    }

    private fun setupCoinGrid(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_coin_options)
        val spanCount = 3
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        recyclerView.addItemDecoration(CoinGridSpacingDecoration(spanCount, dpToPx(10)))
        recyclerView.adapter = CoinOptionAdapter(COIN_OPTIONS, selectedPosition) { position ->
            selectedPosition = position
        }
    }

    private fun setupPayButton(view: View) {
        val btnPay = view.findViewById<TextView>(R.id.btn_pay)
        btnPay.setOnClickListener {
            VideoChatToast.show("充值系统还需您的协助～")
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    /**
     * 金币选项网格间距 ItemDecoration。
     * 在每个 item 四周添加均匀间距，使卡片之间有呼吸感。
     */
    private class CoinGridSpacingDecoration(
        private val spanCount: Int,
        private val spacing: Int,
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            // 水平间距：均匀分配到每列左右
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            // 垂直间距：第一行不加顶部间距，其余行加顶部间距
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }

    companion object {
        /** 充值金币选项列表：金币数量 → 描述 */
        private val COIN_OPTIONS = listOf(
            CoinOption(50, "约5分钟通话"),
            CoinOption(100, "约10分钟通话"),
            CoinOption(150, "约15分钟通话"),
            CoinOption(300, "约30分钟通话"),
            CoinOption(400, "约40分钟通话"),
            CoinOption(600, "约60分钟通话"),
        )

        fun newInstance(): TopUpBottomSheetFragment {
            return TopUpBottomSheetFragment()
        }
    }
}

