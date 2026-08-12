package com.ezvizpro.ui.live

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ezvizpro.core.player.EzvizPlayer
import com.ezvizpro.core.player.EzvizPlayerView
import com.ezvizpro.core.player.PlayerState
import com.ezvizpro.core.player.rememberEzvizPlayer
import kotlinx.coroutines.delay
import com.ezvizpro.ui.live.components.PTZController
import com.ezvizpro.ui.live.components.VideoControlsOverlay

@Composable
fun LivePreviewScreen(
    deviceSerial: String,
    channelNo: Int,
    supportPtz: Boolean = true,
    onBackClick: () -> Unit,
    viewModel: LivePreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = rememberEzvizPlayer()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = LocalContext.current as? Activity

    var controlsVisible by remember { mutableStateOf(true) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showAddPresetDialog by remember { mutableStateOf(false) }

    // 初始化
    LaunchedEffect(deviceSerial) {
        viewModel.initialize(deviceSerial, channelNo, supportPtz)
        viewModel.bindPlayer(player)
        viewModel.loadPresets()
    }

    // 播放/停止
    LaunchedEffect(uiState.streamUrl) {
        uiState.streamUrl?.let { url ->
            player.play(url)
        }
    }

    // 静音控制
    LaunchedEffect(uiState.isMuted) {
        player.setMuted(uiState.isMuted)
    }

    // 离开页面时关闭直播
    DisposableEffect(Unit) {
        onDispose {
            viewModel.closeLive()
            player.release()
        }
    }

    // 控件自动隐藏（3 秒后）
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // 横竖屏切换
    LaunchedEffect(isLandscape) {
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // 视频播放器
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在连接设备…")
                        // 验证码输入（加密设备需要）
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.verifyCode,
                            onValueChange = { if (it.length <= 6) viewModel.setVerifyCode(it) },
                            label = { Text("设备验证码（6位大写字母）") },
                            placeholder = { Text("输入设备标签上的验证码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        // 验证码输入（加密设备需要）
                        if (uiState.showCodeInput) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = uiState.verifyCode,
                                onValueChange = { if (it.length <= 6) viewModel.setVerifyCode(it) },
                                label = { Text("设备验证码（6位大写字母）") },
                                placeholder = { Text("输入设备标签上的验证码") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("重试")
                        }
                    }
                }
            }

            uiState.streamUrl != null -> {
                EzvizPlayerView(
                    player = player,
                    modifier = Modifier.fillMaxSize()
                )

                // 缓冲指示器
                if (uiState.playerState == PlayerState.Buffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 控制浮层
        VideoControlsOverlay(
            isVisible = controlsVisible,
            isMuted = uiState.isMuted,
            isPlaying = uiState.playerState == PlayerState.Playing,
            onBackClick = onBackClick,
            onMuteToggle = { viewModel.toggleMute() },
            onScreenshot = { /* Phase 2 */ },
            onToggleFullscreen = {
                activity?.requestedOrientation =
                    if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        )

        // 云台控制盘（竖屏时在底部，横屏时在右侧）
        if (uiState.isPtzEnabled && controlsVisible) {
            if (!isLandscape) {
                // 竖屏 - 底部
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    PTZController(
                        onPtzStart = viewModel::onPtzStart,
                        onPtzStop = viewModel::onPtzStop,
                        enabled = uiState.playerState == PlayerState.Playing
                    )
                }
            } else {
                // 横屏 - 右侧
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    PTZController(
                        onPtzStart = viewModel::onPtzStart,
                        onPtzStop = viewModel::onPtzStop,
                        enabled = uiState.playerState == PlayerState.Playing
                    )
                }
            }
        }

        // 预置位按钮（竖屏底部左侧）
        if (uiState.isPtzEnabled && controlsVisible && !isLandscape) {
            SmallFloatingActionButton(
                onClick = { showPresetDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 80.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = "预置位")
            }
        }
    }

    // 预置位选择弹窗
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("预置位") },
            text = {
                if (uiState.presets.isEmpty()) {
                    Text("暂无预置位，点击 + 添加当前位置")
                } else {
                    Column {
                        uiState.presets.forEach { preset ->
                            TextButton(
                                onClick = {
                                    viewModel.moveToPreset(preset.index)
                                    showPresetDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(preset.name.ifBlank { "预置位 #${preset.index}" })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPresetDialog = false
                    showAddPresetDialog = true
                }) {
                    Text("+ 添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 添加预置位弹窗
    if (showAddPresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            title = { Text("添加预置位") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("名称（可选）") },
                    placeholder = { Text("例如：门口、客厅全景") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addPreset(presetName)
                    showAddPresetDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPresetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
