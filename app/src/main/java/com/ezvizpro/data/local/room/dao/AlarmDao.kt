package com.ezvizpro.data.local.room.dao

import androidx.room.*
import com.ezvizpro.data.local.room.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms WHERE elderly_id = :elderlyId ORDER BY created_at DESC")
    fun getAlarmsByElderly(elderlyId: String): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY created_at DESC LIMIT :limit")
    fun getRecentAlarms(limit: Int = 50): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<AlarmEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity)

    @Query("UPDATE alarms SET status = :status WHERE id = :alarmId")
    suspend fun updateStatus(alarmId: String, status: String)

    @Query("DELETE FROM alarms WHERE cached_at < :before")
    suspend fun deleteOlderThan(before: Long)
}
