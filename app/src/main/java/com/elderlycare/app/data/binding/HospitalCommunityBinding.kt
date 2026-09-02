package com.elderlycare.app.data.binding

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 医院-社区绑定关系（多对多，管理端审批）。数据库 elderly_binding.db，表 hospital_community_binding。
 *
 * 医院不再直接绑定老人，而是申请绑定社区；ACTIVE 后医院可见该社区下已授权给社区的老人。
 * 一家医院可绑多个社区，一个社区可对接多家医院。
 */
@Entity(
    tableName = "hospital_community_binding",
    indices = [
        Index("hospitalOrgId"),
        Index("communityOrgId"),
        Index(value = ["hospitalOrgId", "communityOrgId"], unique = true)
    ]
)
data class HospitalCommunityBindingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hospitalOrgId: String,
    val communityOrgId: String,
    /** HcBindingStatus：PENDING / ACTIVE / REVOKED / REJECTED */
    val status: String = HcBindingStatus.PENDING.name,
    val applyReason: String = "",
    /** 审批人（管理端账号 id） */
    val reviewedBy: String? = null,
    val reviewNote: String = "",
    val createdAt: Long = 0L,
    val reviewedAt: Long? = null
)

/** 医院-社区绑定状态 */
enum class HcBindingStatus(val label: String) {
    PENDING("待审批"),
    ACTIVE("已绑定"),
    REVOKED("已解除"),
    REJECTED("已驳回")
}
