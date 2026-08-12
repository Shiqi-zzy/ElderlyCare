package com.ezvizpro.data.repository

import com.ezvizpro.core.local.TokenManager
import com.ezvizpro.core.network.EzvizApi
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.core.util.apiCall
import com.ezvizpro.domain.model.Device
import com.ezvizpro.domain.model.DeviceStatus
import com.ezvizpro.domain.repository.AuthRepository
import com.ezvizpro.domain.repository.DeviceRepository
import timber.log.Timber
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val api: EzvizApi,
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : DeviceRepository {

    /**
     * 获取有效 Token，过期时自动用存储的 AppKey/AppSecret 重新认证
     */
    private suspend fun getOrRefreshToken(): String? {
        tokenManager.getValidToken()?.let { return it }

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

    override suspend fun getDeviceList(): NetworkResult<List<Device>> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("获取设备列表")

        val result = apiCall {
            api.getDeviceList(accessToken = token, pageStart = 0, pageSize = 50)
        }

        return result.map { dtoList ->
            dtoList.map { dto ->
                Device(
                    deviceSerial = dto.deviceSerial,
                    deviceName = dto.deviceName,
                    deviceType = dto.deviceType,
                    status = DeviceStatus.fromValue(dto.status),
                    deviceCover = dto.deviceCover,
                    category = dto.category,
                    channelNo = dto.channelNo,
                    isEncrypt = dto.isEncrypt == 1,
                    defence = dto.defence == 1
                )
            }
        }
    }

    override suspend fun getDeviceDetail(deviceSerial: String): NetworkResult<Device> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("获取设备详情: $deviceSerial")

        val result = apiCall {
            api.getDeviceInfo(accessToken = token, deviceSerial = deviceSerial)
        }

        return result.map { dto ->
            Device(
                deviceSerial = dto.deviceSerial,
                deviceName = dto.deviceName,
                deviceType = dto.model ?: "",
                status = DeviceStatus.fromValue(dto.status),
                deviceCover = dto.deviceCover,
                category = null,
                channelNo = dto.channelNo,
                isEncrypt = false,
                defence = false,
                supportPtz = dto.capacity?.supportPtz == 1,
                supportTalk = dto.capacity?.supportTalk == 1,
                supportCloud = dto.capacity?.supportCloud == 1,
                supportWifi = dto.capacity?.supportWifi == 1
            )
        }
    }

    override suspend fun updateDeviceName(
        deviceSerial: String,
        name: String
    ): NetworkResult<Unit> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        return apiCall {
            api.updateDeviceName(
                accessToken = token,
                deviceSerial = deviceSerial,
                deviceName = name
            )
        }
    }
}
