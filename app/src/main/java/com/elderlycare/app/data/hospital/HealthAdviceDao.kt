package com.elderlycare.app.data.hospital

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 健康建议 DAO（表 health_advice） */
@Dao
interface HealthAdviceDao {

    /** 插入一条健康建议，返回自增 id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(advice: HealthAdvice): Long

    /** 按老人观察建议列表（时间倒序，Flow 实时刷新） */
    @Query("SELECT * FROM health_advice WHERE elderlyId = :elderlyId ORDER BY adviceTime DESC")
    fun observeByElderlyId(elderlyId: String): Flow<List<HealthAdvice>>

    /** 按老人一次性查询（时间倒序） */
    @Query("SELECT * FROM health_advice WHERE elderlyId = :elderlyId ORDER BY adviceTime DESC")
    suspend fun getByElderlyId(elderlyId: String): List<HealthAdvice>

    /** 按老人统计建议条数（报告页聚合用） */
    @Query("SELECT COUNT(*) FROM health_advice WHERE elderlyId = :elderlyId")
    fun observeCountByElderlyId(elderlyId: String): Flow<Int>
}
