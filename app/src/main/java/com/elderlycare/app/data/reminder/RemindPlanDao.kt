package com.elderlycare.app.data.reminder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 提醒计划 DAO（表 remind_plan） */
@Dao
interface RemindPlanDao {

    /** 覆盖式同步用：批量插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<RemindPlanEntity>)

    /**
     * 差分同步：设备侧字段更新到本地行。
     * 保留 id / clockId / executed / enabled / createTime——Room REPLACE 会换自增 id，
     * 导致详情页持有的本地 id 失效，故同步必须走 UPDATE 而不是删了重插。
     */
    @Query(
        "UPDATE remind_plan SET tag = :tag, content = :content, timeHour = :timeHour, " +
            "timeMin = :timeMin, repeatType = :repeatType, weekdays = :weekdays, " +
            "year = :year, month = :month, day = :day WHERE id = :id"
    )
    suspend fun updateFromDevice(
        id: Long,
        tag: String,
        content: String,
        timeHour: Int,
        timeMin: Int,
        repeatType: Int,
        weekdays: String,
        year: Int,
        month: Int,
        day: Int
    )

    /** 单条插入，返回本地 id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: RemindPlanEntity): Long

    /** 按设备观察计划列表（创建时间倒序，Flow 实时刷新） */
    @Query("SELECT * FROM remind_plan WHERE deviceSerial = :deviceSerial ORDER BY createTime DESC")
    fun observeByDeviceSerial(deviceSerial: String): Flow<List<RemindPlanEntity>>

    /** 按本地 id 观察单条（详情页用；删除后发 null） */
    @Query("SELECT * FROM remind_plan WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<RemindPlanEntity?>

    /** 按设备一次性查询（轮询匹配用） */
    @Query("SELECT * FROM remind_plan WHERE deviceSerial = :deviceSerial")
    suspend fun getAllByDeviceSerial(deviceSerial: String): List<RemindPlanEntity>

    /** 按 clockId 查询 */
    @Query("SELECT * FROM remind_plan WHERE clockId = :clockId LIMIT 1")
    suspend fun getByClockId(clockId: String): RemindPlanEntity?

    /** 标记已播报完成 */
    @Query("UPDATE remind_plan SET executed = 1 WHERE id = :id")
    suspend fun markExecuted(id: Long)

    /** 删除单条 */
    @Delete
    suspend fun delete(plan: RemindPlanEntity)

    /** 删除设备下全部计划（覆盖式同步前清空） */
    @Query("DELETE FROM remind_plan WHERE deviceSerial = :deviceSerial")
    suspend fun deleteByDeviceSerial(deviceSerial: String)
}
