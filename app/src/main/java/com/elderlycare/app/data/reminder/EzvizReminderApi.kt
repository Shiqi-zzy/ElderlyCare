package com.elderlycare.app.data.reminder

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 萤石 v3 提醒计划（设备本地闹铃）接口。
 *
 * 业务：App 下发闹铃到 RK3 设备本地（设备时钟 + 设备本地 TTS 到点播报），
 * 不走云广播 task/create、不消耗云广播配额、不耦合 EZOpenSDK。
 *
 * 项目首个 @Header/@HTTP/@DELETE 用法：
 * - 鉴权：header `accessToken`（复用 EzvizRepository.obtainValidToken() 登录态）+
 *   header `deviceSerial`（来自 DeviceBindingStore）；
 * - 删除接口 body 是**纯 JSON 字符串数组** ["clockId",...]（实测确认，
 *   Content-Type application/json;charset=UTF-8，Retrofit + Gson 默认即可）。
 *
 * 实测事实（2026-08-19）：新增/列表/执行记录/删除四接口全部 200 验证通过。
 */
interface EzvizReminderApi {

    /** 新增闹铃：返回 data.clockId（删除凭据、同步关联键） */
    @POST("api/v3/device/life/remind/clock")
    suspend fun createClock(
        @Header("accessToken") accessToken: String,
        @Header("deviceSerial") deviceSerial: String,
        @Body body: RemindClockCreateRequest
    ): EzvizV3Response<RemindClockCreateData>

    /** 闹铃列表（deviceSerial 走 query，实测确认） */
    @GET("api/v3/device/life/remind/clock/list")
    suspend fun listClocks(
        @Header("accessToken") accessToken: String,
        @Query("deviceSerial") deviceSerial: String
    ): EzvizV3Response<List<RemindClockItem>>

    /** 执行记录：form date=yyyy-MM-dd */
    @FormUrlEncoded
    @POST("api/v3/device/life/remind/clock/schedule/record")
    suspend fun getScheduleRecords(
        @Header("accessToken") accessToken: String,
        @Header("deviceSerial") deviceSerial: String,
        @Field("date") date: String
    ): EzvizV3Response<List<RemindScheduleRecord>>

    /** 删除闹铃：body 为纯 JSON 字符串数组，幂等（重复删也回 200） */
    @HTTP(method = "DELETE", path = "api/v3/device/life/remind/clock", hasBody = true)
    suspend fun deleteClocks(
        @Header("accessToken") accessToken: String,
        @Header("deviceSerial") deviceSerial: String,
        @Body clockIds: List<String>
    ): EzvizV3Response<Any>
}

/**
 * v3 统一包装：{"meta":{"code":200,"message":"..."},"data":...}。
 * 顶层再冗余放 code/msg 兜底（容错：部分 v3 接口可能回退顶层 code/msg 结构）。
 */
data class EzvizV3Response<T>(
    val meta: EzvizV3Meta? = null,
    val data: T? = null,
    val code: Int? = null,
    val msg: String = ""
) {
    /** 有效业务码：meta.code 优先，顶层 code 兜底 */
    val effectiveCode: Int get() = meta?.code ?: (code ?: -1)

    /** 有效错误信息：meta.message 优先，顶层 msg 兜底 */
    val effectiveMsg: String get() = meta?.message?.takeIf { it.isNotBlank() } ?: msg
}

data class EzvizV3Meta(val code: Int = -1, val message: String = "")

/** 新增闹铃请求体（官方文档 help/5218 参数表） */
data class RemindClockCreateRequest(
    val timeHour: Int,
    val timeMin: Int,
    /** 0=重复 1=单次 */
    val once: Int,
    /** 仅单次有意义，重复计划传 0 */
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    /** 必选，不可重复：0=周日…6=周六；单次=日期对应星期，每日=全 7 天 */
    val weekdays: List<Int> = emptyList(),
    /** 播报内容，≤20 字符（含标点） */
    val content: String = "",
    /** 标题，≤50 字符，默认"闹钟" */
    val tag: String = "闹钟"
)

