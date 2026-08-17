package com.elderlycare.app.data.ezviz

import android.util.Log
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import com.elderlycare.app.data.ezviz.model.Device
import com.elderlycare.app.data.ezviz.model.DeviceStatus
import com.elderlycare.app.data.ezviz.model.LiveStream
import com.elderlycare.app.data.ezviz.model.PlaybackStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 萤石开放平台数据仓库（客户端直连，无自建后端）
 *
 * 负责 accessToken 的获取/刷新，以及设备绑定、直播、回放、告警消息等接口调用。
 */
class EzvizRepository(
    private val api: EzvizApi,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val TAG = "EzvizRepository"
    }

    private val refreshMutex = Mutex()

    /**
     * 用 AppKey/AppSecret 登录萤石，获取 accessToken
     */
    suspend fun login(): NetworkResult<String> = refreshMutex.withLock {
        val appKey = BuildConfig.EZVIZ_APP_KEY.takeIf { it.isNotBlank() }
        val appSecret = BuildConfig.EZVIZ_APP_SECRET.takeIf { it.isNotBlank() }

        if (appKey == null || appSecret == null) {
            return NetworkResult.Error(message = "请配置萤石 AppKey/AppSecret")
        }

        return try {
            val response = api.getAccessToken(appKey, appSecret)
            if (!response.isSuccessful) {
                return NetworkResult.Error(
                    code = response.code().toString(),
                    message = "HTTP ${response.code()}: ${response.message()}"
                )
            }
            val body = response.body()
            if (body == null || body.code != "200" || body.data == null) {
                return NetworkResult.Error(
                    code = body?.code ?: "-1",
                    message = body?.msg?.takeIf { it.isNotBlank() } ?: "获取 token 失败"
                )
            }
            val token = body.data.accessToken
            val expire = if (body.data.expireTime > 0L) {
                body.data.expireTime
            } else {
                System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
            }
            tokenManager.saveToken(token, expire)
            NetworkResult.Success(token)
        } catch (e: java.net.UnknownHostException) {
            NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
        } catch (e: java.net.SocketTimeoutException) {
            NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "登录异常", e)
            NetworkResult.Error(message = e.message ?: "登录失败", throwable = e)
        }
    }

    /**
     * 获取有效 Token，过期时自动用 AppKey/AppSecret 重新认证
     */
    private suspend fun getOrRefreshToken(): String? {
        tokenManager.getValidToken()?.let { return it }
        Log.d(TAG, "Token 已过期，尝试自动刷新…")
        return when (val result = login()) {
            is NetworkResult.Success -> tokenManager.getValidToken()
            is NetworkResult.Error -> {
                Log.e(TAG, "Token 自动刷新失败: ${result.message}")
                null
            }
        }
    }

    // ==================== 设备绑定 ====================

    suspend fun addDevice(deviceSerial: String, validateCode: String): NetworkResult<Unit> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")

        return try {
            val response = api.addDevice(
                accessToken = token,
                deviceSerial = deviceSerial,
                validateCode = validateCode
            )
            if (!response.isSuccessful) {
                NetworkResult.Error(
                    code = response.code().toString(),
                    message = "HTTP ${response.code()}: ${response.message()}"
                )
            } else {
                val body = response.body()
                val code = body?.code ?: "-1"
                val msg = body?.msg?.takeIf { it.isNotBlank() } ?: "未知错误"
                when {
                    code == "200" -> NetworkResult.Success(Unit)
                    // 设备已被自己添加：说明设备已在该账号下，可直接使用，视为绑定成功
                    isAlreadyAddedBySelf(code, msg) -> NetworkResult.Success(Unit)
                    else -> NetworkResult.Error(code = code, message = msg)
                }
            }
        } catch (e: java.net.UnknownHostException) {
            NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
        } catch (e: java.net.SocketTimeoutException) {
            NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
        } catch (e: Exception) {
            Log.e(TAG, "绑定设备异常", e)
            NetworkResult.Error(message = e.message ?: "绑定失败", throwable = e)
        }
    }

    /**
     * 判断是否为「设备已被自己添加」。
     * 萤石错误码 20013 = 设备已被自己添加；20011 = 设备已被他人添加（后者仍需报错）。
     */
    private fun isAlreadyAddedBySelf(code: String, msg: String): Boolean {
        if (code == "20013") return true
        return msg.contains("自己添加") || msg.contains("已被自己")
    }

    // ==================== 设备列表 ====================

    suspend fun getDeviceList(): NetworkResult<List<Device>> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")

        val result = apiCall {
            api.getDeviceList(accessToken = token, pageStart = 0, pageSize = 50)
        }
        return result.map { list ->
            list.map { dto ->
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

    suspend fun getDeviceInfo(deviceSerial: String): NetworkResult<Device> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")

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

    // ==================== 直播 ====================

    suspend fun getLiveAddress(deviceSerial: String, code: String?): NetworkResult<LiveStream> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")

        val result = apiCall {
            api.getLiveAddress(
                accessToken = token,
                source = "$deviceSerial:1",
                protocol = 2,
                code = code
            )
        }
        return when (result) {
            is NetworkResult.Error -> result

            is NetworkResult.Success -> {
                // data 是一个数组；优先取第一个带有效播放地址的项
                val list = result.data
                val dto = list.firstOrNull { !it.hls.isNullOrBlank() || !it.url.isNullOrBlank() }
                    ?: list.firstOrNull()
                    ?: return NetworkResult.Error(message = "直播地址返回为空")

                // 单条地址可能带失败信息（如设备离线 60060=地址未绑定）
                if (dto.hls.isNullOrBlank() && dto.url.isNullOrBlank()) {
                    val msg = when (dto.ret) {
                        "60060" -> "设备不在线，请确认设备已通电并联网"
                        else -> dto.desc?.takeIf { it.isNotBlank() } ?: "无法获取播放地址，设备可能离线"
                    }
                    return NetworkResult.Error(message = msg)
                }

                NetworkResult.Success(
                    LiveStream(
                        deviceSerial = dto.deviceSerial,
                        channelNo = dto.channelNo,
                        hlsUrl = dto.hls,
                        hlsHdUrl = dto.hlsHd,
                        rtmpUrl = dto.rtmp,
                        rtmpHdUrl = dto.rtmpHd,
                        flvUrl = dto.flvAddress,
                        flvHdUrl = dto.hdFlvAddress,
                        liveId = dto.liveId,
                        expireTime = dto.expireTime
                    )
                )
            }
        }
    }

    suspend fun closeLive(deviceSerial: String, channelNo: Int = 1): NetworkResult<Unit> {
        val token = getOrRefreshToken() ?: return NetworkResult.Error(message = "未登录")
        val result = apiCall {
            api.closeLive(accessToken = token, source = "$deviceSerial:$channelNo")
        }
        return result.map { }
    }

    // ==================== 回放 ====================

    suspend fun getPlaybackAddress(
        deviceSerial: String,
        startTime: String,
        stopTime: String,
        code: String?
    ): NetworkResult<PlaybackStream> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")

        val result = apiCall {
            api.getPlaybackAddress(
                accessToken = token,
                deviceSerial = deviceSerial,
                channelNo = 1,
                type = 2,
                protocol = 1,
                startTime = startTime,
                stopTime = stopTime,
                code = code
            )
        }
        return result.map { dto ->
            PlaybackStream(
                deviceSerial = deviceSerial,
                channelNo = 1,
                url = dto.url,
                expireTime = null
            )
        }
    }

    // ==================== 告警消息 ====================

    suspend fun getAlarmList(
        pageStart: Int = 0,
        pageSize: Int = 50,
        alarmType: Int? = null
    ): NetworkResult<List<AlarmMessage>> {
        val token = getOrRefreshToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")

        val result = apiCall {
            api.getAlarmList(
                accessToken = token,
                pageStart = pageStart,
                pageSize = pageSize,
                alarmType = alarmType
            )
        }
        return result.map { list ->
            list.map { dto ->
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

    suspend fun markAlarmRead(alarmId: String): NetworkResult<Unit> {
        val token = getOrRefreshToken() ?: return NetworkResult.Error(message = "未登录")
        val result = apiCall {
            api.markAlarmRead(accessToken = token, alarmId = alarmId)
        }
        return result.map { }
    }

    fun getCurrentToken(): String? = tokenManager.getTokenForcefully()

    private fun formatAlarmTime(timeStr: String): Pair<String, String> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val alarmDate = sdf.parse(timeStr) ?: return Pair(timeStr, "更早")

            val today = Calendar.getInstance()
            val alarm = Calendar.getInstance().apply { time = alarmDate }

            val timeDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmDate)

            val isSameDay = alarm.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                alarm.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            val isYesterday = alarm.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                alarm.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1

            when {
                isSameDay -> Pair("今天 $timeDisplay", "今天")
                isYesterday -> Pair("昨天 $timeDisplay", "昨天")
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
