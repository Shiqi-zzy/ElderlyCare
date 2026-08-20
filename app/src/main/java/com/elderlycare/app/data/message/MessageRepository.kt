package com.elderlycare.app.data.message

import android.content.Context
import android.util.Log
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.EzvizRepository
import com.elderlycare.app.data.ezviz.EzvizSdkManager
import com.elderlycare.app.data.ezviz.LeaveMessageTextRequest
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.RtcBackendApi
import com.elderlycare.app.data.ezviz.VoiceCallSession
import com.elderlycare.app.data.ezviz.VoiceCallState
import com.elderlycare.app.network.ezviz.EZCloudBroadcastManager
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
 * 留言仓库：双通道发送（录音）+ 后端文字发送 + 设备留言拉取 + Room 本地存储。
 *
 * 发送通路（App → 设备）：
 * - 录音留言：CHANNEL_BOTH 双通道 —— ①EZOpenSDK 语音通话（实时对讲，videotalk）
 *   ②云广播 REST（录音文件下发）；两条通路并行发送、互不影响，某条失败只记录失败原因。
 * - 文字留言：CHANNEL_BROADCAST —— 组装参数 HTTP 提交文本给后端
 *   （POST /api/leave-message/text），后端完成云端 TTS + 萤石云广播下发（upload→send）。
 *   App 不调用手机本地 TTS 引擎；TTS 合成在云端完成。
 *   发送状态完全按接口返回更新：成功→绿勾；失败→展示萤石接口原始错误码与错误信息。
 *
 * 接收通路（设备 → App，对齐萤石原生 App，全部使用公开 SDK 接口）：
 * - getLeaveMessageList 拉取，只入库 msgDirection=1（设备发给手机）的留言
 *   （msgDirection=2 手机发出方向跳过，与本地发送记录语义区分）；
 * - 音频优先 cloudServerUrl 直连下载，失败兜底 SDK 数据流回调（getLeaveMessageData
 *   + EZLeaveMessageFlowCallback 拿原始 byte[] 落盘），保存到本地后入库，按 remoteId 去重；
 * - 已读/删除同步云端（setLeaveMessageStatus / deleteLeaveMessages）。
 *
 * ⚠️ EZOpenSDK 留言接口为网络耗时操作，禁止主线程调用：
 * 所有 SDK 调用链统一切后台线程（withContext(Dispatchers.IO) / Room suspend）。
 */
