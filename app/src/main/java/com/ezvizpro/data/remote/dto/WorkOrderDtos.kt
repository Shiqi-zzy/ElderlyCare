package com.ezvizpro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkOrderDto(
    val id: String,
    @SerialName("alarm_id") val alarmId: String? = null,
    @SerialName("elderly_id") val elderlyId: String,
    @SerialName("order_type") val orderType: String,
    val title: String,
    val description: String = "",
    val priority: String = "normal",
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("assigned_institution_id") val assignedInstitutionId: String? = null,
    val status: String = "pending",
    @SerialName("result_json") val resultJson: String? = null,
    @SerialName("result_photos") val resultPhotos: String? = null,
    val deadline: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class WorkOrderListResponse(
    val total: Int,
    val items: List<WorkOrderDto>
)

@Serializable
data class WorkOrderCompleteRequest(
    @SerialName("result_json") val resultJson: String? = null,
    @SerialName("result_photos") val resultPhotos: String? = null
)

@Serializable
data class WorkOrderMessageResponse(val message: String)

// ==================== 社区端 ====================

@Serializable
data class CommunityDashboardResponse(
    @SerialName("pending_work_orders") val pendingWorkOrders: Int = 0,
    val message: String = ""
)

// ==================== 医院端 ====================

@Serializable
data class HospitalDashboardResponse(
    @SerialName("bound_elderly_count") val boundElderlyCount: Int = 0,
    val message: String = ""
)
