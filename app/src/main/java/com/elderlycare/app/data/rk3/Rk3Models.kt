package com.elderlycare.app.data.rk3

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * RK3 局域网接口数据模型。
 *
 * 已确认字段（照实解析）：avgDepressionPercent / avgAnxietyPercent /
 * suggestionText / triggerLevelText / date。
 * 其余字段按多候选键**防御解析**：按序探测、取到非 null 的为准、JsonNull 视为未命中，
 * 任何类型异常返回 null（UI 显示「--」/空态），绝不冒泡崩溃。
 * 真机联调发现实际键名与本清单不符时，只需调整本文件候选键顺序。
 */

// ==================== 防御解析工具 ====================

private fun JsonObject.optString(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k ->
        runCatching { get(k)?.takeUnless { it.isJsonNull }?.asString }.getOrNull()
    }

private fun JsonObject.optNumber(vararg keys: String): Number? =
    keys.firstNotNullOfOrNull { k ->
        runCatching { get(k)?.takeUnless { it.isJsonNull }?.asNumber }.getOrNull()
    }

private fun JsonObject.optBoolean(vararg keys: String): Boolean? =
    keys.firstNotNullOfOrNull { k ->
        runCatching { get(k)?.takeUnless { it.isJsonNull }?.asBoolean }.getOrNull()
    }

private fun JsonObject.optFloat(vararg keys: String): Float? = optNumber(*keys)?.toFloat()

private fun JsonObject.optArray(vararg keys: String): JsonArray? =
    keys.firstNotNullOfOrNull { k ->
        runCatching { get(k)?.takeUnless { it.isJsonNull }?.asJsonArray }.getOrNull()
    }

// ==================== 展示工具（UI 复用） ====================

/** 百分比展示：null → unknown 占位（「--」），绝不显示 0% 乱数据 */
fun Float?.toPercentText(unknown: String): String = this?.let { "${it}%" } ?: unknown

/** 次数/计数展示：null → unknown 占位 */
fun Int?.toCountText(unknown: String): String = this?.toString() ?: unknown

// ==================== /api/health（实时状态 + 最近采集记录） ====================

data class Rk3HealthData(
    val detectStatus: String?,      // 运行中/待机 或设备原样字符串；null → UI「--」
    val frameCount: Long?,
    val faceCount: Long?,
    val recentCaptures: List<Rk3CaptureItem>
) {
    companion object {
        fun fromJson(obj: JsonObject?): Rk3HealthData? {
            if (obj == null) return null
            // 检测状态：优先布尔 isDetecting → 运行中/待机；否则取字符串状态原样展示
            val detectStatus: String? = when (obj.optBoolean("isDetecting", "detecting")) {
                true -> "运行中"
                false -> "待机"
                null -> obj.optString("detectStatus", "detectionStatus", "detectionState", "status")
            }
            val items = obj.optArray(
                "recentCaptures", "recentCaptureList", "captureRecords", "recentRecords", "captures", "records"
            )?.mapNotNull { Rk3CaptureItem.fromJson(it) } ?: emptyList()
            return Rk3HealthData(
                detectStatus = detectStatus,
                frameCount = obj.optNumber("frameCount", "frameTotal", "totalFrames", "frameNum")?.toLong(),
                faceCount = obj.optNumber("faceCount", "faceDetectedCount", "humanCount", "faceNum")?.toLong(),
                recentCaptures = items
            )
        }
    }
}

data class Rk3CaptureItem(
    val time: String?,              // 原始时间字符串/数字，UI 原样展示
    val emotion: String?,
    val depressionPercent: Float?,
    val anxietyPercent: Float?
) {
    companion object {
        fun fromJson(element: JsonElement?): Rk3CaptureItem? {
            val obj = runCatching { element?.takeUnless { it.isJsonNull }?.asJsonObject }
                .getOrNull() ?: return null
            return Rk3CaptureItem(
                time = obj.optString("time", "date", "captureTime", "timestamp", "createdAt"),
                emotion = obj.optString("emotion", "emotionLabel", "emotionType", "mood"),
                depressionPercent = obj.optFloat("depressionPercent", "avgDepressionPercent"),
                anxietyPercent = obj.optFloat("anxietyPercent", "avgAnxietyPercent")
            )
        }
    }
}

// ==================== /api/reports/weekly（周报） ====================

data class Rk3WeeklyData(
    val avgDepressionPercent: Float?,
    val avgAnxietyPercent: Float?,
    val peakDepressionPercent: Float?,
    val peakAnxietyPercent: Float?,
    val totalCaptureCount: Int?,
    val days: List<Rk3DayData>         // 7 条，按 date 升序 = 周一至周日
) {
    /** 有任一情绪数值才算有数据（days 存在但值全 null 视为空态，不画 0 值折线） */
    fun hasAnyValue(): Boolean = days.any { it.avgDepressionPercent != null || it.avgAnxietyPercent != null }

    companion object {
        fun fromJson(obj: JsonObject?): Rk3WeeklyData? {
            if (obj == null) return null
            return Rk3WeeklyData(
                avgDepressionPercent = obj.optFloat("avgDepressionPercent"),
                avgAnxietyPercent = obj.optFloat("avgAnxietyPercent"),
                peakDepressionPercent = obj.optFloat(
                    "peakDepressionPercent", "maxDepressionPercent", "depressionPeak", "depressionPeakPercent"
                ),
                peakAnxietyPercent = obj.optFloat(
                    "peakAnxietyPercent", "maxAnxietyPercent", "anxietyPeak", "anxietyPeakPercent"
                ),
                totalCaptureCount = obj.optNumber(
                    "totalCaptureCount", "totalCaptures", "captureCount", "collectionCount", "totalCount"
                )?.toInt(),
                days = obj.optArray("days", "dailyList", "dayList")
                    ?.mapNotNull { Rk3DayData.fromJson(it) }
                    ?.sortedBy { it.date }      // yyyy-MM-dd 字典序 = ISO 时间序
                    ?: emptyList()
            )
        }

        fun empty(): Rk3WeeklyData = Rk3WeeklyData(null, null, null, null, null, emptyList())
    }
}

