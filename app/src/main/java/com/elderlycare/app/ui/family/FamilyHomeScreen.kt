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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

// ==================== 图四首页设计色值（页面局部常量，不改全局主题） ====================
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
private val ThumbPlaceholderBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(Color(0xFF3D5A73), Color(0xFF2C3E50))
)

/**
 * 家属首页（Phase 3 UI 重设计，图四原型 A）。
 *
 * 功能网格固定 2 行 4 列：
 * 第一行：抓拍（跳全部抓拍页，不直接触发抓拍）/ 视频通话（ERTC）/ 对讲（进入预览页）/ 录像（SD 录像回放列表）
 * 第二行：留言 / 录音（快速进留言）/ 点播（RK3 点播）/ 广播（云广播 FM）。
 * 「告警消息」快捷入口已彻底移除：抓拍图片/自动抓拍快照统一在【全部抓拍】页查看，
 * 告警文字通知进消息 Tab（底部铃铛角标）；首页「抓拍」图标保留后端抓拍未读角标（与消息 Tab 互不干扰）。
 * 图标素材统一走 res/drawable（英文命名），禁止引用 C 盘绝对路径。
 * 数据全部来自真实业务流，禁止 Mock。
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
    // 已绑定设备（响应式）：读 BindingRepository 授权链路（档案 deviceSn），不读 DeviceBindingStore 缓存
    var boundDevice by remember { mutableStateOf<BindingRepository.AccessibleDevice?>(null) }
    LaunchedEffect(Unit) {
        ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { boundDevice = it }
    }
    val deviceSn = boundDevice?.deviceSn

    // 未读数（未绑定设备时恒为 0，隐藏角标）：铃铛=全部合计、留言=分类1；
    // 抓拍图标角标=后端 alarm_events 未读数（与消息 Tab Room 角标互不干扰）
    val totalUnread by remember(deviceSn) {
        deviceSn?.let { ServiceLocator.messageRepository.observeUnreadCount(it) } ?: flowOf(0)
    }.collectAsStateWithLifecycle(initialValue = 0)
    val leaveUnread by remember(deviceSn) {
        deviceSn?.let {
            ServiceLocator.messageRepository.observeUnreadCountByCategory(it, MessageEntity.MESSAGE_CATEGORY_LEAVE_MSG)
        } ?: flowOf(0)
    }.collectAsStateWithLifecycle(initialValue = 0)
    // 后端抓拍未读数：进页拉取 + WS 新告警/图片就绪（captureFeed）实时重拉
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
            BannerCard()

            // 我的设备
            Text("我的设备", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            MyDeviceCard(deviceSn = deviceSn, onClick = onNavigateToVideo)

            // 第一行功能（固定 2×4 网格）：抓拍 / 视频通话 / 对讲 / 录像（统一 Material 矢量图标）
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeFuncIcon(
                    icon = Icons.Filled.CameraAlt, label = "抓拍",
                    bgColor = FuncBlue, badgeCount = captureUnread, onClick = onOpenCaptures
                )
                HomeFuncIcon(
                    icon = Icons.Filled.VideoCall, label = "视频通话",
                    bgColor = FuncGreen, onClick = onNavigateToVideoCall
                )
                HomeFuncIcon(
                    icon = Icons.Filled.VolumeUp, label = "对讲",
                    bgColor = FuncOrange, onClick = onNavigateToVideo
                )
                HomeFuncIcon(
                    icon = Icons.Filled.Videocam, label = "录像",
                    bgColor = FuncPurple, onClick = onNavigateToPlayback
                )
            }

            // 第二行功能：留言 / 录音 / 点播 / 广播
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeFuncIcon(
                    icon = Icons.Filled.Message, label = "留言",
                    bgColor = FuncLightBlue, badgeCount = leaveUnread, onClick = onNavigateToMessage
                )
                HomeFuncIcon(
                    icon = Icons.Filled.Mic, label = "录音",
                    bgColor = FuncLightGreen, onClick = onNavigateToMessage
                )
                HomeFuncIcon(
                    icon = Icons.Filled.PlayArrow, label = "点播",
                    bgColor = FuncLightOrange, onClick = onNavigateToRk3Play
                )
                HomeFuncIcon(
                    icon = Icons.Filled.Campaign, label = "广播",
                    bgColor = FuncLightRed, onClick = onNavigateToBroadcast
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** 顶部品牌 Banner：bg_home_top 全宽背景 + 右侧老人居家插画（illust_oldman_home） */
@Composable
private fun BannerCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_home_top),
            contentDescription = "首页顶部品牌",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Image(
            painter = painterResource(R.drawable.illust_oldman_home),
            contentDescription = "老人居家插画",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .height(134.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 我的设备卡片：设备名（getDeviceInfo 云端真实数据，失败兜底 SN）+ 绿色在线点 +
 * 缩略图（Coil 加载 deviceCover，无则渐变占位）；
 * 整卡点击跳播放器。中间不再渲染圆形视频占位图标。
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
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
            // 缩略预览图：渐变占位为底，云端 cover 加载成功后覆盖其上；无中央占位图标
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(ThumbPlaceholderBrush),
                contentAlignment = Alignment.Center
            ) {
                device?.deviceCover?.takeIf { it.isNotBlank() }?.let { cover ->
                    AsyncImage(
                        model = cover,
                        contentDescription = "设备画面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** 方形圆角色块功能图标 + 文字标签 + 可选右上角未读角标（统一 Material 矢量图标，白色 24dp 居中，不撑满按钮） */
@Composable
private fun HomeFuncIcon(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                modifier = Modifier.size(56.dp)
            ) {
                // 图标居中，四周保留内边距，白色统一风格
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
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
