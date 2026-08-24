package com.elderlycare.app.ui.family

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.elderlycare.app.R
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.RtcSignalingManager
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.Device
import com.elderlycare.app.data.ezviz.model.DeviceStatus
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import kotlinx.coroutines.flow.flowOf

// ==================== 首页设计色值（页面局部常量，不改全局主题） ====================
private val HomeBg = Color(0xFFF7F9FC)
private val FuncBlue = Color(0xFF4086E8)
private val FuncGreen = Color(0xFF42BD67)
private val FuncOrange = Color(0xFFFF9F38)
private val FuncPurple = Color(0xFF9068D8)
private val FuncLightBlue = Color(0xFF7BB3F5)
private val FuncLightGreen = Color(0xFF9BDCA8)
private val FuncLightOrange = Color(0xFFFFBE7E)
private val FuncLightRed = Color(0xFFF59B9B)
private val BadgeRed = Color(0xFFF24848)
private val OnlineGreen = Color(0xFF4CAF50)
private val OfflineGray = Color(0xFFBDBDBD)
private val BannerGradient = Brush.horizontalGradient(
    listOf(Color(0xFF4A7FE8), Color(0xFF6BA5FF))
)

/**
 * 家属首页（UI 美化版）。
 *
 * 功能网格固定 2 行 4 列（功能与回调零改动）：
 * 第一行：抓拍 / 视频通话 / 对讲 / 录像
 * 第二行：留言 / 录音 / 点播 / 广播
 *
 * UI 改动：
 * - 顶部 Banner 改为蓝色渐变卡片 + 文案 + 房子爱心插画 + 轮播指示点
 * - 设备卡片缩略图无云端 cover 时显示默认居家场景图 + 中央播放按钮
 * - 功能图标去掉方形彩色背景，改为扁平彩色线条图标，整体放入白色卡片
 * - 所有数据获取、未读数、点击回调、导航逻辑保持不变
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyHomeScreen(
    onOpenCaptures: () -> Unit,
    onNavigateToVideo: () -> Unit,
    onNavigateToVideoCall: () -> Unit,
    onNavigateToMessage: () -> Unit,
    onNavigateToRk3Play: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onOpenMessagesTab: () -> Unit
) {
    // 已绑定设备（响应式）
    var boundDevice by remember { mutableStateOf<BindingRepository.AccessibleDevice?>(null) }
    LaunchedEffect(Unit) {
        ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { boundDevice = it }
    }
    val deviceSn = boundDevice?.deviceSn

    // 未读数
    val totalUnread by remember(deviceSn) {
        deviceSn?.let { ServiceLocator.messageRepository.observeUnreadCount(it) } ?: flowOf(0)
    }.collectAsStateWithLifecycle(initialValue = 0)
    val leaveUnread by remember(deviceSn) {
        deviceSn?.let {
            ServiceLocator.messageRepository.observeUnreadCountByCategory(it, MessageEntity.MESSAGE_CATEGORY_LEAVE_MSG)
        } ?: flowOf(0)
    }.collectAsStateWithLifecycle(initialValue = 0)
    // 后端抓拍未读数
    var captureUnread by remember { mutableIntStateOf(0) }
    LaunchedEffect(deviceSn) {
        val sn = deviceSn
        if (sn == null) {
            captureUnread = 0
            return@LaunchedEffect
        }
        ServiceLocator.captureRepository.fetchUnreadCount(sn)
            .onSuccess { captureUnread = it }
        RtcSignalingManager.captureFeed.collect {
            ServiceLocator.captureRepository.fetchUnreadCount(sn)
                .onSuccess { captureUnread = it }
        }
    }

    Scaffold(
        containerColor = HomeBg,
        topBar = {
            TopAppBar(
                title = { Text("萤石养老看护", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Box {
                        IconButton(onClick = onOpenMessagesTab) {
                            Icon(Icons.Filled.Notifications, contentDescription = "消息通知")
                        }
                        if (totalUnread > 0) {
                            Badge(
                                containerColor = BadgeRed,
                                contentColor = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    if (totalUnread > 99) "99+" else "$totalUnread",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HomeBg)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部蓝色渐变 Banner
            BannerCard()

            // 我的设备
            Text("我的设备", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            MyDeviceCard(deviceSn = deviceSn, onClick = onNavigateToVideo)

            // 功能图标卡片（2行4列，扁平线条图标）
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 18.dp)) {
                    // 第一行：抓拍 / 视频通话 / 对讲 / 录像
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_capture, label = "抓拍",
                            badgeCount = captureUnread, onClick = onOpenCaptures
                        )
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_video_call, label = "视频通话",
                            onClick = onNavigateToVideoCall
                        )
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_intercom, label = "对讲",
                            onClick = onNavigateToVideo
                        )
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_record, label = "录像",
                            onClick = onNavigateToPlayback
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 第二行：留言 / 录音 / 点播 / 广播
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_message, label = "留言",
                            badgeCount = leaveUnread, onClick = onNavigateToMessage
                        )
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_voice, label = "录音",
                            onClick = onNavigateToMessage
                        )
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_play, label = "点播",
                            onClick = onNavigateToRk3Play
                        )
                        HomeFuncIcon(
                            iconRes = R.drawable.ic_func_broadcast, label = "广播",
                            onClick = onNavigateToBroadcast
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 顶部 Banner：蓝色渐变背景 + 左侧文案 + 右侧房子爱心插画 + 底部轮播指示点。
 */
