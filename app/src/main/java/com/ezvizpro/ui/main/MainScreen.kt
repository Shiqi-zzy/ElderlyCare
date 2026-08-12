package com.ezvizpro.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ezvizpro.ui.alarm.MessagesScreen
import com.ezvizpro.ui.devicelist.DeviceListScreen
import com.ezvizpro.ui.home.FamilyHomeContent

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("home", "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("devices", "设备", Icons.Filled.Videocam, Icons.Outlined.Videocam),
    BottomNavItem("messages", "消息", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem("profile", "我的", Icons.Filled.Person, Icons.Outlined.Person)
)

/**
 * 家属端主页 — 底部导航四Tab（萤石原有功能完整保留）
 *
 * Tab 0: 首页 (FamilyHomeContent — 快捷功能/服务卡片/家庭时光/播放控制)
 * Tab 1: 设备 (DeviceListScreen — 设备网格/实时预览/回放入口)
 * Tab 2: 消息 (MessagesScreen — 萤石告警消息列表)
 * Tab 3: 我的 (养老功能入口 + 系统设置)
 */
@Composable
fun MainScreen(
    onDeviceClick: (String, Int) -> Unit,
    onPlaybackClick: (String, Int) -> Unit,
    onVideoCallClick: () -> Unit = {},
    onFamilyMessageClick: () -> Unit = {},
    onLifeReminderClick: () -> Unit = {},
    onWechatAuthClick: () -> Unit = {},
    onElderlyAlarmsClick: () -> Unit = {},
    onElderlyAuthorizationsClick: () -> Unit = {},
    onElderlyPrivacyClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index)
                                    item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> FamilyHomeContent(
                    onVideoCallClick = onVideoCallClick,
                    onFamilyMessageClick = onFamilyMessageClick,
                    onLifeReminderClick = onLifeReminderClick,
                    onWechatAuthClick = onWechatAuthClick,
                    onPlaybackClick = onPlaybackClick
                )
                1 -> DeviceListScreen(
                    onDeviceClick = onDeviceClick,
                    onPlaybackClick = onPlaybackClick
                )
                2 -> MessagesScreen(
                    onAlarmClick = { message ->
                        onPlaybackClick(message.deviceSerial, message.channelNo)
                    }
                )
                3 -> ProfileTab(
                    onElderlyAlarmsClick = onElderlyAlarmsClick,
                    onElderlyAuthorizationsClick = onElderlyAuthorizationsClick,
                    onElderlyPrivacyClick = onElderlyPrivacyClick,
                    onLogout = onLogout
                )
            }
        }
    }
}

/**
 * 「我的」Tab — 包含养老功能入口 + 原有系统设置
 */
@Composable
private fun ProfileTab(
    onElderlyAlarmsClick: () -> Unit,
    onElderlyAuthorizationsClick: () -> Unit,
    onElderlyPrivacyClick: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("我的", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // ── 用户信息卡片 ──
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("萤石用户", style = MaterialTheme.typography.titleMedium)
                    Text("智慧养老 · 家属端", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 智慧养老功能区 ──
        Text(
            "智慧养老",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Filled.NotificationsActive,
                    title = "告警中心",
                    subtitle = "查看老人异常告警，确认处置",
                    onClick = onElderlyAlarmsClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuItem(
                    icon = Icons.Filled.Security,
                    title = "授权管理",
                    subtitle = "管理社区/医院数据访问权限",
                    onClick = onElderlyAuthorizationsClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuItem(
                    icon = Icons.Filled.Visibility,
                    title = "隐私控制",
                    subtitle = "暂停/恢复监控，管理设备",
                    onClick = onElderlyPrivacyClick
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 系统设置 ──
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsItem("版本信息", "v1.0.0 Phase 2")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("反馈建议", "欢迎提出改进意见")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 退出登录 ──
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("退出登录")
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
