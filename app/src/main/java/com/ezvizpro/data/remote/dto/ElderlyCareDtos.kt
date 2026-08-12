package com.ezvizpro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== 通用 ====================

@Serializable
data class MessageResponse(val message: String)

// ==================== 认证（手机验证码登录） ====================

@Serializable
data class SendCodeRequest(val phone: String)

@Serializable
data class LoginRequest(
    val phone: String,
    val code: String,
    val role: String
)

// ==================== 设备同步（萤石 AppKey 初始化） ====================

@Serializable
data class SyncRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("ezviz_access_token") val ezvizAccessToken: String? = null
)

@Serializable
data class SelectRoleRequest(
    @SerialName("client_id") val clientId: String,
    val role: String,
    @SerialName("real_name") val realName: String = "",
    val phone: String = ""
)

@Serializable
data class SyncResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserDto? = null,
    @SerialName("need_select_role") val needSelectRole: Boolean = false
)

@Serializable
data class UserDto(
    val id: String,
    @SerialName("client_id") val clientId: String = "",
    @SerialName("real_name") val realName: String,
    val phone: String,
    val role: String,
    @SerialName("institution_id") val institutionId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
)

// ==================== 老人档案 ====================

@Serializable
data class ElderlyDto(
    val id: String,
    val name: String,
    val gender: String = "",
    @SerialName("birth_date") val birthDate: String = "",
    @SerialName("id_card") val idCard: String = "",
    val phone: String = "",
    val address: String = "",
    @SerialName("emergency_contact") val emergencyContact: String = "",
    @SerialName("medical_history") val medicalHistory: String = "",
    @SerialName("care_level") val careLevel: String = "自理",
    @SerialName("privacy_paused") val privacyPaused: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ElderlyListResponse(val total: Int, val items: List<ElderlyDto>)

@Serializable
data class CreateElderlyRequest(
    val name: String,
    val gender: String? = "",
    @SerialName("birth_date") val birthDate: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("medical_history") val medicalHistory: String? = null,
    @SerialName("care_level") val careLevel: String = "自理"
)

// ==================== 告警 ====================

@Serializable
data class AlarmDto(
    val id: String,
    @SerialName("elderly_id") val elderlyId: String,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("alarm_type") val alarmType: String,
    @SerialName("alarm_level") val alarmLevel: String,
    @SerialName("ai_score") val aiScore: Double? = null,
    @SerialName("ai_verified") val aiVerified: Int = 0,
    @SerialName("snapshot_url") val snapshotUrl: String? = null,
    @SerialName("video_clip_url") val videoClipUrl: String? = null,
    val title: String,
    val description: String = "",
    val status: String = "active",
    @SerialName("push_family") val pushFamily: Boolean = true,
    @SerialName("push_community") val pushCommunity: Boolean = false,
    @SerialName("push_hospital") val pushHospital: Boolean = false,
    @SerialName("related_work_order_id") val relatedWorkOrderId: String? = null,
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("resolution_note") val resolutionNote: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class AlarmListResponse(val total: Int, val items: List<AlarmDto>)

@Serializable
data class SimulateAlarmResponse(val message: String, val alarm: AlarmDto)

// ==================== 授权 ====================

@Serializable
data class AuthorizationDto(
    val id: String,
    @SerialName("elderly_id") val elderlyId: String,
    @SerialName("grantor_user_id") val grantorUserId: String,
    @SerialName("grantee_user_id") val granteeUserId: String,
    @SerialName("permission_type") val permissionType: String,
    @SerialName("data_scope") val dataScope: String,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("effective_until") val effectiveUntil: String,
    val status: String,
    @SerialName("grantee_name") val granteeName: String = "",
    @SerialName("grantee_phone") val granteePhone: String = "",
    @SerialName("institution_name") val institutionName: String = "",
    // 用于授权申请列表（含老人名、时间等）
    @SerialName("elderly_name") val elderlyName: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class AuthorizationListResponse(val total: Int, val items: List<AuthorizationDto>)

@Serializable
data class GrantAuthorizationRequest(
    @SerialName("elderly_id") val elderlyId: String,
    @SerialName("grantee_user_id") val granteeUserId: String,
    @SerialName("permission_type") val permissionType: String,
    @SerialName("data_scope") val dataScope: String,
    @SerialName("effective_until") val effectiveUntil: String = ""
)

@Serializable
data class RevokeAuthorizationRequest(@SerialName("revoke_reason") val revokeReason: String? = null)

// ==================== 授权申请 ====================

@Serializable
data class AuthorizationRequestResponse(
    val message: String = "",
    @SerialName("authorization_id") val authorizationId: String = "",
    val status: String = ""
)

// ==================== 隐私控制 ====================

@Serializable
data class PrivacyStatusResponse(
    @SerialName("elderly_id") val elderlyId: String,
    @SerialName("privacy_paused") val privacyPaused: Boolean
)

// ==================== 设备 ====================

@Serializable
data class DeviceDto(
    val id: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_type") val deviceType: String,
    @SerialName("elderly_id") val elderlyId: String,
    val location: String = "",
    val status: String = "online",
    @SerialName("stream_url") val streamUrl: String? = null
)

@Serializable
data class DeviceListResponse(val total: Int, val items: List<DeviceDto>)

// ==================== 资质验证（社区/医院 二次认证） ====================

@Serializable
data class QualificationApplyRequest(
    @SerialName("applicant_user_id") val applicantUserId: String,
    @SerialName("institution_name") val institutionName: String = "",
    @SerialName("institution_type") val institutionType: String,
    @SerialName("document_urls") val documentUrls: String = ""
)

@Serializable
data class QualificationApplyResponse(
    val message: String = "",
    @SerialName("review_id") val reviewId: String = "",
    val status: String = "",
    @SerialName("ai_note") val aiNote: String? = null
)

@Serializable
data class QualificationStatusResponse(
    @SerialName("qualification_status") val qualificationStatus: String,
    @SerialName("review_note") val reviewNote: String? = null,
    @SerialName("valid_until") val validUntil: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
)

// ==================== 健康档案（Phase 3） ====================

@Serializable
data class HealthRecordDto(
    val id: String,
    @SerialName("elderly_id") val elderlyId: String,
    @SerialName("record_type") val recordType: String,
    @SerialName("record_date") val recordDate: String,
    @SerialName("doctor_name") val doctorName: String = "",
    @SerialName("hospital_name") val hospitalName: String = "",
    @SerialName("content_json") val contentJson: String,
    @SerialName("attachment_urls") val attachmentUrls: String? = null,
    val visibility: String = "family",
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class HealthRecordListResponse(val total: Int, val items: List<HealthRecordDto>)

@Serializable
data class AddHealthRecordRequest(
    @SerialName("record_type") val recordType: String,
    @SerialName("record_date") val recordDate: String,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("content_json") val contentJson: String,
    @SerialName("attachment_urls") val attachmentUrls: String? = null,
    val visibility: String = "family"
)

// ==================== 急救权限（Phase 3） ====================

@Serializable
data class EmergencyRequestDto(
    @SerialName("elderly_id") val elderlyId: String,
    val reason: String
)

@Serializable
data class EmergencyStatusResponse(
    val active: Boolean,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("elderly_name") val elderlyName: String = "",
    @SerialName("elderly_id") val elderlyId: String = "",
    @SerialName("authorization_id") val authorizationId: String = ""
)
