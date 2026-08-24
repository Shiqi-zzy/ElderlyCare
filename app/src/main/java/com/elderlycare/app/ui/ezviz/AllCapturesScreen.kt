package com.elderlycare.app.ui.ezviz

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.CaptureItem
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 全部抓拍页 FAB 绿色（规格：右下角绿色悬浮相机按钮；与首页「抓拍」色块同色） */
private val CaptureGreen = Color(0xFF42BD67)

/** 顶部横幅极浅蓝渐变（与其他页面统一） */
private val BannerBlueStart = Color(0xFFEAF2FF)
private val BannerBlueEnd = Color(0xFFF5F9FF)
private val BannerText = Color(0xFF1A2332)

/**
 * 全部抓拍页：手动抓拍 + 设备告警自动抓拍快照的唯一查看入口（纯列表页，无顶部播放器）。
 *
 * 数据源=后端 SQLite alarm_events（图片只存后端 media/，App 本地 Room 不落图片）；
 * 与消息中心双轨隔离：消息 Tab 只展示告警文字，点击报警行跳转本页；
 * 本页独立未读角标（后端 is_read 计数），与消息 Tab 角标互不干扰。
 * 旧 AlarmListScreen（萤石云端告警列表）代码保留但不再被引用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCapturesScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToRemindPlan: () -> Unit = {},
    viewModel: AllCapturesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }

    // 一次性 toast（手动抓拍结果文案）
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        floatingActionButton = {
            // 右下角绿色小悬浮相机（手动抓拍入口；非底部通栏大按钮）
            FloatingActionButton(
                onClick = { viewModel.manualCapture() },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = CaptureGreen,
                contentColor = Color.White,
            ) {
                if (uiState.isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_screenshot),
                        contentDescription = "手动抓拍",
                        modifier = Modifier.padding(8.dp).size(26.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 极浅蓝渐变顶部横幅
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BannerBlueStart, BannerBlueEnd)
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = BannerText)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "全部抓拍",
                            fontWeight = FontWeight.SemiBold,
                            color = BannerText,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (uiState.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = Error) {
                                Text(
                                    "${uiState.unreadCount}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnError
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.HelpOutline, "帮助", tint = BannerText)
                    }
                    TextButton(onClick = onNavigateToRemindPlan) {
                        Text("抓拍计划", color = Primary)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.items.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.items.isEmpty() && uiState.loadFailed -> {
                        // 加载失败：网络异常占位图（未绑定设备显示普通空态，不误报网络异常）
                        EmptyStateImage(
                            painter = painterResource(R.drawable.ic_network_error),
                            text = "网络异常，请稍后重试"
                        )
                    }
                    uiState.items.isEmpty() -> {
                        // 空态：插图 + 提示（未绑定设备同样显示空态）
                        EmptyStateImage(
                            painter = painterResource(R.drawable.ic_empty_state),
                            text = "暂无抓拍内容"
                        )
                    }
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)) {
                            items(uiState.items, key = { it.recordId }) { item ->
                                CaptureRow(
                                    item = item,
                                    onClick = { viewModel.markRead(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("全部抓拍") },
            text = {
                Text(
                    "这里集中展示两类抓拍快照：\n" +
                        "• 手动抓拍：点击右下角相机按钮，设备即时抓拍一张快照（两次间隔至少 4 秒）；\n" +
                        "• 设备告警抓拍：设备检测到人形活动时自动抓拍上传。\n\n" +
                        "快照只保存在后端，点按条目可标记已读；「抓拍计划」可配置设备定时抓拍提醒。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("知道了") }
            }
        )
    }
}

/** 抓拍页空态/网络异常占位：插图 + 提示文案（铺满父 Box，内容居中） */
@Composable
private fun EmptyStateImage(painter: androidx.compose.ui.graphics.painter.Painter, text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painter,
            contentDescription = text,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(12.dp))
        Text(text, color = TextSecondary)
    }
}

@Composable
private fun CaptureRow(item: CaptureItem, onClick: () -> Unit) {
    val imageUrl = ServiceLocator.captureRepository.resolveImageUrl(item.localPicUrl)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (!item.isRead) Primary.copy(alpha = 0.06f) else Surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图 96×72dp；图片未就绪（localPicUrl 空）占位
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.PhotoCamera,
                        null,
                        modifier = Modifier.align(Alignment.Center).size(24.dp),
                        tint = TextHint
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标签按 capture_type 渲染：manual→「手动抓拍」，auto→「设备自动抓拍」
                Text(
                    text = if (item.captureType == "manual") "手动抓拍" else "设备自动抓拍",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 自动抓拍附告警描述（如「人形检测告警」）
                if (item.captureType != "manual" && item.alarmName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.alarmName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatEventTime(item.eventTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // 未读小红点
            if (!item.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusRed)
                )
            }
        }
    }
}

/** 毫秒时间戳 → MM-dd HH:mm（无效兜底空串不展示） */
private fun formatEventTime(eventTime: Long): String {
    if (eventTime <= 0) return ""
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(eventTime))
    }.getOrDefault("")
}
