package com.ezvizpro.ui.playback

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.ezvizpro.core.player.EzvizPlayerView
import com.ezvizpro.core.player.rememberEzvizPlayer
import com.ezvizpro.ui.theme.Gray600
import java.time.LocalDate

private val AccentBlue = Color(0xFF3B82F6)
private val SurfaceBg = Color(0xFFF1F5F9)
private val CardBg = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    deviceSerial: String,
    channelNo: Int,
    onBackClick: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = rememberEzvizPlayer()
    var showCalendar by remember { mutableStateOf(false) }

    // 初始化（不自动加载，等验证码输入）
    LaunchedEffect(deviceSerial) {
        viewModel.initialize(deviceSerial, channelNo)
    }

    // 播放回放流（ExoPlayer 用于非 ezopen 协议）
    LaunchedEffect(uiState.playbackUrl) {
        uiState.playbackUrl?.let { url ->
            if (!uiState.useWebView) {
                player.play(url)
            }
        }
    }

    // 播放状态同步
    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) player.resume() else player.pause()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("录像回放", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            deviceSerial,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { showCalendar = true }) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp), tint = AccentBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            uiState.selectedDate.ifBlank { "选择日期" },
                            color = AccentBlue,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
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
            // ══════════════════════════════════════════
            // 1. 顶部卡片：验证码输入
            // ══════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 锁图标
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier
                                .padding(7.dp)
                                .fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = uiState.verifyCode,
                        onValueChange = viewModel::onVerifyCodeChange,
                        label = { Text("设备验证码（6位）", fontSize = 13.sp) },
                        placeholder = { Text("ZIFYKU", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.5f)) },
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
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg
                        )
                    )
                }
            }

            // ══════════════════════════════════════════
            // 2. 播放器区域（头部显示设备ID）
            // ══════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    // 播放器头部：设备ID
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            null,
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            deviceSerial,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "RK3($deviceSerial)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        if (uiState.isLoading) {
                            Spacer(modifier = Modifier.weight(1f))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AccentBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("加载中…", color = AccentBlue, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    // 播放器画面
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color(0xFF0F172A))  // 暗色播放器背景
                    ) {
                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "正在连接设备…",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            uiState.playbackUrl != null && uiState.useWebView -> {
                                // ezopen 加密流 → 萤石 JSSDK 网页播放器
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
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFFF87171)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            uiState.error!!,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.retry() },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))
                                            )
                                        ) {
                                            Text("重试")
                                        }
                                    }
                                }
                            }

                            else -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),
                                            tint = Color.White.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "输入验证码后自动加载",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "支持 SD 卡本地录像回放",
                                            color = Color.White.copy(alpha = 0.3f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════
            // 3. 下方提示卡片
            // ══════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (uiState.playbackUrl != null) Icons.Default.PlayCircle else Icons.Default.Info,
                            null,
                            tint = if (uiState.playbackUrl != null) Color(0xFF22C55E) else AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.playbackUrl != null) "正在回放" else "就绪",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${uiState.selectedDate.ifBlank { "——" }} 全天录像",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 提示信息
                    if (uiState.playbackUrl == null && uiState.error == null) {
                        Text(
                            "请输入设备标签上的6位大写字母验证码",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            "输入完成后自动加载录像回放流",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    if (uiState.error != null) {
                        Text(
                            "提示：如设备无 SD 卡或未开启录像，将无法回放",
                            fontSize = 12.sp,
                            color = Color(0xFFF87171)
                        )
                    }
                    if (uiState.playbackUrl != null) {
                        Text(
                            "录像回放中 · ${uiState.selectedDate}",
                            fontSize = 12.sp,
                            color = Color(0xFF22C55E)
                        )
                    }
                }
            }
        }
    }

    // 日期选择弹窗
    if (showCalendar) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onDateSelected(date.toString())
                    }
                    showCalendar = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * 萤石 JSSDK 网页播放器（用于播放 ezopen 协议的加密视频流）
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EzvizWebPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.allowContentAccess = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val reqUrl = request?.url?.toString() ?: ""
                        if (reqUrl.startsWith("ezopen://")) {
                            timber.log.Timber.d("WebView 拦截 ezopen 导航: $reqUrl")
                            return true
                        }
                        return false
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        timber.log.Timber.e("WebView 加载错误: ${error?.description}, url=${request?.url}")
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}
