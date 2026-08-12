package com.ezvizpro.data.repository

import com.ezvizpro.core.local.TokenManager
import com.ezvizpro.core.network.EzvizApi
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.core.util.apiCall
import com.ezvizpro.domain.model.PlaybackStream
import com.ezvizpro.domain.repository.AuthRepository
import com.ezvizpro.domain.repository.PlaybackRepository
import timber.log.Timber
import javax.inject.Inject

class PlaybackRepositoryImpl @Inject constructor(
    private val api: EzvizApi,
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : PlaybackRepository {

    private suspend fun getOrRefreshToken(): String? {
        tokenManager.getValidToken()?.let { return it }
        val appKey = tokenManager.getStoredAppKey() ?: return null
        val appSecret = tokenManager.getStoredAppSecret() ?: return null
        Timber.d("Token 已过期，尝试自动刷新…")
        return when (val result = authRepository.login(appKey, appSecret)) {
            is NetworkResult.Success -> { Timber.d("Token 自动刷新成功"); tokenManager.getValidToken() }
            is NetworkResult.Error -> { Timber.e("Token 自动刷新失败: ${result.message}"); null }
        }
    }

    override suspend fun getPlaybackAddress(
        deviceSerial: String,
        channelNo: Int,
        startTime: String,
        stopTime: String,
        type: Int,
        protocol: Int,
        code: String?
    ): NetworkResult<PlaybackStream> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("获取回放地址: $deviceSerial, $startTime ~ $stopTime, type=$type")

        val result = apiCall {
            api.getPlaybackAddress(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                type = type,
                protocol = protocol,
                startTime = startTime,
                stopTime = stopTime,
                code = code
            )
        }

        return result.map { dto ->
            PlaybackStream(
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                url = dto.url,
                expireTime = null
            )
        }
    }
}
