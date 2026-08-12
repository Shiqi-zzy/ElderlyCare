package com.ezvizpro.ui.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ezvizpro.domain.model.PtzDirection
import com.ezvizpro.domain.model.PtzSpeed

/**
 * 云台方向控制盘
 *
 * 布局:
 *         [上]
 *  [左] [停止] [右]
 *         [下]
 */
@Composable
fun PTZController(
    onPtzStart: (PtzDirection, PtzSpeed) -> Unit,
    onPtzStop: (PtzDirection?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 上
        PtzButton(
            icon = Icons.Default.KeyboardArrowUp,
            direction = PtzDirection.UP,
            onStart = onPtzStart,
            onStop = onPtzStop,
            enabled = enabled
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // 左
            PtzButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                direction = PtzDirection.LEFT,
                onStart = onPtzStart,
                onStop = onPtzStop,
                enabled = enabled
            )

            // 中间 - 停止按钮 / 速度切换
            IconButton(
                onClick = { onPtzStop(null) },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "停止",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // 右
            PtzButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                direction = PtzDirection.RIGHT,
                onStart = onPtzStart,
                onStop = onPtzStop,
                enabled = enabled
            )
        }

        // 下
        PtzButton(
            icon = Icons.Default.KeyboardArrowDown,
            direction = PtzDirection.DOWN,
            onStart = onPtzStart,
            onStop = onPtzStop,
            enabled = enabled
        )
    }
}

@Composable
private fun PtzButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    direction: PtzDirection,
    onStart: (PtzDirection, PtzSpeed) -> Unit,
    onStop: (PtzDirection?) -> Unit,
    enabled: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    IconButton(
        onClick = { },
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isPressed)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .pointerInput(direction) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStart(direction, PtzSpeed.NORMAL)
                        tryAwaitRelease()
                        isPressed = false
                        onStop(direction)
                    }
                )
            },
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = direction.label,
            tint = if (isPressed)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 预置位选择器
 */
@Composable
fun PresetSelector(
    presets: List<com.ezvizpro.domain.model.Preset>,
    onPresetClick: (Int) -> Unit,
    onAddPreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "预置位",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 已有预置位
            presets.take(4).forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onPresetClick(preset.index) },
                    label = {
                        Text(
                            preset.name.ifBlank { "#${preset.index}" },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            // 添加按钮
            IconButton(
                onClick = onAddPreset,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加预置位",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
