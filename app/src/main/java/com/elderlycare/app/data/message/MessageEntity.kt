package com.elderlycare.app.data.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 留言实体（Room 表 message）
 *
 * 发送通路说明：
 * - sendChannel = 1 微聊（EZOpenSDK 语音对讲实时下发）
 * - sendChannel = 2 云广播（萤石云广播 REST 下发）
 * - sendChannel = 3 双通道（微聊 + 云广播同时下发）
 * - sendChannel = 4 sendonce（一次性语音下发，WAV 转码 ADTS AAC 后调用 /api/lapp/voice/sendonce，不入云广播语音库）
 *
 * 消息分类说明（同一张表按字段区分，不拆表）：
 * - messageCategory = 1 留言消息：手机文字TTS/手机录音/设备视频留言
 * - messageCategory = 2 报警消息：萤石设备告警事件（msgType=5）
 */
@Entity(
    tableName = "message",
    indices = [Index(value = ["deviceSerial"]), Index(value = ["createTime"])]
)
data class MessageEntity(
    /** 主键自增 */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 消息类型：1=录音留言，2=文字TTS留言，3=设备发来的留言，4=系统消息，5=报警消息 */
    val msgType: Int,

    /** 发送人名称（"我"/"设备"/设备名） */
    val senderName: String,

    /** 文字内容（纯音频留言为空字符串；报警消息存告警名称） */
    val content: String = "",

    /** 本地音频文件路径（getFilesDir()/messages/ 下） */
    val localAudioPath: String = "",

    /** 音频时长（秒） */
    val duration: Int = 0,

    /** 创建时间戳（毫秒） */
    val createTime: Long,

    /** 是否已读 */
    val isRead: Boolean = false,

    /** 绑定设备序列号 */
    val deviceSerial: String = "",

    /**
     * 远端消息 id。
     * 设备发来的留言（msgType=3）对应 EZOpenSDK 留言 msgId，用于拉取去重、
     * 云端标记已读与删除；报警消息（msgType=5）对应萤石告警 alarmId；
     * App 发送的留言为空字符串（sendonce 成功后回填萤石返回的 msgId）。
     */
    val remoteId: String = "",

    /**
     * 发送状态：0=发送中，1=成功，2=失败
     * 设备发来的留言（msgType=3）与报警消息（msgType=5）恒为 1
     */
    val sendStatus: Int = 1,

    /** 发送通路：0=无通路（报警消息），1=微聊，2=云广播，3=双通道，4=sendonce 一次性下发 */
    val sendChannel: Int = 1,

    /** 失败原因（可选） */
    val failReason: String = "",

    /**
     * 本地视频文件路径（设备视频留言缓存，getFilesDir()/messages/ 下；下载失败为空）
     * defaultValue 与 MIGRATION_2_3 的 ALTER 语句 DEFAULT '' 对齐（SQLite ADD COLUMN
     * NOT NULL 必须有默认值，Room 校验要求实体与迁移默认值一致）
     */
    @ColumnInfo(defaultValue = "''")
    val localVideoPath: String = "",

    /** 视频云端 URL（设备视频留言的 cloudServerUrl / 报警消息的 alarmVideoUrl） */
    @ColumnInfo(defaultValue = "''")
    val videoCloudUrl: String = "",

    /** 缩略图 URL（设备视频留言的 msgPicUrl / 报警消息的 alarmPicUrl） */
    @ColumnInfo(defaultValue = "''")
    val thumbUrl: String = "",

    /**
     * 消息分类：1=留言消息（文字TTS/手机录音/设备视频），2=报警消息（萤石告警事件）。
     * 同一张表按本字段区分，不拆表；历史数据迁移默认 1（留言）。
     */
    @ColumnInfo(defaultValue = "1")
    val messageCategory: Int = MESSAGE_CATEGORY_LEAVE_MSG
) {
    companion object {
        const val MSG_TYPE_RECORD = 1    // 录音留言（App → 设备）
        const val MSG_TYPE_TEXT = 2      // 文字留言（App → 设备，设备本地 TTS 播报）
        const val MSG_TYPE_DEVICE = 3    // 设备发来的留言（设备 → App）
        const val MSG_TYPE_SYSTEM = 4    // 系统消息（提醒计划播报完成等）
        const val MSG_TYPE_ALERT = 5     // 报警消息（萤石设备告警事件）
        const val MSG_TYPE_ADVICE = 6    // 健康建议（医院医护 → 家属，仅 App 消息模块查看，不走设备播报）

        const val SEND_STATUS_SENDING = 0
        const val SEND_STATUS_SUCCESS = 1
        const val SEND_STATUS_FAILED = 2

        const val CHANNEL_NONE = 0       // 无发送通路（报警消息）
        const val CHANNEL_TALK = 1       // 微聊（EZOpenSDK 语音对讲）
        const val CHANNEL_BROADCAST = 2  // 云广播（REST）
        const val CHANNEL_BOTH = 3       // 双通道
        const val CHANNEL_SENDONCE = 4   // sendonce 一次性语音下发（REST，不入语音库）
        const val CHANNEL_CLOCK = 5      // 设备本地闹铃（文字留言：v3 闹铃接口 RK3 本地 TTS 播报）

        const val MESSAGE_CATEGORY_LEAVE_MSG = 1  // 留言消息
        const val MESSAGE_CATEGORY_ALERT = 2      // 报警消息

        /**
         * 设备会话统一展示名：RK3(设备序列号)。
         * 设备产生的报警/留言消息 senderName 强制填充该格式（禁止 null/空），
         * 会话分组键与 UI 展示共用本函数。
         */
        fun deviceSenderName(deviceSerial: String): String = "RK3($deviceSerial)"
    }
}
