package com.elderlycare.app.ui.ezviz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePreviewScreen(
    deviceSerial: String,
    verifyCode: String,
    onBackClick: () -> Unit,
    viewModel: LivePreviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = rememberEzvizPlayer()

    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(deviceSerial) {
        viewModel.initialize(deviceSerial, verifyCode)
        viewModel.bindPlayer(player)
    }

    LaunchedEffect(uiState.streamUrl) {
        uiState.streamUrl?.let { player.play(it) }
    }

    LaunchedEffect(uiState.isMuted) {
        player.setMuted(uiState.isMuted)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.closeLive()
            player.release()
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    Scaffold(
        topBar = {
            if (controlsVisible) {
                TopAppBar(
                    title = {
                        Column {
                            Text("实时预览")
                            Text(
                                deviceSerial,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleMute() }) {
                            Icon(
                                if (uiState.isMuted) Icons.AutoMirrored.Filled.VolumeOff
                                else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (uiState.isMuted) "取消静音" else "静音"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(androidx.compose.ui.graphics.Color(0xFF0F172A))
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("正在连接设备…", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                uiState.error != null && uiState.streamUrl == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                uiState.error!!,
                                color = androidx.compose.ui.graphics.Color(0xFFFCA5A5),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (uiState.showCodeInput) {
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = uiState.verifyCode,
                                    onValueChange = { if (it.length <= 6) viewModel.setVerifyCode(it) },
                                    label = { Text("设备验证码（6位大写字母）") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
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
                    if (uiState.playerState == PlayerState.Buffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}
