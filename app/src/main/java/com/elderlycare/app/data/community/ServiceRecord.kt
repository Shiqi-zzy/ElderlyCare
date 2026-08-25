package com.elderlycare.app.data.community

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 服务记录实体（Room 表 service_record，永久保留）。随访完成、告警处理完成等自动写入。 */
@Entity(
    tableName = "service_record",
    indices = [Index(value = ["staffId"]), Index(value = ["elderlyId"]), Index(value = ["createdAt"])]
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: String,
    val elderlyId: String,
    val elderlyName: String,
    val serviceType: String,
    val content: String,
    val durationMinutes: Int = 0,
    val createdAt: Long
)
