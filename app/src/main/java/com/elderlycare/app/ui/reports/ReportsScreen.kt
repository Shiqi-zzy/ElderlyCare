package com.elderlycare.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.rk3.*
import com.elderlycare.app.ui.components.charts.LineSeries
import com.elderlycare.app.ui.components.charts.TrendLineChart
import com.elderlycare.app.ui.theme.*

// 页面风格常量（与首页统一：极浅蓝背景 + 极浅蓝渐变横幅）
private val PageBg = Color(0xFFF5F7FA)
private val BannerBlueStart = Color(0xFFEAF2FF)
private val BannerBlueEnd = Color(0xFFF5F9FF)
private val BannerText = Color(0xFF1A2332)

/**
 * 报告页（四 Tab：实时 / 周度 / 年度 / 建议）。
 *
 * 数据源=RK3 局域网 HTTP 服务（端口 8080，无公网 IP，仅手机与设备同一 WiFi 可访问）；
 * 服务器地址从设置读取（我的页-客服与设置-RK3服务器地址），为空/不通/跨网一律友好降级不崩溃。
 * 旧硬编码 mock（日报/周报/异常报告卡片）已全部移除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: Rk3ReportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    // 进入 Tab 惰性加载（仅 Idle 时请求；「刷新」按钮重调对应接口）
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.ensureRealtime()
            1 -> viewModel.ensureWeekly()
            2 -> viewModel.ensureYearly()
            3 -> viewModel.ensureSuggestion()
        }
    }
    val refresh: () -> Unit = {
        when (selectedTab) {
            0 -> viewModel.refreshRealtime()
            1 -> viewModel.refreshWeekly()
            2 -> viewModel.refreshYearly()
            3 -> viewModel.refreshSuggestion()
        }
    }

    Scaffold(
        containerColor = PageBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 蓝色渐变顶部横幅（标题 + 当前 Tab 刷新按钮）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BannerBlueStart, BannerBlueEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "情绪倾向报告",
                        color = BannerText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = refresh) {
                        Text(stringResource(R.string.report_refresh), color = Primary)
                    }
                }
            }

            // Tab 切换（白色背景，选中蓝色）
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Primary
            ) {
                listOf(
                    R.string.report_tab_realtime,
                    R.string.report_tab_weekly,
                    R.string.report_tab_yearly,
                    R.string.report_tab_suggestion
                ).forEachIndexed { index, titleRes ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                stringResource(titleRes),
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> TabStateBox(uiState.realtime) { data -> RealtimeTabContent(data) }
                    1 -> TabStateBox(uiState.weekly) { data -> WeeklyTabContent(data) }
                    2 -> TabStateBox(uiState.yearly) { data -> YearlyTabContent(data) }
                    3 -> SuggestionTabContent(uiState.suggestion, onRefresh = { viewModel.refreshSuggestion() })
                }
            }

            // 局域网使用提示（全部 Tab 底部常驻）
            Text(
                stringResource(R.string.rk3_lan_usage_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Tab 状态通用外壳：Loading 转圈 / Failed 居中文案 / Success 渲染内容 */
