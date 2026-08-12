package com.ezvizpro.data.repository

import com.ezvizpro.data.local.room.dao.AlarmDao
import com.ezvizpro.data.local.room.dao.ElderlyDao
import com.ezvizpro.data.local.room.entity.AlarmEntity
import com.ezvizpro.data.local.room.entity.ElderlyEntity
import com.ezvizpro.data.remote.ElderlyCareApi
import com.ezvizpro.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智慧养老平台数据仓库
 *
 * 策略：远程优先 (remote-first)，本地 Room 作为缓存
 */
@Singleton
class ElderlyCareRepository @Inject constructor(
    private val api: ElderlyCareApi,
    private val elderlyDao: ElderlyDao,
    private val alarmDao: AlarmDao
) {

    private var authToken: String = ""

    fun setToken(token: String) { authToken = "Bearer $token" }

    // ==================== 认证（手机验证码登录） ====================

    suspend fun sendCode(phone: String): Result<String> = runCatching {
        val response = api.sendCode(SendCodeRequest(phone))
        if (response.isSuccessful) "验证码已发送"
        else throw Exception("发送验证码失败: ${response.code()}")
    }

    suspend fun login(phone: String, code: String, role: String): Result<SyncResponse> = runCatching {
        val response = api.login(LoginRequest(phone, code, role))
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("登录失败: ${response.code()}")
        }
    }

    // ==================== 设备同步（萤石 AppKey 初始化） ====================

    suspend fun sync(clientId: String, ezvizToken: String?): Result<SyncResponse> = runCatching {
        val response = api.sync(SyncRequest(clientId, ezvizToken))
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("同步失败: ${response.code()}")
        }
    }

    suspend fun selectRole(clientId: String, role: String, realName: String, phone: String): Result<SyncResponse> = runCatching {
        val response = api.selectRole(SelectRoleRequest(clientId, role, realName, phone))
        if (response.isSuccessful && response.body() != null) {
            response.body()!!
        } else {
            throw Exception("角色选择失败: ${response.code()}")
        }
    }

    // ==================== 老人档案 ====================

    suspend fun getMyElderly(): Result<List<ElderlyDto>> = runCatching {
        val response = api.getMyElderly(authToken)
        if (response.isSuccessful && response.body() != null) {
            val items = response.body()!!.items
            elderlyDao.insertAll(items.map { it.toEntity() })
            items
        } else throw Exception("获取老人列表失败: ${response.code()}")
    }

    // ==================== 告警 ====================

    suspend fun getAlarms(elderlyId: String, level: String? = null, status: String? = null): Result<List<AlarmDto>> = runCatching {
        val response = api.getAlarms(authToken, elderlyId, level, status)
        if (response.isSuccessful && response.body() != null) {
            val items = response.body()!!.items
            alarmDao.insertAll(items.map { it.toEntity() })
            items
        } else throw Exception("获取告警列表失败: ${response.code()}")
    }

    suspend fun acknowledgeAlarm(alarmId: String): Result<String> = runCatching {
        val response = api.acknowledgeAlarm(authToken, alarmId)
        if (response.isSuccessful) {
            alarmDao.updateStatus(alarmId, "acknowledged")
            "已确认告警"
        } else throw Exception("确认告警失败: ${response.code()}")
    }

    suspend fun simulateAlarm(): Result<AlarmDto> = runCatching {
        val response = api.simulateAlarm(authToken)
        if (response.isSuccessful && response.body() != null) response.body()!!.alarm
        else throw Exception("模拟告警失败: ${response.code()}")
    }

    // ==================== 授权管理 ====================

    suspend fun getAuthorizations(elderlyId: String): Result<List<AuthorizationDto>> = runCatching {
        val response = api.getAuthorizations(authToken, elderlyId)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取授权列表失败: ${response.code()}")
    }

    suspend fun grantAuthorization(request: GrantAuthorizationRequest): Result<String> = runCatching {
        val response = api.grantAuthorization(authToken, request)
        if (response.isSuccessful) "授权成功" else throw Exception("授权失败: ${response.code()}")
    }

    suspend fun revokeAuthorization(authId: String, reason: String? = null): Result<String> = runCatching {
        val response = api.revokeAuthorization(authToken, authId, RevokeAuthorizationRequest(reason))
        if (response.isSuccessful) "已撤销授权" else throw Exception("撤销失败: ${response.code()}")
    }

    // ==================== 隐私控制 ====================

    suspend fun pauseMonitoring(elderlyId: String): Result<String> = runCatching {
        val response = api.pauseMonitoring(authToken, elderlyId)
        if (response.isSuccessful) "监控已暂停" else throw Exception("操作失败")
    }

    suspend fun resumeMonitoring(elderlyId: String): Result<String> = runCatching {
        val response = api.resumeMonitoring(authToken, elderlyId)
        if (response.isSuccessful) "监控已恢复" else throw Exception("操作失败")
    }

    suspend fun getPrivacyStatus(elderlyId: String): Result<Boolean> = runCatching {
        val response = api.getPrivacyStatus(authToken, elderlyId)
        if (response.isSuccessful && response.body() != null) response.body()!!.privacyPaused
        else throw Exception("获取状态失败")
    }

    // ==================== 授权申请审批（家属端） ====================

    suspend fun getPendingAuthorizationRequests(): Result<List<AuthorizationDto>> = runCatching {
        val response = api.getPendingAuthorizationRequests(authToken)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取待审批列表失败: ${response.code()}")
    }

    suspend fun approveAuthorizationRequest(requestId: String): Result<String> = runCatching {
        val response = api.approveAuthorizationRequest(authToken, requestId)
        if (response.isSuccessful) "审批通过" else throw Exception("审批失败: ${response.code()}")
    }

    suspend fun rejectAuthorizationRequest(requestId: String): Result<String> = runCatching {
        val response = api.rejectAuthorizationRequest(authToken, requestId)
        if (response.isSuccessful) "已拒绝" else throw Exception("操作失败: ${response.code()}")
    }

    // ==================== 设备 ====================

    suspend fun getDevices(elderlyId: String): Result<List<DeviceDto>> = runCatching {
        val response = api.getDevices(authToken, elderlyId)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取设备列表失败")
    }

    // ==================== 设备验证码 ====================

    suspend fun generateDeviceCode(deviceId: String): Result<GenerateCodeResponse> = runCatching {
        val response = api.generateDeviceCode(authToken, deviceId)
        if (response.isSuccessful && response.body() != null) response.body()!!
        else throw Exception("生成验证码失败: ${response.code()}")
    }

    suspend fun getActiveDeviceCodes(): Result<List<VerificationCodeItem>> = runCatching {
        val response = api.getActiveDeviceCodes(authToken)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取验证码列表失败")
    }

    suspend fun revokeDeviceCode(codeId: String): Result<String> = runCatching {
        val response = api.revokeDeviceCode(authToken, codeId)
        if (response.isSuccessful) "验证码已撤销" else throw Exception("撤销失败: ${response.code()}")
    }

    // ==================== 社区 — 设备绑定与巡检 ====================

    suspend fun bindCommunityDevice(code: String): Result<BindDeviceResponse> = runCatching {
        val response = api.bindCommunityDevice(authToken, BindDeviceRequestDto(code))
        if (response.isSuccessful && response.body() != null) response.body()!!
        else throw Exception("绑定失败: ${response.code()}")
    }

    suspend fun getCommunityDevices(elderlyId: String): Result<List<DeviceDto>> = runCatching {
        val response = api.getCommunityDevices(authToken, elderlyId)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取设备失败")
    }

    suspend fun logInspection(
        deviceId: String, type: String, status: String, findings: String = "", photos: String = ""
    ): Result<String> = runCatching {
        val response = api.logDeviceInspection(authToken, deviceId, InspectionRequest(type, status, findings, photos))
        if (response.isSuccessful) "巡检已记录" else throw Exception("巡检记录失败: ${response.code()}")
    }

    suspend fun getMaintenanceHistory(deviceId: String): Result<List<InspectionDto>> = runCatching {
        val response = api.getDeviceMaintenanceHistory(authToken, deviceId)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取维护历史失败")
    }

    // ==================== 医院 — 设备绑定 ====================

    suspend fun bindHospitalDevice(code: String): Result<BindDeviceResponse> = runCatching {
        val response = api.bindHospitalDevice(authToken, BindDeviceRequestDto(code))
        if (response.isSuccessful && response.body() != null) response.body()!!
        else throw Exception("绑定失败: ${response.code()}")
    }

    suspend fun getHospitalDevices(elderlyId: String): Result<List<DeviceDto>> = runCatching {
        val response = api.getHospitalDevices(authToken, elderlyId)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取设备失败")
    }

    // ==================== 健康档案（Phase 3） ====================

    suspend fun addHealthRecord(elderlyId: String, request: AddHealthRecordRequest): Result<String> = runCatching {
        val response = api.addHealthRecord(authToken, elderlyId, request)
        if (response.isSuccessful && response.body() != null) response.body()!!.message
        else throw Exception("录入健康档案失败: ${response.code()}")
    }

    suspend fun getHospitalHealthRecords(elderlyId: String): Result<List<HealthRecordDto>> = runCatching {
        val response = api.getHospitalHealthRecords(authToken, elderlyId)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取健康档案失败")
    }

    suspend fun getFamilyHealthRecords(elderlyId: String): Result<List<HealthRecordDto>> = runCatching {
        val response = api.getFamilyHealthRecords(authToken, elderlyId)
        if (response.isSuccessful && response.body() != null) response.body()!!.items
        else throw Exception("获取健康档案失败")
    }

    // ==================== 急救权限（Phase 3） ====================

    suspend fun requestEmergencyAccess(elderlyId: String, reason: String): Result<String> = runCatching {
        val response = api.requestEmergencyAccess(authToken, EmergencyRequestDto(elderlyId, reason))
        if (response.isSuccessful && response.body() != null) response.body()!!.message
        else throw Exception("急救权限请求失败: ${response.code()}")
    }

    suspend fun getEmergencyStatus(): Result<EmergencyStatusResponse> = runCatching {
        val response = api.getEmergencyStatus(authToken)
        if (response.isSuccessful && response.body() != null) response.body()!!
        else throw Exception("获取急救状态失败")
    }
}

// ==================== 扩展：DTO → Room Entity ====================

private fun ElderlyDto.toEntity() = ElderlyEntity(
    id = id, name = name, gender = gender, birthDate = birthDate,
    phone = phone, address = address, emergencyContact = emergencyContact,
    careLevel = careLevel, privacyPaused = privacyPaused
)

private fun AlarmDto.toEntity() = AlarmEntity(
    id = id, elderlyId = elderlyId, alarmType = alarmType,
    alarmLevel = alarmLevel, aiScore = aiScore, aiVerified = aiVerified,
    title = title, description = description, status = status,
    createdAt = createdAt, acknowledgedAt = acknowledgedAt
)
