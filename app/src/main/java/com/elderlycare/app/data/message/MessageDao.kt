package com.elderlycare.app.data.message

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * 留言数据访问接口。
 * 全部返回 suspend 或 Flow，保证在 Room 自有调度器执行，不阻塞 UI。
 */
@Dao
interface MessageDao {

    /** 插入留言，返回自增 id */
    @Insert
    suspend fun insert(message: MessageEntity): Long

    /** 按远端 id 查询（设备留言拉取去重用） */
    @Query("SELECT * FROM message WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): MessageEntity?

    /** 更新音频信息（TTS 合成完成后回填本地路径与时长） */
    @Query("UPDATE message SET localAudioPath = :path, duration = :duration WHERE id = :id")
    suspend fun updateAudioInfo(id: Long, path: String, duration: Int)

    /** 按设备查询全部留言（时间倒序） */
    @Query("SELECT * FROM message WHERE deviceSerial = :deviceSerial ORDER BY createTime DESC")
    suspend fun getAllByDeviceSerial(deviceSerial: String): List<MessageEntity>

    /** 按设备查询全部留言，Flow 方式供 UI 实时刷新 */
    @Query("SELECT * FROM message WHERE deviceSerial = :deviceSerial ORDER BY createTime DESC")
    fun observeByDeviceSerial(deviceSerial: String): kotlinx.coroutines.flow.Flow<List<MessageEntity>>

    /** 未读数 */
    @Query("SELECT COUNT(*) FROM message WHERE deviceSerial = :deviceSerial AND isRead = 0")
    fun observeUnreadCount(deviceSerial: String): kotlinx.coroutines.flow.Flow<Int>

    /** 标记单条已读 */
    @Query("UPDATE message SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    /** 更新发送状态（发送通路 + 状态 + 失败原因） */
    @Query("UPDATE message SET sendStatus = :status, sendChannel = :channel, failReason = :failReason WHERE id = :id")
    suspend fun updateSendStatus(id: Long, status: Int, channel: Int, failReason: String)

    /** 删除单条 */
    @Delete
    suspend fun delete(message: MessageEntity)

    /** 按 id 查询（删除本地文件前需要拿到路径） */
    @Query("SELECT * FROM message WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?
}
