package com.elderlycare.app.data.community

import com.elderlycare.app.data.message.MessageRepository
import kotlinx.coroutines.flow.Flow

/**
 * 社区端业务仓库：随访、排班、服务记录、待办事项。
 * 完成随访/告警处理时自动写入服务记录，并向家属端发送消息通知。
 */
class CommunityRepository(
    private val dao: CommunityDao,
    private val messageRepository: MessageRepository
) {

    // ==================== 随访 ====================

    fun observeFollowUps(staffId: String): Flow<List<CommunityFollowUpRecord>> =
        dao.observeFollowUps(staffId)

    suspend fun createFollowUp(
        staffId: String,
        elderlyId: String,
        elderlyName: String,
        followUpType: String,
        scheduledTime: Long,
        content: String
    ): Long {
        val now = System.currentTimeMillis()
        val id = dao.insertFollowUp(
            CommunityFollowUpRecord(
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                staffId = staffId,
                followUpType = followUpType,
                scheduledTime = scheduledTime,
                content = content,
                createdAt = now
            )
        )
        // 同步生成待办事项
        dao.insertTodo(
            TodoItem(
                staffId = staffId,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                todoType = followUpType,
                title = "$followUpType - $elderlyName",
                content = content,
                priority = if (followUpType == "上门随访") TodoItem.PRIORITY_HIGH else TodoItem.PRIORITY_NORMAL,
                createdAt = now
            )
        )
        // 向家属端发送通知消息（TODO：后续接入 MessageRepository 系统消息）
        // runCatching {
        //     messageRepository.sendSystemMessage(...)
        // }
        return id
    }

    suspend fun completeFollowUp(id: Long, staffId: String, elderlyId: String, elderlyName: String, followUpType: String, content: String) {
        val now = System.currentTimeMillis()
        dao.updateFollowUpStatus(id, CommunityFollowUpRecord.STATUS_DONE, now)
        // 写入服务记录
        dao.insertServiceRecord(
            ServiceRecord(
                staffId = staffId,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                serviceType = followUpType,
                content = content,
                durationMinutes = 30,
                createdAt = now
            )
        )
        // 同步完成对应的待办事项（按老人+类型匹配，创建随访时生成的待办）
        val relatedTodos = dao.getTodosByElderlyAndType(elderlyId, followUpType, TodoItem.STATUS_PENDING)
        relatedTodos.forEach { todo ->
            dao.updateTodoStatus(todo.id, TodoItem.STATUS_DONE, now)
        }
    }

    // ==================== 排班 ====================

    fun observeSchedules(staffId: String): Flow<List<StaffScheduleRecord>> =
        dao.observeSchedules(staffId)

    suspend fun createSchedule(
        staffId: String,
        title: String,
        scheduleDate: Long,
        startTime: String,
        endTime: String,
        location: String
    ): Long = dao.insertSchedule(
        StaffScheduleRecord(
            staffId = staffId,
            title = title,
            scheduleDate = scheduleDate,
            startTime = startTime,
            endTime = endTime,
            location = location,
            createdAt = System.currentTimeMillis()
        )
    )

    suspend fun completeSchedule(id: Long) {
        dao.updateScheduleStatus(id, StaffScheduleRecord.STATUS_DONE)
    }

    // ==================== 服务记录 ====================

    fun observeServiceRecords(staffId: String): Flow<List<ServiceRecord>> =
        dao.observeServiceRecords(staffId)

    suspend fun countServiceRecords(staffId: String): Int =
        dao.countServiceRecords(staffId)

    // ==================== 待办事项 ====================

    fun observePendingTodos(staffId: String): Flow<List<TodoItem>> =
        dao.observeTodosByStatus(staffId, TodoItem.STATUS_PENDING)

    fun observeAllTodos(staffId: String): Flow<List<TodoItem>> =
        dao.observeAllTodos(staffId)

    suspend fun completeTodo(id: Long, staffId: String, elderlyId: String, elderlyName: String, todoType: String, content: String) {
        val now = System.currentTimeMillis()
        dao.updateTodoStatus(id, TodoItem.STATUS_DONE, now)

        val isFollowUp = todoType == "上门随访" || todoType == "健康随访" || todoType == "电话随访"

        if (isFollowUp) {
            // 随访类型：同步完成对应的随访记录（与随访计划页完成效果一致）
            val relatedFollowUps = dao.getFollowUpsByElderlyAndType(elderlyId, todoType, CommunityFollowUpRecord.STATUS_PENDING)
            relatedFollowUps.forEach { fu ->
                dao.updateFollowUpStatus(fu.id, CommunityFollowUpRecord.STATUS_DONE, now)
            }
            // 写入服务记录（随访30分钟）
            dao.insertServiceRecord(
                ServiceRecord(
                    staffId = staffId,
                    elderlyId = elderlyId,
                    elderlyName = elderlyName,
                    serviceType = todoType,
                    content = content,
                    durationMinutes = 30,
                    createdAt = now
                )
            )
        } else {
            // 非随访类型（如告警消息）：写入服务记录（15分钟）
            dao.insertServiceRecord(
                ServiceRecord(
                    staffId = staffId,
                    elderlyId = elderlyId,
                    elderlyName = elderlyName,
                    serviceType = todoType,
                    content = content,
                    durationMinutes = 15,
                    createdAt = now
                )
            )
        }
    }

    suspend fun deleteTodo(id: Long) {
        dao.deleteTodo(id)
    }

    /** 从告警消息生成待办事项 */
    suspend fun createTodoFromAlarm(
        staffId: String,
        elderlyId: String,
        elderlyName: String,
        title: String,
        content: String
    ): Long = dao.insertTodo(
        TodoItem(
            staffId = staffId,
            elderlyId = elderlyId,
            elderlyName = elderlyName,
            todoType = "告警消息",
            title = title,
            content = content,
            priority = TodoItem.PRIORITY_HIGH,
            createdAt = System.currentTimeMillis()
        )
    )
}
