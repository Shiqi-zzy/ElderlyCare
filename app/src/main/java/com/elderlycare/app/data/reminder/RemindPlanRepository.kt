package com.elderlycare.app.data.reminder

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.EzvizRepository
import com.elderlycare.app.data.ezviz.LeaveMessageTextResponse
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.RtcBackendApi
import com.elderlycare.app.data.ezviz.TtsPreviewRequest
import com.elderlycare.app.data.message.MessageDao
import com.elderlycare.app.data.message.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 提醒计划仓库：萤石 v3 设备本地闹铃（life/remind/clock 系列）+ Room 本地存储。
 *
 * 数据流：
 * - 新增：App 表单 → POST clock（**不传音色**，设备播报为硬件固定音色）→ clockId 入库；
 * - 同步：打开列表页/日程页 → GET clock/list **按 clockId 差分对齐本地**
 *   （设备有→新增/更新，设备无→删本地脏行；保留本地 id 防详情页失效）；
 * - 播报完成：schedule/record 轮询 → 标记 executed + 留言表插系统消息；
 * - 删除：DELETE clock（纯 JSON 字符串数组 body）成功后才删 Room；
 * - 试听：文本+音色 → 后端 edge-tts 合成 mp3 落盘，ExoPlayer 播放本地文件。
 *
 * 错误处理：统一返回 NetworkResult（错误信息即展示文案）；
 * 轮询接口全程 try/catch 静默（轮询不能崩，失败不打扰用户）。
 */
