package com.elderlycare.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.TextSecondary

/**
 * 图表数据切片
 */
data class PieSlice(val label: String, val value: Float, val color: Color)

/**
 * 环形图：分段圆环 + 中心总数值，配 [ChartLegend] 使用。
 */
@Composable
fun DonutChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String = ""
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) return@Canvas
            val strokeWidth = size.minDimension * 0.18f
            val inset = strokeWidth / 2f
            var start = -90f
            slices.forEach { slice ->
                val sweep = slice.value / total * 360f
                drawArc(
                    color = slice.color,
                    startAngle = start,
                    sweepAngle = sweep - 2.5f, // 分段间隙
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        if (centerLabel.isNotBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("总量", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

/**
 * 柱状图：带水平网格线与底部类目标签，柱顶显示数值。
 */
@Composable
fun BarChart(
    entries: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = Primary
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        if (entries.isEmpty()) return@Canvas
        val valueTextSize = with(density) { 11.dp.toPx() }
        val labelTextSize = with(density) { 11.dp.toPx() }
        val labelSpace = with(density) { 24.dp.toPx() }
        val topPadding = with(density) { 30.dp.toPx() }
        val valueGap = with(density) { 6.dp.toPx() }
        val labelGap = with(density) { 8.dp.toPx() }
        val maxVal = entries.maxOf { it.second }.coerceAtLeast(1f)
        val chartHeight = size.height - labelSpace - topPadding
        val slotWidth = size.width / entries.size

        // 水平网格线（4 条）
        val gridColor = Color(0xFFEFEDEA)
        for (i in 0..3) {
            val y = topPadding + i * chartHeight / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        entries.forEachIndexed { i, (label, value) ->
            val barWidth = slotWidth * 0.5f
            val x = slotWidth * i + (slotWidth - barWidth) / 2f
            val barHeight = (value / maxVal) * chartHeight
            val y = topPadding + chartHeight - barHeight

            // 柱体（圆角顶部）
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // 柱顶数值（柱顶上方留出间隙，避免重叠）
            drawContext.canvas.nativeCanvas.drawText(
                value.toInt().toString(),
                x + barWidth / 2f,
                y - valueGap,
                android.graphics.Paint().apply {
                    color = 0xFF3D3D3D.toInt()
                    textSize = valueTextSize
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            // 底部类目标签（位于柱底下方 labelSpace 区，避免重叠）
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2f,
                size.height - labelGap,
                android.graphics.Paint().apply {
                    color = 0xFF999999.toInt()
                    textSize = labelTextSize
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

/**
 * 图表图例（颜色点 + 名称 + 数值）
 */
@Composable
fun ChartLegend(slices: List<PieSlice>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(slice.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(slice.label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.weight(1f))
                Text(
                    slice.value.toInt().toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
