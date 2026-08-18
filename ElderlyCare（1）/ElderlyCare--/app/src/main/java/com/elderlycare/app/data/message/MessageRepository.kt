package com.elderlycare.app.data.message

import android.content.Context
import android.util.Log
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.EzvizRepository
import com.elderlycare.app.data.ezviz.EzvizSdkManager
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.VoiceCallSession
import com.elderlycare.app.data.ezviz.VoiceCallState
import com.elderlycare.app.network.ezviz.EZCloudBroadcastManager
import com.elderlycare.app.util.TtsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 留言仓库：双通道发送 + 设备留言拉取 + Room 本地存储。
 *
 * 发送通路（App → 设备）：
 * - 录音留言：CHANNEL_BOTH 双通道 —— ①EZOpenSDK 语音通话（实时对讲）②云广播 REST（录音文件下发）；
 *   两条通路并行发送、互不影响，某条失败只记录失败原因。
 * - 文字留言：CHANNEL_BROADCAST —— TTS 合成 WAV 后走云广播
 *   （EZOpenSDK 没有文字/文件下发 API，文字留言不适用实时通话通路）。
 *
 * 接收通路（设备 → App）：
 * - 通过 EZOpenSDK 微聊留言接口拉取（getLeaveMessageList），
 *   音频优先用 cloudServerUrl 直连下载，失败时兜底走 SDK 数据流回调
 *   （getLeaveMessageData），保存到本地后入库，按 remoteId 去重。
 *
 * 所有耗时操作均切后台线程执行（withContext(Dispatchers.IO) / Room suspend）。
 */
