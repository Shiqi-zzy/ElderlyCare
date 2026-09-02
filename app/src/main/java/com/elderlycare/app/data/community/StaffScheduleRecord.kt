package com.elderlycare.app.data.community

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 工作人员排班记录实体（Room 表 staff_schedule，社区/医院共用，role 区分）。 */
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
    val createdAt: Long,
    /** 排班模式：0=按周循环（看 weekday）/ 1=指定日期（看 scheduleDate） */
    val scheduleMode: Int = 0,
    /** 周循环时的星期 1..7（周一..周日），指定日期为 -1 */
    val weekday: Int = -1,
    /** 角色：community / hospital */
    val role: String = "community"
) {
    companion object {
        const val STATUS_PENDING = "待执行"
        const val STATUS_DONE = "已完成"
        const val STATUS_CANCELLED = "已取消"
    }
}
