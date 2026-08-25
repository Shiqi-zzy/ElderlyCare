package com.elderlycare.app.ui.reminder

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.data.rk3.toCountText
import com.elderlycare.app.data.rk3.toPercentText
import com.elderlycare.app.ui.theme.OnPrimary
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import java.time.LocalDate

/**
 * 日程 Tab（聚合总览）：按日期过滤展示提醒计划（只读）。
 *
 * - 标题「日程」，右上角【提醒计划】入口（UI 与留言页右上角按钮一致）→ 跳提醒计划页；
 * - 顶部横向星期日期 Tab（14 天，可横向滑动）：星期 + 日期数字，当天标记「今」，
 *   选中/今天蓝色（Primary）高亮，点击切换选中日期；
 * - 下方列表只展示所选日期的提醒计划（Room 数据 + 日期过滤），无计划时
 *   空态「当日暂无提醒计划」；卡片沿用提醒计划卡片 UI，点击跳详情；
 * - 进入页面默认选中今天；VM init 自动同步设备闹铃（clock/list）+ 轮询执行记录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindPlanCalendarScreen(
    onNavigateToRemindPlan: () -> Unit,
    onPlanClick: (RemindPlanEntity) -> Unit
) {
    val context = LocalContext.current
    val viewModel: RemindPlanViewModel = viewModel()
    // 情绪日卡 VM：选中日期变化时调 RK3 周报接口匹配当日数据（提醒计划功能不受影响）
    val emotionViewModel: Rk3DailyEmotionViewModel = viewModel()

    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val emotionState by emotionViewModel.uiState.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    // 横向日期条：今天起 14 天
    val days = remember { (0L..13L).map { today.plusDays(it) } }

    // 切换选中日期 → 加载对应情绪日卡（进页面默认今天，也会触发一次）
    LaunchedEffect(selectedDate) {
        emotionViewModel.load(selectedDate)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.reminder_calendar_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    // 右上角【提醒计划】入口（与留言页右上角按钮完全一致）
                    TextButton(onClick = onNavigateToRemindPlan) {
                        Icon(
                            Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.message_reminder_plan))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 星期日期横向 Tab
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days, key = { it.toEpochDay() }) { date ->
                    DateStripItem(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        onClick = { selectedDate = date }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 情绪日卡：所选日期的抑郁/焦虑均值、采集次数（RK3 局域网周报接口）
            EmotionDayCard(emotionState)

            // 所选日期的提醒计划（Room 全量 + 日期过滤）
            val dayPlans = plans.filter { remindPlanMatchesDate(it, selectedDate) }
            if (dayPlans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.reminder_calendar_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHint
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dayPlans, key = { it.id }) { plan ->
                        RemindPlanCard(
                            plan = plan,
                            onClick = { onPlanClick(plan) },
                            showDeleteButton = false,
                            onDeleteClick = { }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

/** 日期条单项：星期 + 日期数字（当天显示「今」）；选中项 Primary 高亮 */
@Composable
private fun DateStripItem(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Primary else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                weekdayLabel(date),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) OnPrimary else TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                // 当天标记「今」，其余显示日期数字
                if (isToday) stringResource(R.string.reminder_calendar_today)
                else date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> OnPrimary
                    isToday -> Primary
                    else -> TextPrimary
                }
            )
        }
    }
}

/**
 * 情绪日卡：标题 + 局域网小字提示 + 三列指标。
 * 状态语义：loading → 转圈；error → 统一降级文案；day!=null → 指标（缺省「--」）；
 * day==null 且无 error → 「该日期暂无采集数据」。
 */
@Composable
private fun EmotionDayCard(state: EmotionDayUiState) {
    val unknown = stringResource(R.string.rk3_value_unknown)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceColor
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.calendar_emotion_card_title),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.rk3_lan_usage_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
            Spacer(Modifier.height(10.dp))
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.errorMessage != null -> Text(
                    state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                state.day != null -> {
                    val day = state.day
                    Row(Modifier.fillMaxWidth()) {
                        EmotionMetric(
                            stringResource(R.string.calendar_emotion_avg_depression),
                            day.avgDepressionPercent.toPercentText(unknown),
                            Modifier.weight(1f)
                        )
                        EmotionMetric(
                            stringResource(R.string.calendar_emotion_avg_anxiety),
                            day.avgAnxietyPercent.toPercentText(unknown),
                            Modifier.weight(1f)
                        )
                        EmotionMetric(
                            stringResource(R.string.calendar_emotion_capture_count),
                            day.captureCount.toCountText(unknown),
                            Modifier.weight(1f)
                        )
                    }
                }

                else -> Text(
                    stringResource(R.string.calendar_emotion_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }
        }
    }
}

/** 情绪日卡单列指标：数值（缺省「--」）+ 标签 */
@Composable
private fun EmotionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** DayOfWeek → 周日/周一/…（对照 reminder_calendar_weekday_names 周日周一周二周三四五六） */
@Composable
private fun weekdayLabel(date: LocalDate): String {
    val names = stringResource(R.string.reminder_calendar_weekday_names)
    val start = (date.dayOfWeek.value % 7) * 2
    return names.substring(start, start + 2)
}