class RemindPlanRepository(
    private val context: Context,
    private val planDao: RemindPlanDao,
    private val messageDao: MessageDao,
    private val reminderApi: EzvizReminderApi,
    private val ezvizRepository: EzvizRepository,
    private val rtcBackendApi: RtcBackendApi
) {

    companion object {
        private const val TAG = "RemindPlanRepository"

        /** 试听音频落盘文件名（覆盖写，仅存最新一条试听） */
        private const val PREVIEW_FILE_NAME = "remind_preview.mp3"
    }

    /** 计划列表（Flow 实时刷新，创建时间倒序） */
    fun observePlans(deviceSerial: String): Flow<List<RemindPlanEntity>> =
        planDao.observeByDeviceSerial(deviceSerial)

    /** 单条计划（详情页用；删除后发 null） */
    fun observePlanById(id: Long): Flow<RemindPlanEntity?> = planDao.observeById(id)

    // ==================== 列表同步（以设备为准覆盖本地） ====================

    /**
     * 拉取设备闹铃列表，与本地 Room 按 clockId 差分对齐：
     * - 设备有、本地没有 → 新增入库；
     * - 设备有、本地已有 → 更新字段（保留本地 id/executed/createTime）；
     * - 本地有、设备没有 → 删除本地脏记录（含 clockId 为空的未下发脏数据）。
     *
     * 差分 UPDATE 而非「wipe + 全量重插」：Room REPLACE 会换自增 id，
     * 导致详情页/列表持有的本地 id 失效（点条目 → 「计划不存在」的根因）。
     *
     * 容错底线：设备列表非空但所有条目 clockId 都解析为空（字段名猜测错误）时
     * **不动本地**，打 Warning 日志 + 返回 Error 提示校准。列表为空是设备侧
     * 确实无计划，清空本地。
     *
     * @return 成功时返回设备计划条数
     */
    suspend fun refreshFromDevice(deviceSerial: String): NetworkResult<Int> {
        val token = ezvizRepository.obtainValidToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")
        return try {
            val resp = reminderApi.listClocks(token, deviceSerial)
            if (resp.effectiveCode != 200) {
                Log.w(
                    TAG,
                    "拉取提醒计划列表失败: code=${resp.effectiveCode} msg=${resp.effectiveMsg}"
                )
                return NetworkResult.Error(
                    code = resp.effectiveCode.toString(),
                    message = mapEzvizError(resp.effectiveCode, resp.effectiveMsg)
                )
            }
            val list = resp.data ?: emptyList()
            val now = System.currentTimeMillis()
            val entities = list.mapNotNull { it.toEntity(deviceSerial, now) }
            // 容错：字段名猜错时所有条目 clockId 全空 → 不动本地，提示校准
            if (list.isNotEmpty() && entities.isEmpty()) {
                Log.w(TAG, "提醒计划列表字段待校准：所有条目 clockId 均解析为空，保留本地数据")
                return NetworkResult.Error(message = "列表字段待校准")
            }
            val local = planDao.getAllByDeviceSerial(deviceSerial)
            // 设备有 → 新增 / 更新（保留本地 id）
            for (entity in entities) {
                val existing = local.firstOrNull { it.clockId == entity.clockId }
                if (existing == null) {
                    planDao.insert(entity)
                    Log.i(TAG, "设备闹铃本地新增: clockId=${entity.clockId} tag=${entity.tag}")
                } else {
                    planDao.updateFromDevice(
                        id = existing.id,
                        tag = entity.tag,
                        content = entity.content,
                        timeHour = entity.timeHour,
                        timeMin = entity.timeMin,
                        repeatType = entity.repeatType,
                        weekdays = entity.weekdays,
                        year = entity.year,
                        month = entity.month,
                        day = entity.day
                    )
                    Log.i(TAG, "设备闹铃本地更新: clockId=${entity.clockId}")
                }
            }
            // 本地有、设备没有（含 clockId 空脏行）→ 删除
            val deviceClockIds = entities.map { it.clockId }.toSet()
            for (localPlan in local) {
                if (localPlan.clockId.isBlank() || localPlan.clockId !in deviceClockIds) {
                    Log.i(
                        TAG,
                        "清理本地脏记录: id=${localPlan.id} clockId=${localPlan.clockId} tag=${localPlan.tag}"
                    )
                    planDao.delete(localPlan)
                }
            }
            val localAfter = planDao.getAllByDeviceSerial(deviceSerial)
            Log.i(TAG, "提醒计划差分同步完成：设备 ${entities.size} 条，本地 ${localAfter.size} 条")
            NetworkResult.Success(entities.size)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "同步提醒计划失败：网络连接失败", e)
            NetworkResult.Error(message = "网络连接失败，请检查网络")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "同步提醒计划失败：请求超时", e)
            NetworkResult.Error(message = "请求超时，请稍后重试")
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "同步提醒计划失败: HTTP ${e.code()}", e)
            NetworkResult.Error(message = friendlyHttpError(e, "同步失败"))
        } catch (e: Exception) {
            Log.e(TAG, "同步提醒计划失败", e)
            NetworkResult.Error(message = e.message ?: "同步失败，请稍后重试")
        }
    }

    // ==================== 新增 / 删除 ====================

    /**
     * 新增计划：下发萤石闹铃接口 → 拿 clockId → 本地入库。
     * 失败透传萤石错误码映射文案（20007 设备不在线等），不入库。
     *
     * @return 成功时返回 clockId
     */
    suspend fun addPlan(deviceSerial: String, draft: PlanDraft): NetworkResult<String> {
        val token = ezvizRepository.obtainValidToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")
        val once = if (draft.repeatType == RemindPlanEntity.REPEAT_ONCE) 1 else 0
        val request = RemindClockCreateRequest(
            timeHour = draft.timeHour,
            timeMin = draft.timeMin,
            once = once,
            year = if (once == 1) draft.year else 0,
            month = if (once == 1) draft.month else 0,
            day = if (once == 1) draft.day else 0,
            weekdays = draft.weekdays.distinct().sorted(),
            content = draft.content,
            tag = draft.tag.ifBlank { "闹钟" }
        )
        return try {
            Log.i(TAG, "新增提醒计划请求: timeHour=${draft.timeHour} timeMin=${draft.timeMin} " +
                "once=$once weekdays=${request.weekdays} tag=${request.tag}")
            val resp = reminderApi.createClock(token, deviceSerial, request)
            if (resp.effectiveCode != 200) {
                Log.w(
                    TAG,
                    "新增提醒计划失败: code=${resp.effectiveCode} msg=${resp.effectiveMsg}"
                )
                return NetworkResult.Error(
                    code = resp.effectiveCode.toString(),
                    message = mapEzvizError(resp.effectiveCode, resp.effectiveMsg)
                )
            }
            val clockId = resp.data?.clockId.orEmpty()
            if (clockId.isBlank()) {
                Log.w(TAG, "新增提醒计划返回 clockId 为空，不入库")
                return NetworkResult.Error(message = "保存失败：接口未返回计划 id")
            }
            planDao.insert(
                RemindPlanEntity(
                    clockId = clockId,
                    tag = request.tag,
                    content = request.content,
                    timeHour = request.timeHour,
                    timeMin = request.timeMin,
                    repeatType = draft.repeatType,
                    weekdays = request.weekdays.joinToString(","),
                    year = request.year,
                    month = request.month,
                    day = request.day,
                    deviceSerial = deviceSerial,
                    createTime = System.currentTimeMillis()
                )
            )
            Log.i(TAG, "新增提醒计划成功: clockId=$clockId")
            NetworkResult.Success(clockId)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "新增提醒计划失败：网络连接失败", e)
            NetworkResult.Error(message = "网络连接失败，请检查网络")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "新增提醒计划失败：请求超时", e)
            NetworkResult.Error(message = "请求超时，请稍后重试")
        } catch (e: retrofit2.HttpException) {
            // 萤石 HTTP 4xx/5xx（如 400+meta 业务错误「指定的日期与星期不匹配」）：
            // 解析 errorBody 的 meta.message 给出友好文案
            Log.e(TAG, "新增提醒计划失败: HTTP ${e.code()}", e)
            NetworkResult.Error(message = friendlyHttpError(e, "保存失败"))
        } catch (e: Exception) {
            Log.e(TAG, "新增提醒计划失败", e)
            NetworkResult.Error(message = e.message ?: "保存失败，请稍后重试")
        }
    }

    /**
     * 删除计划：先删萤石闹铃（幂等，重复删也 200），成功后才删本地。
     * clockId 为空的本地脏数据（未成功下发）直接本地删除。
     */
    suspend fun deletePlan(deviceSerial: String, plan: RemindPlanEntity): NetworkResult<Unit> {
        if (plan.clockId.isBlank()) {
            Log.w(TAG, "删除无 clockId 的本地脏数据: id=${plan.id} tag=${plan.tag}")
            planDao.delete(plan)
            return NetworkResult.Success(Unit)
        }
        val token = ezvizRepository.obtainValidToken()
            ?: return NetworkResult.Error(message = "未登录或 Token 已过期，请重试")
        return try {
            val resp = reminderApi.deleteClocks(token, deviceSerial, listOf(plan.clockId))
            if (resp.effectiveCode != 200) {
                Log.w(
                    TAG,
                    "删除提醒计划失败: code=${resp.effectiveCode} msg=${resp.effectiveMsg}"
                )
                return NetworkResult.Error(
                    code = resp.effectiveCode.toString(),
                    message = mapEzvizError(resp.effectiveCode, resp.effectiveMsg)
                )
            }
            planDao.delete(plan)
            Log.i(TAG, "删除提醒计划成功: clockId=${plan.clockId}")
            NetworkResult.Success(Unit)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "删除提醒计划失败：网络连接失败", e)
            NetworkResult.Error(message = "网络连接失败，请检查网络")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "删除提醒计划失败：请求超时", e)
            NetworkResult.Error(message = "请求超时，请稍后重试")
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "删除提醒计划失败: HTTP ${e.code()}", e)
            NetworkResult.Error(message = friendlyHttpError(e, "删除失败"))
        } catch (e: Exception) {
            Log.e(TAG, "删除提醒计划失败", e)
            NetworkResult.Error(message = e.message ?: "删除失败，请稍后重试")
        }
    }

    /**
     * 设备核对：clock/list 查询该 clockId 是否仍存在于设备。
     * 接口失败/未登录返回 null（调用方保留页面展示 Room 数据，不做删除）。
     */
    suspend fun verifyClockExists(deviceSerial: String, clockId: String): Boolean? {
        val token = ezvizRepository.obtainValidToken() ?: return null
        return try {
            val resp = reminderApi.listClocks(token, deviceSerial)
            if (resp.effectiveCode != 200) return null
            resp.data?.any { it.effectiveClockId == clockId }
        } catch (e: Exception) {
            Log.w(TAG, "核对提醒计划是否存在失败: clockId=$clockId", e)
            null
        }
    }

    /** 仅删 Room 记录（设备侧 clock 已删除 / 本地脏数据清理），不调萤石删除接口 */
    suspend fun deleteLocalRecord(plan: RemindPlanEntity) {
        Log.i(TAG, "删除本地提醒计划记录（设备侧已不存在）: id=${plan.id} clockId=${plan.clockId}")
        planDao.delete(plan)
    }

    // ==================== 播报完成轮询（系统消息联动） ====================

    /** 轮询串行锁：留言页与日程页两个 ViewModel 都会轮询，防止检查-插入竞态重复插系统消息 */
    private val pollMutex = Mutex()

    /**
     * 轮询执行记录（今天 + 昨天两个日期，防跨零点漏抓），识别已播报完成的计划：
     * 1. 记录匹配本地计划（clockId 优先，tag 兜底），匹配不上跳过；
     * 2. remoteId = "remind_{clockId或tag}_{executeKey}" 去重（防止重复插系统消息）；
     * 3. markExecuted(plan) + 留言表插系统消息（MSG_TYPE_SYSTEM）。
     *
     * 容错底线：解析不出可用键就不插系统消息，绝不插脏数据。
     * 全程 try/catch 静默（轮询不能崩，失败打日志即可）。
     *
     * @return 新增系统消息条数
     */
    suspend fun pollExecutedAndInsert(deviceSerial: String): Int = pollMutex.withLock {
        val token = ezvizRepository.obtainValidToken() ?: return@withLock 0
        val localPlans = planDao.getAllByDeviceSerial(deviceSerial)
        if (localPlans.isEmpty()) return@withLock 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dates = listOf(
            dateFormat.format(Date()),
            dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)
        )

        var inserted = 0
        for (date in dates) {
            try {
                val resp = reminderApi.getScheduleRecords(token, deviceSerial, date)
                if (resp.effectiveCode != 200 || resp.data.isNullOrEmpty()) continue
                for (record in resp.data) {
                    val plan = findPlan(localPlans, record) ?: continue
                    val executeKey = record.effectiveExecuteKey
                        .ifBlank { "$date@${record.effectiveHour}:${record.effectiveMin}" }
                    val dedupeKey = "remind_${plan.clockId.ifBlank { plan.tag }}_${executeKey}"
                    if (messageDao.getByRemoteId(dedupeKey) != null) continue

                    val executeTimeMs = record.effectiveExecuteTimeMs
                        ?: System.currentTimeMillis()
                    messageDao.insert(
                        MessageEntity(
                            msgType = MessageEntity.MSG_TYPE_SYSTEM,
                            senderName = context.getString(R.string.reminder_system_sender),
                            content = context.getString(
                                R.string.reminder_system_msg_format, plan.tag
                            ),
                            createTime = executeTimeMs,
                            isRead = true,
                            deviceSerial = deviceSerial,
                            remoteId = dedupeKey,
                            sendStatus = MessageEntity.SEND_STATUS_SUCCESS,
                            sendChannel = MessageEntity.CHANNEL_BROADCAST
                        )
                    )
                    if (plan.executed != RemindPlanEntity.EXECUTED_YES) {
                        planDao.markExecuted(plan.id)
                    }
                    inserted++
                    Log.i(
                        TAG,
                        "识别到提醒计划已播报完成: tag=${plan.tag} clockId=${plan.clockId} date=$date"
                    )
                }
            } catch (e: Exception) {
                // 轮询静默：单日失败不影响另一天与下一轮
                Log.w(TAG, "轮询提醒计划执行记录失败: date=$date", e)
            }
        }
        inserted
    }

    /** 记录匹配本地计划：clockId 优先，tag 兜底 */
    private fun findPlan(
        plans: List<RemindPlanEntity>,
        record: RemindScheduleRecord
    ): RemindPlanEntity? {
        val cid = record.effectiveClockId
        if (cid.isNotBlank()) {
            plans.firstOrNull { it.clockId == cid }?.let { return it }
        }
        val tag = record.effectiveTag
        if (tag.isNotBlank()) {
            plans.firstOrNull { it.tag == tag }?.let { return it }
        }
        return null
    }

    // ==================== 手机试听 ====================

    /**
     * 手机试听：文本 + 音色 → 后端 edge-tts 合成 mp3 → 落盘本地文件。
     * 音色仅用于试听（设备播报为硬件固定音色），试听请求不触碰萤石。
     */
    suspend fun previewTts(text: String, voiceKey: String): NetworkResult<File> {
        return try {
            val body = rtcBackendApi.ttsPreview(TtsPreviewRequest(text = text, voice = voiceKey))
            // 后端错误也是 HTTP 200 + JSON（如边缘 TTS 合成失败）：
            // 用 content-type 区分，非 audio/* 解析 message 展示给用户
            val contentType = body.contentType()?.toString().orEmpty()
            if (!contentType.startsWith("audio/")) {
                val msg = runCatching {
                    body.use { Gson().fromJson(it.string(), LeaveMessageTextResponse::class.java).message }
                }.getOrDefault("").ifBlank { "试听合成失败，请稍后重试" }
                Log.w(TAG, "试听合成失败: $msg")
                return NetworkResult.Error(message = msg)
            }
            val target = File(context.filesDir, PREVIEW_FILE_NAME)
            body.byteStream().use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            if (target.exists() && target.length() > 0) {
                NetworkResult.Success(target)
            } else {
                NetworkResult.Error(message = "试听合成失败，请稍后重试")
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "试听合成失败：网络连接失败", e)
            NetworkResult.Error(message = "网络连接失败，请检查网络")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "试听合成失败：请求超时", e)
            NetworkResult.Error(message = "请求超时，请稍后重试")
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "试听合成失败: HTTP ${e.code()}", e)
            NetworkResult.Error(message = friendlyHttpError(e, "试听合成失败"))
        } catch (e: Exception) {
            Log.e(TAG, "试听合成失败", e)
            NetworkResult.Error(message = e.message ?: "试听合成失败，请稍后重试")
        }
    }

    // ==================== 工具 ====================

    /** HttpException → 友好文案：优先解析 errorBody 的 meta.message（如「指定的日期与星期不匹配」） */
    private fun friendlyHttpError(e: retrofit2.HttpException, prefix: String): String {
        val ezvizMsg = runCatching {
            val raw = e.response()?.errorBody()?.string().orEmpty()
            if (raw.isBlank()) "" else Gson().fromJson(raw, EzvizV3Response::class.java).effectiveMsg
        }.getOrDefault("")
        return if (ezvizMsg.isNotBlank()) "$prefix：$ezvizMsg"
        else "$prefix（HTTP ${e.code()}），请稍后重试"
    }

    /** 萤石常见错误码 → 用户可读文案（沿用文字留言 failReason 的展示风格） */
    private fun mapEzvizError(code: Int, msg: String): String = when (code) {
        20007 -> context.getString(R.string.reminder_error_device_offline)
        10005 -> context.getString(R.string.reminder_error_no_permission)
        else -> "萤石错误码 $code：${msg.ifBlank { "操作失败" }}"
    }
}

/**
 * 表单草稿（不含音色——音色只用于试听，不传给萤石）。
 * weekdays 由表单按 repeatType 组装：单次=[日期对应星期]、每日=全 7 天、每周=用户多选。
 */
data class PlanDraft(
    val tag: String,
    val content: String,
    val timeHour: Int,
    val timeMin: Int,
    val repeatType: Int,
    val weekdays: List<Int>,
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0
)
