package com.elderlycare.app.data.community

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 工作人员排班记录实体（Room 表 staff_schedule）。 */
@Entity(
    tableName = "staff_schedule",
    indices = [Index(value = ["staffId"]), Index(value = ["scheduleDate"])]
)
data class StaffScheduleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: String,
    val title: String,
    val scheduleDate: Long,
    val startTime: String,
    val endTime: String,
    val location: String,
    val status: String = STATUS_PENDING,
    val createdAt: Long
) {
    companion object {
        const val STATUS_PENDING = "待执行"
        const val STATUS_DONE = "已完成"
        const val STATUS_CANCELLED = "已取消"
    }
}
