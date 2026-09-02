package com.elderlycare.app.data.community

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 工作台待办事项实体（Room 表 todo_item）。完成后延迟自动消失，服务记录永久保留。 */
@Entity(
    tableName = "todo_item",
    indices = [Index(value = ["staffId"]), Index(value = ["elderlyId"]), Index(value = ["status"])]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: String,
    val elderlyId: String,
    val elderlyName: String,
    val todoType: String,
    val title: String,
    val content: String,
    val priority: String = PRIORITY_NORMAL,
    val status: String = STATUS_PENDING,
    val createdAt: Long,
    val completedAt: Long? = null,
    /** 关联事件 id（跌倒事件待办用） */
    val incidentId: Long? = null,
    /** 待办子类型（如 FALL_DISPATCH） */
    val todoSubType: String = ""
) {
    companion object {
        const val STATUS_PENDING = "待处理"
        const val STATUS_DONE = "已完成"
        const val PRIORITY_HIGH = "高"
        const val PRIORITY_NORMAL = "中"
        const val PRIORITY_LOW = "低"
    }
}
