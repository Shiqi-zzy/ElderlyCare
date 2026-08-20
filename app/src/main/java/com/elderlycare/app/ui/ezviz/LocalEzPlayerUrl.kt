package com.elderlycare.app.ui.ezviz

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 组装本地 assets/ez-player.html 的 file:// 加载地址（带 query 参数）。
 *
 * HTML 侧约定（见 assets/ez-player.html 顶部注释）：
 * - mode/deviceSerial/channelNo/accessToken 必填，begin/end 仅 rec 必填；
 * - url 为 API 返回的原始 ezopen:// 地址（加密设备自带验证码），提供时 HTML 优先原样使用。
 */
object LocalEzPlayerUrl {

    fun build(
        mode: String,
        deviceSerial: String,
        channelNo: Int,
        accessToken: String,
        rawUrl: String,
        beginSec: Long? = null,
        endSec: Long? = null,
    ): String {
        val sb = StringBuilder("file:///android_asset/ez-player.html")
        sb.append("?mode=").append(mode)
        sb.append("&deviceSerial=").append(Uri.encode(deviceSerial))
        sb.append("&channelNo=").append(channelNo)
        sb.append("&accessToken=").append(Uri.encode(accessToken))
        sb.append("&url=").append(Uri.encode(rawUrl))
        if (beginSec != null) sb.append("&begin=").append(beginSec)
        if (endSec != null) sb.append("&end=").append(endSec)
        return sb.toString()
    }

    /** "yyyy-MM-dd HH:mm:ss"（设备本地时区）→ 秒级 Unix 时间戳；解析失败返回 null */
    fun toEpochSeconds(dateTime: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(dateTime)?.time?.div(1000L)
    } catch (e: Exception) {
        null
    }
}
