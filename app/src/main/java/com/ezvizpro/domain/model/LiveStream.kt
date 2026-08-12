package com.ezvizpro.domain.model

/**
 * 直播流信息领域模型
 */
data class LiveStream(
    val deviceSerial: String,
    val channelNo: Int,
    val hlsUrl: String?,        // HLS 拉流地址
    val hlsHdUrl: String?,      // HLS 高清地址
    val rtmpUrl: String?,       // RTMP 拉流地址
    val rtmpHdUrl: String?,     // RTMP 高清地址
    val flvUrl: String?,        // FLV 拉流地址
    val flvHdUrl: String?,      // FLV 高清地址
    val liveId: String?,
    val expireTime: Long?,       // 流地址过期时间戳
    val fetchTime: Long = System.currentTimeMillis()  // 地址获取时间
) {
    /**
     * 获取推荐的播放地址
     * 优先 HLS (兼容性最好)，次选 FLV (低延迟)
     */
    fun getPreferredUrl(): String? = hlsUrl ?: flvUrl ?: rtmpUrl

    /**
     * 获取高清播放地址
     */
    fun getHdUrl(): String? = hlsHdUrl ?: flvHdUrl ?: rtmpHdUrl

    /**
     * 检查流地址是否过期
     * 地址通常 2 分钟过期，提前 30 秒刷新
     */
    fun isExpired(): Boolean {
        val expire = expireTime ?: return false
        return System.currentTimeMillis() > expire - 30_000L
    }
}
