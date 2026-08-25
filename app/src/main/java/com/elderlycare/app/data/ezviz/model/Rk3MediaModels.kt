package com.elderlycare.app.data.ezviz.model

/**
 * RK3 点播 / 广播FM 数据模型（设备端媒体播放，与 sendOnce 留言广播完全独立）。
 *
 * ⚠️ contentId / fmId 的字段名与取值规则待萤石内部接口文档确认
 * 【待萤石商务开通权限，拿到官方抓包报文/内部PDF文档，再实现真实请求】，
 * 当前仅用于 UI 与 Mock 演示。
 */

/** 点播音频分类（UI Tab 顺序） */
enum class AudioCategory { RECOMMEND, MUSIC, OPERA, FAIRY_TALE, POETRY }

/** 广播FM 电台分组 */
enum class FmGroup { RECOMMEND, NATIONAL, LOCAL }

/** 点播音频条目（contentId = 萤石内容库音频 ID，待接口文档确认） */
data class AudioTrack(
    val contentId: String,
    val title: String,
    val subtitle: String = "",
    val durationSec: Int = 0,
    /** true = 需要「智控畅享」增值服务，点击弹提示（与萤石 App 行为一致） */
    val premium: Boolean = false
)

/** 广播FM 电台条目（fmId = 平台内置电台 ID，不支持自定义外部 URL） */
data class FmStation(
    val fmId: String,
    val name: String
)

/** 设备媒体播放状态（点播 / FM 共用） */
enum class MediaPlayState { IDLE, PLAYING, PAUSED }
