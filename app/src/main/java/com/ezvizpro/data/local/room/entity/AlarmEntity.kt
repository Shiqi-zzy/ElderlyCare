package com.ezvizpro.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 告警 Room 实体（本地缓存最近的告警记录，支持离线查看）
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "elderly_id") val elderlyId: String,
    @ColumnInfo(name = "alarm_type") val alarmType: String,
    @ColumnInfo(name = "alarm_level") val alarmLevel: String,
    @ColumnInfo(name = "ai_score") val aiScore: Double? = null,
    @ColumnInfo(name = "ai_verified") val aiVerified: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "status") val status: String = "active",
    @ColumnInfo(name = "created_at") val createdAt: String = "",
    @ColumnInfo(name = "acknowledged_at") val acknowledgedAt: String? = null,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)
