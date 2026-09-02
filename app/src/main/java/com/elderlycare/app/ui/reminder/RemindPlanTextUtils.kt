package com.elderlycare.app.ui.reminder

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elderlycare.app.R
import com.elderlycare.app.data.reminder.RemindPlanEntity

/** 「HH:mm · 单次 · 2026-08-19 / 每日 / 每周 · 一、三、五」展示文案（列表页与留言 feed 共用） */
@Composable
fun remindPlanTimeRepeatText(plan: RemindPlanEntity): String {
    val time = stringResource(R.string.reminder_time_format, plan.timeHour, plan.timeMin)
    return when (plan.repeatType) {
        RemindPlanEntity.REPEAT_ONCE -> {
            // 日期字段可能缺失（同步数据不完整），缺省退化为「单次」
            val dateText = if (plan.year > 0) {
                stringResource(
                    R.string.reminder_repeat_once_format,
                    stringResource(R.string.reminder_date_format, plan.year, plan.month, plan.day)
                )
            } else {
                stringResource(R.string.reminder_repeat_once)
            }
            "$time · $dateText"
        }
        RemindPlanEntity.REPEAT_DAILY ->
            "$time · " + stringResource(R.string.reminder_repeat_daily)
        else ->
            "$time · " + stringResource(R.string.reminder_repeat_weekly_format, plan.remindWeekdayText())
    }
}

/** weekdays "1,3,5" → "一、三、五"（0=周日，对照 reminder_weekday_names 日一二三四五六） */
@Composable
fun RemindPlanEntity.remindWeekdayText(): String {
    val names = stringResource(R.string.reminder_weekday_names)
    return weekdays.split(",")
        .mapNotNull { it.toIntOrNull() }
        .filter { it in 0..6 }
        .joinToString("、") { names.getOrNull(it)?.toString() ?: "" }
}

/**
 * 计划是否匹配指定日期（日程页日期过滤）：
 * 单次=year/month/day 相等；每日=恒匹配；每周=所选日期的星期命中 weekdays。
 * 萤石星期 0=周日…6=周六；java.time DayOfWeek 1=周一…7=周日 → value % 7。
 */
fun remindPlanMatchesDate(plan: RemindPlanEntity, date: java.time.LocalDate): Boolean =
    when (plan.repeatType) {
        RemindPlanEntity.REPEAT_ONCE ->
            plan.year == date.year && plan.month == date.monthValue && plan.day == date.dayOfMonth
        RemindPlanEntity.REPEAT_DAILY -> true
        else -> (date.dayOfWeek.value % 7) in plan.weekdays.split(",")
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 0..6 }
    }
