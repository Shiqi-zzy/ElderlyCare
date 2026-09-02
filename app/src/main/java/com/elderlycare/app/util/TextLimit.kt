package com.elderlycare.app.util

/**
 * 按 Unicode 码点截断文本。
 * 直接用 String.take() 按 UTF-16 单元截断会把 emoji/生僻字截成两半产生非法字符，
 * 文字留言/闹铃播报内容（≤20 字符）必须按码点边界截断。
 */
fun String.limitCodePoints(max: Int): String {
    if (max <= 0) return ""
    if (codePointCount(0, length) <= max) return this
    return buildString {
        var count = 0
        var i = 0
        while (i < length && count < max) {
            val cp = codePointAt(i)
            appendCodePoint(cp)
            count++
            i += Character.charCount(cp)
        }
    }
}
