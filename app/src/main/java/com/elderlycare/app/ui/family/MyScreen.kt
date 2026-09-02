package com.elderlycare.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.Device
import com.elderlycare.app.data.ezviz.model.DeviceStatus
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.FamilyUser
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.ui.wizard.steps.PrivacyConsentDialog
import kotlinx.coroutines.launch

// ==================== 我的页色值（页面局部常量，不改全局主题） ====================
private val PageBg = Color(0xFFF7F9FC)
private val CardWhite = Color.White
private val OnlineGreen = Color(0xFF4CAF50)
private val OfflineGray = Color(0xFFBDBDBD)
private val AvatarBg = Color(0xFF4086E8)
private val BannerBlueStart = Color(0xFFEAF2FF)
private val BannerBlueEnd = Color(0xFFF5F9FF)
private val BannerText = Color(0xFF1A2332)
private val BannerTextSecondary = Color(0xFF4A5568)

/** 手机号脱敏：181****6373（不足 7 位原样展示，空则空串） */
private fun maskPhone(phone: String): String =
    if (phone.length >= 7) phone.take(3) + "****" + phone.takeLast(4) else phone

/** 身高体重计算 BMI（任一无效返回 null） */
private fun calcBmi(profile: ElderlyProfile): String? {
    val h = profile.height.toDoubleOrNull() ?: return null
    val w = profile.weight.toDoubleOrNull() ?: return null
    if (h <= 0 || w <= 0) return null
    return String.format("%.1f", w / ((h / 100.0) * (h / 100.0)))
}

