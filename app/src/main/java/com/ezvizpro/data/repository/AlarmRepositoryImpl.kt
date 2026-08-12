package com.ezvizpro.data.repository

import com.ezvizpro.core.local.TokenManager
import com.ezvizpro.core.network.EzvizApi
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.core.util.apiCall
import com.ezvizpro.domain.model.AlarmMessage
import com.ezvizpro.domain.repository.AlarmRepository
import com.ezvizpro.domain.repository.AuthRepository
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AlarmRepositoryImpl @Inject constructor(
    private val api: EzvizApi,
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : AlarmRepository {

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

    override suspend fun getAlarmList(
        pageStart: Int,
        pageSize: Int,
        alarmType: Int?
    ): NetworkResult<List<AlarmMessage>> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        Timber.d("获取报警列表: page=$pageStart, size=$pageSize, type=${alarmType ?: "全部"}")

        val result = apiCall {
            api.getAlarmList(
                accessToken = token,
                pageStart = pageStart,
                pageSize = pageSize,
                alarmType = alarmType
            )
        }

        return result.map { dtoList ->
            dtoList.map { dto ->
                val formatted = formatAlarmTime(dto.alarmTime)
                AlarmMessage(
                    alarmId = dto.alarmId,
                    deviceSerial = dto.deviceSerial,
                    channelNo = dto.channelNo,
                    alarmName = dto.alarmName,
                    alarmType = dto.alarmType,
                    alarmTime = dto.alarmTime,
                    alarmPicUrl = dto.alarmPicUrl,
                    alarmVideoUrl = dto.alarmVideoUrl,
                    isRead = dto.isRead == 1,
                    isChecked = dto.isChecked == 1,
                    deviceName = dto.deviceName,
                    preRecordUrl = dto.preRecordUrl,
                    formattedTime = formatted.first,
                    timeGroup = formatted.second
                )
            }
        }
    }

    override suspend fun markAsRead(alarmId: String): NetworkResult<Unit> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重启应用")

        return apiCall {
            api.markAlarmRead(accessToken = token, alarmId = alarmId)
        }
    }

    /**
     * 格式化告警时间
     * 返回 Pair<formattedTime, timeGroup>
     */
    private fun formatAlarmTime(timeStr: String): Pair<String, String> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val alarmDate = sdf.parse(timeStr) ?: return Pair(timeStr, "更早")

            val cal = Calendar.getInstance()
            val today = Calendar.getInstance()

            // 重置时间部分
            cal.time = alarmDate
            today.time = Date()

            val timeDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmDate)

            when {
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                    Pair("今天 $timeDisplay", "今天")
                }
                // 昨天
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> {
                    Pair("昨天 $timeDisplay", "昨天")
                }
                else -> {
                    val dateDisplay = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(alarmDate)
                    Pair(dateDisplay, "更早")
                }
            }
        } catch (e: Exception) {
            Pair(timeStr, "更早")
        }
    }
}
