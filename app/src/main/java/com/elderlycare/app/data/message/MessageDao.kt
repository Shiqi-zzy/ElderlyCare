package com.elderlycare.app.data.message

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /**
     * 插入留言，remoteId 冲突时忽略（返回 -1）。
     * 幂等去重主要靠仓库层 dedupMutex 串行化 + getByRemoteId 预查，
     * 本方法作最后兜底（remoteId 部分唯一索引已在 v4 移除：Room 不支持声明部分索引）。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(message: MessageEntity): Long

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

    /** 按 id 集合批量标记已读（会话对话页「全部已读」，内存分组后回写，不改表结构） */
    @Query("UPDATE message SET isRead = 1 WHERE id IN (:ids) AND isRead = 0")
    suspend fun markAsReadByIds(ids: List<Long>)

    /** 更新发送状态（发送通路 + 状态 + 失败原因） */
    @Query("UPDATE message SET sendStatus = :status, sendChannel = :channel, failReason = :failReason WHERE id = :id")
    suspend fun updateSendStatus(id: Long, status: Int, channel: Int, failReason: String)

    /** 删除单条 */
    @Delete
    suspend fun delete(message: MessageEntity)

    /** 按 id 查询（删除本地文件前需要拿到路径） */
    @Query("SELECT * FROM message WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?

    // ===== 消息分类（v3 新增，同一张表按 messageCategory 区分） =====

    /** 按设备 + 分类查询（时间倒序）。留言页只展示 messageCategory = 1（留言消息） */
    @Query("SELECT * FROM message WHERE deviceSerial = :deviceSerial AND messageCategory = :category ORDER BY createTime DESC")
    fun observeByDeviceSerialAndCategory(deviceSerial: String, category: Int): kotlinx.coroutines.flow.Flow<List<MessageEntity>>

    /** 按设备 + 分类的未读数（消息中心 Tab 角标用） */
    @Query("SELECT COUNT(*) FROM message WHERE deviceSerial = :deviceSerial AND messageCategory = :category AND isRead = 0")
    fun observeUnreadCountByCategory(deviceSerial: String, category: Int): kotlinx.coroutines.flow.Flow<Int>

    /** 标记该设备全部消息已读（消息中心「全部已读」） */
    @Query("UPDATE message SET isRead = 1 WHERE deviceSerial = :deviceSerial AND isRead = 0")
    suspend fun markAllRead(deviceSerial: String)

    /** 标记该设备某分类消息全部已读（进入分类 Tab 时批量已读，微信会话已读逻辑） */
    @Query("UPDATE message SET isRead = 1 WHERE deviceSerial = :deviceSerial AND messageCategory = :category AND isRead = 0")
    suspend fun markAllReadByCategory(deviceSerial: String, category: Int)

    /** sendonce 成功后回填远端消息 id（用于去重与追溯） */
    @Query("UPDATE message SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: Long, remoteId: String)

    /** 按远端 id 更新已读状态（告警云端已读状态回写 Room） */
    @Query("UPDATE message SET isRead = :isRead WHERE remoteId = :remoteId")
    suspend fun updateIsReadByRemoteId(remoteId: String, isRead: Boolean)
}
