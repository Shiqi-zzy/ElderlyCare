package com.elderlycare.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.ui.theme.*

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int = 6,
    stepLabels: List<String> = listOf("1", "2", "3", "4", "5", "6"),
    stepTexts: List<String> = listOf("基础信息", "生活习惯", "疾病史", "体检记录", "兴趣爱好", "设备绑定"),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 步骤圆点行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepLabels.forEachIndexed { index, label ->
                val stepNum = index + 1
                val isCompleted = stepNum < currentStep
                val isCurrent = stepNum == currentStep
                val isPending = stepNum > currentStep

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isCompleted -> Primary
                        isCurrent -> Primary
                        isPending -> Color.White
                        else -> Color.White
                    },
                    animationSpec = tween(300),
                    label = "stepBg"
                )

                val borderColor = when {
                    isCompleted -> Primary
                    isCurrent -> Primary
                    else -> CardBorder
                }

                val textColor = when {
                    isCompleted -> OnPrimary
                    isCurrent -> OnPrimary
                    else -> TextSecondary
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .then(
                                if (!isCompleted && !isCurrent)
                                    Modifier.clip(CircleShape)
                                        .background(Color.White)
                                        .then(
                                            Modifier
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                else Modifier
                            )
                            .then(
                                if (!isCompleted && !isCurrent)
                                    Modifier.padding(2.dp)
                                        .clip(CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Text("✓", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                if (isCurrent) label else stepNum.toString(),
                                color = if (isCurrent) textColor else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    // 标签
                    Text(
                        text = stepTexts.getOrElse(index) { "" },
                        fontSize = 14.sp,
                        color = if (isCurrent || isCompleted) Primary else TextHint,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1
                    )
                }

                // 连接线
                if (index < stepLabels.size - 1) {
                    val lineColor by animateColorAsState(
                        targetValue = if (isCompleted) Primary else CardBorder,
                        animationSpec = tween(300),
                        label = "lineColor"
                    )
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(1.dp))
                            .background(lineColor)
                    )
                }
            }
        }
    }
}
