package com.elderlycare.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 折线趋势图：1-N 条数据线 + 底部类目标签（复用 ReportDetailScreen 原 TrendChart 绘制逻辑参数化而来）。
 * 周报/年报 Tab 用两条线（抑郁 + 焦虑，同单位百分比，单 Y 轴 0-100）；
 * 空数据由调用方拦截（置灰占位 + 空态文案），本组件仅保证空输入不绘制。
 */
data class LineSeries(
    val label: String,
    val values: List<Float>,
    val color: Color
)

@Composable
fun TrendLineChart(
    series: List<LineSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    minVal: Float = 0f,
    maxVal: Float = 100f
) {
    if (series.isEmpty() || series.all { it.values.isEmpty() } || xLabels.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = with(density) { 16.dp.toPx() }
        val labelTextSize = with(density) { 11.dp.toPx() }
        val labelGap = with(density) { 6.dp.toPx() }
        val labelSpace = with(density) { 22.dp.toPx() }
        // 绘图区上边界为 padding、下边界留出标签空间
        val chartBottom = height - labelSpace
        val span = (maxVal - minVal).coerceAtLeast(1f)
        // 数据点总数（所有线等长，取第一条线的长度；分母防除零）
        val n = (series.firstOrNull { it.values.isNotEmpty() }?.values?.size ?: 1).coerceAtLeast(2)

        fun toY(v: Float): Float =
            chartBottom - ((v.coerceIn(minVal, maxVal) - minVal) / span) * (chartBottom - padding)

        fun toX(i: Int): Float = padding + i * (width - 2 * padding) / (n - 1)

        // 水平网格线（4 条，弱化色，与 BarChart 一致）
        val gridColor = Color(0xFFEFEDEA)
        for (i in 0..3) {
            val y = padding + i * (chartBottom - padding) / 3f
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // 数据线 + 数据点
        series.forEach { s ->
            if (s.values.isEmpty()) return@forEach
            val path = Path()
            s.values.forEachIndexed { i, v ->
                val pt = Offset(toX(i), toY(v))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color = s.color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            s.values.forEachIndexed { i, v ->
                drawCircle(color = s.color, radius = 4f, center = Offset(toX(i), toY(v)))
            }
        }

        // 底部类目标签（按实际 xLabels 数量绘制，等分 x 轴）
        xLabels.forEachIndexed { i, label ->
            drawContext.canvas.nativeCanvas.drawText(
                label,
                if (xLabels.size > 1) padding + i * (width - 2 * padding) / (xLabels.size - 1)
                else padding + (width - 2 * padding) / 2f,
                height - labelGap,
                android.graphics.Paint().apply {
                    color = 0xFF999999.toInt()
                    textSize = labelTextSize
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
