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
    BottomNavItem("消息", "messages", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem("我的", "my", Icons.Filled.Person, Icons.Outlined.Person)
)

// ===== 社区端底部导航（台账与健康档案同源重复，健康档案 tab 调整为告警消息） =====
val communityBottomNavItems = listOf(
    BottomNavItem("工作台", "community_dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem("用户台账", "community_roster", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem("告警消息", "community_alarm", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem("工作资格", "community_qual", Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser),
    BottomNavItem("我的", "community_my", Icons.Filled.Person, Icons.Outlined.Person)
)

// ===== 医院端底部导航（新增告警消息 tab；资质管理 → 工作资格） =====
val hospitalBottomNavItems = listOf(
    BottomNavItem("急救大屏", "hospital_emergency", Icons.Filled.LocalHospital, Icons.Outlined.LocalHospital),
    BottomNavItem("健康档案", "hospital_records", Icons.Filled.Folder, Icons.Outlined.Folder),
    BottomNavItem("告警消息", "hospital_alarm", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem("工作资格", "hospital_qual", Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser),
    BottomNavItem("我的", "hospital_my", Icons.Filled.Person, Icons.Outlined.Person)
)

// 向后兼容别名
val bottomNavItems = familyBottomNavItems
