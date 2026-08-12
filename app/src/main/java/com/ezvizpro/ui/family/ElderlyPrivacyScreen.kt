package com.ezvizpro.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val Accent = Color(0xFF3B82F6)

/**
 * 隐私控制（独立子页面，从家属端 MainScreen → 我的Tab 导航进入）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlyPrivacyScreen(
    onBack: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToast() }
    }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🛡 隐私控制", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        val elderly = state.selectedElderly
        if (elderly == null && state.elderlyList.isNotEmpty()) {
            LaunchedEffect(Unit) { viewModel.selectElderly(state.elderlyList.first()) }
        }

        Column(Modifier.padding(padding)) {
            // 老人选择器
            if (state.elderlyList.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .height(64.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    items(state.elderlyList) { e ->
                        val selected = state.selectedElderly?.id == e.id
                        Surface(
                            onClick = { viewModel.selectElderly(e) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Accent else CardBg,
                            contentColor = if (selected) Color.White else TextSecondary
                        ) {
                            Text(
                                e.name,
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }

            Column(
                Modifier.padding(24.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                Icon(
                    if (state.privacyPaused) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null,
                    modifier = Modifier.size(72.dp),
                    tint = if (state.privacyPaused) Color(0xFFF59E0B) else Color(0xFF22C55E)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (state.privacyPaused) "监控已暂停" else "监控运行中",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (state.privacyPaused) "摄像头与传感器数据暂不上报，家属可随时恢复"
                    else "设备在线，数据正常采集上报",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.togglePrivacy() },
                    Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.privacyPaused) Color(0xFF22C55E) else Color(0xFFF59E0B)
                    )
                ) {
                    Icon(
                        if (state.privacyPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.privacyPaused) "恢复监控" else "暂停监控",
                        fontSize = 16.sp
                    )
                }

                // 设备列表
                if (state.devices.isNotEmpty()) {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "绑定设备 (${state.devices.size})",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.devices) { device ->
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg)
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        deviceIcon(device.deviceType),
                                        null,
                                        tint = if (device.status == "online") Color(0xFF22C55E) else TextSecondary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            device.deviceName,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${deviceTypeLabel(device.deviceType)} · ${if (device.status == "online") "在线" else "离线"}",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun deviceIcon(t: String): ImageVector = when (t) {
    "camera" -> Icons.Default.Videocam
    "wearable" -> Icons.Default.Watch
    "bed_sensor" -> Icons.Default.Bed
    "smoke_detector" -> Icons.Default.LocalFireDepartment
    else -> Icons.Default.DevicesOther
}

private fun deviceTypeLabel(t: String) = when (t) {
    "camera" -> "摄像头"
    "wearable" -> "穿戴设备"
    "bed_sensor" -> "床垫传感器"
    "smoke_detector" -> "烟感"
    "gas_detector" -> "燃气"
    else -> t
}
