package com.elderlycare.app.data.community

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 社区端/医院端共用表的统一 DAO（随访、排班、服务记录、待办）。 */
@Dao
interface CommunityDao {

    // ==================== 随访记录 ====================
    @Insert
    suspend fun insertFollowUp(record: CommunityFollowUpRecord): Long

    @Update
    suspend fun updateFollowUp(record: CommunityFollowUpRecord)

    @Query("SELECT * FROM community_follow_up WHERE staffId = :staffId ORDER BY scheduledTime DESC")
    fun observeFollowUps(staffId: String): Flow<List<CommunityFollowUpRecord>>

    @Query("SELECT * FROM community_follow_up WHERE staffId = :staffId AND status = :status ORDER BY scheduledTime DESC")
    suspend fun getFollowUpsByStatus(staffId: String, status: String): List<CommunityFollowUpRecord>

    /** 按老人+类型+状态查随访记录（待办⇄随访双向联动用） */
    @Query("SELECT * FROM community_follow_up WHERE elderlyId = :elderlyId AND followUpType = :followUpType AND status = :status ORDER BY createdAt DESC")
    suspend fun getFollowUpsByElderlyAndType(elderlyId: String, followUpType: String, status: String): List<CommunityFollowUpRecord>

    @Query("UPDATE community_follow_up SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateFollowUpStatus(id: Long, status: String, completedAt: Long?)

    // ==================== 排班 ====================
    @Insert
    suspend fun insertSchedule(record: StaffScheduleRecord): Long

    @Update
    suspend fun updateSchedule(record: StaffScheduleRecord)

    @Query("SELECT * FROM staff_schedule WHERE staffId = :staffId ORDER BY scheduleDate ASC")
    fun observeSchedules(staffId: String): Flow<List<StaffScheduleRecord>>

    /** 某员工按角色观察排班（医生/社区分开） */
    @Query("SELECT * FROM staff_schedule WHERE staffId = :staffId AND role = :role ORDER BY scheduleDate ASC")
    fun observeSchedulesByRole(staffId: String, role: String): Flow<List<StaffScheduleRecord>>

    /** 某角色全部排班（在班判定一次性读取） */
    @Query("SELECT * FROM staff_schedule WHERE role = :role")
    suspend fun getAllSchedulesByRole(role: String): List<StaffScheduleRecord>

    @Query("UPDATE staff_schedule SET status = :status WHERE id = :id")
    suspend fun updateScheduleStatus(id: Long, status: String)

    // ==================== 服务记录 ====================
    @Insert
    suspend fun insertServiceRecord(record: ServiceRecord): Long

    @Query("SELECT * FROM service_record WHERE staffId = :staffId ORDER BY createdAt DESC")
    fun observeServiceRecords(staffId: String): Flow<List<ServiceRecord>>

    /** 某老人的服务记录（家属"我的社区/医院"时间线用） */
    @Query("SELECT * FROM service_record WHERE elderlyId = :elderlyId ORDER BY createdAt DESC")
    fun observeServiceRecordsByElderly(elderlyId: String): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_record WHERE elderlyId = :elderlyId ORDER BY createdAt DESC")
    suspend fun getServiceRecordsByElderly(elderlyId: String): List<ServiceRecord>

    @Query("SELECT COUNT(*) FROM service_record WHERE staffId = :staffId")
    suspend fun countServiceRecords(staffId: String): Int

    // ==================== 待办事项 ====================
    @Insert
    suspend fun insertTodo(item: TodoItem): Long

    @Update
    suspend fun updateTodo(item: TodoItem)

    @Query("SELECT * FROM todo_item WHERE staffId = :staffId AND status = :status ORDER BY createdAt DESC")
    fun observeTodosByStatus(staffId: String, status: String): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_item WHERE staffId = :staffId ORDER BY createdAt DESC")
    fun observeAllTodos(staffId: String): Flow<List<TodoItem>>

    /** 按老人+类型+状态查待办（随访完成时联动关闭对应待办用） */
    @Query("SELECT * FROM todo_item WHERE elderlyId = :elderlyId AND todoType = :todoType AND status = :status ORDER BY createdAt DESC")
    suspend fun getTodosByElderlyAndType(elderlyId: String, todoType: String, status: String): List<TodoItem>

    /** 按事件 id 查待办（事件闭环时联动完成） */
    @Query("SELECT * FROM todo_item WHERE incidentId = :incidentId AND status = :status")
    suspend fun getTodosByIncident(incidentId: Long, status: String): List<TodoItem>

    @Query("UPDATE todo_item SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTodoStatus(id: Long, status: String, completedAt: Long?)

    @Query("DELETE FROM todo_item WHERE id = :id")
    suspend fun deleteTodo(id: Long)
}
