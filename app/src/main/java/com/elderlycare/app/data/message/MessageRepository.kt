package com.elderlycare.app.data.message

import android.content.Context
import android.util.Log
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.EzvizRepository
import com.elderlycare.app.data.ezviz.EzvizSdkManager
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.VoiceCallSession
import com.elderlycare.app.data.ezviz.VoiceCallState
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import com.elderlycare.app.data.reminder.EzvizReminderApi
import com.elderlycare.app.data.reminder.EzvizV3Response
import com.elderlycare.app.data.reminder.RemindClockCreateRequest
import com.elderlycare.app.network.ezviz.EZCloudBroadcastManager
import com.elderlycare.app.util.SendOnceAudioValidator
import com.elderlycare.app.util.WavToAacTranscoder
import com.elderlycare.app.util.limitCodePoints
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 留言仓库：录音级联发送（双通道优先 + sendonce 降级）+ 后端文字发送 + 设备视频留言拉取 +
 * 报警消息落库 + Room 本地存储（同一张表按 messageCategory 分类）。
 *
 * 发送通路（App → 设备）：
 * - 录音留言：前置能力校验（拦截明确 support_talk=0，空对象/异常放行）→
 *   CHANNEL_BOTH 双通道 —— ①EZOpenSDK 语音通话（实时对讲，videotalk）
 *   ②云广播 REST（录音文件下发）；两条通路并行发送、任一成功即整体成功。
 *   双通道失败 → 自动降级 CHANNEL_SENDONCE：WAV 经 MediaCodec 转码 ADTS 裸 AAC，
 *   调 /api/lapp/voice/sendonce multipart 一步下发设备一次性播放（不入云广播语音库）。
 *   两级均失败才判定整体失败，failReason 汇总，支持重发（完整复现级联）。
 * - 文字留言：CHANNEL_SENDONCE —— 本地 TextToSpeech 合成 WAV → 转码 ADTS 裸 AAC →
 *   sendonce 一步下发设备一次性播放（EZOpenSDK 没有文字/文件下发 API，
 *   且本设备云广播上传 WAV 被萤石拒收，sendonce+AAC 为实测可用通路）；
 *   成功 → 绿勾，失败 → failReason 展示失败原因。
 *
 * 接收通路（设备 → App，对齐萤石原生 App，全部使用公开 SDK 接口）：
 * - getLeaveMessageList 拉取，只入库 msgDirection=1（设备发给手机）且 contentType=2
 *   的视频留言（type=1 设备语音忽略丢弃；msgDirection=2 手机发出方向跳过）；
 * - 视频优先 cloudServerUrl 直连下载（超 30M 只存云端 URL），失败兜底 SDK 数据流回调，
 *   入库 messageCategory=1、localVideoPath/videoCloudUrl/thumbUrl(msgPicUrl)，按 remoteId 去重；
 * - 已读/删除同步云端（setLeaveMessageStatus / deleteLeaveMessages）。
 *
 * 报警消息（messageCategory=2）：getAlarmList 拉取的告警事件经 saveAlertMessages 落库，
 * alarmId 作 remoteId 幂等去重；与留言同表按分类区分，不拆表。
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
    private val reminderApi: EzvizReminderApi,
    private val cleanupScope: CoroutineScope
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

        /**
         * EZLeaveMessage.contentType 内容类型语义（SDK 注释 + javap 实测一致）：
         * 1=语音留言（本项目忽略，设备只上传视频，不处理设备语音）；
         * 2=视频留言（本项目消费，下载入库 + 缩略图 + 云端 URL）。
         * 拉取时每条 contentType 会打印日志，若真机语义相反只需改这里两个常量。
         */
        private const val MSG_CONTENT_TYPE_VOICE = 1
        private const val MSG_CONTENT_TYPE_VIDEO = 2

        /** 设备视频留言本地缓存上限：30M，超限只存云端 URL（点击时直连播放） */
        private const val MAX_VIDEO_DOWNLOAD_BYTES = 30L * 1024 * 1024

        /** 文字留言上限：萤石 v3 闹铃接口 content ≤20 字符（含标点），超出按码点截断 */
        private const val MAX_CLOCK_TEXT_LEN = 20

        /** 临时闹铃播报后延迟清理时长：覆盖「到点（≤60s）+ 文本播报（≤30s）」 */
        private const val CLOCK_CLEANUP_DELAY_MS = 90_000L

        /** 临时闹铃标题（设备端展示，播报后即删除） */
        private const val CLOCK_TAG = "留言"
    }

    /** 语音通话会话（通路①，生命周期与仓库相同） */
    val voiceCall = VoiceCallSession()

    /** 设备留言音频下载用 HTTP 客户端 */
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 拉取落库去重互斥锁：留言页与消息中心两个 VM 会并发轮询
     * refreshDeviceMessages / saveAlertMessages，remoteId 幂等去重靠
     * 「getByRemoteId 预查 + insertIgnore」两步组合，必须串行化才能防并发穿透
     * （曾用 remoteId 部分唯一索引兜底，但 Room 不支持声明部分索引、升级校验
     * 索引集合不一致会崩溃，v4 已移除，改由本互斥锁保证）。
     */
    private val dedupMutex = Mutex()

    // ==================== 数据流 ====================

    /**
     * 留言页消息列表（时间倒序，Flow 实时刷新）。
     * 留言页只展示留言分类（messageCategory = 1：文字TTS/手机录音/设备视频），
     * 报警消息（分类 2）由消息中心展示；历史数据迁移默认分类 1，行为不变。
     */
    fun observeMessages(deviceSerial: String): Flow<List<MessageEntity>> =
        dao.observeByDeviceSerialAndCategory(deviceSerial, MessageEntity.MESSAGE_CATEGORY_LEAVE_MSG)

    /** 全部消息列表（留言 + 报警，消息中心用，时间倒序） */
    fun observeAllMessages(deviceSerial: String): Flow<List<MessageEntity>> =
        dao.observeByDeviceSerial(deviceSerial)

    /** 未读数（全部分类） */
    fun observeUnreadCount(deviceSerial: String): Flow<Int> =
        dao.observeUnreadCount(deviceSerial)

    /** 某分类未读数（消息中心 Tab 角标用） */
    fun observeUnreadCountByCategory(deviceSerial: String, category: Int): Flow<Int> =
        dao.observeUnreadCountByCategory(deviceSerial, category)

    /** 按 id 取消息（设备视频播放页用） */
    suspend fun getMessageById(id: Long): MessageEntity? = dao.getById(id)

    /** 标记该设备全部消息已读（消息中心「全部已读」） */
    suspend fun markAllRead(deviceSerial: String) = dao.markAllRead(deviceSerial)

    /** 标记该设备某分类全部已读（进入分类 Tab 批量已读，微信会话已读逻辑） */
    suspend fun markAllReadByCategory(deviceSerial: String, category: Int) =
        dao.markAllReadByCategory(deviceSerial, category)

    /** 批量标记已读（会话对话页「全部已读」：内存分组得出的消息 id 集合回写，不改表结构） */
    suspend fun markMessagesRead(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.markAsReadByIds(ids)
    }

    // ==================== 发送：文字留言（v3 闹铃接口 → RK3 本地 TTS 播报） ====================

    /**
     * 发送文字留言：萤石 v3 闹铃接口（POST /api/v3/device/life/remind/clock）实现 RK3 本地 TTS 播报。
     *
     * 流程（用户拍板方案，不走任何语音上传/TTS/sendonce 逻辑）：
     * 1. 取当前时间（+1 分钟，保证闹铃时间必定晚于设备当前时刻），once=1 创建单次即时闹铃，
     *    content 为用户输入文本（≤20 字符，按 Unicode 码点截断），weekdays=今日星期；
     * 2. 创建成功 → 标记发送成功；延迟 CLOCK_CLEANUP_DELAY_MS（覆盖「到点 + 播报」）
     *    在 cleanupScope 内调用 deleteClocks 清理这条临时闹铃（清理失败只记日志，不影响发送结果）；
     * 3. Room 消息 msgType=TEXT、sendChannel=CHANNEL_CLOCK。
     * 任何一步失败只更新本条发送状态，不影响本地存储与其他留言。
     * @return 留言 id（失败时为 -1）
     */
    suspend fun sendTextMessage(deviceSerial: String, text: String): Long {
        val trimmed = text.trim().limitCodePoints(MAX_CLOCK_TEXT_LEN)
        if (trimmed.isEmpty()) return -1L
        val rowId = dao.insert(
            MessageEntity(
                msgType = MessageEntity.MSG_TYPE_TEXT,
                senderName = context.getString(R.string.message_sender_me),
                content = trimmed,
                createTime = System.currentTimeMillis(),
                deviceSerial = deviceSerial,
                sendStatus = MessageEntity.SEND_STATUS_SENDING,
                sendChannel = MessageEntity.CHANNEL_CLOCK,
                messageCategory = MessageEntity.MESSAGE_CATEGORY_LEAVE_MSG
            )
        )
        if (rowId <= 0) return -1L

        val token = ezvizRepository.obtainValidToken()
        if (token == null) {
            dao.updateSendStatus(
                rowId, MessageEntity.SEND_STATUS_FAILED,
                MessageEntity.CHANNEL_CLOCK,
                context.getString(R.string.message_not_logged_in)
            )
            return rowId
        }

        // 当前时间 +1 分钟：闹铃时间必须晚于设备当前时刻，否则设备可能视为已过期永不播报
        val now = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
        val body = RemindClockCreateRequest(
            timeHour = now.get(Calendar.HOUR_OF_DAY),
            timeMin = now.get(Calendar.MINUTE),
            once = 1,
            year = now.get(Calendar.YEAR),
            month = now.get(Calendar.MONTH) + 1,
            day = now.get(Calendar.DAY_OF_MONTH),
            weekdays = listOf(now.get(Calendar.DAY_OF_WEEK) - 1), // 0=周日…6=周六，单次=日期对应星期
            content = trimmed,
            tag = CLOCK_TAG
        )
        return try {
            Log.i(TAG, "文字留言创建闹铃: ${now.get(Calendar.HOUR_OF_DAY)}:${now.get(Calendar.MINUTE)} " +
                "weekdays=${body.weekdays} content=$trimmed")
            val resp = reminderApi.createClock(token, deviceSerial, body)
            Log.i(TAG, "文字留言闹铃创建响应: code=${resp.effectiveCode} msg=${resp.effectiveMsg}")
            if (resp.effectiveCode != 200) {
                dao.updateSendStatus(
                    rowId, MessageEntity.SEND_STATUS_FAILED,
                    MessageEntity.CHANNEL_CLOCK,
                    mapClockError(resp.effectiveCode, resp.effectiveMsg)
                )
                return rowId
            }
            dao.updateSendStatus(
                rowId, MessageEntity.SEND_STATUS_SUCCESS, MessageEntity.CHANNEL_CLOCK, ""
            )
            // 等设备播报后清理临时闹铃（cleanupScope 执行，页面关闭不中断；清理失败只记日志）
            val clockId = resp.data?.clockId.orEmpty()
            if (clockId.isBlank()) {
                Log.w(TAG, "闹铃创建成功但未返回 clockId，无法清理临时闹铃")
            } else {
                cleanupScope.launch {
                    delay(CLOCK_CLEANUP_DELAY_MS)
                    try {
                        val freshToken = ezvizRepository.obtainValidToken()
                        val del = if (freshToken != null) {
                            reminderApi.deleteClocks(freshToken, deviceSerial, listOf(clockId))
                        } else null
                        Log.i(TAG, "临时闹铃清理响应: clockId=$clockId " +
                            "code=${del?.effectiveCode} msg=${del?.effectiveMsg}")
                    } catch (e: Exception) {
                        Log.w(TAG, "临时闹铃清理失败: clockId=$clockId", e)
                    }
                }
            }
            rowId
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "文字留言闹铃创建失败：网络连接失败", e)
            dao.updateSendStatus(
                rowId, MessageEntity.SEND_STATUS_FAILED, MessageEntity.CHANNEL_CLOCK,
                "网络连接失败，请检查网络"
            )
            rowId
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "文字留言闹铃创建失败：请求超时", e)
            dao.updateSendStatus(
                rowId, MessageEntity.SEND_STATUS_FAILED, MessageEntity.CHANNEL_CLOCK,
                "请求超时，请稍后重试"
            )
            rowId
        } catch (e: HttpException) {
            Log.e(TAG, "文字留言闹铃创建失败: HTTP ${e.code()}", e)
            dao.updateSendStatus(
                rowId, MessageEntity.SEND_STATUS_FAILED, MessageEntity.CHANNEL_CLOCK,
                clockHttpError(e)
            )
            rowId
        } catch (e: Exception) {
            Log.e(TAG, "文字留言闹铃创建失败", e)
            dao.updateSendStatus(
                rowId, MessageEntity.SEND_STATUS_FAILED, MessageEntity.CHANNEL_CLOCK,
                e.message ?: "发送失败，请稍后重试"
            )
            rowId
        }
    }

    /** 闹铃接口常见错误码 → failReason 文案（20007 设备不在线等） */
    private fun mapClockError(code: Int, msg: String): String = when (code) {
        20007 -> context.getString(R.string.message_send_failed_device_offline)
        else -> "萤石错误码 $code：${msg.ifBlank { "操作失败" }}"
    }

    /** HttpException → 优先解析 errorBody 的 meta.message（与提醒计划错误处理同源） */
    private fun clockHttpError(e: HttpException): String {
        val ezvizMsg = runCatching {
            val raw = e.response()?.errorBody()?.string().orEmpty()
            if (raw.isBlank()) "" else Gson().fromJson(raw, EzvizV3Response::class.java).effectiveMsg
        }.getOrDefault("")
        return if (ezvizMsg.isNotBlank()) "发送失败：$ezvizMsg"
        else "发送失败（HTTP ${e.code()}），请稍后重试"
    }

    // ==================== 发送：录音留言（双通道优先，失败自动降级 sendonce） ====================

    /**
     * 发送录音留言（内部自动降级，用户无感知）：
     *
     * 1. 前置能力校验（api/lapp/device/capacity）——「拦截明确不支持，异常放行」：
     *    capacity 正常返回且 support_talk 明确为 0 → 拦截并记 failReason，不发任何请求；
     *    RK3 空对象（HTTP200 无 support_talk 字段）或接口异常 → 放行尝试发送，
     *    真实结果以各通路接口返回为准。禁止把空对象判定为不支持（RK3 语音留言功能依赖此规则）。
     * 2. 双通道并行发送（①SDK 语音通话 videotalk ②云广播 upload+send），任一成功即整体成功，
     *    sendChannel 标记 CHANNEL_BOTH。
     * 3. 双通道失败 → 自动降级 sendonce 一次性下发：WAV 经 MediaCodec 转码 ADTS 裸 AAC
     *    （禁止直传 WAV/禁止 m4a，萤石仅接受 ADTS 裸 AAC 流），前置校验后 multipart 一步下发设备；
     *    成功 sendChannel 标记 CHANNEL_SENDONCE 并回填 remoteId。
     * 4. 两级均失败才判定整体失败，failReason 汇总两级原因，UI 提供重发
     *    （重发 = 完整复现本套级联流程，见 [resendMessage]）。
     *
     * @return 留言 id（入库失败时为 -1）
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
                sendChannel = MessageEntity.CHANNEL_BOTH,
                messageCategory = MessageEntity.MESSAGE_CATEGORY_LEAVE_MSG
            )
        )
        if (rowId <= 0) return -1L

        sendRecordCascade(rowId, deviceSerial, recordFile, durationSec)
        return rowId
    }

    /**
     * 重发失败的录音留言：完整复现「双通道 → 失败自动降级 sendonce」整套级联流程。
     *
     * 守卫：仅发送失败状态的录音留言可重发（文字 TTS 不提供重发，不碰其逻辑）；
     * 本地音频文件缺失时置 failReason 提示，不发起请求。
     * @return 留言 id（不可重发时为 -1）
     */
    suspend fun resendMessage(message: MessageEntity): Long {
        if (message.sendStatus != MessageEntity.SEND_STATUS_FAILED ||
            message.msgType != MessageEntity.MSG_TYPE_RECORD
        ) return -1L
        val file = File(message.localAudioPath)
        if (!file.exists() || file.length() == 0L) {
            dao.updateSendStatus(
                message.id,
                MessageEntity.SEND_STATUS_FAILED,
                message.sendChannel,
                context.getString(R.string.message_resend_file_missing)
            )
            return -1L
        }
        dao.updateSendStatus(message.id, MessageEntity.SEND_STATUS_SENDING, MessageEntity.CHANNEL_BOTH, "")
        sendRecordCascade(message.id, message.deviceSerial, file, message.duration)
        return message.id
    }

    /**
     * 级联发送核心（首轮发送与重发共用）：能力校验 → 双通道 → 降级 sendonce。
     * 注意 [recordFile] 可能是 WAV（首轮录音）或 AAC（上轮降级转码产物，重发时复用）。
     */
    private suspend fun sendRecordCascade(
        rowId: Long,
        deviceSerial: String,
        recordFile: File,
        durationSec: Int
    ) {
        // 1. 前置能力校验：仅「明确 support_talk=0」拦截；空对象/接口异常放行（RK3 固件适配缺陷，
        //    萤石官方靠内部白名单放行 RK3 微聊，第三方拿不到该白名单）
        when (val cap = ezvizRepository.getDeviceSupportTalkExplicit(deviceSerial)) {
            is NetworkResult.Success -> if (cap.data == 0) {
                Log.w(TAG, "设备明确不支持对讲（support_talk=0），拦截语音下发: $deviceSerial")
                dao.updateSendStatus(
                    rowId,
                    MessageEntity.SEND_STATUS_FAILED,
                    MessageEntity.CHANNEL_BOTH,
                    context.getString(R.string.message_send_once_unsupported)
                )
                return
            }
            is NetworkResult.Error ->
                Log.w(TAG, "能力查询失败，按规则放行尝试发送: ${cap.message}")
        }

        // 2. 双通道并行发送，任一成功即整体成功
        val dualOk = sendRecordDual(rowId, deviceSerial, recordFile)
        if (dualOk) {
            dao.updateSendStatus(rowId, MessageEntity.SEND_STATUS_SUCCESS, MessageEntity.CHANNEL_BOTH, "")
            return
        }

        // 3. 双通道失败 → 自动降级 sendonce 一次性下发（用户无感知）
        Log.i(TAG, "双通道发送失败，自动降级 sendonce 一次性下发: rowId=$rowId")
        val (onceOk, onceReason) = sendOnceFromFile(rowId, deviceSerial, recordFile, durationSec)
        dao.updateSendStatus(
            rowId,
            if (onceOk) MessageEntity.SEND_STATUS_SUCCESS else MessageEntity.SEND_STATUS_FAILED,
            if (onceOk) MessageEntity.CHANNEL_SENDONCE else MessageEntity.CHANNEL_BOTH,
            if (onceOk) "" else onceReason
        )
    }

    /** 双通道并行发送（videotalk + 云广播），互不阻塞；返回是否至少一条通路成功 */
    private suspend fun sendRecordDual(
        rowId: Long,
        deviceSerial: String,
        recordFile: File
    ): Boolean = coroutineScope {
        val talkDeferred = async { sendViaVoiceCall(deviceSerial) }
        val broadcastDeferred = async { sendViaBroadcast(deviceSerial, recordFile, rowId) }
        val talkResult = talkDeferred.await()
        val broadcastResult = broadcastDeferred.await()
        val failParts = listOfNotNull(talkResult.second, broadcastResult.second)
        if (!(talkResult.first || broadcastResult.first)) {
            Log.w(TAG, "双通道发送失败: ${failParts.joinToString("；")}")
        }
        talkResult.first || broadcastResult.first
    }

    /**
     * sendonce 一次性下发（双通道失败的降级通路）：
     * WAV → MediaCodec 转码 ADTS 裸 AAC（已是 aac 则跳过转码）→ 前置校验 → multipart 一步下发。
     *
     * 注意：萤石 sendonce 仅接受带 ADTS 头的裸 AAC 流，禁止直传 WAV、禁止 m4a 容器；
     * 转码/校验失败直接置 failReason，不回退 WAV（用户拍板）。
     */
    private suspend fun sendOnceFromFile(
        rowId: Long,
        deviceSerial: String,
        audioFile: File,
        durationSec: Int
    ): Pair<Boolean, String> {
        // 转码：WAV → ADTS 裸 AAC；已是 AAC（上轮降级产物重发）则直接复用
        val aacFile = if (audioFile.extension.lowercase() == "aac") {
            audioFile
        } else {
            val target = MessageFiles.newRecordAacFile(context)
            if (!WavToAacTranscoder.transcode(audioFile, target)) {
                MessageFiles.deleteQuietly(target)
                Log.e(TAG, "WAV→AAC 转码失败，无法执行 sendonce")
                return false to context.getString(R.string.message_transcode_failed)
            }
            // 转码成功：原 WAV 不再需要，本地音频路径切换为 AAC 产物
            MessageFiles.deleteQuietly(audioFile)
            dao.updateAudioInfo(rowId, target.absolutePath, durationSec)
            target
        }

        // 前置校验（时长/格式/大小/ADTS 同步字），提前拦截规避萤石
        // 「上传的语音文件长度不正确或文件格式错误」
        val invalidRes = SendOnceAudioValidator.validate(aacFile, durationSec)
        if (invalidRes != null) {
            MessageFiles.deleteQuietly(aacFile)
            Log.w(TAG, "sendonce 音频前置校验失败: $invalidRes")
            return false to context.getString(invalidRes)
        }

        val send = broadcastManager.sendOnceToDevice(deviceSerial, aacFile)
        return when (send) {
            is NetworkResult.Success -> {
                // 回填萤石消息 id（待实测字段；空值不影响功能）
                send.data?.takeIf { it.isNotBlank() }?.let { dao.updateRemoteId(rowId, it) }
                true to ""
            }
            is NetworkResult.Error -> false to mapSendOnceError(send.message)
        }
    }

    /** sendonce 错误文案映射（萤石原始错误 → 用户可读文案） */
    private fun mapSendOnceError(raw: String): String = when {
        raw.contains("60020") || raw.contains("离线") || raw.contains("不在线") ->
            context.getString(R.string.message_send_failed_device_offline)
        raw.contains("不支持") || raw.contains("support_talk") || raw.contains("权限") ->
            context.getString(R.string.message_send_once_unsupported)
        else -> raw
    }

    /** 通路①：EZOpenSDK 语音通话（实时对讲），发起成功即算通路成功 */
    private suspend fun sendViaVoiceCall(deviceSerial: String): Pair<Boolean, String?> {
        // 能力校验已在 sendRecordCascade 前置完成（明确不支持已拦截；空对象/异常已放行），
        // 通路内不再重复判断——RK3 capacity 空对象按规则放行后由通路实际结果决定。
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

    /** 通路②：云广播 REST（上传 → 下发设备播放；能力校验已在级联入口前置完成） */
    private suspend fun sendViaBroadcast(
        deviceSerial: String,
        audioFile: File,
        rowId: Long
    ): Pair<Boolean, String?> {
        val upload = broadcastManager.uploadVoiceFile(audioFile, "msg_$rowId")
        val url = upload.getOrNull() ?: return false to upload.errMsg()
        val send = broadcastManager.sendVoiceToDevice(deviceSerial, url)
        if (send.isError) return false to send.errMsg()
        return true to null
    }

    // ==================== 接收：设备留言拉取（仅视频） ====================

    /**
     * 拉取设备发来的留言（EZOpenSDK 微聊接口），仅消费设备视频留言并下载入库。
     *
     * 方向过滤：只入库 msgDirection=1（设备发给手机）的留言；
     * msgDirection=2（手机发出）跳过——App 发送侧在本地记录（录音/文字），
     * 云端若存在历史「手机发出」留言也不入库，避免与本地记录语义重复。
     *
     * 内容过滤（复刻萤石微聊单向留言行为）：
     * - contentType=1 设备语音留言 → 本项目忽略，打日志丢弃，不入库；
     * - contentType=2 设备视频留言 → 下载本地缓存（超 30M 只存云端 URL），
     *   入库 messageCategory=1（留言）、localVideoPath/videoCloudUrl/thumbUrl(msgPicUrl)；
     * - 其他未知类型打日志跳过。
     *
     * ⚠️ EZOpenSDK 留言接口为网络耗时操作，禁止主线程调用：
     * 整段逻辑（SDK 拉取/下载/落库）统一切 Dispatchers.IO 执行。
     * 下载失败只打日志不阻断——仍入库（localVideoPath 为空，点击时直连 videoCloudUrl 播放）。
     * @return 新增留言条数（-1 表示拉取失败）
     */
    suspend fun refreshDeviceMessages(deviceSerial: String, deviceName: String): Int =
        dedupMutex.withLock {
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

                // 内容类型过滤：type=1 设备语音忽略（需求：本项目不处理设备语音）
                if (msg.contentType == MSG_CONTENT_TYPE_VOICE) {
                    Log.i(TAG, "丢弃设备语音留言（type=1，本项目不处理）: msgId=$msgId")
                    continue
                }
                if (msg.contentType != MSG_CONTENT_TYPE_VIDEO) {
                    Log.w(TAG, "跳过未知类型设备留言: msgId=$msgId contentType=${msg.contentType}")
                    continue
                }

                // 按 remoteId 去重，避免重复入库
                if (dao.getByRemoteId(msgId) != null) continue

                Log.i(
                    TAG,
                    "拉取到设备视频留言: msgId=$msgId direction=${msg.msgDirection} " +
                        "contentType=${msg.contentType} status=${msg.msgStatus} duration=${msg.duration}s"
                )

                // 视频下载失败不阻断：仍入库，点击时直连 videoCloudUrl 播放
                val local = downloadLeaveContent(context, msg, msgId)
                val duration = if (msg.duration > 0) msg.duration
                else local?.let { MessageFiles.audioDurationSec(it) } ?: 0
                val rowId = dao.insertIgnore(
                    MessageEntity(
                        msgType = MessageEntity.MSG_TYPE_DEVICE,
                        // 设备留言 senderName 强制设备序列号格式（会话列表按发送方聚合），绝不落 null/空
                        senderName = MessageEntity.deviceSenderName(deviceSerial),
                        createTime = msg.createTime?.timeInMillis ?: System.currentTimeMillis(),
                        // 已读语义（javap 实测 EZMessageStatus）：1=未读，2=已读
                        isRead = msg.msgStatus == MSG_STATUS_READ,
                        deviceSerial = deviceSerial,
                        remoteId = msgId,
                        localVideoPath = local?.absolutePath ?: "",
                        videoCloudUrl = msg.cloudServerUrl.orEmpty(),
                        thumbUrl = msg.msgPicUrl.orEmpty(),
                        duration = duration,
                        messageCategory = MessageEntity.MESSAGE_CATEGORY_LEAVE_MSG
                    )
                )
                if (rowId > 0) added++
            }
                Log.i(TAG, "设备视频留言同步完成，新增 $added 条")
                return@withContext added
        }
        }

    /** 下载设备留言内容（视频）：优先 cloudServerUrl 直连（超 30M 放弃），失败兜底 SDK 数据流 */
    private suspend fun downloadLeaveContent(
        context: Context,
        msg: com.videogo.openapi.bean.EZLeaveMessage,
        msgId: String
    ): File? {
        val target = MessageFiles.newDeviceFile(context, msgId)
        if (target.exists() && target.length() > 0) return target

        val url = msg.cloudServerUrl
        if (!url.isNullOrBlank() &&
            downloadUrlToFile(url, target, MAX_VIDEO_DOWNLOAD_BYTES)
        ) return target

        // 兜底：SDK 流式下载（见 EzvizSdkManager.downloadLeaveMessageData）
        return if (sdkManager.downloadLeaveMessageData(msg, target)) target else null
    }

    /** 直连下载云端内容到本地（后台线程）；maxBytes 超限放弃下载返回 false */
    private suspend fun downloadUrlToFile(url: String, target: File, maxBytes: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                response.use {
                    if (!it.isSuccessful) return@withContext false
                    val body = it.body ?: return@withContext false
                    if (body.contentLength() > maxBytes) {
                        Log.w(TAG, "留言内容超过 ${maxBytes / 1024 / 1024}M 上限，跳过本地缓存: $url")
                        return@withContext false
                    }
                    body.byteStream().use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                }
                target.exists() && target.length() > 0
            } catch (e: Exception) {
                Log.e(TAG, "下载设备留言内容失败: $url", e)
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

    /** 删除留言：本地记录 + 本地音频/视频文件 + 云端留言（设备留言） */
    suspend fun deleteMessage(message: MessageEntity) {
        dao.delete(message)
        if (message.localAudioPath.isNotBlank()) {
            MessageFiles.deleteQuietly(File(message.localAudioPath))
        }
        if (message.localVideoPath.isNotBlank()) {
            MessageFiles.deleteQuietly(File(message.localVideoPath))
        }
        if (message.msgType == MessageEntity.MSG_TYPE_DEVICE && message.remoteId.isNotBlank()) {
            sdkManager.deleteLeaveMessages(listOf(message.remoteId))
        }
    }

    // ==================== 报警消息落库（消息分类：messageCategory = 2） ====================

    /**
     * 把萤石告警事件写入 message 表（messageCategory = 2，与留言同表按分类区分）。
     *
     * 幂等：alarmId 作为 remoteId 去重（dedupMutex 串行 + getByRemoteId 预查 +
     * insertIgnore 兜底并发）；已存在则只回写云端已读状态。
     * 入参 [alarms] 由调用方按授权设备序列号过滤后传入；
     * deviceSerial 按告警自带值入库，消息中心查询按当前设备过滤，天然满足「切换设备自动过滤」。
     *
     * @return 新写入条数
     */
    suspend fun saveAlertMessages(alarms: List<AlarmMessage>): Int = dedupMutex.withLock {
        withContext(Dispatchers.IO) {
            var added = 0
        for (alarm in alarms) {
            val alarmId = alarm.alarmId
            if (alarmId.isBlank()) continue

            // 幂等：已存在只同步已读状态（不重复插入）
            if (dao.getByRemoteId(alarmId) != null) {
                dao.updateIsReadByRemoteId(alarmId, alarm.isRead)
                continue
            }
            val rowId = dao.insertIgnore(
                MessageEntity(
                    msgType = MessageEntity.MSG_TYPE_ALERT,
                    // 修复 null bug：报警消息 senderName 强制设备序列号格式（UI 绝不渲染 null 文本）
                    senderName = MessageEntity.deviceSenderName(alarm.deviceSerial),
                    content = alarm.alarmName,
                    createTime = parseAlarmTime(alarm.alarmTime),
                    isRead = alarm.isRead,
                    deviceSerial = alarm.deviceSerial,
                    remoteId = alarmId,
                    sendStatus = MessageEntity.SEND_STATUS_SUCCESS,
                    sendChannel = MessageEntity.CHANNEL_NONE,
                    // 图片只进后端 alarm_events（全部抓拍页独享），Room 报警文字不落图字段
                    messageCategory = MessageEntity.MESSAGE_CATEGORY_ALERT
                )
            )
                if (rowId > 0) added++
            }
            return@withContext added
        }
        }

    /**
     * 标记报警消息已读（仅本地 Room）。
     * msgType=5 天然不触发 SDK 留言云端已读，与设备留言（msgType=3）路径隔离。
     */
    suspend fun markAlarmMessageRead(alarmId: String) {
        dao.getByRemoteId(alarmId)?.let { dao.markAsRead(it.id) }
    }

    /** 解析告警时间（yyyy-MM-dd HH:mm:ss）为毫秒时间戳，解析失败兜底当前时间 */
    private fun parseAlarmTime(alarmTime: String): Long {
        if (alarmTime.isBlank()) return System.currentTimeMillis()
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
            sdf.parse(alarmTime)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "告警时间解析失败: $alarmTime", e)
            System.currentTimeMillis()
        }
    }

    // ==================== 健康建议消息落库（医院端 → 家属端消息模块） ====================

    /**
     * 医院端医护录入健康建议 → message 表落库（msgType = MSG_TYPE_ADVICE，
     * messageCategory = 1 留言分类，家属端留言页/消息中心渲染独立气泡）。
     *
     * 约束：健康建议**不走萤石设备播报**（不调 clock/语音接口），仅在 App 消息模块查看；
     * 本方法只做本地插库，消息发送通路恒为 CHANNEL_NONE。
     *
     * @param deviceSerial 老人绑定设备序列号（为空则返回 -1——老人未绑定设备，
     * 家属端无消息入口可查，建议数据仍存 health_advice 表，仅不推消息）
     * @return 消息行 id；-1 = 未插入
     */
    suspend fun saveHealthAdviceMessage(
        deviceSerial: String,
        senderName: String,
        adviceContent: String
    ): Long {
        if (deviceSerial.isBlank()) {
            Log.w(TAG, "健康建议未推消息：老人未绑定设备")
            return -1L
        }
        val rowId = dao.insert(
            MessageEntity(
                msgType = MessageEntity.MSG_TYPE_ADVICE,
                senderName = senderName.ifBlank { context.getString(R.string.message_sender_doctor) },
                content = adviceContent,
                createTime = System.currentTimeMillis(),
                isRead = false,
                deviceSerial = deviceSerial,
                sendStatus = MessageEntity.SEND_STATUS_SUCCESS,
                sendChannel = MessageEntity.CHANNEL_NONE,
                messageCategory = MessageEntity.MESSAGE_CATEGORY_LEAVE_MSG
            )
        )
        Log.i(TAG, "健康建议消息落库: id=$rowId sender=$senderName")
        return rowId
    }

    // ==================== 工具 ====================

    /** 提取 NetworkResult 失败信息 */
    private fun NetworkResult<*>.errMsg(): String = when (this) {
        is NetworkResult.Error -> message
        is NetworkResult.Success -> ""
    }
}