/** 新增闹铃返回 data */
data class RemindClockCreateData(val clockId: String = "")

/**
 * 闹铃列表项 —— ⚠️ 字段结构待真机实测：首次创建计划后拉一次 list 对照
 * logcat（OkHttp BODY 拦截器全量 JSON）修正字段名。
 * 当前用 nullable + 别名字段兜底（clockId/id、timeHour/hour、weekdays/weeks），
 * 任何字段缺失都不会导致解析失败。
 */
data class RemindClockItem(
    val clockId: String? = null,
    val id: String? = null,
    val tag: String? = null,
    val name: String? = null,
    val content: String? = null,
    val timeHour: Int? = null,
    val hour: Int? = null,
    val timeMin: Int? = null,
    val minute: Int? = null,
    val once: Int? = null,
    val repeatType: Int? = null,
    val weekdays: List<Int>? = null,
    val weeks: List<Int>? = null,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val enabled: Int? = null
) {
    val effectiveClockId: String get() = clockId?.takeIf { it.isNotBlank() } ?: (id ?: "")
    val effectiveTag: String get() = tag?.takeIf { it.isNotBlank() } ?: (name ?: "")
    val effectiveHour: Int get() = timeHour ?: (hour ?: 0)
    val effectiveMin: Int get() = timeMin ?: (minute ?: 0)
    val effectiveWeekdays: List<Int> get() = weekdays ?: (weeks ?: emptyList())

    /** 映射为本地实体；clockId 解析不出来返回 null（调用方按「字段待校准」处理） */
    fun toEntity(deviceSerial: String, createTime: Long): RemindPlanEntity? {
        val cid = effectiveClockId
        if (cid.isBlank()) return null
        val days = effectiveWeekdays.distinct().sorted()
        val repeat = when {
            once == 1 -> RemindPlanEntity.REPEAT_ONCE
            days.size >= 7 -> RemindPlanEntity.REPEAT_DAILY
            else -> RemindPlanEntity.REPEAT_WEEKLY
        }
        return RemindPlanEntity(
            clockId = cid,
            tag = effectiveTag,
            content = content ?: "",
            timeHour = effectiveHour,
            timeMin = effectiveMin,
            repeatType = repeat,
            weekdays = days.joinToString(","),
            year = year ?: 0,
            month = month ?: 0,
            day = day ?: 0,
            deviceSerial = deviceSerial,
            createTime = createTime
        )
    }
}

/**
 * 执行记录项 —— ⚠️ 结构同样待实测：假设含 clockId/tag/executeTime，
 * 若不匹配真机返回再校准（容错底线：解析不出可用键就不插系统消息）。
 */
data class RemindScheduleRecord(
    val clockId: String? = null,
    val id: String? = null,
    val tag: String? = null,
    val content: String? = null,
    val date: String? = null,
    /** 假设：执行时间字符串（yyyy-MM-dd HH:mm:ss 等） */
    val executeTime: String? = null,
    /** 假设：执行时间毫秒时间戳 */
    val executeTimeMs: Long? = null,
    val timeHour: Int? = null,
    val hour: Int? = null,
    val timeMin: Int? = null,
    val minute: Int? = null,
    val status: Int? = null
) {
    val effectiveClockId: String get() = clockId?.takeIf { it.isNotBlank() } ?: (id ?: "")
    val effectiveTag: String get() = tag ?: ""
    val effectiveHour: Int get() = timeHour ?: (hour ?: 0)
    val effectiveMin: Int get() = timeMin ?: (minute ?: 0)

    /** 去重键素材：优先执行时间字符串，其次毫秒时间戳，都缺失由调用方兜底 */
    val effectiveExecuteKey: String
        get() = executeTime?.takeIf { it.isNotBlank() }
            ?: (executeTimeMs?.takeIf { it > 0 }?.toString() ?: "")

    val effectiveExecuteTimeMs: Long? get() = executeTimeMs?.takeIf { it > 0 }
}
