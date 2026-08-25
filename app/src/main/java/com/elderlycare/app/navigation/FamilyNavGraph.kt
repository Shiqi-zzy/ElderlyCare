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
import com.elderlycare.app.ui.family.FamilyHomeScreen
import com.elderlycare.app.ui.family.MyScreen
import com.elderlycare.app.ui.login.WelcomeScreen
import com.elderlycare.app.ui.message.MessageCenterScreen
import com.elderlycare.app.ui.reports.ReportsScreen
import com.elderlycare.app.ui.reminder.RemindPlanCalendarScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun FamilyMainScreen(navController: NavHostController, onLogout: () -> Unit) {
    val innerNavController = rememberNavController()

    // 底部「消息」Tab 全局未读角标：授权链路当前设备 → 消息 Room 未读合计（无设备恒 0 隐藏）
    // （observeCurrentUserDevice 为 suspend：先 collect 到 state，再切未读数流，与 MyScreen 同款模式）
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
            // 家属端欢迎页（点击进入首页）
            composable("family_welcome") {
                WelcomeScreen(
                    onEnter = { innerNavController.navigate("home") }
                )
            }
            composable("home") {
                val scope = rememberCoroutineScope()
                FamilyHomeScreen(
                    onOpenCaptures = {
                        // 首页「抓拍」：只跳全部抓拍页，不直接触发抓拍；
                        // 手动抓拍由全部抓拍页 FAB / 预览页截图按钮发起（同一后端接口，4s 限流）
                        navController.navigate(Screen.Captures.route) { launchSingleTop = true }
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
                    onNavigateToVideoCall = {
                        // 首页「视频通话」：发起 ERTC 呼叫（与紧急通话同一接线模式）
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
                        // 首页「点播」：与预览页底部「点播」同一入口（RK3 点播页）
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.Rk3Play.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToPlayback = {
                        // 首页「录像」：SD 录像回放列表（startTime 空 = 不自动定位）
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.Playback.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onNavigateToBroadcast = {
                        // 首页「广播」：云广播 FM 页（TTS 下发 RK3，与留言 sendOnce 广播独立）
                        scope.launch {
                            val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                            device?.deviceSn?.let {
                                navController.navigate(Screen.Rk3Fm.createRoute(it)) { launchSingleTop = true }
                            }
                        }
                    },
                    onOpenMessagesTab = {
                        // 铃铛 / 告警消息：切底部「消息」Tab（消息中心）
                        innerNavController.navigate("messages") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
                // 报告 Tab：实时/周度/年度/建议四子 Tab，数据源 = RK3 局域网 HTTP 服务
                ReportsScreen()
            }
            composable("messages") {
                // 消息 Tab = 消息中心（会话列表模式：按发送方内存聚合）；
                // 会话点击进聊天对话页、对话页视频跳视频播放页、去留言跳留言页
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
                MyScreen(
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
        }
    }
}
