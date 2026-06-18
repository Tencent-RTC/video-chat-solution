package io.trtc.uikit.demo.meet

/**
 * MeetPage 交互事件协议。
 */
interface MeetPageListener {
    fun onPageSelected(pageTag: String)
    fun onPageTitleClicked(pageTag: String)
    fun onRefreshRequested(pageTag: String)
    fun onLoadMoreRequested(pageTag: String)
}
