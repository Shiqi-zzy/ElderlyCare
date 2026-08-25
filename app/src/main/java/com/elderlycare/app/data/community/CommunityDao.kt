package com.elderlycare.app.data.community

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 社区端4张表的统一 DAO。 */
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

    @Query("UPDATE community_follow_up SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateFollowUpStatus(id: Long, status: String, completedAt: Long?)

    // ==================== 排班 ====================
    @Insert
    suspend fun insertSchedule(record: StaffScheduleRecord): Long

    @Update
    suspend fun updateSchedule(record: StaffScheduleRecord)

    @Query("SELECT * FROM staff_schedule WHERE staffId = :staffId ORDER BY scheduleDate ASC")
    fun observeSchedules(staffId: String): Flow<List<StaffScheduleRecord>>

    @Query("UPDATE staff_schedule SET status = :status WHERE id = :id")
    suspend fun updateScheduleStatus(id: Long, status: String)

    // ==================== 服务记录 ====================
    @Insert
    suspend fun insertServiceRecord(record: ServiceRecord): Long

    @Query("SELECT * FROM service_record WHERE staffId = :staffId ORDER BY createdAt DESC")
    fun observeServiceRecords(staffId: String): Flow<List<ServiceRecord>>

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

    @Query("UPDATE todo_item SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTodoStatus(id: Long, status: String, completedAt: Long?)

    @Query("DELETE FROM todo_item WHERE id = :id")
    suspend fun deleteTodo(id: Long)
}
