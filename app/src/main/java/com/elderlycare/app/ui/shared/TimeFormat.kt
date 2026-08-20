package com.elderlycare.app.ui.shared

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间戳 → 展示文本（项目内统一格式化工具）。
 * null / <=0 返回 "-"（申请/绑定的审核时间尚未写入时展示）。
 */
fun formatTimestamp(millis: Long?, pattern: String = "yyyy-MM-dd HH:mm"): String {
    if (millis == null || millis <= 0L) return "-"
    return runCatching {
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }.getOrElse { "-" }
}
