package com.elderlycare.app.data.message

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
 */
@Entity(
    tableName = "message",
    indices = [Index(value = ["deviceSerial"]), Index(value = ["createTime"])]
)
data class MessageEntity(
    /** 主键自增 */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 消息类型：1=录音留言，2=文字TTS留言，3=设备发来的留言 */
    val msgType: Int,

    /** 发送人名称（"我"/"设备"/设备名） */
    val senderName: String,

    /** 文字内容（纯音频留言为空字符串） */
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
     * 云端标记已读与删除；App 发送的留言为空字符串。
     */
    val remoteId: String = "",

    /**
     * 发送状态：0=发送中，1=成功，2=失败
     * 设备发来的留言（msgType=3）恒为 1
     */
    val sendStatus: Int = 1,

    /** 发送通路：1=微聊，2=云广播，3=双通道 */
    val sendChannel: Int = 1,

    /** 失败原因（可选） */
    val failReason: String = ""
) {
    companion object {
        const val MSG_TYPE_RECORD = 1    // 录音留言（App → 设备）
        const val MSG_TYPE_TEXT = 2      // 文字TTS留言（App → 设备）
        const val MSG_TYPE_DEVICE = 3    // 设备发来的留言（设备 → App）
        const val MSG_TYPE_SYSTEM = 4    // 系统消息（提醒计划播报完成等）

        const val SEND_STATUS_SENDING = 0
        const val SEND_STATUS_SUCCESS = 1
        const val SEND_STATUS_FAILED = 2

        const val CHANNEL_TALK = 1       // 微聊（EZOpenSDK 语音对讲）
        const val CHANNEL_BROADCAST = 2  // 云广播（REST）
        const val CHANNEL_BOTH = 3       // 双通道
    }
}