@Composable
private fun <T> TabStateBox(state: Rk3TabState<T>, content: @Composable (T) -> Unit) {
    when (state) {
        is Rk3TabState.Idle, is Rk3TabState.Loading -> Box(
            Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        is Rk3TabState.Failed -> Box(
            Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center
        ) {
            Text(state.message, color = TextSecondary, textAlign = TextAlign.Center)
        }

        is Rk3TabState.Success -> content(state.data)
    }
}

// ==================== 实时 Tab ====================

@Composable
private fun RealtimeTabContent(data: Rk3HealthData) {
    val unknown = stringResource(R.string.rk3_value_unknown)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                stringResource(R.string.report_realtime_detect_status),
                data.detectStatus ?: unknown, Modifier.weight(1f)
            )
            MetricCard(
                stringResource(R.string.report_realtime_frame_count),
                data.frameCount?.toString() ?: unknown, Modifier.weight(1f)
            )
            MetricCard(
                stringResource(R.string.report_realtime_face_count),
                data.faceCount?.toString() ?: unknown, Modifier.weight(1f)
            )
        }

        Text(
            stringResource(R.string.report_realtime_recent_captures),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium
        )
        if (data.recentCaptures.isEmpty()) {
            Text(
                stringResource(R.string.report_realtime_captures_empty),
                color = TextHint,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    // 表头
                    Row(Modifier.fillMaxWidth()) {
                        listOf("时间", "情绪", stringResource(R.string.report_depression_series), stringResource(R.string.report_anxiety_series))
                            .forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextHint) }
                    }
                    data.recentCaptures.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(item.time ?: unknown, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(item.emotion ?: unknown, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text(item.depressionPercent.toPercentText(unknown), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text(item.anxietyPercent.toPercentText(unknown), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

// ==================== 周度 Tab ====================

@Composable
private fun WeeklyTabContent(data: Rk3WeeklyData) {
    val unknown = stringResource(R.string.rk3_value_unknown)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            MiniMetric(stringResource(R.string.report_weekly_avg_depression), data.avgDepressionPercent.toPercentText(unknown), Modifier.weight(1f))
            MiniMetric(stringResource(R.string.report_weekly_avg_anxiety), data.avgAnxietyPercent.toPercentText(unknown), Modifier.weight(1f))
            MiniMetric(stringResource(R.string.report_weekly_peak_depression), data.peakDepressionPercent.toPercentText(unknown), Modifier.weight(1f))
            MiniMetric(stringResource(R.string.report_weekly_peak_anxiety), data.peakAnxietyPercent.toPercentText(unknown), Modifier.weight(1f))
            MiniMetric(stringResource(R.string.report_weekly_total_captures), data.totalCaptureCount.toCountText(unknown), Modifier.weight(1f))
        }

        if (!data.hasAnyValue()) {
            // 无数据：置灰占位，不画图（不显示 0% 乱数据）
            EmptyChartPlaceholder(stringResource(R.string.report_weekly_empty))
        } else {
            Text(
                stringResource(R.string.report_weekly_days_title),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            WeekDayTable(data.days, unknown)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    TrendLineChart(
                        series = listOf(
                            LineSeries(
                                stringResource(R.string.report_depression_series),
                                data.days.map { it.avgDepressionPercent }.fillNulls(),
                                Primary
                            ),
                            LineSeries(
                                stringResource(R.string.report_anxiety_series),
                                data.days.map { it.avgAnxietyPercent }.fillNulls(),
                                Secondary
                            )
                        ),
                        xLabels = data.days.map { it.date.takeLast(5).ifBlank { unknown } },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    TrendLegend(
                        stringResource(R.string.report_depression_series),
                        stringResource(R.string.report_anxiety_series)
                    )
                }
            }
        }
    }
}

/** 7 日明细表：表头周一..周日 + 抑郁均值/焦虑均值/采集次数三行 */
@Composable
private fun WeekDayTable(days: List<Rk3DayData>, unknown: String) {
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                weekdays.take(days.size).forEach {
                    Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextHint, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(6.dp))
            TableValueRow(
                days.map { it.avgDepressionPercent.toPercentText(unknown) },
                stringResource(R.string.report_depression_series)
            )
            TableValueRow(
                days.map { it.avgAnxietyPercent.toPercentText(unknown) },
                stringResource(R.string.report_anxiety_series)
            )
            TableValueRow(
                days.map { it.captureCount.toCountText(unknown) },
                stringResource(R.string.calendar_emotion_capture_count)
            )
        }
    }
}

/** 表格数值行：行标签 + 每列数值（列数与表头一致） */
@Composable
private fun TableValueRow(cells: List<String>, rowLabel: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            rowLabel,
            Modifier.width(56.dp),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
        cells.forEach {
            Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.Center)
        }
    }
}

// ==================== 年度 Tab ====================