@Composable
private fun BannerCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BannerGradient)
    ) {
        // 左侧文案
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            Text(
                text = "科技守护家人",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "陪伴就在身边",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
        }

        // 右侧房子爱心插画
        Image(
            painter = painterResource(R.drawable.home_banner_illustration),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .height(95.dp),
            contentScale = ContentScale.Fit
        )

        // 底部轮播指示点（3页，第1页高亮）
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BannerDot(isActive = true)
            BannerDot(isActive = false)
            BannerDot(isActive = false)
        }
    }
}

/** Banner 底部小圆点 */
@Composable
private fun BannerDot(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(if (isActive) 7.dp else 5.dp)
            .clip(CircleShape)
            .background(
                if (isActive) Color.White
                else Color.White.copy(alpha = 0.5f)
            )
    )
}

/**
 * 我的设备卡片：设备名 + 在线点 + 右箭头 + 缩略预览图。
 * 无云端 cover 时显示默认居家场景占位图 + 中央播放按钮。
 */
@Composable
private fun MyDeviceCard(deviceSn: String?, onClick: () -> Unit) {
    var device by remember(deviceSn) { mutableStateOf<Device?>(null) }
    LaunchedEffect(deviceSn) {
        device = deviceSn?.let { sn ->
            when (val result = ServiceLocator.repository.getDeviceInfo(sn)) {
                is NetworkResult.Success -> result.data
                else -> null
            }
        }
    }
    val displayName = device?.deviceName?.takeIf { it.isNotBlank() } ?: deviceSn ?: "未绑定 RK3 设备"
    val online = device?.status == DeviceStatus.ONLINE
    val hasCover = device?.deviceCover?.isNotBlank() == true

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (online) OnlineGreen else OfflineGray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    displayName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextHint)
            }
            // 缩略预览图：云端 cover 优先，无则默认居家场景图；中央半透明播放按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCover) {
                    AsyncImage(
                        model = device!!.deviceCover,
                        contentDescription = "设备画面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.device_scene_placeholder),
                        contentDescription = "设备画面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // 中央半透明播放按钮
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "播放",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 圆润卡通功能图标：彩色圆形背景 + 白色粗线条图标（本地 PNG 资源）。
 * 图标大小 52dp，下方文字标签，可选右上角未读角标。
 */
@Composable
private fun HomeFuncIcon(
    iconRes: Int,
    label: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(52.dp),
                contentScale = ContentScale.Fit
            )
            if (badgeCount > 0) {
                Badge(
                    containerColor = BadgeRed,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        if (badgeCount > 99) "99+" else "$badgeCount",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
