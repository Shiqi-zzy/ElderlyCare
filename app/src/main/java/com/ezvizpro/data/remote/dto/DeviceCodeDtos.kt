package com.ezvizpro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== 设备验证码 ====================

@Serializable
data class GenerateCodeResponse(
    @SerialName("code_id") val codeId: String,
    val code: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("elderly_id") val elderlyId: String,
    @SerialName("elderly_name") val elderlyName: String = "",
    @SerialName("device_name") val deviceName: String = ""
)

@Serializable
data class BindDeviceRequestDto(val code: String)

@Serializable
data class BindDeviceResponse(
    val message: String = "",
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("elderly_id") val elderlyId: String = "",
    @SerialName("elderly_name") val elderlyName: String = "",
    @SerialName("authorization_id") val authorizationId: String = "",
    @SerialName("already_bound") val alreadyBound: Boolean = false
)

@Serializable
data class VerificationCodeItem(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("elderly_id") val elderlyId: String,
    val code: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("use_count") val useCount: Int = 0,
    @SerialName("max_uses") val maxUses: Int = 1,
    val status: String = "active",
    @SerialName("device_name") val deviceName: String = "",
    @SerialName("elderly_name") val elderlyName: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class VerificationCodeListResponse(val total: Int, val items: List<VerificationCodeItem>)

// ==================== 设备巡检 ====================

@Serializable
data class InspectionRequest(
    @SerialName("maintenance_type") val maintenanceType: String,
    val status: String,
    val findings: String = "",
    val photos: String = "",
    @SerialName("next_inspection_date") val nextInspectionDate: String = ""
)

@Serializable
data class InspectionDto(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("maintenance_type") val maintenanceType: String,
    @SerialName("inspector_id") val inspectorId: String = "",
    @SerialName("inspector_name") val inspectorName: String = "",
    @SerialName("inspection_date") val inspectionDate: String,
    val status: String,
    val findings: String = "",
    val photos: String = "",
    @SerialName("next_inspection_date") val nextInspectionDate: String = "",
    @SerialName("device_name") val deviceName: String = "",
    @SerialName("device_type") val deviceType: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class InspectionListResponse(val total: Int, val items: List<InspectionDto>)

@Serializable
data class InspectionMessageResponse(val message: String, @SerialName("inspection_id") val inspectionId: String = "")