class MessageRepository(
    private val context: Context,
    private val dao: MessageDao,
    private val sdkManager: EzvizSdkManager,
    private val broadcastManager: EZCloudBroadcastManager,
    private val ezvizRepository: EzvizRepository,
    private val rtcBackendApi: RtcBackendApi
) {

    companion object {
        private const val TAG = "MessageRepository"

        /** 设备留言拉取时间窗：近 7 天 */
        private const val PULL_DAYS = 7

        /**
         * EZLeaveMessage.msgDirection 方向语义（对齐萤石官方 iOS 文档；Android 字段名相同）：
         * 1 = 用户接收（设备发给手机）；2 = 用户回复（手机发出）。
         * 真机拉取时每条消息的方向值会打印日志（见 refreshDeviceMessages），
         * 若实测语义相反，只需改这里两个常量。
         */
        private const val MSG_DIRECTION_DEVICE_TO_PHONE = 1
        private const val MSG_DIRECTION_PHONE_TO_DEVICE = 2

        /**
         * EZLeaveMessage.msgStatus 状态语义（javap 实测 EZConstants.EZMessageStatus）：
         * EZMessageStatusUnRead=1，EZMessageStatusRead=2。
         * 注意与 iOS 文档（0=未读/1=已读）不同，勿混用。
         */
        private const val MSG_STATUS_UNREAD = 1
        private const val MSG_STATUS_READ = 2
    }

    /** 语音通话会话（通路①，生命周期与仓库相同） */
    val voiceCall = VoiceCallSession()

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

    // ==================== 发送：文字留言（HTTP 提交文本 → 后端 → 萤石云广播） ====================

    /**
     * 发送文字留言：组装参数 HTTP 提交文本给后端
     * （后端完成云端 TTS + 萤石云广播 upload→send 下发设备播报）。
     *
     * App 端不调用手机本地 TTS；发送状态完全按后端接口返回更新：
     * - 成功 → SEND_STATUS_SUCCESS（列表绿勾）
     * - 失败 → SEND_STATUS_FAILED + failReason = 萤石接口原始错误码与错误信息
     * 任何一步失败只更新本条发送状态，不影响本地存储与其他留言。
     *
     * logcat 打印：请求体、HTTP 状态码、完整返回 JSON（见下方日志）。
     * @return 留言 id（入库失败时为 -1）
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

        var ok: Boolean
        var failReason: String?
        try {
            // 请求体日志（完整参数）
            Log.i(
                TAG,
                "文字留言请求体: POST /api/leave-message/text {\"device_serial\":\"$deviceSerial\",\"text\":\"$trimmed\"}"
            )
            val response = rtcBackendApi.sendTextMessage(
                LeaveMessageTextRequest(device_serial = deviceSerial, text = trimmed)
            )
            // 响应日志：业务码 + 萤石原始错误码/信息 + 完整返回（OkHttp BODY 拦截器另有 HTTP 码与原始 JSON）
            Log.i(
                TAG,
                "文字留言响应: code=${response.code} message=${response.message} " +
                    "ezviz_code=${response.ezviz_code} ezviz_msg=${response.ezviz_msg}"
            )
            ok = response.code == 200
            failReason = if (ok) null else when {
                // 优先展示萤石接口原始错误码与错误信息
                response.ezviz_code.isNotBlank() ->
                    "萤石错误码 ${response.ezviz_code}：${response.ezviz_msg.ifBlank { response.message }}"
                response.message.isNotBlank() -> response.message
                else -> context.getString(R.string.message_send_failed)
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "文字留言提交失败：网络连接失败", e)
            ok = false
            failReason = "网络连接失败，请检查网络"
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "文字留言提交失败：请求超时", e)
            ok = false
            failReason = "请求超时，请稍后重试"
        } catch (e: Exception) {
            Log.e(TAG, "文字留言提交失败", e)
            ok = false
            failReason = e.message ?: context.getString(R.string.message_send_failed)
        }

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
     *
     * 方向过滤：只入库 msgDirection=1（设备发给手机）的留言；
     * msgDirection=2（手机发出）跳过——App 发送侧在本地记录（录音/文字），
     * 云端若存在历史「手机发出」留言也不入库，避免与本地记录语义重复。
     *
     * ⚠️ EZOpenSDK 留言接口为网络耗时操作，禁止主线程调用：
     * 整段逻辑（SDK 拉取/下载/落库）统一切 Dispatchers.IO 执行。
     * @return 新增留言条数（-1 表示拉取失败）
     */
    suspend fun refreshDeviceMessages(deviceSerial: String, deviceName: String): Int =
        withContext(Dispatchers.IO) {
            val token = ezvizRepository.obtainValidToken() ?: return@withContext -1
            sdkManager.updateToken(token)

            val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -PULL_DAYS) }
            val end = Calendar.getInstance()
            val list = sdkManager.getLeaveMessageList(deviceSerial, 0, 50, start, end)
            if (list.isEmpty()) return@withContext 0

            var added = 0
            for (msg in list) {
                val msgId = msg.msgId
                if (msgId.isNullOrBlank()) continue

                // 方向过滤：只收设备发来的留言；其他方向打日志跳过（便于真机核对 direction 语义）
                if (msg.msgDirection != MSG_DIRECTION_DEVICE_TO_PHONE) {
                    Log.i(
                        TAG,
                        "跳过非设备发来的留言: msgId=$msgId direction=${msg.msgDirection} " +
                            "senderType=${msg.senderType} contentType=${msg.contentType}"
                    )
                    continue
                }

                // 按 remoteId 去重，避免重复入库
                if (dao.getByRemoteId(msgId) != null) continue

                Log.i(
                    TAG,
                    "拉取到设备留言: msgId=$msgId direction=${msg.msgDirection} " +
                        "status=${msg.msgStatus} duration=${msg.duration}s"
                )

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
                        // 已读语义（javap 实测 EZMessageStatus）：1=未读，2=已读
                        isRead = msg.msgStatus == MSG_STATUS_READ,
                        deviceSerial = deviceSerial,
                        remoteId = msgId
                    )
                )
                if (rowId > 0) added++
            }
            Log.i(TAG, "设备留言同步完成，新增 $added 条")
            return@withContext added
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
