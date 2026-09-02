package com.elderlycare.app.data.hospital

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 医疗随访记录 DAO（表 medical_follow_up_record） */
@Dao
interface MedicalFollowUpDao {

    /** 插入一条随访记录，返回自增 id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicalFollowUpRecord): Long

    /** 按老人观察随访记录（时间倒序，Flow 实时刷新） */
    @Query(
        "SELECT * FROM medical_follow_up_record WHERE elderlyId = :elderlyId " +
            "ORDER BY followUpTime DESC"
    )
    fun observeByElderlyId(elderlyId: String): Flow<List<MedicalFollowUpRecord>>

    /** 全部随访记录（时间倒序，医院端「全部随访」入口用） */
    @Query("SELECT * FROM medical_follow_up_record ORDER BY followUpTime DESC")
    fun observeAll(): Flow<List<MedicalFollowUpRecord>>

    /** 按老人 + 时间范围一次性查询（startInclusive/endInclusive 毫秒时间戳） */
    @Query(
        "SELECT * FROM medical_follow_up_record WHERE elderlyId = :elderlyId " +
            "AND followUpTime >= :startInclusive AND followUpTime <= :endInclusive " +
            "ORDER BY followUpTime DESC"
    )
    suspend fun getByElderlyIdAndTimeRange(
        elderlyId: String,
        startInclusive: Long,
        endInclusive: Long
    ): List<MedicalFollowUpRecord>

    /** 按老人统计随访次数（报告页聚合用） */
    @Query("SELECT COUNT(*) FROM medical_follow_up_record WHERE elderlyId = :elderlyId")
    fun observeCountByElderlyId(elderlyId: String): Flow<Int>
}
