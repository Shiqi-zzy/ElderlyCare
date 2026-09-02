package com.elderlycare.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.ui.theme.*

enum class RiskLevel(val label: String, val color: androidx.compose.ui.graphics.Color) {
    NORMAL("正常", StatusGreen),
    ATTENTION("关注", StatusYellow),
    RISK("风险", StatusRed)
}

@Composable
fun RiskLevelIndicator(
    level: RiskLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val bgColor by animateColorAsState(
        targetValue = level.color.copy(alpha = 0.15f),
        animationSpec = tween(300),
        label = "riskBg"
    )
    val textColor by animateColorAsState(
        targetValue = level.color,
        animationSpec = tween(300),
        label = "riskText"
    )

    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(level.color)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = level.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = level.color
            )
        }
    }
}
