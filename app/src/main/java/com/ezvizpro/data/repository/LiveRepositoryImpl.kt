package com.ezvizpro.data.repository

import com.ezvizpro.core.local.TokenManager
import com.ezvizpro.core.network.EzvizApi
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.core.util.apiCall
import com.ezvizpro.domain.model.LiveStream
import com.ezvizpro.domain.model.Preset
import com.ezvizpro.domain.model.PtzDirection
import com.ezvizpro.domain.model.PtzSpeed
import com.ezvizpro.domain.repository.AuthRepository
import com.ezvizpro.domain.repository.LiveRepository
import timber.log.Timber
import javax.inject.Inject

class LiveRepositoryImpl @Inject constructor(
    private val api: EzvizApi,
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : LiveRepository {

    /**
     * 获取有效 Token，过期时自动用存储的 AppKey/AppSecret 重新认证
     */
    private suspend fun getOrRefreshToken(): String? {
        tokenManager.getValidToken()?.let { return it }

        // Token 过期，尝试重新认证
        val appKey = tokenManager.getStoredAppKey() ?: return null
        val appSecret = tokenManager.getStoredAppSecret() ?: return null

        Timber.d("Token 已过期，尝试自动刷新…")
        return when (val result = authRepository.login(appKey, appSecret)) {
            is NetworkResult.Success -> {
                Timber.d("Token 自动刷新成功")
                tokenManager.getValidToken()
            }
            is NetworkResult.Error -> {
                Timber.e("Token 自动刷新失败: ${result.message}")
                null
            }
        }
    }

    override suspend fun getLiveAddress(
        deviceSerial: String,
        channelNo: Int,
        protocol: Int,
        code: String?
    ): NetworkResult<LiveStream> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("获取直播地址: $deviceSerial, channel=$channelNo, protocol=$protocol, code=${if (code != null) "***" else "无"}")

        val result = apiCall {
            api.getLiveAddress(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                protocol = protocol,
                code = code
            )
        }

        return result.map { dto ->
            LiveStream(
                deviceSerial = dto.deviceSerial,
                channelNo = dto.channelNo,
                hlsUrl = dto.hls,
                hlsHdUrl = dto.hlsHd,
                rtmpUrl = dto.rtmp,
                rtmpHdUrl = dto.rtmpHd,
                flvUrl = dto.flv,
                flvHdUrl = dto.flvHd,
                liveId = dto.liveId,
                expireTime = dto.expireTime
            )
        }
    }

    override suspend fun closeLive(
        deviceSerial: String,
        channelNo: Int
    ): NetworkResult<Unit> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("关闭直播: $deviceSerial, channel=$channelNo")

        return apiCall {
            api.closeLive(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo
            )
        }
    }

    override suspend fun startPtz(
        deviceSerial: String,
        channelNo: Int,
        direction: PtzDirection,
        speed: PtzSpeed
    ): NetworkResult<Unit> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("开始云台控制: $deviceSerial, direction=${direction.label}, speed=${speed.label}")

        return apiCall {
            api.startPtz(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                direction = direction.value,
                speed = speed.value
            )
        }
    }

    override suspend fun stopPtz(
        deviceSerial: String,
        channelNo: Int,
        direction: PtzDirection?
    ): NetworkResult<Unit> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("停止云台控制: $deviceSerial, direction=${direction?.label ?: "全部"}")

        return apiCall {
            api.stopPtz(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                direction = if (direction != null) direction.value else null
            )
        }
    }

    override suspend fun getPresetList(
        deviceSerial: String,
        channelNo: Int
    ): NetworkResult<List<Preset>> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        val result = apiCall {
            api.getPresetList(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo
            )
        }

        return result.map { dtoList ->
            dtoList.map { Preset(index = it.index, name = it.presetName) }
        }
    }

    override suspend fun moveToPreset(
        deviceSerial: String,
        channelNo: Int,
        presetIndex: Int
    ): NetworkResult<Unit> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        return apiCall {
            api.moveToPreset(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                presetIndex = presetIndex
            )
        }
    }

    override suspend fun addPreset(
        deviceSerial: String,
        channelNo: Int,
        presetName: String
    ): NetworkResult<Preset> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        val result = apiCall {
            api.addPreset(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                presetName = presetName
            )
        }

        return result.map { Preset(index = it.index, name = it.presetName) }
    }
}
