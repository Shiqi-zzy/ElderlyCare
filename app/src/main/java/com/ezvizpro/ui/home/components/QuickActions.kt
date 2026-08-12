package com.ezvizpro.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezvizpro.ui.theme.*

@Composable
fun QuickActions(
    onVideoCall: () -> Unit,
    onFamilyMessage: () -> Unit,
    onLifeReminder: () -> Unit,
    hasMedicineReminder: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.Videocam,
            label = "视频通话",
            iconTint = Blue500,
            onClick = onVideoCall
        )
        QuickActionButton(
            icon = Icons.AutoMirrored.Filled.Chat,
            label = "家人留言",
            iconTint = Green500,
            onClick = onFamilyMessage
        )
        QuickActionButton(
            icon = Icons.Default.Notifications,
            label = "生活提醒",
            iconTint = Orange500,
            showBadge = hasMedicineReminder,
            badgeText = "药",
            onClick = onLifeReminder
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    showBadge: Boolean = false,
    badgeText: String = ""
) {
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    Modifier.let {
                        // 微妙的可点击区域
                        it
                    }
                )
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = iconTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        }

        // 右上角红点角标
        if (showBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = 4.dp)
                    .size(18.dp),
                shape = CircleShape,
                color = Red500
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