class MessageRepository(
    private val context: Context,
    private val dao: MessageDao,
    private val sdkManager: EzvizSdkManager,
    private val broadcastManager: EZCloudBroadcastManager,
    private val ezvizRepository: EzvizRepository
) {

    companion object {
        private const val TAG = "MessageRepository"

        /** 设备留言拉取时间窗：近 7 天 */
        private const val PULL_DAYS = 7
    }

    /** 语音通话会话（通路①，生命周期与仓库相同） */
    val voiceCall = VoiceCallSession()

    private val ttsHelper by lazy { TtsHelper(context) }

    /** 设备留言音频下载用 HTTP 客户端 */
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ==================== 数据流 ====================

    /** 设备留言列表（时间倒序，Flow 实时刷新） */
    fun observeMessages(deviceSerial: String): Flow<List<MessageEntity>> =
        dao.observeByDeviceSerial(deviceSerial)

    /** 未读数 */
    fun observeUnreadCount(deviceSerial: String): Flow<Int> =
        dao.observeUnreadCount(deviceSerial)

    // ==================== 发送：文字留言（云广播单通道） ====================

    /**
     * 发送文字留言：TTS 合成 → 云广播上传/下发。
     * 任何一步失败只更新本条发送状态，不影响本地存储与其他留言。
     * @return 留言 id（失败时为 -1）
     */
    suspend fun sendTextMessage(deviceSerial: String, text: String): Long {
        val trimmed = text.trim()
        val rowId = dao.insert(
            MessageEntity(
                msgType = MessageEntity.MSG_TYPE_TEXT,
                senderName = context.getString(R.string.message_sender_me),
                content = trimmed,
                createTime = System.currentTimeMillis(),
                deviceSerial = deviceSerial,
                sendStatus = MessageEntity.SEND_STATUS_SENDING,
                sendChannel = MessageEntity.CHANNEL_BROADCAST
            )
        )
        if (rowId <= 0) return -1L

        // 1) TTS 合成（失败则本条失败，不阻断其他留言）
        val ttsFile = MessageFiles.newTtsFile(context)
        val duration = ttsHelper.synthesizeToFile(trimmed, ttsFile)
        if (duration == null) {
            MessageFiles.deleteQuietly(ttsFile)
            dao.updateSendStatus(
                rowId, MessageEntity.SEND_STATUS_FAILED,
                MessageEntity.CHANNEL_BROADCAST,
                context.getString(R.string.message_tts_failed)
            )
            return rowId
        }
        dao.updateAudioInfo(rowId, ttsFile.absolutePath, duration)

        // 2) 云广播：能力判断 → 上传 → 下发
        val (ok, failReason) = sendViaBroadcast(deviceSerial, ttsFile, rowId)
        dao.updateSendStatus(
            rowId,
            if (ok) MessageEntity.SEND_STATUS_SUCCESS else MessageEntity.SEND_STATUS_FAILED,
            MessageEntity.CHANNEL_BROADCAST,
            failReason ?: ""
        )
        return rowId
    }

    // ==================== 发送：录音留言（双通道并行） ====================

    /**
     * 发送录音留言：①SDK 语音通话（实时）与 ②云广播（文件）并行发送，互不阻塞。
     * 两条通路至少一条成功即视为发送成功，失败通路原因写入 failReason。
     * @return 留言 id（失败时为 -1）
     */
    suspend fun sendRecordMessage(deviceSerial: String, recordFile: File, durationSec: Int): Long {
        val rowId = dao.insert(
            MessageEntity(
                msgType = MessageEntity.MSG_TYPE_RECORD,
                senderName = context.getString(R.string.message_sender_me),
                localAudioPath = recordFile.absolutePath,
                duration = durationSec,
                createTime = System.currentTimeMillis(),
                deviceSerial = deviceSerial,
                sendStatus = MessageEntity.SEND_STATUS_SENDING,
                sendChannel = MessageEntity.CHANNEL_BOTH
            )
        )
        if (rowId <= 0) return -1L

        // 双通道并行发送，各自失败互不影响
        coroutineScope {
            val talkDeferred = async { sendViaVoiceCall(deviceSerial) }
            val broadcastDeferred = async { sendViaBroadcast(deviceSerial, recordFile, rowId) }
            val talkResult = talkDeferred.await()
            val broadcastResult = broadcastDeferred.await()

            val failParts = listOfNotNull(talkResult.second, broadcastResult.second)
            val ok = talkResult.first || broadcastResult.first
            dao.updateSendStatus(
                rowId,
                if (ok) MessageEntity.SEND_STATUS_SUCCESS else MessageEntity.SEND_STATUS_FAILED,
                MessageEntity.CHANNEL_BOTH,
                failParts.joinToString("；")
            )
        }
        return rowId
    }

    /** 通路①：EZOpenSDK 语音通话（实时双向对讲），发起成功即算通路成功 */
    private suspend fun sendViaVoiceCall(deviceSerial: String): Pair<Boolean, String?> {
        val cap = broadcastManager.checkDeviceTalkSupport(deviceSerial)
        val supported = cap.getOrNull() ?: return false to cap.errMsg()
        if (!supported) {
            return false to context.getString(R.string.message_send_failed_talk_unsupported)
        }
        // 注入登录态（token 过期会自动刷新）
        val token = ezvizRepository.obtainValidToken()
            ?: return false to context.getString(R.string.message_not_logged_in)
        sdkManager.updateToken(token)
        return try {
            // SDK 内部要求主线程发起
            val ok = withContext(Dispatchers.Main) { voiceCall.start(context, deviceSerial) }
            if (ok) {
                true to null
            } else {
                val reason = (voiceCall.state.value as? VoiceCallState.Failed)?.reason
                false to (reason ?: "语音通话失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "语音通话通路异常", e)
            false to (e.message ?: "语音通话失败")
        }
    }

    /** 通路②：云广播 REST（能力判断 → 上传 → 下发设备播放） */
    private suspend fun sendViaBroadcast(
        deviceSerial: String,
        audioFile: File,
        rowId: Long
    ): Pair<Boolean, String?> {
        val cap = broadcastManager.checkDeviceTalkSupport(deviceSerial)
        val supported = cap.getOrNull() ?: return false to cap.errMsg()
        if (!supported) {
            return false to context.getString(R.string.message_send_failed_broadcast_unsupported)
        }
        val upload = broadcastManager.uploadVoiceFile(audioFile, "msg_$rowId")
        val url = upload.getOrNull() ?: return false to upload.errMsg()
        val send = broadcastManager.sendVoiceToDevice(deviceSerial, url)
        if (send.isError) return false to send.errMsg()
        return true to null
    }

    // ==================== 接收：设备留言拉取 ====================

    /**
     * 拉取设备发来的留言（EZOpenSDK 微聊接口），新留言下载音频并入库。
     * @return 新增留言条数（-1 表示拉取失败）
     */
    suspend fun refreshDeviceMessages(deviceSerial: String, deviceName: String): Int {
        val token = ezvizRepository.obtainValidToken() ?: return -1
        sdkManager.updateToken(token)

        val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -PULL_DAYS) }
        val end = Calendar.getInstance()
        val list = sdkManager.getLeaveMessageList(deviceSerial, 0, 50, start, end)
        if (list.isEmpty()) return 0

        var added = 0
        for (msg in list) {
            val msgId = msg.msgId
            if (msgId.isNullOrBlank()) continue
            // 按 remoteId 去重，避免重复入库
            if (dao.getByRemoteId(msgId) != null) continue

            val local = downloadLeaveAudio(context, msg, msgId) ?: continue
            val rowId = dao.insert(
                MessageEntity(
                    msgType = MessageEntity.MSG_TYPE_DEVICE,
                    senderName = deviceName.ifBlank {
                        context.getString(R.string.message_sender_device)
                    },
                    localAudioPath = local.absolutePath,
                    duration = MessageFiles.audioDurationSec(local),
                    createTime = msg.createTime?.timeInMillis ?: System.currentTimeMillis(),
                    // TODO(用户需确认): msgStatus 语义（0=未读/1=已读）以官方文档为准
                    isRead = msg.msgStatus == 1,
                    deviceSerial = deviceSerial,
                    remoteId = msgId
                )
            )
            if (rowId > 0) added++
        }
        Log.i(TAG, "设备留言同步完成，新增 $added 条")
        return added
    }

    /** 下载设备留言音频：优先 cloudServerUrl 直连，失败兜底 SDK 数据流 */
    private suspend fun downloadLeaveAudio(
        context: Context,
        msg: com.videogo.openapi.bean.EZLeaveMessage,
        msgId: String
    ): File? {
        val target = MessageFiles.newDeviceFile(context, msgId)
        if (target.exists() && target.length() > 0) return target

        val url = msg.cloudServerUrl
        if (!url.isNullOrBlank() && downloadUrlToFile(url, target)) return target

        // 兜底：SDK 流式下载（见 EzvizSdkManager.downloadLeaveMessageData）
        return if (sdkManager.downloadLeaveMessageData(msg, target)) target else null
    }

    /** 直连下载云端音频到本地（后台线程） */
    private suspend fun downloadUrlToFile(url: String, target: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                response.use {
                    if (!it.isSuccessful) return@withContext false
                    val body = it.body ?: return@withContext false
                    body.byteStream().use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                }
                target.exists() && target.length() > 0
            } catch (e: Exception) {
                Log.e(TAG, "下载设备留言音频失败: $url", e)
                MessageFiles.deleteQuietly(target)
                false
            }
        }

    // ==================== 已读 / 删除 ====================

    /** 标记已读：本地 + 云端（设备留言同步云端状态，避免下次拉取又变未读） */
    suspend fun markMessageRead(message: MessageEntity) {
        if (message.isRead) return
        dao.markAsRead(message.id)
        if (message.msgType == MessageEntity.MSG_TYPE_DEVICE && message.remoteId.isNotBlank()) {
            sdkManager.markLeaveMessageRead(listOf(message.remoteId))
        }
    }

    /** 删除留言：本地记录 + 本地音频文件 + 云端留言（设备留言） */
    suspend fun deleteMessage(message: MessageEntity) {
        dao.delete(message)
        if (message.localAudioPath.isNotBlank()) {
            MessageFiles.deleteQuietly(File(message.localAudioPath))
        }
        if (message.msgType == MessageEntity.MSG_TYPE_DEVICE && message.remoteId.isNotBlank()) {
            sdkManager.deleteLeaveMessages(listOf(message.remoteId))
        }
    }

    // ==================== 工具 ====================

    /** 提取 NetworkResult 失败信息 */
    private fun NetworkResult<*>.errMsg(): String = when (this) {
        is NetworkResult.Error -> message
        is NetworkResult.Success -> ""
    }
}
