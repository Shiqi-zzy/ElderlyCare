package com.elderlycare.app.ui.ezviz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.videogo.openapi.EZConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 预览页云台控制圆盘（白色圆盘风格，参考萤石云视频APP原生设计）。
 *
 * 布局：白色圆形底盘 + 灰色方向箭头（上下左右）+ 中心小圆 + 上下变焦键（小）。
 * 交互：按住方向/变焦键 → controlPtz(START)，松开 → controlPtz(STOP)（必须成对，否则云台不停转）。
 * [onPressedChange] 供页面侧抑制「3 秒自动隐藏控件」（按住期间不隐藏，保证 STOP 不漏发）。
 */
@Composable
fun PtzControlDial(
    deviceSerial: String,
    onToast: (String) -> Unit,
    onPressedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val send: (EZConstants.EZPTZCommand, EZConstants.EZPTZAction) -> Unit = { cmd, action ->
        scope.launch {
            val ok = withContext(Dispatchers.IO) { sendPtz(deviceSerial, cmd, action) }
            if (!ok) onToast("云台操作失败，请重试")
        }
    }

    Surface(
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shadowElevation = 0.dp,
        modifier = modifier
            .size(150.dp)
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 顶部变焦（小）
            PtzKey(
                icon = Icons.Filled.ZoomIn,
                command = EZConstants.EZPTZCommand.EZPTZCommandZoomIn,
                onPressedChange = onPressedChange,
                onSend = send,
                small = true
            )
            // 上方向
            PtzKey(
                icon = Icons.Filled.KeyboardArrowUp,
                command = EZConstants.EZPTZCommand.EZPTZCommandUp,
                onPressedChange = onPressedChange,
                onSend = send
            )
            // 中间行：左方向 + 中心小圆 + 右方向
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                PtzKey(
                    icon = Icons.Filled.KeyboardArrowLeft,
                    command = EZConstants.EZPTZCommand.EZPTZCommandLeft,
                    onPressedChange = onPressedChange,
                    onSend = send
                )
                // 中心装饰小圆
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8E8E8))
                )
                PtzKey(
                    icon = Icons.Filled.KeyboardArrowRight,
                    command = EZConstants.EZPTZCommand.EZPTZCommandRight,
                    onPressedChange = onPressedChange,
                    onSend = send
                )
            }
            // 下方向
            PtzKey(
                icon = Icons.Filled.KeyboardArrowDown,
                command = EZConstants.EZPTZCommand.EZPTZCommandDown,
                onPressedChange = onPressedChange,
                onSend = send
            )
            // 底部变焦（小）
            PtzKey(
                icon = Icons.Filled.ZoomOut,
                command = EZConstants.EZPTZCommand.EZPTZCommandZoomOut,
                onPressedChange = onPressedChange,
                onSend = send,
                small = true
            )
        }
    }
}

/** 圆盘单键：按下 START / 松开（或按压被取消）STOP */
@Composable
private fun PtzKey(
    icon: ImageVector,
    command: EZConstants.EZPTZCommand,
    onPressedChange: (Boolean) -> Unit,
    onSend: (EZConstants.EZPTZCommand, EZConstants.EZPTZAction) -> Unit,
    small: Boolean = false
) {
    val size = if (small) 26.dp else 34.dp
    val iconSize = if (small) 16.dp else 22.dp
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .size(size)
            .pointerInput(command) {
                detectTapGestures(
                    onPress = {
                        onPressedChange(true)
                        onSend(command, EZConstants.EZPTZAction.EZPTZActionSTART)
                        try {
                            tryAwaitRelease()
                        } finally {
                            // 松手或按压被取消（控件随自动隐藏移除）：都补发 STOP 防云台不停
                            onPressedChange(false)
                            onSend(command, EZConstants.EZPTZAction.EZPTZActionSTOP)
                        }
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF888888), modifier = Modifier.size(iconSize))
        }
    }
}

/** 云台 SDK 调用（通道 1）：START 前刷新 SDK accessToken 防过期 */
private suspend fun sendPtz(
    deviceSerial: String,
    command: EZConstants.EZPTZCommand,
    action: EZConstants.EZPTZAction
): Boolean {
    if (action == EZConstants.EZPTZAction.EZPTZActionSTART) {
        val token = runCatching { ServiceLocator.repository.obtainValidToken() }.getOrNull()
        ServiceLocator.sdkManager.updateToken(token)
    }
    return ServiceLocator.sdkManager.controlPtz(deviceSerial, 1, command, action)
}
