package com.ezvizpro.domain.repository

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.PlaybackStream

/**
 * 录像回放仓库接口
 */
interface PlaybackRepository {

    /**
     * 获取录像回放地址（统一播放地址接口 v2）
     *
     * @param type 回放类型: 2=本地录像(SD卡), 3=云存储
     * @param protocol 协议: 1=ezopen, 3=rtmp, 4=flv
     */
    suspend fun getPlaybackAddress(
        deviceSerial: String,
        channelNo: Int = 1,
        startTime: String,       // yyyy-MM-dd HH:mm:ss
        stopTime: String,        // yyyy-MM-dd HH:mm:ss
        type: Int = 2,           // 默认本地录像
        protocol: Int = 1,       // 默认 ezopen
        code: String? = null     // 设备验证码（加密设备必填）
    ): NetworkResult<PlaybackStream>
}
