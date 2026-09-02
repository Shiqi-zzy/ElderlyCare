package com.elderlycare.app.data.reminder

/**
 * 提醒场景模板（添加计划弹窗）。
 * key 用 ASCII（进 route，防中文）；defaultTag/defaultContent 为表单预填值。
 */
enum class RemindTemplate(
    val key: String,
    val label: String,
    val defaultTag: String,
    val defaultContent: String
) {
    ALARM("alarm", "闹钟", "闹钟", "时间到了"),
    MEDICINE("medicine", "提醒吃药", "提醒吃药", "该吃药了"),
    BLOOD_PRESSURE("blood_pressure", "量血压", "量血压", "该量血压了"),
    BILL("bill", "生活缴费", "生活缴费", "该缴费了"),
    HOME_CARE("home_care", "离家看护", "离家看护", "出门注意安全"),
    DATE("date", "约会", "约会", "约会时间到了"),
    EXERCISE("exercise", "运动提醒", "运动提醒", "该运动了"),
    WORDS("words", "背单词提醒", "背单词提醒", "该背单词了"),
    ONLINE_COURSE("online_course", "上网课提醒", "上网课提醒", "该上网课了"),
    CUSTOM("custom", "自定义", "", "");

    companion object {
        /** 路由 key → 模板；未知 key 回落自定义（空表单） */
        fun fromKey(key: String): RemindTemplate = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

/**
 * 试听音色（edge-tts 中文音色，仅用于手机试听）。
 * 设备播报为硬件固定音色——保存计划时不传音色给萤石。
 */
data class PreviewVoice(val key: String, val displayName: String)

object PreviewVoices {
    val ALL = listOf(
        PreviewVoice("zh-CN-XiaoxiaoNeural", "晓晓（标准女声）"),
        PreviewVoice("zh-CN-XiaoyiNeural", "晓伊（活泼女声）"),
        PreviewVoice("zh-CN-YunjianNeural", "云健（沉稳男声）"),
        PreviewVoice("zh-CN-YunxiNeural", "云希（阳光男声）"),
        PreviewVoice("zh-CN-YunxiaNeural", "云夏（男童声）"),
        PreviewVoice("zh-CN-YunyangNeural", "云扬（新闻男声）")
    )

    val DEFAULT_KEY: String = ALL.first().key

    fun displayNameOf(key: String): String =
        ALL.firstOrNull { it.key == key }?.displayName ?: ALL.first().displayName
}
