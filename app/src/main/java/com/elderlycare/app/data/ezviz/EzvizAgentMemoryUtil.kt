package com.elderlycare.app.data.ezviz

import android.util.Log
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.buildAgentMemoryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 萤石智能体长期记忆上报工具（家属端 6 步档案最终提交后调用）。
 *
 * 本代码仅完成档案写入智能体长期记忆；
 * SOP工作流侧必须开启【读取长期记忆】开关，才能读取这份画像，
 * SOP逻辑不由本模块实现。（现在还没开启）
 *
 * 现状说明（2026-08-20 与萤石开放平台控制台核对）：
 * 1. 萤石未对外开放独立的长期记忆 HTTP 接口（memory* 路径均为 404）；
 *    长期记忆只能由智能体内 SOP 工作流的「记忆写入」节点操作。
 *    后续由萤石提供 SOP 工作流对外触发接口后，填入 [AGENT_MEMORY_API_PATH] 即可打通；
 *    请求/响应/全量覆盖语义均已按该形态预留（mode=cover，整份替换旧记忆，
 *    禁止增量追加，防止记忆堆积混乱）。
 * 2. 设备序列号→智能体 appId 无查询 API（绑定在控制台「智能体分发」页人工完成），
 *    由 [DEVICE_AGENT_MAP] 人工抄取维护；命中失败时兜底取账号下智能体列表第一个。
 *
 * 异常处理：token 失败 / 网络错误 / 业务错误全部完整打印请求与响应日志；
 * 绝不抛异常、绝不阻断表单提交、不弹窗——失败仅记日志，留给后续重试逻辑。
 */
object EzvizAgentMemoryUtil {

    private const val TAG = "EzvizAgentMemory"

    /**
     * 长期记忆写入接口路径（相对 EZVIZ_BASE_URL）。
     * 待萤石提供「SOP 工作流记忆写入」对外触发接口后填入（如
     * "api/service/open/intelligent/agent/app/memory/update"）；
     * 当前外部无此接口，留空 = 跳过 HTTP 请求，仅生成画像并打日志。
     */
    private const val AGENT_MEMORY_API_PATH = ""

    /** 智能体长期记忆文本上限（萤石未公开官方上限，保守取值，可调） */
    private const val MAX_AGENT_MEMORY_LENGTH = 4000

    /**
     * 设备序列号 → 智能体 appId 分发映射。
     * 萤石无 API 可查（绑定在控制台「智能体分发」页人工完成），本表人工抄取维护；
     * 设备/智能体变更时需同步更新。key = 第 6 步绑定的 RK3 设备序列号。
     */
    private val DEVICE_AGENT_MAP: Map<String, String> = mapOf(
        "BK9267115" to "5e5b093a0f074903a2e1" // 老人居家管理（控制台分发页人工抄取）
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * 完整上报链路：解析智能体ID → 组装画像文本 → 全量覆盖更新长期记忆。
     * 任意环节失败仅打日志、绝不抛出（调用方 fire-and-forget，不阻断表单流程）。
     */
    suspend fun reportElderlyProfile(profile: ElderlyProfile) {
        try {
            val serial = profile.deviceSn
            if (serial.isBlank()) {
                Log.w(TAG, "未绑定 RK3 设备（deviceSn 为空），跳过记忆上报")
                return
            }
            val agentId = resolveAgentId(serial)
            if (agentId == null) {
                Log.w(TAG, "设备 $serial 无对应智能体ID（控制台分发映射未收录），跳过记忆上报")
                return
            }
            val profileText = profile.buildAgentMemoryText(agentId)
            Log.d(TAG, "画像文本已生成（${profileText.length} 字）:\n$profileText")
            updateAgentMemory(agentId, profileText)
        } catch (t: Throwable) {
            Log.e(TAG, "记忆上报异常（不影响表单流程）", t)
        }
    }

    /** 解析设备对应智能体ID：人工分发映射优先，兜底取账号智能体列表第一个 */
    private suspend fun resolveAgentId(deviceSerial: String): String? {
        DEVICE_AGENT_MAP[deviceSerial]?.let {
            Log.d(TAG, "智能体ID命中人工分发映射：$deviceSerial → $it")
            return it
        }
        return withContext(Dispatchers.IO) {
            val token = getEzvizAccessToken() ?: return@withContext null
            val request = Request.Builder()
                .url("${BuildConfig.EZVIZ_BASE_URL}api/service/open/intelligent/agent/app/list?accessToken=$token")
                .get()
                .build()
            Log.d(TAG, "GET api/service/open/intelligent/agent/app/list（智能体ID兜底解析）")
            try {
                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    Log.d(TAG, "app/list 响应: HTTP ${resp.code} $body")
                    if (!resp.isSuccessful) return@withContext null
                    val appId = JSONObject(body)
                        .optJSONObject("data")
                        ?.optJSONArray("appList")
                        ?.optJSONObject(0)
                        ?.optString("appId")
                        ?.takeIf { it.isNotBlank() }
                    if (appId != null) Log.d(TAG, "智能体ID兜底解析：$appId")
                    appId
                }
            } catch (e: Exception) {
                Log.e(TAG, "app/list 查询异常", e)
                null
            }
        }
    }

