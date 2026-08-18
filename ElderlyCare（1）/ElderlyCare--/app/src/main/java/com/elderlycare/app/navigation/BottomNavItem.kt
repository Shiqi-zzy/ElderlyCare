package com.elderlycare.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ===== 家属端底部导航 =====
val familyBottomNavItems = listOf(
    BottomNavItem("首页", "home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("日程", "calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem("报告", "reports", Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    BottomNavItem("消息", "messages", Icons.Filled.Notifications, Icons.Outlined.Notifications)
)

// ===== 社区端底部导航 =====
val communityBottomNavItems = listOf(
    BottomNavItem("辖区看板", "community_dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem("工单中心", "community_orders", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    BottomNavItem("用户台账", "community_roster", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem("资质管理", "community_qual", Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser),
    BottomNavItem("告警回放", "community_playback", Icons.Filled.Videocam, Icons.Outlined.Videocam)
)

// ===== 医院端底部导航 =====
val hospitalBottomNavItems = listOf(
    BottomNavItem("急救大屏", "hospital_emergency", Icons.Filled.LocalHospital, Icons.Outlined.LocalHospital),
    BottomNavItem("健康档案", "hospital_records", Icons.Filled.Folder, Icons.Outlined.Folder),
    BottomNavItem("应急视频", "hospital_video", Icons.Filled.Videocam, Icons.Outlined.Videocam),
    BottomNavItem("随访工单", "hospital_followup", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    BottomNavItem("资质管理", "hospital_qual", Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser)
)

// 向后兼容别名
val bottomNavItems = familyBottomNavItems
