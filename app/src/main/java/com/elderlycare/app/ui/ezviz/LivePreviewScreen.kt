package com.elderlycare.app.ui.ezviz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.Device
import com.elderlycare.app.data.ezviz.model.DeviceStatus
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.data.message.MessageFiles
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.util.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ==================== 预览页设计色值（页面局部常量，不改全局主题） ====================
private val PanelBg = Color(0xCC1A2330)
private val FuncGreen = Color(0xFF42BD67)
private val MicBlue = Color(0xFF3B7EEB)
private val FuncOrange = Color(0xFFFF9F38)
private val FuncPurple = Color(0xFF9068D8)
private val CaptureBlue = Color(0xFF49A1E7)
private val RecordRed = Color(0xFFE53935)
private val PageDarkBg = Color(0xFF0F172A)

/** 按住说话时长上限（秒），与留言页 MessageViewModel.MAX_RECORD_SEC 一致 */
private const val MAX_HOLD_TALK_SEC = 60

/**
 * 视频预览播放器页（Phase 3 UI 重设计，图四原型 B）。
 *
 * 布局：顶栏（返回+动态设备名+齿轮）｜左上「直播中」小字｜叠加控件（暂停/音量/高清/截图/投屏）
 * ｜云台圆盘（supportPtz 设备）｜底部半透明面板单行五控件均分（视频通话/录制/按住说话/对讲/截图）
 * ｜点播/广播FM 仅保留首页网格入口，播放器页不再渲染。VM 取流/录制状态机零改动；按住说话复用留言页录音+级联发送底层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePreviewScreen(
    deviceSerial: String,
    verifyCode: String,
    onBackClick: () -> Unit,
    onNavigateToPlay: (String) -> Unit = {},
    onNavigateToFm: (String) -> Unit = {},
    onNavigateToVideoCall: () -> Unit = {},
    onNavigateToMessage: () -> Unit = {},
    viewModel: LivePreviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = rememberEzvizPlayer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }

    // 预览是否已连接：取流成功且未在加载/出错；H5 路径取流即视为连接（无原生回调）
    val previewReady = uiState.streamUrl != null && !uiState.isLoading && uiState.error == null &&
        (uiState.useWebView || uiState.playerState == PlayerState.Playing)

    // 设备云端信息（顶栏设备名 / 齿轮对话框 / 云台显隐共用一次调用；失败兜底 SN）
    var deviceInfo by remember { mutableStateOf<Device?>(null) }
    LaunchedEffect(deviceSerial) {
        deviceInfo = when (val result = ServiceLocator.repository.getDeviceInfo(deviceSerial)) {
            is NetworkResult.Success -> result.data
            else -> null
        }
    }
    var showDeviceDialog by remember { mutableStateOf(false) }

    fun toast(msg: String) {
        // Toast 只能在主线程弹：后台协程（录音/发送/计时）里也会调这里，非主线程时抛回主线程执行
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== 按住说话（Screen 内状态机；录音/发送复用留言页底层，业务零改动） ====================
    val recorder = remember { AudioRecorder() }
    var isHoldingTalk by remember { mutableStateOf(false) }
    var holdElapsed by remember { mutableStateOf(0) }
    var holdAmplitude by remember { mutableStateOf(0) }
    var currentRecordFile by remember { mutableStateOf<File?>(null) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    /** 松开发送：录音文件 → 级联发送（双通道 → 失败自动降级 sendonce + Room 落库） */
    fun finishHoldTalk() {
        if (!isHoldingTalk) return
        holdJob?.cancel()
        holdJob = null
        scope.launch(Dispatchers.IO) {
            val duration = recorder.stop()
            isHoldingTalk = false
            val file = currentRecordFile ?: return@launch
            currentRecordFile = null
            when {
                duration <= 0 -> {
                    MessageFiles.deleteQuietly(file)
                    toast(context.getString(R.string.message_record_failed))
                }
                duration < 1 -> {
                    MessageFiles.deleteQuietly(file)
                    toast(context.getString(R.string.message_record_too_short))
                }
                else -> {
                    val rowId = ServiceLocator.messageRepository.sendRecordMessage(deviceSerial, file, duration)
                    val msg = ServiceLocator.messageRepository.getMessageById(rowId)
                    toast(
                        when (msg?.sendStatus) {
                            MessageEntity.SEND_STATUS_SUCCESS -> "语音留言已发送"
                            MessageEntity.SEND_STATUS_FAILED -> msg.failReason.ifBlank { "语音留言发送失败" }
                            else -> "语音留言发送中"
                        }
                    )
                }
            }
        }
    }

    fun startHoldTalk() {
        if (isHoldingTalk) return
        val file = MessageFiles.newRecordFile(context)
        currentRecordFile = file
        scope.launch(Dispatchers.IO) {
            val ok = recorder.start(file)
            if (!ok) {
                currentRecordFile = null
                isHoldingTalk = false
                toast(context.getString(R.string.message_record_failed))
                return@launch
            }
            isHoldingTalk = true
            holdElapsed = 0
            holdAmplitude = 0
            // 声波采样 + 计时循环（60s 上限自动结束，镜像留言页逻辑）
            holdJob = scope.launch(Dispatchers.Default) {
                var tick = 0
                while (isActive) {
                    holdAmplitude = recorder.getAmplitude()
                    delay(100)
                    tick++
                    if (tick >= 10) {
                        tick = 0
                        holdElapsed++
                        if (holdElapsed >= MAX_HOLD_TALK_SEC) {
                            toast(context.getString(R.string.message_record_max_reached))
                            finishHoldTalk()
                            break
                        }
                    }
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startHoldTalk()
        else toast(context.getString(R.string.message_permission_denied))
    }

    // ==================== 存储权限（仅 API≤28 写公共目录需要；29+ 走 MediaStore 免权限） ====================
    var pendingAction by remember { mutableStateOf(PendingAction.NONE) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when (pendingAction) {
            PendingAction.CAPTURE -> if (granted) viewModel.captureSnapshot()
            else Toast.makeText(context, "缺少存储权限，无法保存截图", Toast.LENGTH_SHORT).show()
            PendingAction.RECORD -> if (granted) viewModel.toggleRecord()
            else Toast.makeText(context, "缺少存储权限，无法保存录像", Toast.LENGTH_SHORT).show()
            PendingAction.NONE -> Unit
        }
        pendingAction = PendingAction.NONE
    }

    /** 抓拍/录制入口统一权限校验：29+ 免权限直行；≤28 无权限先弹窗申请 */
    fun withStoragePermission(action: PendingAction) {
        val needPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (!needPermission) {
            if (action == PendingAction.CAPTURE) viewModel.captureSnapshot() else viewModel.toggleRecord()
        } else {
            pendingAction = action
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val onCaptureClick: () -> Unit = {
        if (uiState.isCapturing) {
            toast("抓拍处理中，请稍候")
        } else if (!previewReady) {
            Toast.makeText(context, "请等待视频流连接成功", Toast.LENGTH_SHORT).show()
        } else {
            withStoragePermission(PendingAction.CAPTURE)
        }
    }

    val onRecordClick: () -> Unit = {
        if (uiState.isRecording) {
            viewModel.toggleRecord() // 录制中点击 = 停止，无需再校验权限
        } else if (!previewReady) {
            Toast.makeText(context, "请等待视频流连接成功", Toast.LENGTH_SHORT).show()
        } else {
            withStoragePermission(PendingAction.RECORD)
        }
    }

    LaunchedEffect(deviceSerial) {
        viewModel.initialize(deviceSerial, verifyCode)
        viewModel.bindPlayer(player)
    }

    LaunchedEffect(uiState.streamUrl, uiState.useWebView) {
        // ezopen 加密流由 EzvizWebPlayer（H5）播放，本地 HTML URL 不能喂给原生 ExoPlayer
        val url = uiState.streamUrl
        if (url != null && !uiState.useWebView) {
            player.play(url)
        }
    }

    LaunchedEffect(uiState.isMuted) {
        player.setMuted(uiState.isMuted)
    }

    DisposableEffect(Unit) {
        onDispose {
            // 页面销毁边界：正在录制必须自动停止，防止录像文件损坏
            viewModel.stopRecording(auto = true)
            viewModel.closeLive()
            player.release()
            // 按住说话兜底：页面销毁时录音中直接丢弃文件（不发送）
            if (isHoldingTalk) {
                holdJob?.cancel()
                runCatching { recorder.cancel() }
                currentRecordFile?.let { MessageFiles.deleteQuietly(it) }
            }
        }
    }

    // 控件自动隐藏：按住说话 / 云台按压期间不隐藏（保证 STOP 不漏发）
    var ptzPressed by remember { mutableStateOf(false) }
    LaunchedEffect(controlsVisible, isHoldingTalk, ptzPressed) {
        if (controlsVisible && !isHoldingTalk && !ptzPressed) {
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
                            Text(
                                deviceInfo?.deviceName?.takeIf { it.isNotBlank() } ?: deviceSerial,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                deviceSerial,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                        }
                    },
                    actions = {
                        // 齿轮：设备信息对话框（名称/序列号/在线状态）
                        IconButton(onClick = { showDeviceDialog = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "设备信息", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PageDarkBg)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PageDarkBg)
        ) {
            // 隐藏 1x1 SurfaceView：手机本地录制/抓拍的解码 surface（不可见，不参与布局交互）
            AndroidView(
                modifier = Modifier.size(1.dp).alpha(0f),
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        setZOrderOnTop(false)
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                viewModel.onRecordSurfaceReady(holder)
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) = Unit

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                viewModel.onRecordSurfaceReady(null)
                            }
                        })
                    }
                }
            )

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("正在连接设备…", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                uiState.error != null && uiState.streamUrl == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            // 视频预览连接失败占位图
                            Image(
                                painter = painterResource(R.drawable.ic_connect_fail),
                                contentDescription = "连接失败",
                                modifier = Modifier.size(120.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                uiState.error!!,
                                color = Color(0xFFFCA5A5),
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
                    if (uiState.useWebView) {
                        // ezopen 加密流：萤石 JSSDK 网页播放器（与回放页共用）
                        EzvizWebPlayer(
                            url = uiState.streamUrl!!,
                            modifier = Modifier.fillMaxSize(),
                            onConsoleMessage = { msg ->
                                // H5 播放器报错视为预览断开（录制中自动停止）
                                if (msg.contains("播放器错误")) viewModel.notifyPreviewDisconnected()
                            }
                        )
                    } else {
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

            // 播放画面点击：切换显示/隐藏顶部操作栏与叠加控件
            // （覆盖在视频层上、所有控件之下；控件自身消费事件，不会穿透触发切换）
            if (uiState.streamUrl != null && uiState.error == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { controlsVisible = !controlsVisible }
                )
            }

            // 左上角「直播中 | 128KB/s」小字（静态文案，非交互不挡点击）
            if (previewReady) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                ) {
                    Text(
                        "直播中 | 128KB/s",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 视频底部控制栏已移至底部面板上方（暂停/音量/投屏，线条图标风格）

            // 云台控制已移至底部白色面板（白色圆盘风格）

            // 底部：视频控制栏 + 白色面板（功能行线条图标 + 云台控制圆盘）
            if (uiState.streamUrl != null && uiState.error == null) {
                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    // 视频底部控制栏（暂停/音量/投屏，线条图标，渐变背景）
                    if (controlsVisible && previewReady) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.5f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (uiState.useWebView) {
                                    toast("网页播放器暂不支持暂停")
                                } else if (uiState.playerState == PlayerState.Paused) {
                                    player.resume()
                                } else {
                                    player.pause()
                                }
                            }) {
                                Icon(
                                    if (uiState.playerState == PlayerState.Paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    null, tint = Color.White, modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.toggleMute() }) {
                                Icon(
                                    if (uiState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    null, tint = Color.White, modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { toast("投屏功能即将上线") }) {
                                Icon(Icons.Filled.Cast, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }

                    // 白色圆角面板
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White,
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .padding(vertical = 16.dp)
                    ) {
                        // 功能行：对讲 / 视频通话 / 按住说话 / 录制 / 截图（线条图标风格）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LineActionButton(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                label = "对讲",
                                onClick = onNavigateToMessage
                            )
                            LineActionButton(
                                icon = Icons.Filled.VideoCall,
                                label = "视频通话",
                                onClick = onNavigateToVideoCall
                            )
                            // 按住说话（中间，保留圆形按钮+按住手势）
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isHoldingTalk) MicBlue.copy(alpha = 0.75f) else MicBlue,
                                    modifier = Modifier
                                        .size(if (isHoldingTalk) 58.dp else 52.dp)
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                down.consume()
                                                if (ContextCompat.checkSelfPermission(
                                                        context, Manifest.permission.RECORD_AUDIO
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    startHoldTalk()
                                                }
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id }
                                                    if (change == null || !change.pressed) break
                                                    change.consume()
                                                }
                                                finishHoldTalk()
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (isHoldingTalk) "松开结束" else "按住说话",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            LineActionButton(
                                icon = if (uiState.isRecording) Icons.Filled.FiberManualRecord else Icons.Filled.Videocam,
                                label = if (uiState.isRecording) "停止" else "录制",
                                onClick = onRecordClick
                            )
                            LineActionButton(
                                icon = Icons.Filled.CameraAlt,
                                label = if (uiState.isCapturing) "抓拍中" else "截图",
                                onClick = onCaptureClick,
                                enabled = !uiState.isCapturing
                            )
                        }

                        // 云台控制圆盘（仅支持云台的设备显示）
                        if (deviceInfo?.supportPtz == true) {
                            Spacer(Modifier.height(16.dp))
                            PtzControlDial(
                                deviceSerial = deviceSerial,
                                onToast = { toast(it) },
                                onPressedChange = { ptzPressed = it },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text("设备信息") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeviceInfoRow("设备名称", deviceInfo?.deviceName?.takeIf { it.isNotBlank() } ?: deviceSerial)
                    DeviceInfoRow("序列号", deviceSerial)
                    DeviceInfoRow(
                        "状态",
                        when {
                            deviceInfo == null -> "未知"
                            deviceInfo?.status == DeviceStatus.ONLINE -> "在线"
                            else -> "离线"
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showDeviceDialog = false }) { Text("知道了") } }
        )
    }
}

/** 存储权限申请后的待执行动作（仅 API≤28 需要） */
private enum class PendingAction { NONE, CAPTURE, RECORD }

/** 齿轮对话框信息行 */
@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** 叠加控件小圆钮（暂停/音量/高清/截图/投屏）；统一 Material 内置白色矢量图标 */
@Composable
private fun OverlayControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick)
    ) {
        // 圆形容器内四周预留内边距，白色矢量图标居中不顶边
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** 底部面板线条图标按钮（无背景色块，深色线条图标 + 文字标签）；enabled=false 时半透明且不可点 */
@Composable
private fun LineActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextPrimary, style = MaterialTheme.typography.labelSmall)
    }
}

/** 底部面板圆按钮（圆形色块 + 小字标签）；统一 Material 内置白色矢量图标 */
@Composable
private fun RoundActionButton(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    labelColor: Color = Color.White.copy(alpha = 0.85f),
    size: Dp = 56.dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(size)) {
            // 圆形容器内四周预留内边距，白色矢量图标居中不顶边
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = labelColor, style = MaterialTheme.typography.labelSmall)
    }
}
