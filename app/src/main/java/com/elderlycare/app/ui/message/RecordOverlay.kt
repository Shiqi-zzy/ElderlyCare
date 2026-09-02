package com.elderlycare.app.ui.message

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusRed

/**
 * 按住留言录音遮罩层。
 * 展示：麦克风图标、声波动画（按实时振幅驱动）、已录音时长、松开/上滑提示；
 * 上滑取消时整体变红色。
 */
@Composable
fun RecordOverlay(
    isRecording: Boolean,
    cancelled: Boolean,
    amplitude: Int,
    elapsedSec: Int
) {
    if (!isRecording) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 麦克风圆钮
            Surface(
                shape = CircleShape,
                color = if (cancelled) StatusRed else Primary.copy(alpha = 0.95f),
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 声波动画
            AmplitudeBars(amplitude = amplitude, cancelled = cancelled)

            Spacer(Modifier.height(16.dp))

            // 计时
            Text(
                stringResource(R.string.message_record_duration_format, elapsedSec),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            // 操作提示
            Text(
                stringResource(
                    if (cancelled) R.string.message_release_to_cancel
                    else R.string.message_slide_up_to_cancel
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * 声波动画：20 根竖条，高度随振幅（0~32767）与索引伪随机变化。
 */
@Composable
private fun AmplitudeBars(amplitude: Int, cancelled: Boolean) {
    val barColor = if (cancelled) StatusRed else Color.White
    val barCount = 20
    val normalized = (amplitude / 32767f).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.size(width = 160.dp, height = 48.dp)) {
        val barWidth = size.width / (barCount * 2f)
        val gap = barWidth
        val maxBarHeight = size.height
        for (i in 0 until barCount) {
            // 索引伪随机让各柱高度错落，整体随振幅缩放
            val pseudo = ((i * 7) % 10) / 10f
            val height = (0.2f + 0.8f * pseudo) * normalized.coerceAtLeast(0.08f) * maxBarHeight
            val left = i * (barWidth + gap) + gap / 2f
            drawRoundRect(
                color = barColor.copy(alpha = 0.9f),
                topLeft = androidx.compose.ui.geometry.Offset(left, (size.height - height) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}
