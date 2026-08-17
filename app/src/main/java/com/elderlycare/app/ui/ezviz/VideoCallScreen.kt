package com.elderlycare.app.ui.ezviz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ez.basertc.view.VideoCanvasView

/**
 * 云通话界面（双向音视频）。
 *
 * 需要导航层传入 [param]（含 clientToken/deviceToken/roomId，来自后端 /api/rtc/token）。
 * 导航接入点见 TODO：联调时在 NavGraph 里挂上。
 */
@Composable
fun VideoCallScreen(
    deviceSerial: String,
    account: String,
    roomId: String,
    isClientCall: Boolean,
    onExit: () -> Unit,
    viewModel: VideoCallViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasPermissions by remember { mutableStateOf(hasCallPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasPermissions = result.values.all { it } }

    // 请求相机/麦克风权限
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    // 权限就绪后启动通话（先向后端取 token 再入会）
    LaunchedEffect(hasPermissions) {
        if (hasPermissions && state.state == CallState.IDLE) {
            viewModel.startCallFromBackend(deviceSerial, account, "family001", roomId, isClientCall)
        }
    }

    // 通话结束后退出页面
    LaunchedEffect(state.state) {
        if (state.state == CallState.ENDED) {
            onExit()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 远端画面（全屏）
        AndroidView(
            factory = { ctx -> VideoCanvasView(ctx).also(viewModel::bindRemoteView) },
            modifier = Modifier.fillMaxSize()
        )
        // 本地画面（小窗）
        AndroidView(
            factory = { ctx -> VideoCanvasView(ctx).also(viewModel::bindLocalView) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(width = 110.dp, height = 160.dp)
        )

        // 顶部状态
        Text(
            text = when (state.state) {
                CallState.INIT -> "正在连接…"
                CallState.ENTERING -> if (isClientCall) "等待对方接听…" else "正在接通…"
                CallState.CONNECTED -> formatDuration(state.elapsedSeconds)
                CallState.ERROR -> state.error ?: "通话出错"
                else -> ""
            },
            color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
        )

        // 底部控制栏
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(label = "麦克风", active = !state.isMuted) { viewModel.toggleMic() }
            ControlButton(label = "扬声器", active = state.isSpeakerOn) { viewModel.toggleSpeaker() }
            ControlButton(label = "摄像头", active = state.isCameraOn) { viewModel.toggleCamera() }
            ControlButton(label = "切换", active = true) { viewModel.switchCamera() }
            Button(
                onClick = { viewModel.hangUp() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            ) {
                Text("挂断", color = Color.White)
            }
        }
    }
}

@Composable
private fun ControlButton(label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) Color(0xFF424242) else Color(0xFF1E1E1E)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        ) {
            Text(label, color = if (active) Color.White else Color.Gray)
        }
    }
}

private fun hasCallPermissions(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
