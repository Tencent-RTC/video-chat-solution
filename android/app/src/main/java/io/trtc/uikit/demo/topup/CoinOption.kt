package io.trtc.uikit.demo.topup

/**
 * 充值金币选项数据模型。
 *
 * @param amount 金币数量
 * @param description 描述文案（如"约5分钟通话"）
 */
data class CoinOption(
    val amount: Int,
    val description: String,
)
