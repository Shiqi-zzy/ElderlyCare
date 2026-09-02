package com.elderlycare.app.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elderlycare.app.data.ezviz.RtcSignalingManager
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.UserRole
import com.elderlycare.app.data.reminder.PreviewVoices
import com.elderlycare.app.data.reminder.RemindTemplate
import com.elderlycare.app.ui.ezviz.AlarmListScreen
import com.elderlycare.app.ui.ezviz.AllCapturesScreen
import com.elderlycare.app.ui.ezviz.LivePreviewScreen
import com.elderlycare.app.ui.ezviz.PlaybackScreen
import com.elderlycare.app.ui.ezviz.Rk3FmScreen
import com.elderlycare.app.ui.ezviz.Rk3PlayScreen
import com.elderlycare.app.ui.ezviz.VideoCallScreen
import com.elderlycare.app.ui.family.AlertCenterScreen
import com.elderlycare.app.ui.family.AuthorizationManagementScreen
import com.elderlycare.app.ui.family.BindingRequestScreen
import com.elderlycare.app.ui.home.ProfileDetailScreen
import com.elderlycare.app.ui.login.CommunityLoginScreen
import com.elderlycare.app.ui.login.FamilyLoginScreen
import com.elderlycare.app.ui.login.HospitalLoginScreen
import com.elderlycare.app.ui.login.PortalSelectionScreen
import com.elderlycare.app.ui.login.WelcomeScreen
import com.elderlycare.app.ui.message.ConversationScreen
import com.elderlycare.app.ui.message.DeviceVideoPlayerScreen
import com.elderlycare.app.ui.message.MessageScreen
import com.elderlycare.app.ui.reminder.RemindPlanDetailScreen
import com.elderlycare.app.ui.reminder.RemindPlanFormScreen
import com.elderlycare.app.ui.reminder.RemindPlanListScreen
import com.elderlycare.app.ui.reminder.VoiceSelectScreen
import com.elderlycare.app.ui.reports.ReportDetailScreen
import com.elderlycare.app.ui.shared.DeviceAuthorizedGate
import com.elderlycare.app.ui.shared.UserDetailScreen
import com.elderlycare.app.ui.wizard.FamilyProfileEditScreen
import com.elderlycare.app.ui.wizard.FamilyWizardScreen
import com.elderlycare.app.ui.wizard.CommunityWizardScreen
import com.elderlycare.app.ui.wizard.HospitalWizardScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val incomingCall by RtcSignalingManager.incomingCall.collectAsState()

    // 启动会话判断（欢迎页点击后进入的真实首屏）：
    // 1. 已登录家属 → FamilyMain
    // 2. 已登录工作人员（按角色）→ CommunityMain / HospitalMain
    // 3. 均无 → PortalSelection；current_staff_id 对应账号不存在/损坏 → 清除登录态回门户
    var realStart by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        realStart = resolveStartDestination()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        // ===== 启动欢迎页：点击「开启安心守护」再进入真实首屏（按登录态） =====
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onEnter = {
                    val target = realStart ?: Screen.PortalSelection.route
                    navController.navigate(target) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // ===== 门户选择 =====
        composable(Screen.PortalSelection.route) {
            PortalSelectionScreen(
                onFamilyLogin = {
                    navController.navigate(Screen.FamilyLogin.route)
                },
                onCommunityLogin = {
                    navController.navigate(Screen.CommunityLogin.route)
                },
                onHospitalLogin = {
                    navController.navigate(Screen.HospitalLogin.route)
                }
            )
        }

        // ===== 社区/医院端登录注册（统一认证） =====
        composable(Screen.CommunityLogin.route) {
            CommunityLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.CommunityMain.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HospitalLogin.route) {
            HospitalLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.HospitalMain.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 家属端登录/注册 =====
        composable(Screen.FamilyLogin.route) {
            FamilyLoginScreen(
                onNavigateToWizard = {
                    navController.navigate(Screen.FamilyWizard.route)
                },
                onNavigateToMain = {
                    navController.navigate(Screen.FamilyMain.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 家属端 Wizard =====
        composable(Screen.FamilyWizard.route) {
            FamilyWizardScreen(
                onWizardComplete = {
                    // 档案保存与记忆上报在 Wizard 内部完成（保存成功才回调）
                    navController.navigate(Screen.FamilyMain.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onExit = { navController.popBackStack() }
            )
        }

        // ===== 社区端 Wizard =====
        composable(Screen.CommunityWizard.route) {
            CommunityWizardScreen(
                onWizardComplete = {
                    navController.navigate(Screen.CommunityMain.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onExit = { navController.popBackStack() }
            )
        }

        // ===== 医院端 Wizard =====
        composable(Screen.HospitalWizard.route) {
            HospitalWizardScreen(
                onWizardComplete = {
                    navController.navigate(Screen.HospitalMain.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onExit = { navController.popBackStack() }
            )
        }

        // ===== 家属端主界面 =====
        composable(Screen.FamilyMain.route) {
            val scope = rememberCoroutineScope()
            FamilyMainScreen(
                navController = navController,
                onLogout = {
                    scope.launch {
                        // 家属退出：清登录态 + 萤石 Token + 设备绑定（档案等其他本地数据保留）
                        ServiceLocator.userStore.clearCurrentUser()
                        ServiceLocator.tokenManager.clearToken()
                        ServiceLocator.deviceBindingStore.clear()
                        navController.navigate(Screen.PortalSelection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ===== 社区端主界面 =====
        composable(Screen.CommunityMain.route) {
            val scope = rememberCoroutineScope()
            CommunityMainScreen(
                navController = navController,
                onLogout = {
                    scope.launch {
                        // 只清登录态与萤石凭据，不清 staff_users_json / 机构 / 绑定 / 告警
                        ServiceLocator.staffUserStore.clearCurrentStaff()
                        ServiceLocator.tokenManager.clearToken()
                        ServiceLocator.deviceBindingStore.clear()
                        navController.navigate(Screen.PortalSelection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ===== 医院端主界面 =====
        composable(Screen.HospitalMain.route) {
            val scope = rememberCoroutineScope()
            HospitalMainScreen(
                navController = navController,
                onLogout = {
                    scope.launch {
                        ServiceLocator.staffUserStore.clearCurrentStaff()
                        ServiceLocator.tokenManager.clearToken()
                        ServiceLocator.deviceBindingStore.clear()
                        navController.navigate(Screen.PortalSelection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ===== 共享详情页 =====
        composable(Screen.ProfileDetail.route) {
            ProfileDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.ProfileEdit.route) { launchSingleTop = true }
                }
            )
        }

        // 家属端档案编辑（只能编辑当前登录家属的档案，路由无 elderlyId 参数）
        composable(Screen.ProfileEdit.route) {
            FamilyProfileEditScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDevice = { deviceSn ->
                    // 设备卡点击：跳直播预览页（与我的页「查看设备」行为一致）
                    navController.navigate(Screen.LivePreview.createRoute(deviceSn)) { launchSingleTop = true }
                }
            )
        }

        composable(Screen.ReportDetail.route) {
            ReportDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ===== 家属端详情页 =====
        composable(Screen.AlertCenter.route) {
            val scope = rememberCoroutineScope()
            AlertCenterScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewPlayback = {
                    // 告警定位回放：设备串号取授权链路当前设备（与回放页闸门一致）
                    scope.launch {
                        val device = ServiceLocator.bindingRepository.getCurrentUserDevice()
                        device?.deviceSn?.let {
                            navController.navigate(Screen.Playback.createRoute(it)) { launchSingleTop = true }
                        }
                    }
                }
            )
        }

        composable(Screen.AuthorizationMgmt.route) {
            AuthorizationManagementScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.BindingRequest.route) {
            BindingRequestScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ===== 家属端 — 萤石 RK3 直播 / 回放 / 告警 =====
        // deviceSerial 路由参数必须匹配当前用户可访问设备（闸门），否则拦截；
        // verifyCode 改取自授权档案的 deviceValidateCode（不再读 DeviceBindingStore 旧缓存）。
        composable(
            route = Screen.LivePreview.route,
            arguments = listOf(navArgument("deviceSerial") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            DeviceAuthorizedGate(
                deviceSerial = deviceSerial,
                onBack = { navController.popBackStack() }
            ) { device ->
                LivePreviewScreen(
                    deviceSerial = device.deviceSn,
                    verifyCode = device.deviceValidateCode,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToPlay = { sn -> navController.navigate(Screen.Rk3Play.createRoute(sn)) },
                    onNavigateToFm = { sn -> navController.navigate(Screen.Rk3Fm.createRoute(sn)) },
                    onNavigateToVideoCall = {
                        // 预览页「视频通话」：发起 ERTC 呼叫（闸门已校验设备，直接带 SN）
                        navController.navigate(Screen.VideoCall.createRoute(device.deviceSn)) { launchSingleTop = true }
                    },
                    onNavigateToMessage = { navController.navigate(Screen.Message.route) }
                )
            }
        }

        // ===== 家属端 — RK3 点播（设备音频播放，网络层占位 + Mock 演示） =====
        composable(
            route = Screen.Rk3Play.route,
            arguments = listOf(navArgument("deviceSerial") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            DeviceAuthorizedGate(
                deviceSerial = deviceSerial,
                onBack = { navController.popBackStack() }
            ) { device ->
                Rk3PlayScreen(
                    deviceSerial = device.deviceSn,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        // ===== 家属端 — RK3 广播FM（网络电台，网络层占位 + Mock 演示） =====
        composable(
            route = Screen.Rk3Fm.route,
            arguments = listOf(navArgument("deviceSerial") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            DeviceAuthorizedGate(
                deviceSerial = deviceSerial,
                onBack = { navController.popBackStack() }
            ) { device ->
                Rk3FmScreen(
                    deviceSerial = device.deviceSn,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.Playback.route,
            arguments = listOf(
                navArgument("deviceSerial") { type = NavType.StringType },
                navArgument("startTime") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            val startTime = backStackEntry.arguments?.getString("startTime").orEmpty()
            DeviceAuthorizedGate(
                deviceSerial = deviceSerial,
                onBack = { navController.popBackStack() }
            ) { device ->
                PlaybackScreen(
                    deviceSerial = device.deviceSn,
                    verifyCode = device.deviceValidateCode,
                    startAtTime = startTime,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.EzvizAlarms.route) {
            AlarmListScreen(
                onViewPlayback = { message ->
                    // 告警回放：携带告警发生时间，回放页自动定位到该时刻附近（±30 秒窗口）
                    navController.navigate(
                        Screen.Playback.createRoute(message.deviceSerial, message.alarmTime)
                    ) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 家属端 — 全部抓拍（手动抓拍 + 告警自动抓拍，取代旧告警中心为唯一入口） =====
        // 「抓拍计划」点击弹授权提示弹窗（不跳转提醒计划）；提醒计划页仍可由日程 Tab / 留言页进入
        composable(Screen.Captures.route) {
            AllCapturesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 家属端 — 留言（音频收发模块） =====
        composable(Screen.Message.route) {
            MessageScreen(
                onBack = { navController.popBackStack() },
                onNavigateToRemindPlan = { navController.navigate(Screen.RemindPlanList.route) },
                onNavigateToDeviceVideo = { messageId ->
                    navController.navigate(Screen.DeviceVideo.createRoute(messageId))
                }
            )
        }

        // ===== 家属端 — 设备视频留言播放 =====
        // messageId 为本机 message 表主键（非设备串号），无需 DeviceAuthorizedGate
        composable(
            route = Screen.DeviceVideo.route,
            arguments = listOf(navArgument("messageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getLong("messageId") ?: return@composable
            DeviceVideoPlayerScreen(
                messageId = messageId,
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 家属端 — 聊天对话页（消息中心会话列表点击进入；会话键即展示标题） =====
        composable(
            route = Screen.Conversation.route,
            arguments = listOf(navArgument("senderName") { type = NavType.StringType })
        ) { backStackEntry ->
            val senderName = backStackEntry.arguments?.getString("senderName") ?: return@composable
            ConversationScreen(
                conversationKey = senderName,
                onBack = { navController.popBackStack() },
                onOpenVideo = { messageId ->
                    navController.navigate(Screen.DeviceVideo.createRoute(messageId))
                }
            )
        }

        // ===== 家属端 — 提醒计划（RK3 设备本地闹铃） =====
        composable(Screen.RemindPlanList.route) {
            RemindPlanListScreen(
                showBackButton = true,
                onBack = { navController.popBackStack() },
                onNavigateToForm = { templateKey ->
                    navController.navigate(Screen.RemindPlanForm.createRoute(templateKey))
                },
                onPlanClick = { plan ->
                    navController.navigate(Screen.RemindPlanDetail.createRoute(plan.id))
                }
            )
        }

        composable(
            route = Screen.RemindPlanDetail.route,
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            RemindPlanDetailScreen(
                planId = planId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RemindPlanForm.route,
            arguments = listOf(
                navArgument("templateKey") {
                    type = NavType.StringType
                    defaultValue = RemindTemplate.CUSTOM.key
                }
            )
        ) { backStackEntry ->
            val templateKey =
                backStackEntry.arguments?.getString("templateKey") ?: RemindTemplate.CUSTOM.key
            RemindPlanFormScreen(
                templateKey = templateKey,
                // 音色跨页回传：VoiceSelect 写入本 entry 的 savedStateHandle
                voiceKeyFlow = backStackEntry.savedStateHandle
                    .getStateFlow("voice_key", PreviewVoices.DEFAULT_KEY),
                onBack = { navController.popBackStack() },
                onNavigateToVoiceSelect = { navController.navigate(Screen.VoiceSelect.route) }
            )
        }

        composable(Screen.VoiceSelect.route) {
            VoiceSelectScreen(
                currentVoiceKey = navController.previousBackStackEntry
                    ?.savedStateHandle?.get<String>("voice_key")
                    ?: PreviewVoices.DEFAULT_KEY,
                onVoiceSelected = { key ->
                    // 写回表单 entry 的 savedStateHandle 后返回
                    navController.previousBackStackEntry?.savedStateHandle?.set("voice_key", key)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== 家属端 — 云通话（RK3 视频通话） =====
        // 路由先过设备闸门（来电弹窗导航也经此，deviceSerial 不匹配 → 拦截）；
        // account=family001 / ERTC 入会逻辑保持原样，仅串号来源统一为授权设备。
        composable(
            route = Screen.VideoCall.route,
            arguments = listOf(
                navArgument("deviceSerial") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType; defaultValue = "" },
            )
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            val roomId = backStackEntry.arguments?.getString("roomId").orEmpty()
            DeviceAuthorizedGate(
                deviceSerial = deviceSerial,
                onBack = { navController.popBackStack() }
            ) { device ->
                // roomId 为空 → App 主动呼叫设备；不为空 → 设备呼叫 App，加入设备已创建的房间
                VideoCallScreen(
                    deviceSerial = device.deviceSn,
                    account = "family001",
                    roomId = roomId,
                    isClientCall = roomId.isBlank(),
                    onExit = { navController.popBackStack() }
                )
            }
        }

        // ===== 共享用户详情（按 elderlyId 权限读取） =====
        composable(
            route = Screen.UserDetail.route,
            arguments = listOf(navArgument("elderlyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val elderlyId = backStackEntry.arguments?.getString("elderlyId") ?: return@composable
            UserDetailScreen(
                elderlyId = elderlyId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // 设备来电弹窗（RK3 主动呼叫家属，覆盖在任意页面之上）
    incomingCall?.let { call ->
        AlertDialog(
            onDismissRequest = { RtcSignalingManager.consume() },
            title = { Text("来电") },
            text = { Text("RK3 设备正在呼叫") },
            confirmButton = {
                TextButton(onClick = {
                    RtcSignalingManager.consume()
                    navController.navigate(Screen.VideoCall.createRoute(call.deviceSerial, call.roomId)) { launchSingleTop = true }
                }) { Text("接听") }
            },
            dismissButton = {
                TextButton(onClick = { RtcSignalingManager.consume() }) { Text("拒绝") }
            },
        )
    }
}

/**
 * 启动会话恢复：
 * 家属登录态优先 → FamilyMain；否则按工作人员 current_staff_id 恢复角色 →
 * CommunityMain / HospitalMain；staff 账号不存在或数据损坏时清除登录态回门户。
 */
private suspend fun resolveStartDestination(): String {
    // 家属登录态（family_data，现有存储方式不变）
    if (ServiceLocator.userStore.getCurrentUserId() != null) return Screen.FamilyMain.route

    // 工作人员登录态（staff_data，仅存手机号，需恢复完整账号与角色）
    val staffId = ServiceLocator.staffUserStore.getCurrentStaffId()
        ?: return Screen.PortalSelection.route
    val staff = ServiceLocator.staffUserStore.getStaffByPhone(staffId)
    if (staff == null) {
        // 登录态对应账号不存在 / 数据损坏：只清登录态，不删业务数据
        ServiceLocator.staffUserStore.clearCurrentStaff()
        return Screen.PortalSelection.route
    }
    return when (staff.role) {
        UserRole.COMMUNITY -> Screen.CommunityMain.route
        UserRole.HOSPITAL -> Screen.HospitalMain.route
        else -> Screen.PortalSelection.route
    }
}
