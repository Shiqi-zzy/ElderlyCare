package com.elderlycare.app.data.hospital

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 医疗随访记录实体（Room 表 medical_follow_up_record，医院端录入）。
 *
 * 数据性质：全部为医护人员人工录入（RK3 硬件无法产出随访数据）；
 * 单机演示阶段仅本地 Room 存储，不做跨端后端同步。
 * 字段语义：
 * - elderlyId：老人档案 userId（= 家属手机号，与 user_elderly_binding.elderlyId 对齐）
 * - followUpTime：随访时间戳（毫秒，录入时间）
 * - content：随访内容（多行文本）
 * - status：随访状态（待处理 / 已完成，String 存储业务文案）
 */
@Entity(
    tableName = "medical_follow_up_record",
    indices = [Index(value = ["elderlyId"]), Index(value = ["followUpTime"])]
)
data class MedicalFollowUpRecord(
    /** 本地主键自增 */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 老人档案 userId（家属手机号） */
    val elderlyId: String,

    /** 随访时间戳（毫秒） */
    val followUpTime: Long,

    /** 随访内容 */
    val content: String,

    /** 随访状态（待处理 / 已完成） */
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "待处理"
        const val STATUS_DONE = "已完成"
    }
}
