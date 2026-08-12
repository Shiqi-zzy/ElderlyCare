package com.ezvizpro.data.local.room.dao

import androidx.room.*
import com.ezvizpro.data.local.room.entity.ElderlyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ElderlyDao {

    @Query("SELECT * FROM elderly ORDER BY cached_at DESC")
    fun getAllElderly(): Flow<List<ElderlyEntity>>

    @Query("SELECT * FROM elderly WHERE id = :elderlyId")
    suspend fun getById(elderlyId: String): ElderlyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(elderly: List<ElderlyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(elderly: ElderlyEntity)

    @Query("DELETE FROM elderly")
    suspend fun deleteAll()
}
