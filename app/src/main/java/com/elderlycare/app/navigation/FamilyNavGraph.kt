package com.elderlycare.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.ezviz.AlarmListScreen
import com.elderlycare.app.ui.family.FamilyHomeScreen
import com.elderlycare.app.ui.family.MyScreen
import com.elderlycare.app.ui.reports.ReportsScreen
import com.elderlycare.app.ui.reminder.RemindPlanCalendarScreen
import kotlinx.coroutines.launch

@Composable
fun FamilyMainScreen(navController: NavHostController, onLogout: () -> Unit) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                familyBottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == item.route)
                                    item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                innerNavController.navigate(item.route) {
                                    popUpTo(innerNavController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                val scope = rememberCoroutineScope()
                FamilyHomeScreen(
                    onNavigateToProfile = {
                        navController.navigate(Screen.ProfileDetail.route)
                    },
                    onNavigateToReport = {
                        innerNavController.navigate("reports") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCalendar = {
                        innerNavController.navigate("calendar") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToVideo = {
                        // 设备串号取授权链路当前设备（与直播页闸门一致）
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.LivePreview.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToEmergencyCall = {
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.VideoCall.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToAlertCenter = {
                        navController.navigate(Screen.EzvizAlarms.route) { launchSingleTop = true }
                    },
                    onNavigateToMessage = {
                        navController.navigate(Screen.Message.route)
                    },
                    onNavigateToAuthorizationMgmt = {
                        navController.navigate(Screen.AuthorizationMgmt.route)
                    },
                    onLogout = onLogout
                )
            }
            composable("calendar") {
                // 日程 Tab（聚合总览）：日期 Tab 过滤展示提醒计划（只读）；
                // 右上角【提醒计划】跳提醒计划页（新增入口）；点击条目跳详情（外层导航）；
                // 进 Tab 时 VM 自动同步设备闹铃（clock/list）+ 轮询执行记录
                RemindPlanCalendarScreen(
                    onNavigateToRemindPlan = {
                        navController.navigate(Screen.RemindPlanList.route)
                    },
                    onPlanClick = { plan ->
                        navController.navigate(Screen.RemindPlanDetail.createRoute(plan.id))
                    }
                )
            }
            composable("reports") {
                ReportsScreen(
                    onNavigateToDetail = {
                        navController.navigate(Screen.ReportDetail.route)
                    }
                )
            }
            composable("messages") {
                AlarmListScreen(
                    onViewPlayback = { message ->
                        navController.navigate(
                            Screen.Playback.createRoute(message.deviceSerial, message.alarmTime)
                        ) { launchSingleTop = true }
                    }
                )
            }
            composable("my") {
                MyScreen(
                    onNavigateToProfile = {
                        navController.navigate(Screen.ProfileDetail.route)
                    },
                    onNavigateToBindingRequest = {
                        navController.navigate(Screen.BindingRequest.route)
                    },
                    onNavigateToAuthorizationMgmt = {
                        navController.navigate(Screen.AuthorizationMgmt.route)
                    },
                    onLogout = onLogout
                )
            }
        }
    }
}