/**
 * 「我的」页面（卡片式重构）：个人信息头 / 健康档案 / 我的设备 / 授权管理 / 客服与设置。
 *
 * 业务数据只展示一次：档案信息只在健康档案卡、设备信息只在我的设备卡、
 * 授权信息只在授权管理卡；头部卡片仅账号信息（姓名/脱敏手机号/在线状态）。
 * 字段为空一律浅灰占位提示，不渲染异常文本。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToDevice: () -> Unit,
    onNavigateToAuthorizationMgmt: () -> Unit,
    onOpenMessagesTab: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 账号（FamilyUser）
    var user by remember { mutableStateOf<FamilyUser?>(null) }
    LaunchedEffect(Unit) { user = ServiceLocator.userStore.getCurrentUser() }
    val currentUser = user

    // 家人档案（健康档案卡数据源）
    var profile by remember { mutableStateOf<ElderlyProfile?>(null) }
    LaunchedEffect(Unit) {
        val uid = ServiceLocator.userStore.getCurrentUserId() ?: ""
        profile = ServiceLocator.profileStore.getPrimaryProfile(uid)
    }
    val currentProfile = profile

    // 已绑定设备（响应式，授权链路）+ 云端设备详情（在线状态）
    var boundDevice by remember { mutableStateOf<BindingRepository.AccessibleDevice?>(null) }
    LaunchedEffect(Unit) {
        ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { boundDevice = it }
    }
    var deviceInfo by remember(boundDevice?.deviceSn) { mutableStateOf<Device?>(null) }
    LaunchedEffect(boundDevice?.deviceSn) {
        val sn = boundDevice?.deviceSn
        deviceInfo = sn?.let {
            when (val result = ServiceLocator.repository.getDeviceInfo(sn)) {
                is NetworkResult.Success -> result.data
                else -> null
            }
        }
    }

    // 授权绑定关系（授权管理卡空态判定；家属侧查询本人家人档案的绑定）
    var bindingCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(profile?.userId) {
        val uid = profile?.userId
        bindingCount = uid?.let { ServiceLocator.bindingRepository.getBindingsForFamily(it).size } ?: 0
    }

    // 绑定申请未读数（授权管理卡红色角标：申请数量，进入授权管理页后批量已读消失）
    var unreadBindingCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        ServiceLocator.messageRepository.observeUnreadBindingCount().collect { unreadBindingCount = it }
    }

    // 弹窗状态
    var showHelpDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showConsentView by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRk3ServerDialog by remember { mutableStateOf(false) }
    var rk3ServerInput by remember { mutableStateOf(ServiceLocator.settingsStore.getRk3ServerAddress()) }

    Scaffold(
        containerColor = PageBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ① 个人信息头卡片（蓝色渐变背景，白色文字）
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BannerBlueStart, BannerBlueEnd)
                        ),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "我的",
                        color = BannerText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = AvatarBg.copy(alpha = 0.12f), modifier = Modifier.size(60.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    (currentUser?.name ?: "家").take(1),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AvatarBg
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentUser?.name ?: "未登录",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleLarge,
                                color = BannerText
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                maskPhone(currentUser?.phone.orEmpty()).ifBlank { "未绑定手机号" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = BannerTextSecondary
                            )
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(OnlineGreen)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("在线", style = MaterialTheme.typography.labelSmall, color = OnlineGreen)
                            }
                        }
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(Icons.Filled.Settings, "设置", tint = BannerTextSecondary)
                        }
                        IconButton(onClick = onOpenMessagesTab) {
                            Icon(Icons.Filled.Notifications, "消息", tint = BannerTextSecondary)
                        }
                    }
                }
            }

            // ② 健康档案卡片
            ModuleCard(title = "健康档案", actionText = "编辑档案", onAction = onNavigateToProfileEdit) {
                FieldRow("家人姓名", currentProfile?.name?.takeIf { it.isNotBlank() } ?: "未填写")
                FieldRow("性别", currentProfile?.gender?.label?.takeIf { it.isNotBlank() } ?: "未填写")
                FieldRow("BMI", calcBmi(currentProfile ?: ElderlyProfile()) ?: "未填写")
                FieldRow(
                    "血压",
                    currentProfile?.let {
                        if (it.bloodPressureHigh.isNotBlank() && it.bloodPressureLow.isNotBlank())
                            "${it.bloodPressureHigh}/${it.bloodPressureLow} mmHg" else "未填写"
                    } ?: "未填写"
                )
                FieldRow("血糖", "暂无数据")
            }

            // ③ 我的设备卡片（仅当前绑定主设备，不循环多条）
            ModuleCard(
                title = "我的设备",
                actionText = if (boundDevice != null) "查看设备" else null,
                onAction = onNavigateToDevice
            ) {
                if (boundDevice == null) {
                    PlaceholderText("暂无绑定设备")
                } else {
                    val online = deviceInfo?.status == DeviceStatus.ONLINE
                    FieldRow("设备名称", "RK3(${boundDevice!!.deviceSn})")
                    FieldRow(
                        "设备状态",
                        if (online) "在线" else "离线",
                        valueColor = if (online) OnlineGreen else OfflineGray
                    )
                }
            }

            // ④ 授权管理卡片（机构/家属/设备三类授权；无授权数据显示浅灰占位；
            // 标题红角标 = 未处理的绑定申请数量，微信未读式，进入授权管理后消失）
            ModuleCard(
                title = "授权管理",
                actionText = "管理授权",
                onAction = onNavigateToAuthorizationMgmt,
                badgeCount = unreadBindingCount
            ) {
                val hasAuth = bindingCount > 0 || boundDevice?.deviceBound == true
                if (!hasAuth) {
                    PlaceholderText("暂无授权信息")
                } else {
                    AuthRow(Icons.Filled.Apartment, "机构授权")
                    AuthRow(Icons.Filled.Group, "家属授权")
                    AuthRow(Icons.Filled.Devices, "设备授权")
                }
            }

            // ⑤ 客服与设置卡片（列表布局，退出登录红色置底）
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("客服与设置", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    ServiceRow(Icons.Filled.HelpOutline, "帮助中心") { showHelpDialog = true }
                    ServiceRow(Icons.Filled.HeadsetMic, "联系客服") { showContactDialog = true }
                    ServiceRow(Icons.Filled.Lock, "隐私设置") { showPrivacyDialog = true }
                    ServiceRow(Icons.Filled.Dns, stringResource(R.string.rk3_server_address_row)) {
                        rk3ServerInput = ServiceLocator.settingsStore.getRk3ServerAddress()
                        showRk3ServerDialog = true
                    }
                    ServiceRow(Icons.Filled.Logout, "退出登录", danger = true) { showLogoutDialog = true }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("帮助中心") },
            text = {
                Text(
                    "• 首页功能网格：抓拍 / 视频通话 / 对讲 / 录像 / 留言 / 录音 / 点播 / 广播；\n" +
                        "• 全部抓拍：右下角相机按钮手动抓拍，设备告警自动抓拍；\n" +
                        "• 消息中心：按发送方聚合会话，点击进入对话查看全部消息。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("知道了") } }
        )
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("联系客服") },
            text = {
                Text(
                    "如需帮助，请联系萤石官方客服：\n官方 APP 内「我的-在线客服」或官网客服中心。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = { TextButton(onClick = { showContactDialog = false }) { Text("知道了") } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("隐私设置") },
            text = {
                Column {
                    Text(
                        "本应用仅采集家庭看护所需数据（家人档案、设备告警与留言），" +
                            "数据保存在本机与您绑定的设备/后端服务，不对外共享。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showConsentView = true }) {
                        Text("查看隐私同意协议", color = Primary)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("知道了") } }
        )
    }

    // 只读版隐私同意协议（仅查看文案与当前授权状态，不修改数据）
    if (showConsentView) {
        PrivacyConsentDialog(
            onAgree = {},
            onReject = {},
            readOnly = true,
            consentGiven = currentProfile?.privacyConsentGiven == true,
            onDismiss = { showConsentView = false }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于我们") },
            text = { Text("萤石养老看护 · ElderlyCare\n面向家属的远程看护助手") },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("知道了") } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定退出登录吗？\n（用户与家人档案数据会保留）") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    scope.launch {
                        ServiceLocator.userStore.clearCurrentUser()
                        // 清跨账号兼容缓存：登出后 deviceBindingStore 残留会使下一位登录家属
                        // 首页/设备弹窗显示上一账号设备、留言发送到上一账号设备（权限闸门不受影响）。
                        ServiceLocator.deviceBindingStore.clear()
                        onLogout()
                    }
                }) { Text("退出登录", color = Error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } }
        )
    }

    // RK3 服务器地址（局域网报告/建议接口 baseUrl；空串=未设置，报告页走「请前往设置」提示）
    if (showRk3ServerDialog) {
        AlertDialog(
            onDismissRequest = { showRk3ServerDialog = false },
            title = { Text(stringResource(R.string.rk3_server_address_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = rk3ServerInput,
                        onValueChange = { rk3ServerInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.rk3_server_address_hint), color = TextHint) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.rk3_lan_usage_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ServiceLocator.settingsStore.setRk3ServerAddress(rk3ServerInput)
                    showRk3ServerDialog = false
                }) { Text(stringResource(R.string.rk3_server_address_save)) }
            },
            dismissButton = { TextButton(onClick = { showRk3ServerDialog = false }) { Text("取消") } }
        )
    }
}

/** 通用业务模块卡片：白底圆角 + 柔和阴影 + 模块标题居左 + 可选右上操作按钮 + 可选标题红角标 */
@Composable
private fun ModuleCard(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    badgeCount: Int = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    if (badgeCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = Error) {
                            Text(
                                if (badgeCount > 99) "99+" else "$badgeCount",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnError
                            )
                        }
                    }
                }
                if (actionText != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(actionText, color = Primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

/** 档案/设备字段行：浅灰标签 + 值（空值由调用方传占位文案，值颜色可指定） */
@Composable
private fun FieldRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.startsWith("未") || value.startsWith("暂无")) TextHint else valueColor
        )
    }
}

/** 授权管理列表行：图标 + 名称 */
@Composable
private fun AuthRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

/** 客服与设置列表行：图标 + 名称 + 右侧箭头（退出登录红色） */
@Composable
private fun ServiceRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (danger) Error else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (danger) Error else TextPrimary
        )
        if (!danger) {
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint)
        }
    }
}

/** 浅灰占位提示文字（无数据模块统一使用） */
@Composable
private fun PlaceholderText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextHint)
}