    /**
     * 获取萤石 accessToken（官方 token 接口；凭证来自 gradle.properties
     * 经 BuildConfig 注入，绝不硬编码）。失败返回 null，完整打印请求与响应日志。
     */
    suspend fun getEzvizAccessToken(): String? = withContext(Dispatchers.IO) {
        val appKey = BuildConfig.EZVIZ_APP_KEY
        val appSecret = BuildConfig.EZVIZ_APP_SECRET
        if (appKey.isBlank() || appSecret.isBlank()) {
            Log.e(TAG, "EZVIZ_APP_KEY / EZVIZ_APP_SECRET 未配置（gradle.properties）")
            return@withContext null
        }
        val form = FormBody.Builder()
            .add("appKey", appKey)
            .add("appSecret", appSecret)
            .build()
        val request = Request.Builder()
            .url("${BuildConfig.EZVIZ_BASE_URL}api/lapp/token/get")
            .post(form)
            .build()
        Log.d(TAG, "POST api/lapp/token/get appKey=$appKey") // appSecret 不打日志
        try {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "token/get 响应: HTTP ${resp.code} $body")
                if (!resp.isSuccessful) return@withContext null
                val token = JSONObject(body)
                    .optJSONObject("data")
                    ?.optString("accessToken")
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "token/get 未返回 accessToken")
                    null
                } else {
                    Log.d(TAG, "accessToken 获取成功")
                    token
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取 accessToken 异常", e)
            null
        }
    }

    /**
     * 全量覆盖更新智能体长期记忆。
     * - 每次档案提交都整份覆盖旧记忆（mode=cover），禁止增量追加，防止记忆堆积混乱；
     * - 文本超出 [MAX_AGENT_MEMORY_LENGTH] 时打警告日志并安全截断；
     * - 失败仅记日志并返回 false，绝不抛异常、绝不阻断表单。
     */
    suspend fun updateAgentMemory(agentId: String, profileText: String): Boolean {
        val safeText = applyLengthLimit(profileText)

        if (AGENT_MEMORY_API_PATH.isBlank()) {
            Log.w(
                TAG,
                "长期记忆写入接口尚未开通（萤石无对外 memory API，待 SOP 工作流触发接口补充后填入 AGENT_MEMORY_API_PATH）；本次跳过 HTTP 上报，画像文本已生成"
            )
            return false
        }

        return withContext(Dispatchers.IO) {
            val token = getEzvizAccessToken()
            if (token == null) {
                Log.e(TAG, "accessToken 获取失败，记忆上报失败（不阻断表单）")
                return@withContext false
            }
            val url = "${BuildConfig.EZVIZ_BASE_URL}${AGENT_MEMORY_API_PATH}"
            // 全量覆盖语义：mode=cover（整份替换旧记忆），禁止增量追加
            val form = FormBody.Builder()
                .add("accessToken", token)
                .add("agentId", agentId)
                .add("memoryContent", safeText)
                .add("mode", "cover")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("accessToken", token)
                .post(form)
                .build()
            Log.d(TAG, "POST $url agentId=$agentId contentLength=${safeText.length} mode=cover")
            try {
                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    Log.d(TAG, "updateAgentMemory 响应: HTTP ${resp.code} $body")
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "记忆更新业务失败 HTTP ${resp.code}（不阻断表单，仅记录日志）")
                        return@withContext false
                    }
                    Log.d(TAG, "智能体长期记忆全量覆盖更新成功")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "记忆更新网络异常（不阻断表单，仅记录日志）", e)
                false
            }
        }
    }

    /** 长度保护：超上限打警告日志 + 安全截断 */
    private fun applyLengthLimit(text: String): String {
        if (text.length <= MAX_AGENT_MEMORY_LENGTH) return text
        Log.w(TAG, "画像文本 ${text.length} 字超出记忆上限 $MAX_AGENT_MEMORY_LENGTH 字，已安全截断")
        return text.take(MAX_AGENT_MEMORY_LENGTH)
    }
}
