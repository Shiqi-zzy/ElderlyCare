package com.elderlycare.app.data.hospital

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 健康建议实体（Room 表 health_advice，医院端医护录入）。
 *
 * 约束：健康建议**不走萤石设备播报**，仅在 App 消息模块（家属端留言页/消息中心）
 * 以独立消息气泡查看——提交时除插入本表外，同步插一条 message 表消息
 * （msgType = MSG_TYPE_ADVICE），由 MessageRepository.saveHealthAdviceMessage 完成。
 *
 * 字段语义：
 * - elderlyId：老人档案 userId（= 家属手机号）
 * - adviceTime：建议时间戳（毫秒，录入时间）
 * - adviceContent：建议内容（多行文本）
 */
@Entity(
    tableName = "health_advice",
    indices = [Index(value = ["elderlyId"]), Index(value = ["adviceTime"])]
)
data class HealthAdvice(
    /** 本地主键自增 */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 老人档案 userId（家属手机号） */
    val elderlyId: String,

    /** 建议时间戳（毫秒） */
    val adviceTime: Long,

    /** 建议内容 */
    val adviceContent: String
)
