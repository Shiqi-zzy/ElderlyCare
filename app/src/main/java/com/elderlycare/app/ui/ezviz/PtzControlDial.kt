package com.elderlycare.app.ui.ezviz

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * 预览页云台控制圆盘（仅 supportPtz=true 设备显示，显隐由页面侧控制）。
 *
 * 交互：按住方向/变焦键 → controlPtz(START)（按住期间设备持续动作），
 * 松开 → controlPtz(STOP)（必须成对，否则云台不停转）。失败 toast「云台操作失败，请重试」。
 * START/STOP 从页面级 scope 发出：即使按压协程因控件隐藏被取消，finally 仍会补发 STOP。
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
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.14f), CircleShape)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PtzKey(Icons.Filled.ZoomIn, EZConstants.EZPTZCommand.EZPTZCommandZoomIn, onPressedChange, send)
        PtzKey(Icons.Filled.KeyboardArrowUp, EZConstants.EZPTZCommand.EZPTZCommandUp, onPressedChange, send)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PtzKey(Icons.Filled.KeyboardArrowLeft, EZConstants.EZPTZCommand.EZPTZCommandLeft, onPressedChange, send)
            PtzKey(Icons.Filled.KeyboardArrowRight, EZConstants.EZPTZCommand.EZPTZCommandRight, onPressedChange, send)
        }
        PtzKey(Icons.Filled.KeyboardArrowDown, EZConstants.EZPTZCommand.EZPTZCommandDown, onPressedChange, send)
        PtzKey(Icons.Filled.ZoomOut, EZConstants.EZPTZCommand.EZPTZCommandZoomOut, onPressedChange, send)
    }
}

/** 圆盘单键：按下 START / 松开（或按压被取消）STOP */
@Composable
private fun PtzKey(
    icon: ImageVector,
    command: EZConstants.EZPTZCommand,
    onPressedChange: (Boolean) -> Unit,
    onSend: (EZConstants.EZPTZCommand, EZConstants.EZPTZAction) -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        modifier = Modifier
            .size(44.dp)
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
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
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
