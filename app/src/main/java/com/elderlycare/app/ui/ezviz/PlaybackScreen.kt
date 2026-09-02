package com.elderlycare.app.ui.ezviz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
            // 验证码输入卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.padding(7.dp).fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = uiState.verifyCode,
                        onValueChange = viewModel::onVerifyCodeChange,
                        label = { Text("设备验证码（6位）", fontSize = 14.sp) },
                        placeholder = { Text("设备标签上的6位大写字母", fontSize = 14.sp, color = TextHint) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center,
                            color = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

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
                            .height(240.dp)
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
                                        Text("输入验证码后自动加载", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("支持 SD 卡本地录像回放", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 提示卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (uiState.playbackUrl != null) Icons.Default.PlayCircle else Icons.Default.Info,
                            null,
                            tint = if (uiState.playbackUrl != null) StatusGreen else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.playbackUrl != null) "正在回放" else "就绪",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        // 告警定位时显示实际回放时间窗口（如 15:29:30 ~ 15:30:30），默认显示全天
                        "${uiState.selectedDate.ifBlank { "——" }} " +
                            if (uiState.startTime.endsWith("00:00:00") && uiState.stopTime.endsWith("23:59:59"))
                                "全天录像"
                            else
                                "${uiState.startTime.takeLast(8)} ~ ${uiState.stopTime.takeLast(8)}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
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
