package com.ezvizpro.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezvizpro.ui.theme.*

@Composable
fun ServiceCards(
    onWechatAuthorize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 微信视频通话
        ServiceCard(
            icon = Icons.Default.VideoCall,
            iconTint = Green500,
            title = "微信视频通话",
            subtitle = "绑定微信，接听家人视频呼叫",
            actionLabel = "去授权",
            onClick = onWechatAuthorize
        )

        // AI 跌倒检测
        ServiceCard(
            icon = Icons.Default.AccessibilityNew,
            iconTint = Blue500,
            title = "智能跌倒检测",
            subtitle = "AI 识别老人跌倒，实时告警通知",
            tags = listOf(
                ServiceTag("AI", Blue500),
                ServiceTag("免费试用", Orange500)
            ),
            onClick = { /* 预留 */ }
        )
    }
}

@Composable
private fun ServiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    tags: List<ServiceTag> = emptyList(),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 文字区域
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // 标签
                    tags.forEach { tag ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tag.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                tag.text,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = tag.color,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600
                )
            }

            // 操作按钮
            if (actionLabel != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onClick) {
                    Text(actionLabel, fontSize = 13.sp)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Gray600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class ServiceTag(
    val text: String,
    val color: androidx.compose.ui.graphics.Color
)
