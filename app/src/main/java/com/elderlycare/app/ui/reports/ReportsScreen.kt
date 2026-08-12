package com.elderlycare.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.RiskLevel
import com.elderlycare.app.ui.components.RiskLevelIndicator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateToDetail: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("情绪倾向报告", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab 切换
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary
            ) {
                listOf("日报", "周报", "异常报告").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        DailyReportCard(
                            date = "7月10日", level = RiskLevel.ATTENTION,
                            loneliness = "38", depression = "42", activity = "65",
                            emotion = "平静", summary = "综合评估：轻度孤独",
                            onClick = onNavigateToDetail
                        )
                        DailyReportCard(
                            date = "7月9日", level = RiskLevel.NORMAL,
                            loneliness = "32", depression = "36", activity = "68",
                            emotion = "平静", summary = "综合评估：正常",
                            onClick = onNavigateToDetail
                        )
                        DailyReportCard(
                            date = "7月8日", level = RiskLevel.NORMAL,
                            loneliness = "35", depression = "38", activity = "62",
                            emotion = "一般", summary = "综合评估：正常",
                            onClick = onNavigateToDetail
                        )
                    }
                    1 -> {
                        WeeklyReportCard(
                            dateRange = "7月1日 - 7月7日",
                            level = RiskLevel.ATTENTION,
                            summary = "本周孤独指数较上周有轻微上升趋势，建议增加陪伴互动",
                            onClick = onNavigateToDetail
                        )
                        WeeklyReportCard(
                            dateRange = "6月24日 - 6月30日",
                            level = RiskLevel.NORMAL,
                            summary = "各项指标稳定，情绪状态良好",
                            onClick = onNavigateToDetail
                        )
                    }
                    2 -> {
                        AlertReportCard(
                            date = "7月8日 15:30",
                            level = RiskLevel.RISK,
                            title = "连续独处超过6小时",
                            description = "老人在客厅区域连续独处6小时，语音情绪分析显示低落倾向",
                            onClick = onNavigateToDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyReportCard(
    date: String, level: RiskLevel, loneliness: String, depression: String,
    activity: String, emotion: String, summary: String, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$date 日报", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = level.label, color = level.color)
                }
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextHint)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniMetric("孤独指数", loneliness)
                MiniMetric("抑郁倾向", depression)
                MiniMetric("活跃度", activity)
                MiniMetric("情绪", emotion)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
fun WeeklyReportCard(dateRange: String, level: RiskLevel, summary: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("周报", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = level.label, color = level.color)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(dateRange, style = MaterialTheme.typography.bodyMedium, color = TextHint)
            Spacer(modifier = Modifier.height(8.dp))
            // 趋势示意图
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("7天趋势折线图（演示占位）", style = MaterialTheme.typography.labelMedium, color = TextHint)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
fun AlertReportCard(date: String, level: RiskLevel, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // 左侧红色边框指示
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(level.color)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$title", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(date, style = MaterialTheme.typography.labelSmall, color = TextHint)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("查看实时画面 →", color = Primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
