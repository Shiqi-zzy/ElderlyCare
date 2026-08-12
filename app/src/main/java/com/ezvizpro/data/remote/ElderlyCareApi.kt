package com.ezvizpro.data.remote

import com.ezvizpro.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 智慧养老平台自建后端 API 接口
 *
 * 萤石负责：设备管理、直播拉流、云台控制、录像回放
 * 自建后端负责：用户角色、老人档案、告警闭环、授权体系、工单处置
 */
interface ElderlyCareApi {

    // ==================== 认证（手机验证码登录） ====================

    @POST("api/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<MessageResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<SyncResponse>

    // ==================== 设备同步（萤石 AppKey 初始化） ====================

    @POST("api/auth/sync")
    suspend fun sync(@Body request: SyncRequest): Response<SyncResponse>

    @POST("api/auth/select-role")
    suspend fun selectRole(@Body request: SelectRoleRequest): Response<SyncResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserDto>

    // ==================== 老人档案 ====================

    @GET("api/family/elderly/list")
    suspend fun getMyElderly(@Header("Authorization") token: String): Response<ElderlyListResponse>

    @GET("api/family/elderly/{elderlyId}")
    suspend fun getElderly(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<ElderlyDto>

    @POST("api/family/elderly")
    suspend fun createElderly(
        @Header("Authorization") token: String,
        @Body request: CreateElderlyRequest
    ): Response<ElderlyDto>

    // ==================== 告警 ====================

    @GET("api/family/alarms/{elderlyId}")
    suspend fun getAlarms(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String,
        @Query("level") level: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<AlarmListResponse>

    @GET("api/family/alarm/{alarmId}")
    suspend fun getAlarmDetail(
        @Header("Authorization") token: String,
        @Path("alarmId") alarmId: String
    ): Response<AlarmDto>

    @POST("api/family/alarm/{alarmId}/acknowledge")
    suspend fun acknowledgeAlarm(
        @Header("Authorization") token: String,
        @Path("alarmId") alarmId: String
    ): Response<MessageResponse>

    @POST("api/alarm/simulate/trigger")
    suspend fun simulateAlarm(@Header("Authorization") token: String): Response<SimulateAlarmResponse>

    // ==================== 授权管理 ====================

    @GET("api/family/authorization/list/{elderlyId}")
    suspend fun getAuthorizations(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<AuthorizationListResponse>

    @POST("api/family/authorization/grant")
    suspend fun grantAuthorization(
        @Header("Authorization") token: String,
        @Body request: GrantAuthorizationRequest
    ): Response<MessageResponse>

    @POST("api/family/authorization/revoke/{authId}")
    suspend fun revokeAuthorization(
        @Header("Authorization") token: String,
        @Path("authId") authId: String,
        @Body request: RevokeAuthorizationRequest = RevokeAuthorizationRequest()
    ): Response<MessageResponse>

    @GET("api/family/authorization/requests")
    suspend fun getPendingAuthorizationRequests(
        @Header("Authorization") token: String
    ): Response<AuthorizationListResponse>

    @POST("api/family/authorization/requests/{requestId}/approve")
    suspend fun approveAuthorizationRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String
    ): Response<MessageResponse>

    @POST("api/family/authorization/requests/{requestId}/reject")
    suspend fun rejectAuthorizationRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String
    ): Response<MessageResponse>

    // ==================== 隐私控制 ====================

    @POST("api/family/monitoring/pause/{elderlyId}")
    suspend fun pauseMonitoring(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<MessageResponse>

    @POST("api/family/monitoring/resume/{elderlyId}")
    suspend fun resumeMonitoring(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<MessageResponse>

    @GET("api/family/privacy/status/{elderlyId}")
    suspend fun getPrivacyStatus(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<PrivacyStatusResponse>

    // ==================== 设备 ====================

    @GET("api/family/devices/{elderlyId}")
    suspend fun getDevices(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<DeviceListResponse>

    // ==================== 设备验证码（三端联动） ====================

    @POST("api/family/device/{deviceId}/generate-code")
    suspend fun generateDeviceCode(
        @Header("Authorization") token: String,
        @Path("deviceId") deviceId: String
    ): Response<GenerateCodeResponse>

    @GET("api/family/device/codes")
    suspend fun getActiveDeviceCodes(
        @Header("Authorization") token: String
    ): Response<VerificationCodeListResponse>

    @POST("api/family/device/code/{codeId}/revoke")
    suspend fun revokeDeviceCode(
        @Header("Authorization") token: String,
        @Path("codeId") codeId: String
    ): Response<MessageResponse>

    // ==================== 社区端 ====================

    @GET("api/community/dashboard")
    suspend fun getCommunityDashboard(@Header("Authorization") token: String): Response<CommunityDashboardResponse>

    @GET("api/community/elderly/list")
    suspend fun getCommunityElderlyList(@Header("Authorization") token: String): Response<ElderlyListResponse>

    @POST("api/community/authorization/request")
    suspend fun requestCommunityAuthorization(
        @Header("Authorization") token: String,
        @Body request: GrantAuthorizationRequest
    ): Response<AuthorizationRequestResponse>

    @GET("api/community/authorization/requests")
    suspend fun getCommunityAuthorizationRequests(
        @Header("Authorization") token: String
    ): Response<AuthorizationListResponse>

    // 社区端 — 设备绑定与巡检
    @POST("api/community/device/bind")
    suspend fun bindCommunityDevice(
        @Header("Authorization") token: String,
        @Body request: BindDeviceRequestDto
    ): Response<BindDeviceResponse>

    @GET("api/community/devices/{elderlyId}")
    suspend fun getCommunityDevices(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<DeviceListResponse>

    @POST("api/community/device/{deviceId}/inspection")
    suspend fun logDeviceInspection(
        @Header("Authorization") token: String,
        @Path("deviceId") deviceId: String,
        @Body request: InspectionRequest
    ): Response<InspectionMessageResponse>

    @GET("api/community/device/{deviceId}/maintenance")
    suspend fun getDeviceMaintenanceHistory(
        @Header("Authorization") token: String,
        @Path("deviceId") deviceId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<InspectionListResponse>

    @GET("api/community/maintenance/my")
    suspend fun getMyMaintenanceRecords(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<InspectionListResponse>

    // ==================== 医院端 ====================

    @GET("api/hospital/dashboard")
    suspend fun getHospitalDashboard(@Header("Authorization") token: String): Response<HospitalDashboardResponse>

    @GET("api/hospital/elderly/list")
    suspend fun getHospitalElderlyList(@Header("Authorization") token: String): Response<ElderlyListResponse>

    @POST("api/hospital/authorization/request")
    suspend fun requestHospitalAuthorization(
        @Header("Authorization") token: String,
        @Body request: GrantAuthorizationRequest
    ): Response<AuthorizationRequestResponse>

    @GET("api/hospital/authorization/requests")
    suspend fun getHospitalAuthorizationRequests(
        @Header("Authorization") token: String
    ): Response<AuthorizationListResponse>

    // 医院端 — 设备绑定
    @POST("api/hospital/device/bind")
    suspend fun bindHospitalDevice(
        @Header("Authorization") token: String,
        @Body request: BindDeviceRequestDto
    ): Response<BindDeviceResponse>

    @GET("api/hospital/devices/{elderlyId}")
    suspend fun getHospitalDevices(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<DeviceListResponse>

    // ==================== 资质验证（社区/医院 二次认证） ====================

    @GET("api/auth/verification/status")
    suspend fun getVerificationStatus(@Header("Authorization") token: String): Response<QualificationStatusResponse>

    @POST("api/auth/verification/apply")
    suspend fun submitVerification(
        @Header("Authorization") token: String,
        @Body request: QualificationApplyRequest
    ): Response<QualificationApplyResponse>

    // ==================== 工单 ====================

    @GET("api/work_order/my")
    suspend fun getMyWorkOrders(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<WorkOrderListResponse>

    @POST("api/work_order/{orderId}/accept")
    suspend fun acceptWorkOrder(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: String
    ): Response<WorkOrderMessageResponse>

    @POST("api/work_order/{orderId}/start")
    suspend fun startWorkOrder(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: String
    ): Response<WorkOrderMessageResponse>

    @POST("api/work_order/{orderId}/complete")
    suspend fun completeWorkOrder(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: String,
        @Body request: WorkOrderCompleteRequest
    ): Response<WorkOrderMessageResponse>

    // ==================== 健康档案（Phase 3） ====================

    @POST("api/hospital/health/{elderlyId}/add")
    suspend fun addHealthRecord(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String,
        @Body request: AddHealthRecordRequest
    ): Response<MessageResponse>

    @GET("api/hospital/health/{elderlyId}")
    suspend fun getHospitalHealthRecords(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<HealthRecordListResponse>

    @GET("api/family/health/{elderlyId}")
    suspend fun getFamilyHealthRecords(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<HealthRecordListResponse>

    // ==================== 急救权限（Phase 3） ====================

    @POST("api/hospital/emergency/request")
    suspend fun requestEmergencyAccess(
        @Header("Authorization") token: String,
        @Body request: EmergencyRequestDto
    ): Response<MessageResponse>

    @GET("api/hospital/emergency/status")
    suspend fun getEmergencyStatus(
        @Header("Authorization") token: String
    ): Response<EmergencyStatusResponse>
}
