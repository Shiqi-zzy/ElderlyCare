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
    val createdAt: Long,
    /** 处置方：community / hospital（普通随访为空） */
    val side: String = "",
    /** 关联事件 id（事件处置双写服务记录用） */
    val incidentId: Long? = null,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    /** 处置人姓名（事件记录用） */
    val staffName: String = "",
    /** 医院处置措施（事件记录用） */
    val treatment: String = ""
)
