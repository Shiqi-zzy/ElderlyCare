package com.elderlycare.app.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.collect

/**
 * 设备访问闸门（第五阶段）：路由携带的 [deviceSerial] 必须等于
 * 当前登录用户可访问设备的 deviceSn（BindingRepository.getCurrentUserDevice()），
 * 才渲染 [content]；否则渲染「无可访问设备」占位。
 *
 * 统一拦截 直播 / 回放 / 云通话 三个设备入口，防深链或任意序列号绕过权限。
 * deviceSn 为空、当前用户无 ACTIVE 绑定/本人档案、serial 不匹配时一律不放行。
 */
@Composable
fun DeviceAuthorizedGate(
    deviceSerial: String,
    onBack: () -> Unit,
    content: @Composable (BindingRepository.AccessibleDevice) -> Unit
) {
    var state by remember { mutableStateOf<DeviceAccessState>(DeviceAccessState.Loading) }
    LaunchedEffect(deviceSerial) {
        // 实时收集当前用户可访问设备：解绑/切换老人时流自动重发，
        // 串号不匹配或为 null → 立即切「无可访问设备」，旧设备页面不再存活。
        state = DeviceAccessState.Loading
        ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { device ->
            state = if (device != null && device.deviceSn == deviceSerial) {
                DeviceAccessState.Authorized(device)
            } else {
                DeviceAccessState.Denied
            }
        }
    }
    when (val s = state) {
        DeviceAccessState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DeviceAccessState.Authorized -> content(s.device)
        DeviceAccessState.Denied -> DeviceAccessDenied(onBack)
    }
}

private sealed interface DeviceAccessState {
    data object Loading : DeviceAccessState
    data class Authorized(val device: BindingRepository.AccessibleDevice) : DeviceAccessState
    data object Denied : DeviceAccessState
}

@Composable
private fun DeviceAccessDenied(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("无可访问设备", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "当前账号未授权访问该设备",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("返回") }
    }
}
