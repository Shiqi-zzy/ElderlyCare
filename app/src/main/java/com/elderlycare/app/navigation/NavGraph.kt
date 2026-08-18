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
import com.elderlycare.app.ui.ezviz.AlarmListScreen
import com.elderlycare.app.ui.ezviz.LivePreviewScreen
import com.elderlycare.app.ui.ezviz.PlaybackScreen
import com.elderlycare.app.ui.ezviz.VideoCallScreen
import com.elderlycare.app.ui.family.AlertCenterScreen
import com.elderlycare.app.ui.family.AuthorizationManagementScreen
import com.elderlycare.app.ui.home.ProfileDetailScreen
import com.elderlycare.app.ui.login.FamilyLoginScreen
import com.elderlycare.app.ui.login.PortalSelectionScreen
import com.elderlycare.app.ui.reports.ReportDetailScreen
import com.elderlycare.app.ui.shared.UserDetailScreen
import com.elderlycare.app.ui.wizard.FamilyWizardScreen
import com.elderlycare.app.ui.wizard.CommunityWizardScreen
import com.elderlycare.app.ui.wizard.HospitalWizardScreen
import com.elderlycare.app.ui.hospital.HospitalHealthRecordDetailScreen
import com.elderlycare.app.ui.hospital.HospitalFollowUpDetailScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val incomingCall by RtcSignalingManager.incomingCall.collectAsState()

    // 启动会话判断：已登录家属直接进首页，否则进门户选择
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = if (ServiceLocator.userStore.getCurrentUserId() != null)
            Screen.FamilyMain.route else Screen.PortalSelection.route
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
                    navController.navigate(Screen.CommunityWizard.route)
                },
                onHospitalLogin = {
                    navController.navigate(Screen.HospitalWizard.route)
                }
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
            CommunityMainScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.PortalSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ===== 医院端主界面 =====
        composable(Screen.HospitalMain.route) {
            HospitalMainScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.PortalSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ===== 共享详情页 =====
        composable(Screen.ProfileDetail.route) {
            ProfileDetailScreen(onNavigateBack = { navController.popBackStack() })
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

        // ===== 家属端 — 萤石 RK3 直播 / 回放 / 告警 =====
        composable(
            route = Screen.LivePreview.route,
            arguments = listOf(navArgument("deviceSerial") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            val bound = ServiceLocator.deviceBindingStore.load()
            LivePreviewScreen(
                deviceSerial = deviceSerial,
                verifyCode = bound?.validateCode ?: "",
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Playback.route,
            arguments = listOf(navArgument("deviceSerial") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            val bound = ServiceLocator.deviceBindingStore.load()
            PlaybackScreen(
                deviceSerial = deviceSerial,
                verifyCode = bound?.validateCode ?: "",
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.EzvizAlarms.route) {
            AlarmListScreen(
                onViewPlayback = { message ->
                    navController.navigate(Screen.Playback.createRoute(message.deviceSerial))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 云通话（RK3 视频看护）
        composable(
            route = Screen.VideoCall.route,
            arguments = listOf(
                navArgument("deviceSerial") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType; defaultValue = "" },
            )
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            val roomId = backStackEntry.arguments?.getString("roomId").orEmpty()
            // roomId 为空 → App 主动呼叫设备；不为空 → 设备呼叫 App，加入设备已创建的房间
            VideoCallScreen(
                deviceSerial = deviceSerial,
                account = "family001",
                roomId = roomId,
                isClientCall = roomId.isBlank(),
                onExit = { navController.popBackStack() }
            )
        }

        // ===== 共享用户详情 =====
        composable(
            route = Screen.UserDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserDetailScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 医院端详情页 =====
        composable(Screen.HealthRecordDetail.route) {
            HospitalHealthRecordDetailScreen(onNavigateBack = { navController.popBackStack() })
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
                    navController.navigate(Screen.VideoCall.createRoute(call.deviceSerial, call.roomId))
                }) { Text("接听") }
            },
            dismissButton = {
                TextButton(onClick = { RtcSignalingManager.consume() }) { Text("拒绝") }
            },
        )
    }
}
