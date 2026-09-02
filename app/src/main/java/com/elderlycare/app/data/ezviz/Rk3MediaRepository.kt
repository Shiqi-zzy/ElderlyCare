package com.elderlycare.app.data.ezviz

import com.elderlycare.app.data.ezviz.model.AudioCategory
import com.elderlycare.app.data.ezviz.model.AudioTrack
import com.elderlycare.app.data.ezviz.model.FmGroup
import com.elderlycare.app.data.ezviz.model.FmStation

/**
 * RK3 点播 / 广播FM 媒体能力仓库（网络层全部占位）。
 *
 * ⚠️⚠️⚠️ 重要约束（2026-08-23 拍板）⚠️⚠️⚠️
 * - 已知接口路径：点播 POST /v2/device/play/create、FM 广播 POST /v2/device/fm/create。
 * - 该组接口为萤石内部私有接口，open.ys7.com 公开文档无任何参数说明，
 *   且 AppKey 需萤石商务定向开通权限。
 * - 当前缺少：资源列表接口、全部入参出参、webhook 事件、业务错误码。
 * - 禁止猜测入参、禁止硬编码猜想 JSON 字段——以下全部方法只做占位，
 *   不产生任何可执行 HTTP 请求。
 *   //【待萤石商务开通权限，拿到官方抓包报文/内部PDF文档，再实现真实请求】
 *
 * 本仓库与 sendOnce 云广播留言（EZCloudBroadcastManager）完全独立、严禁混用：
 * - sendOnce = 手机留言 TTS 下发设备播报（已实现、已实测）；
 * - 点播/FM = 设备端媒体播放（内容库在设备/平台侧，不做音频下载到手机）。
 */
object Rk3MediaRepository {

    /** 占位统一失败文案（UI 层展示 toast，勿与具体错误码耦合） */
    private const val PENDING_MSG = "接口待萤石商务开通权限，暂不可用"

    /** 拉取点播音频列表（推荐/音乐/戏曲/童话故事/诗词跟学）。【占位：资源列表接口待确认】 */
    suspend fun getAudioList(category: AudioCategory): NetworkResult<List<AudioTrack>> =
        NetworkResult.Error(message = PENDING_MSG)

    /** 创建点播播放（POST /v2/device/play/create；deviceSerial/contentId 之外入参待补）。【占位】 */
    suspend fun createPlay(deviceSerial: String, contentId: String): NetworkResult<Unit> =
        NetworkResult.Error(message = PENDING_MSG)

    /** 暂停点播播放。【占位】 */
    suspend fun pausePlay(deviceSerial: String): NetworkResult<Unit> =
        NetworkResult.Error(message = PENDING_MSG)

    /** 停止点播播放。【占位】 */
    suspend fun stopPlay(deviceSerial: String): NetworkResult<Unit> =
        NetworkResult.Error(message = PENDING_MSG)

    /** 查询设备播放状态。【占位：正常链路不轮询、靠 webhook 回调，此接口仅异常兜底】 */
    suspend fun getPlayStatus(deviceSerial: String): NetworkResult<Unit> =
        NetworkResult.Error(message = PENDING_MSG)

    /** 拉取广播FM电台列表（推荐/国家广播/地方广播）。【占位：电台列表接口待确认】 */
    suspend fun getFmList(group: FmGroup): NetworkResult<List<FmStation>> =
        NetworkResult.Error(message = PENDING_MSG)

    /** 开启FM电台播放（POST /v2/device/fm/create；fmId = 平台内置电台 ID，不支持自定义外部 URL）。【占位】 */
    suspend fun createFm(deviceSerial: String, fmId: String): NetworkResult<Unit> =
        NetworkResult.Error(message = PENDING_MSG)

    /** 停止FM播放。【占位】 */
    suspend fun stopFm(deviceSerial: String): NetworkResult<Unit> =
        NetworkResult.Error(message = PENDING_MSG)
}