data class Rk3DayData(
    val date: String,               // yyyy-MM-dd（日历页按此匹配当日）
    val avgDepressionPercent: Float?,
    val avgAnxietyPercent: Float?,
    val peakDepressionPercent: Float?,
    val peakAnxietyPercent: Float?,
    val captureCount: Int?
) {
    companion object {
        fun fromJson(element: JsonElement?): Rk3DayData? {
            val obj = runCatching { element?.takeUnless { it.isJsonNull }?.asJsonObject }
                .getOrNull() ?: return null
            return Rk3DayData(
                date = obj.optString("date").orEmpty(),
                avgDepressionPercent = obj.optFloat("avgDepressionPercent"),
                avgAnxietyPercent = obj.optFloat("avgAnxietyPercent"),
                peakDepressionPercent = obj.optFloat(
                    "peakDepressionPercent", "maxDepressionPercent", "depressionPeak", "depressionPeakPercent"
                ),
                peakAnxietyPercent = obj.optFloat(
                    "peakAnxietyPercent", "maxAnxietyPercent", "anxietyPeak", "anxietyPeakPercent"
                ),
                captureCount = obj.optNumber("captureCount", "collectionCount", "captureNum", "count")?.toInt()
            )
        }
    }
}

// ==================== /api/reports/yearly（年报） ====================

data class Rk3YearlyData(
    val avgDepressionPercent: Float?,
    val avgAnxietyPercent: Float?,
    val topMonthLabel: String?,     // 已归一化展示文本（如「3月」）
    val months: List<Rk3MonthData>  // 12 条，按 month 1-12 升序
) {
    fun hasAnyValue(): Boolean = months.any { it.avgDepressionPercent != null || it.avgAnxietyPercent != null }

    companion object {
        fun fromJson(obj: JsonObject?): Rk3YearlyData? {
            if (obj == null) return null
            return Rk3YearlyData(
                avgDepressionPercent = obj.optFloat("avgDepressionPercent"),
                avgAnxietyPercent = obj.optFloat("avgAnxietyPercent"),
                topMonthLabel = topMonthLabel(obj),
                months = obj.optArray("months", "monthList")
                    ?.mapNotNull { Rk3MonthData.fromJson(it) }
                    ?.filter { it.month != null }
                    ?.sortedBy { it.month }
                    ?: emptyList()
            )
        }

        fun empty(): Rk3YearlyData = Rk3YearlyData(null, null, null, emptyList())

        /** 最高月份归一化：「2026-03」/「03」/「3月」/数字 3 → 「3月」 */
        private fun topMonthLabel(obj: JsonObject): String? {
            val s = obj.optString("topMonth", "highestMonth", "peakMonth", "maxMonth")
            if (!s.isNullOrBlank()) {
                val m = Regex("""\d{1,2}""").find(s)?.value?.toIntOrNull()
                if (m != null && m in 1..12) return "${m}月"
                return s
            }
            val n = obj.optNumber("topMonth", "highestMonth", "peakMonth", "maxMonth")?.toInt()
            if (n != null && n in 1..12) return "${n}月"
            return null
        }
    }
}

data class Rk3MonthData(
    val month: Int?,                // 1-12（解析失败 null，被过滤）
    val avgDepressionPercent: Float?,
    val avgAnxietyPercent: Float?
) {
    companion object {
        fun fromJson(element: JsonElement?): Rk3MonthData? {
            val obj = runCatching { element?.takeUnless { it.isJsonNull }?.asJsonObject }
                .getOrNull() ?: return null
            return Rk3MonthData(
                month = monthNumber(obj),
                avgDepressionPercent = obj.optFloat("avgDepressionPercent"),
                avgAnxietyPercent = obj.optFloat("avgAnxietyPercent")
            )
        }

        /** 月份归一化：数字 3 / 字符串「03」「3月」「2026-03」 → Int 3 */
        private fun monthNumber(obj: JsonObject): Int? {
            obj.optNumber("month")?.toInt()?.takeIf { it in 1..12 }?.let { return it }
            val s = obj.optString("month", "monthLabel") ?: return null
            val m = Regex("""\d{1,2}""").find(s)?.value?.toIntOrNull()
            return m?.takeIf { it in 1..12 }
        }
    }
}

// ==================== /api/suggestions/latest（家属建议） ====================

data class Rk3SuggestionData(
    val suggestionText: String,
    val triggerLevelText: String?,
    val date: String?
) {
    companion object {
        fun fromJson(obj: JsonObject?): Rk3SuggestionData? {
            if (obj == null) return null
            return Rk3SuggestionData(
                suggestionText = obj.optString("suggestionText").orEmpty(),
                triggerLevelText = obj.optString("triggerLevelText"),
                date = obj.optString("date")
            )
        }
    }
}
