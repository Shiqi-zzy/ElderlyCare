package com.elderlycare.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.family.FamilyCareScreen
import com.elderlycare.app.ui.family.FamilyHomeScreen
import com.elderlycare.app.ui.family.FamilyMyV2Screen
import com.elderlycare.app.ui.family.FamilyWelcomeScreen
import com.elderlycare.app.ui.family.IncidentTimelineScreen
import com.elderlycare.app.ui.message.MessageCenterScreen
import com.elderlycare.app.ui.reports.ReportsScreen
import com.elderlycare.app.ui.reminder.RemindPlanCalendarScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun FamilyMainScreen(navController: NavHostController, onLogout: () -> Unit) {
    val innerNavController = rememberNavController()

    // 底部「消息」Tab 全局未读角标：授权链路当前设备 → 消息 Room 未读合计（无设备恒 0 隐藏）
    var globalBoundDevice by remember { mutableStateOf<BindingRepository.AccessibleDevice?>(null) }
    LaunchedEffect(Unit) {
        ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { globalBoundDevice = it }
    }
    val globalUnread by remember(globalBoundDevice?.deviceSn) {
        globalBoundDevice?.deviceSn?.let { ServiceLocator.messageRepository.observeUnreadCount(it) } ?: flowOf(0)
    }.collectAsStateWithLifecycle(initialValue = 0)

    Scaffold(
        bottomBar = {
            // 欢迎页隐藏底部导航栏
            val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "family_welcome") {
                NavigationBar {
                    familyBottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                val showBadge = item.route == "messages" && globalUnread > 0
                                if (showBadge) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = Color(0xFFF24848), contentColor = Color.White) {
                                                Text(if (globalUnread > 99) "99+" else "$globalUnread")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentRoute == item.route)
                                                item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (currentRoute == item.route)
                                            item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
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
        }
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = "family_welcome",
            modifier = Modifier.padding(paddingValues)
        ) {
            // 居家用户「进入加载页」：真实加载设备后自动进入首页
            composable("family_welcome") {
                FamilyWelcomeScreen(
                    onEnter = { innerNavController.navigate("home") }
                )
            }
            composable("home") {
                val scope = rememberCoroutineScope()
                FamilyHomeScreen(
                    onOpenCaptures = {
                        navController.navigate(Screen.Captures.route) { launchSingleTop = true }
                    },
                    onNavigateToVideo = {
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.LivePreview.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToVideoCall = {
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.VideoCall.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToMessage = {
                        navController.navigate(Screen.Message.route)
                    },
                    onNavigateToRk3Play = {
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.Rk3Play.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToPlayback = {
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.Playback.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToBroadcast = {
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.Rk3Fm.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onOpenMessagesTab = {
                        innerNavController.navigate("messages") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("calendar") {
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
                ReportsScreen()
            }
            composable("messages") {
                MessageCenterScreen(
                    onOpenConversation = { conversationKey ->
                        navController.navigate(Screen.Conversation.createRoute(conversationKey)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenVideo = { messageId ->
                        navController.navigate(Screen.DeviceVideo.createRoute(messageId))
                    },
                    onOpenLeave = { navController.navigate(Screen.Message.route) }
                )
            }
            composable("my") {
                val scope = rememberCoroutineScope()
                FamilyMyV2Screen(
                    onNavigateToProfileEdit = {
                        // 「编辑档案」：跳档案编辑页（只能编辑当前登录家属的档案）
                        navController.navigate(Screen.ProfileEdit.route)
                    },
                    onNavigateToDevice = {
                        // 「查看设备」：跳直播预览页（设备串号取授权链路当前设备）
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.LivePreview.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToAuthorizationMgmt = {
                        navController.navigate(Screen.AuthorizationMgmt.route)
                    },
                    onNavigateToCareCommunity = {
                        innerNavController.navigate("family_care_community") { launchSingleTop = true }
                    },
                    onNavigateToCareHospital = {
                        innerNavController.navigate("family_care_hospital") { launchSingleTop = true }
                    },
                    onNavigateToIncidents = {
                        innerNavController.navigate("family_incidents") { launchSingleTop = true }
                    },
                    onOpenMessagesTab = {
                        // 头部消息图标：切底部「消息」Tab
                        innerNavController.navigate("messages") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLogout = onLogout
                )
            }
            // 我的社区 / 我的医院（基础信息 + 对应服务记录）
            composable("family_care_community") { FamilyCareScreen(side = "community") }
            composable("family_care_hospital") { FamilyCareScreen(side = "hospital") }
            // 家属端事件处置时间线
            composable("family_incidents") { IncidentTimelineScreen() }
        }
    }
}
