package com.ezvizpro.domain.repository

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.LiveStream
import com.ezvizpro.domain.model.Preset
import com.ezvizpro.domain.model.PtzDirection
import com.ezvizpro.domain.model.PtzSpeed

/**
 * 直播和云台仓库接口
 */
interface LiveRepository {

    /**
     * 获取直播流地址
     * @param code 设备验证码（加密设备必填，6位大写字母）
     */
    suspend fun getLiveAddress(
        deviceSerial: String,
        channelNo: Int = 1,
        protocol: Int = 2,  // 默认 HLS
        code: String? = null    // 设备验证码
    ): NetworkResult<LiveStream>

    /**
     * 关闭直播通道
     */
    suspend fun closeLive(deviceSerial: String, channelNo: Int = 1): NetworkResult<Unit>

    /**
     * 开始云台控制
     */
    suspend fun startPtz(
        deviceSerial: String,
        channelNo: Int,
        direction: PtzDirection,
        speed: PtzSpeed = PtzSpeed.NORMAL
    ): NetworkResult<Unit>

    /**
     * 停止云台控制
     */
    suspend fun stopPtz(
        deviceSerial: String,
        channelNo: Int,
        direction: PtzDirection? = null
    ): NetworkResult<Unit>

    /**
     * 获取预置位列表
     */
    suspend fun getPresetList(
        deviceSerial: String,
        channelNo: Int = 1
    ): NetworkResult<List<Preset>>

    /**
     * 调用预置位
     */
    suspend fun moveToPreset(
        deviceSerial: String,
        channelNo: Int,
        presetIndex: Int
    ): NetworkResult<Unit>

    /**
     * 添加预置位
     */
    suspend fun addPreset(
        deviceSerial: String,
        channelNo: Int,
        presetName: String
    ): NetworkResult<Preset>
}
