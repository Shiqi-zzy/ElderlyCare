package com.ezvizpro.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 老人档案 Room 实体（本地缓存家属绑定的老人信息）
 */
@Entity(tableName = "elderly")
data class ElderlyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "gender") val gender: String = "",
    @ColumnInfo(name = "birth_date") val birthDate: String = "",
    @ColumnInfo(name = "phone") val phone: String = "",
    @ColumnInfo(name = "address") val address: String = "",
    @ColumnInfo(name = "emergency_contact") val emergencyContact: String = "",
    @ColumnInfo(name = "care_level") val careLevel: String = "自理",
    @ColumnInfo(name = "privacy_paused") val privacyPaused: Boolean = false,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)
