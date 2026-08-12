package com.elderlycare.app.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.RiskLevel
import com.elderlycare.app.ui.components.RiskLevelIndicator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("报告详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Share, contentDescription = "导出PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 报告头部
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("7月10日 情绪倾向健康日报", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                        StatusBadge(text = "关注", color = StatusYellow)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "生成时间：2024-07-10 23:59 · RK3 AI 分析",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHint
                    )
                }
            }

            // 趋势图表
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("7天趋势", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    TrendChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(color = Color(0xFF4A9E8F), label = "孤独指数")
                        LegendItem(color = Color(0xFFF5A623), label = "抑郁倾向")
                        LegendItem(color = Color(0xFF6BBF7A), label = "活跃度")
                    }
                }
            }

            // 30天趋势图
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("30天趋势", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    TrendChart30(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // 系统评估结论
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("系统评估结论", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "老人近期情绪状态总体稳定，孤独指数处于正常范围。抑郁倾向较上周有轻微上升（+4分），可能与本周社交活动减少有关。活跃度保持良好水平。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }

            // 干预建议
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("干预建议", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestionItem("增加正念练习频率至每日2次，缓解轻微焦虑倾向")
                    SuggestionItem("建议家属增加视频通话频率，本周已3天无互动")
                    SuggestionItem("结合高血压病史，关注情绪波动对血压的影响")
                    SuggestionItem("如抑郁倾向持续上升，建议2周内安排心理门诊")
                }
            }

            // 导出按钮
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("导出 PDF 报告（演示）", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun SuggestionItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        // 圆点标记
        Box(
            modifier = Modifier
                .padding(top = 7.dp, end = 10.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Primary)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = 5.dp.toPx())
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun TrendChart(modifier: Modifier = Modifier) {
    val lonelinessData = listOf(35f, 38f, 32f, 36f, 40f, 42f, 38f)
    val depressionData = listOf(38f, 40f, 36f, 39f, 42f, 44f, 42f)
    val activityData = listOf(68f, 65f, 70f, 66f, 62f, 60f, 65f)

    val primaryColor = Color(0xFF4A9E8F)
    val secondaryColor = Color(0xFFF5A623)
    val greenColor = Color(0xFF6BBF7A)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 20f
        val maxVal = 100f
        val minVal = 20f

        fun toY(v: Float) = height - padding - ((v - minVal) / (maxVal - minVal)) * (height - 2 * padding)
        fun toX(i: Int) = padding + i * (width - 2 * padding) / (lonelinessData.size - 1)

        // 网格线
        for (i in 0..4) {
            val y = padding + i * (height - 2 * padding) / 4
            drawLine(
                color = Color(0xFFEFEDEA),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // 数据线
        fun drawLine(data: List<Float>, color: Color) {
            val path = Path()
            data.forEachIndexed { i, v ->
                val pt = Offset(toX(i), toY(v))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            // 数据点
            data.forEachIndexed { i, v ->
                drawCircle(color = color, radius = 4f, center = Offset(toX(i), toY(v)))
            }
        }

        drawLine(lonelinessData, primaryColor)
        drawLine(depressionData, secondaryColor)
        drawLine(activityData, greenColor)

        // 日期标签
        val days = listOf("7/4", "7/5", "7/6", "7/7", "7/8", "7/9", "7/10")
        days.forEachIndexed { i, day ->
            drawContext.canvas.nativeCanvas.drawText(
                day, toX(i), height - 4f,
                android.graphics.Paint().apply {
                    color = 0xFF999999.toInt()
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

@Composable
fun TrendChart30(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 20f

        // 模拟30天数据
        val bars = 30
        val barWidth = (width - 2 * padding) / bars * 0.7f
        val gap = (width - 2 * padding) / bars * 0.3f

        for (i in 0 until bars) {
            val barHeight = (30 + Math.sin(i * 0.3).toFloat() * 15 + Math.random().toFloat() * 10) / 80f * (height - 2 * padding)
            val x = padding + i * (barWidth + gap)
            val color = when {
                barHeight < height * 0.3f -> Color(0xFF6BBF7A)
                barHeight < height * 0.5f -> Color(0xFF4A9E8F)
                barHeight < height * 0.7f -> Color(0xFFF5A623)
                else -> Color(0xFFE8857C)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, height - padding - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }
    }
}
