package com.elderlycare.app.data.community

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 社区随访记录实体（Room 表 community_follow_up）。
 * 社区工作人员创建的随访计划，完成后写入服务记录并通知家属。
 */
@Entity(
    tableName = "community_follow_up",
    indices = [Index(value = ["elderlyId"]), Index(value = ["staffId"]), Index(value = ["scheduledTime"])]
)
data class CommunityFollowUpRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val elderlyId: String,
    val elderlyName: String,
    val staffId: String,
    val followUpType: String,
    val scheduledTime: Long,
    val content: String,
    val status: String = STATUS_PENDING,
    val createdAt: Long,
    val completedAt: Long? = null
) {
    companion object {
        const val STATUS_PENDING = "待处理"
        const val STATUS_DONE = "已完成"
        const val STATUS_CANCELLED = "已取消"
    }
}
