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
import com.elderlycare.app.ui.ezviz.AlarmListScreen
import com.elderlycare.app.ui.ezviz.LivePreviewScreen
import com.elderlycare.app.ui.ezviz.PlaybackScreen
import com.elderlycare.app.ui.ezviz.VideoCallScreen
import com.elderlycare.app.ui.family.AlertCenterScreen
import com.elderlycare.app.ui.family.AuthorizationManagementScreen
import com.elderlycare.app.ui.family.BindingRequestScreen
import com.elderlycare.app.ui.home.ProfileDetailScreen
import com.elderlycare.app.ui.login.CommunityLoginScreen
import com.elderlycare.app.ui.login.FamilyLoginScreen
import com.elderlycare.app.ui.login.HospitalLoginScreen
import com.elderlycare.app.ui.login.PortalSelectionScreen
import com.elderlycare.app.ui.message.MessageScreen
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

    // 启动会话判断：
    // 1. 已登录家属 → FamilyMain
    // 2. 已登录工作人员（按角色）→ CommunityMain / HospitalMain
    // 3. 均无 → PortalSelection；current_staff_id 对应账号不存在/损坏 → 清除登录态回门户
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = resolveStartDestination()
    }
    val start = startDestination ?: return

    NavHost(
        navController = navController,
        startDestination = start
    ) {
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
            val scope = rememberCoroutineScope()
            FamilyWizardScreen(
                onWizardComplete = { profile ->
                    scope.launch {
                        val uid = ServiceLocator.userStore.getCurrentUserId() ?: ""
                        ServiceLocator.profileStore.saveProfile(profile.copy(userId = uid))
                        navController.navigate(Screen.FamilyMain.route) {
                            popUpTo(Screen.PortalSelection.route) { inclusive = true }
                        }
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
            FamilyMainScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.PortalSelection.route) {
                        popUpTo(0) { inclusive = true }
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
                        // 只清 current_staff_id，不清 staff_users_json / 机构 / 绑定 / 告警
                        ServiceLocator.staffUserStore.clearCurrentStaff()
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
            FamilyProfileEditScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.ReportDetail.route) {
            ReportDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ===== 家属端详情页 =====
        composable(Screen.AlertCenter.route) {
            AlertCenterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AuthorizationMgmt.route) {
            AuthorizationManagementScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.BindingRequest.route) {
            BindingRequestScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ===== 家属端 — 萤石 RK3 直播 / 回放 / 告警 =====
        // deviceSerial 路由参数必须匹配当前用户可访问设备（第五阶段闸门），否则拦截；
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
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.Playback.route,
            arguments = listOf(navArgument("deviceSerial") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            DeviceAuthorizedGate(
                deviceSerial = deviceSerial,
                onBack = { navController.popBackStack() }
            ) { device ->
                PlaybackScreen(
                    deviceSerial = device.deviceSn,
                    verifyCode = device.deviceValidateCode,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.EzvizAlarms.route) {
            AlarmListScreen(
                onViewPlayback = { message ->
                    navController.navigate(Screen.Playback.createRoute(message.deviceSerial)) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 留言（音频收发模块）
        composable(Screen.Message.route) {
            MessageScreen(onBack = { navController.popBackStack() })
        }

        // 云通话（RK3 视频看护）
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

    // 设备来电弹窗（RK3 主动呼叫家属）
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
