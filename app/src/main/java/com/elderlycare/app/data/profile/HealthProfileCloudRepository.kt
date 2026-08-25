package com.elderlycare.app.data.profile

import android.util.Log
import com.elderlycare.app.data.ezviz.BackendBaseResponse
import com.elderlycare.app.data.ezviz.HealthProfileUploadRequest
import com.elderlycare.app.data.ezviz.RtcBackendApi
import com.elderlycare.app.data.model.ElderlyProfile
import com.google.gson.Gson

/**
 * 健康档案云同步仓库（自家后端 profile_routes，与萤石云无关）。
 *
 * - 上传：家属端保存档案后 fire-and-forget，失败仅记日志不阻断本地保存；
 * - 拉取：机构端授权后查看，返回档案 JSON 原文；失败返回 null（UI 静默回退本地展示）。
 * 后端不做鉴权，权限由 App 侧把关（UserDetailScreen 权限校验通过后才拉取）。
 */
class HealthProfileCloudRepository(private val api: RtcBackendApi) {

    private val gson = Gson()

    /** 云端档案拉取结果（profileJson = 档案原文，updatedAt = 后端秒级时间戳） */
    data class CloudProfileData(
        val profileJson: String = "",
        val updatedAt: Long = 0L
    )

    /** 上传档案（fire-and-forget）：userId 空跳过；失败仅记日志。 */
    suspend fun upload(profile: ElderlyProfile) {
        if (profile.userId.isBlank()) {
            Log.w(TAG, "健康档案上传跳过：userId 为空")
            return
        }
        val err = runCatching {
            val resp = api.uploadHealthProfile(
                HealthProfileUploadRequest(
                    userId = profile.userId,
                    deviceSn = profile.deviceSn,
                    profileJson = gson.toJson(profile)
                )
            )
            if (resp.code == 200) null else resp.message.ifBlank { "后端返回错误" }
        }.getOrElse { it.message ?: "网络异常" }
        if (err != null) {
            Log.w(TAG, "健康档案上传失败 userId=${profile.userId}: $err")
        } else {
            Log.d(TAG, "健康档案上传成功 userId=${profile.userId}")
        }
    }

    /** 拉取云端档案；无档案/失败返回 null（UI 静默回退本地）。 */
    suspend fun fetch(userId: String): CloudProfileData? {
        if (userId.isBlank()) return null
        return runCatching {
            val resp = api.getHealthProfile(userId)
            if (resp.code != 200) {
                Log.w(TAG, "云端档案拉取失败 userId=$userId code=${resp.code} msg=${resp.message}")
                null
            } else {
                val data = resp.data?.asJsonObject ?: return@runCatching null
                val json = data.get("profileJson")
                    ?.takeUnless { it.isJsonNull }?.asString ?: return@runCatching null
                CloudProfileData(
                    profileJson = json,
                    updatedAt = data.get("updatedAt")?.takeUnless { it.isJsonNull }?.asLong ?: 0L
                )
            }
        }.getOrElse {
            Log.w(TAG, "云端档案拉取异常 userId=$userId", it)
            null
        }
    }

    private companion object {
        const val TAG = "HealthProfileCloud"
    }
}
