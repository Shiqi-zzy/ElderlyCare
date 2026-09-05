package com.elderlycare.app.ui.ezviz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    deviceSerial: String,
    verifyCode: String,
    onBackClick: () -> Unit,
    startAtTime: String = "",
    viewModel: PlaybackViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = rememberEzvizPlayer()
    var showCalendar by remember { mutableStateOf(false) }

    // 自动读取本地持久化的验证码带入（无需手动输入），并直接触发回放加载
    LaunchedEffect(deviceSerial, startAtTime) {
        viewModel.initialize(deviceSerial, startAtTime = startAtTime)
        if (verifyCode.length == 6) {
            viewModel.onVerifyCodeChange(verifyCode)
        }
    }

    LaunchedEffect(uiState.playbackUrl) {
        uiState.playbackUrl?.let { url ->
            if (!uiState.useWebView) {
                player.play(url)
            }
        }
    }

    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) player.resume() else player.pause()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("录像回放", fontWeight = FontWeight.SemiBold)
                        Text(deviceSerial, fontSize = 14.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showCalendar = true }) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp), tint = Primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            uiState.selectedDate.ifBlank { "选择日期" },
                            color = Primary,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 播放器区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            deviceSerial,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("RK3", color = TextSecondary, fontSize = 14.sp)

                        if (uiState.isLoading) {
                            Spacer(Modifier.weight(1f))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("加载中…", color = Primary, fontSize = 14.sp)
                        }
                    }

                    HorizontalDivider(color = DividerColor, thickness = 1.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .background(Color(0xFF0F172A))
                    ) {
                        when {
                            uiState.isLoading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
                                        Spacer(Modifier.height(12.dp))
                                        Text("正在连接设备…", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                                    }
                                }
                            }

                            uiState.playbackUrl != null && uiState.useWebView -> {
                                EzvizWebPlayer(
                                    url = uiState.playbackUrl!!,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            uiState.playbackUrl != null && !uiState.useWebView -> {
                                EzvizPlayerView(
                                    player = player,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            uiState.error != null -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            null,
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFFF87171)
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            uiState.error!!,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedButton(onClick = { viewModel.retry() }) {
                                            Text("重试")
                                        }
                                    }
                                }
                            }

                            else -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            null,
                                            modifier = Modifier.size(56.dp),
                                            tint = Color.White.copy(alpha = 0.3f)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text("选择下方录像片段进行回放", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("支持 SD 卡本地录像", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== 录像片段列表（萤石风格）=====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "录像片段",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.recordFiles.isEmpty()) "" else "共 ${uiState.recordFiles.size} 段",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        if (uiState.isListLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                        } else {
                            TextButton(onClick = { viewModel.loadRecordFiles() }) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = Primary)
                                Spacer(Modifier.width(2.dp))
                                Text("刷新", color = Primary, fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = DividerColor, thickness = 1.dp)

                    when {
                        uiState.isListLoading && uiState.recordFiles.isEmpty() -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Primary)
                                    Spacer(Modifier.height(10.dp))
                                    Text("正在获取录像…", color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        }

                        uiState.listError != null && uiState.recordFiles.isEmpty() -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.WifiOff,
                                        null,
                                        modifier = Modifier.size(36.dp),
                                        tint = Color(0xFFF87171)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        uiState.listError!!,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    TextButton(onClick = { viewModel.loadRecordFiles() }) {
                                        Text("重试", color = Primary)
                                    }
                                }
                            }
                        }

                        uiState.recordFiles.isEmpty() -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.VideocamOff,
                                        null,
                                        modifier = Modifier.size(36.dp),
                                        tint = TextHint
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("当天暂无录像片段", color = TextSecondary, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("请确认设备已插入 SD 卡并开启录像", color = TextHint, fontSize = 12.sp)
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(uiState.recordFiles, key = { "${it.startTime}-${it.endTime}" }) { item ->
                                    RecordFileRow(
                                        item = item,
                                        selected = uiState.startTime == formatFull(item.startTime),
                                        onClick = { viewModel.onFileSelected(item.startTime, item.endTime) }
                                    )
                                    HorizontalDivider(color = DividerColor.copy(alpha = 0.6f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCalendar) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onDateSelected(date.toString())
                    }
                    showCalendar = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatFull(ms: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ms))
}

@Composable
private fun RecordFileRow(
    item: RecordFileItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) Primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 类型图标（事件=感叹号，定时=时钟）
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (item.localType.contains("ALARM", ignoreCase = true)) {
                Color(0xFFFFF4E6)
            } else {
                Primary.copy(alpha = 0.1f)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                if (item.localType.contains("ALARM", ignoreCase = true)) {
                    Icons.Default.NotificationsActive
                } else {
                    Icons.Default.Schedule
                },
                contentDescription = null,
                tint = if (item.localType.contains("ALARM", ignoreCase = true)) Color(0xFFF59E0B) else Primary,
                modifier = Modifier.padding(8.dp).fillMaxSize()
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                item.timeLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    item.localType.contains("ALARM", ignoreCase = true) -> "事件录像"
                    item.localType.contains("TIMING", ignoreCase = true) -> "定时录像"
                    else -> "本地录像"
                },
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // 时长徽标（0'43"）
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF1F5F9)
        ) {
            Text(
                item.durationLabel,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 12.sp,
                color = TextPrimary
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "回放",
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