@Composable
private fun YearlyTabContent(data: Rk3YearlyData) {
    val unknown = stringResource(R.string.rk3_value_unknown)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            MiniMetric(stringResource(R.string.report_yearly_avg_depression), data.avgDepressionPercent.toPercentText(unknown), Modifier.weight(1f))
            MiniMetric(stringResource(R.string.report_yearly_avg_anxiety), data.avgAnxietyPercent.toPercentText(unknown), Modifier.weight(1f))
            MiniMetric(stringResource(R.string.report_yearly_top_month), data.topMonthLabel ?: unknown, Modifier.weight(1f))
        }

        if (!data.hasAnyValue()) {
            EmptyChartPlaceholder(stringResource(R.string.report_yearly_empty))
        } else {
            Text(
                stringResource(R.string.report_yearly_months_title),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            MonthlyTable(data.months, unknown)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    TrendLineChart(
                        series = listOf(
                            LineSeries(
                                stringResource(R.string.report_depression_series),
                                data.months.map { it.avgDepressionPercent }.fillNulls(),
                                Primary
                            ),
                            LineSeries(
                                stringResource(R.string.report_anxiety_series),
                                data.months.map { it.avgAnxietyPercent }.fillNulls(),
                                Secondary
                            )
                        ),
                        xLabels = data.months.map { stringResource(R.string.report_month_format, it.month ?: 0) },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    TrendLegend(
                        stringResource(R.string.report_depression_series),
                        stringResource(R.string.report_anxiety_series)
                    )
                }
            }
        }
    }
}

/** 12 月明细表：月份 + 抑郁均值 + 焦虑均值 */
@Composable
private fun MonthlyTable(months: List<Rk3MonthData>, unknown: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("月份", stringResource(R.string.report_depression_series), stringResource(R.string.report_anxiety_series))
                    .forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextHint, textAlign = TextAlign.Center) }
            }
            Spacer(Modifier.height(6.dp))
            months.forEach { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.report_month_format, m.month ?: 0),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(m.avgDepressionPercent.toPercentText(unknown), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.Center)
                    Text(m.avgAnxietyPercent.toPercentText(unknown), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ==================== 建议 Tab ====================

@Composable
private fun SuggestionTabContent(state: Rk3TabState<Rk3SuggestionData?>, onRefresh: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 「生成家属建议」= 仅刷新重调 /api/suggestions/latest（不做本地大模型请求）
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(stringResource(R.string.report_suggestion_generate), modifier = Modifier.padding(vertical = 4.dp))
        }

        when (state) {
            is Rk3TabState.Idle, is Rk3TabState.Loading -> Box(
                Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is Rk3TabState.Failed -> Box(
                Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center
            ) {
                Text(state.message, color = TextSecondary, textAlign = TextAlign.Center)
            }

            is Rk3TabState.Success -> {
                val suggestion = state.data
                if (suggestion == null || suggestion.suggestionText.isBlank()) {
                    // 设备还没有生成任何建议
                    Box(
                        Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.report_suggestion_empty),
                            color = TextHint,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                suggestion.suggestionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            if (suggestion.triggerLevelText != null || suggestion.date != null) {
                                Spacer(Modifier.height(10.dp))
                                if (suggestion.triggerLevelText != null) {
                                    Text(
                                        "${stringResource(R.string.report_suggestion_level)}：${suggestion.triggerLevelText}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                                if (suggestion.date != null) {
                                    Text(
                                        "${stringResource(R.string.report_suggestion_date)}：${suggestion.date}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI 智能体配置表单（仅本地保存参数，不发起请求）
        AiAgentConfigForm()
    }
}

// ==================== 通用小组件 ====================

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

/** 无数据置灰占位：不画图、不显示 0% 乱数据 */
@Composable
private fun EmptyChartPlaceholder(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant) {
        Box(
            Modifier.fillMaxWidth().height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextHint)
        }
    }
}

/** 双线图例：抑郁 Primary / 焦虑 Secondary（颜色跟实体，不跟排名） */
@Composable
private fun TrendLegend(depressionLabel: String, anxietyLabel: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(Primary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(depressionLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(10.dp).background(Secondary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(anxietyLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

/** null → 前一个非 null 值（首段 0f），保证折线连续不画 0 值假坑 */
private fun List<Float?>.fillNulls(): List<Float> {
    var last = 0f
    return map { v -> if (v != null) { last = v; v } else last }
}
