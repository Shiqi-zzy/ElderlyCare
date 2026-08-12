package com.ezvizpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ezvizpro.ui.community.CommunityMainScreen
import com.ezvizpro.ui.family.ElderlyAlarmsScreen
import com.ezvizpro.ui.family.ElderlyAuthorizationsScreen
import com.ezvizpro.ui.family.ElderlyPrivacyScreen
import com.ezvizpro.ui.hospital.HospitalMainScreen
import com.ezvizpro.ui.live.LivePreviewScreen
import com.ezvizpro.ui.login.LoginScreen
import com.ezvizpro.ui.login.LoginViewModel
import com.ezvizpro.ui.login.PortalSelectionScreen
import com.ezvizpro.ui.main.MainScreen
import com.ezvizpro.ui.message.FamilyMessageScreen
import com.ezvizpro.ui.playback.PlaybackScreen
import com.ezvizpro.ui.reminder.LifeReminderScreen
import com.ezvizpro.ui.splash.SplashScreen
import com.ezvizpro.ui.verify.VerificationScreen
import com.ezvizpro.ui.wechat.WechatAuthScreen

/**
 * 智慧养老平台路由导航（基于萤石设备绑定）
 *
 * 流程：
 *   Splash → Login(萤石AppKey获取accessToken + 后端sync) → PortalSelect(三选一) → 对应Portal
 *
 * 家属端 → MainScreen（保留原有底部导航四Tab + 萤石全部功能）
 * 社区端 → VerificationGate → CommunityMainScreen
 * 医院端 → VerificationGate → HospitalMainScreen
 *
 * 登录走萤石设备绑定体系，无需手机验证码。
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {

        // ── 启动页 ──
        composable("splash") {
            SplashScreen(
                onReady = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // ── 萤石设备绑定登录（自动获取 accessToken） ──
        composable(Screen.Login.route) {
            LoginScreen(
                onEzvizReady = { clientId, ezvizToken, currentRole, backendToken ->
                    navController.navigate("portal_select") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 门户选择（三端入口） ──
        composable("portal_select") {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val loginState by loginViewModel.state.collectAsStateWithLifecycle()

            PortalSelectionScreen(
                onPortalSelected = { role, token ->
                    navigateToPortal(navController, role, token)
                }
            )
        }

        // ── 角色选择页（新用户注册后选角色） ──
        composable(
            route = Screen.RoleSelect.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loginViewModel: LoginViewModel = hiltViewModel()
            PortalSelectionScreen(
                onPortalSelected = { role, token ->
                    navigateToPortal(navController, role, token)
                }
            )
        }

        // ═══════════════════════════════════════════════════════
        // 家属端：原有 MainScreen（底部导航四Tab + 萤石全部功能）
        // ═══════════════════════════════════════════════════════
        composable(Screen.Main.route) {
            MainScreen(
                onDeviceClick = { serial, channel ->
                    navController.navigate(Screen.LivePlay.createRoute(serial, channel))
                },
                onPlaybackClick = { serial, channel ->
                    navController.navigate(Screen.Playback.createRoute(serial, channel))
                },
                onVideoCallClick = {
                    navController.navigate(Screen.VideoCall.route)
                },
                onFamilyMessageClick = {
                    navController.navigate(Screen.FamilyMessage.route)
                },
                onLifeReminderClick = {
                    navController.navigate(Screen.LifeReminder.route)
                },
                onWechatAuthClick = {
                    navController.navigate(Screen.WechatAuth.route)
                },
                onElderlyAlarmsClick = {
                    navController.navigate(Screen.ElderlyAlarms.route)
                },
                onElderlyAuthorizationsClick = {
                    navController.navigate(Screen.ElderlyAuthorizations.route)
                },
                onElderlyPrivacyClick = {
                    navController.navigate(Screen.ElderlyPrivacy.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── 实时预览页（萤石原有功能） ──
        composable(
            route = Screen.LivePlay.route,
            arguments = listOf(
                navArgument("deviceSerial") { type = NavType.StringType },
                navArgument("channelNo") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            val channelNo = backStackEntry.arguments?.getInt("channelNo") ?: 1
            LivePreviewScreen(
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── 录像回放页（萤石原有功能） ──
        composable(
            route = Screen.Playback.route,
            arguments = listOf(
                navArgument("deviceSerial") { type = NavType.StringType },
                navArgument("channelNo") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val deviceSerial = backStackEntry.arguments?.getString("deviceSerial") ?: return@composable
            val channelNo = backStackEntry.arguments?.getInt("channelNo") ?: 1
            PlaybackScreen(
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── 视频通话页（P2 占位） ──
        composable(Screen.VideoCall.route) {
            com.ezvizpro.ui.home.components.PlaceholderScreen("视频通话", "阶段2实现") {
                navController.popBackStack()
            }
        }

        // ── 家人留言页 ──
        composable(Screen.FamilyMessage.route) {
            FamilyMessageScreen(onBackClick = { navController.popBackStack() })
        }

        // ── 生活提醒页 ──
        composable(Screen.LifeReminder.route) {
            LifeReminderScreen(onBackClick = { navController.popBackStack() })
        }

        // ── 微信授权页 ──
        composable(Screen.WechatAuth.route) {
            WechatAuthScreen(onBackClick = { navController.popBackStack() })
        }

        // ═══════════════════════════════════════════════════════
        // 养老子页面（家属端内部导航）
        // ═══════════════════════════════════════════════════════
        composable(Screen.ElderlyAlarms.route) {
            ElderlyAlarmsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.ElderlyAuthorizations.route) {
            ElderlyAuthorizationsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.ElderlyPrivacy.route) {
            ElderlyPrivacyScreen(onBack = { navController.popBackStack() })
        }

        // ═══════════════════════════════════════════════════════
        // 社区端（验证门控 → 社区门户）
        // ═══════════════════════════════════════════════════════
        composable(
            route = Screen.CommunityPortal.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            VerificationScreen(
                role = "community",
                token = token,
                onVerified = {
                    navController.navigate("community_main/$token") {
                        popUpTo(Screen.CommunityPortal.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CommunityVerify.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            VerificationScreen(
                role = "community",
                token = token,
                onVerified = {
                    navController.navigate("community_main/$token") {
                        popUpTo(Screen.CommunityVerify.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "community_main/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            CommunityMainScreen(
                token = token,
                onBack = {
                    navController.navigate("portal_select") {
                        popUpTo("community_main/{token}") { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════
        // 医院端（验证门控 → 医院门户）
        // ═══════════════════════════════════════════════════════
        composable(
            route = Screen.HospitalPortal.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            VerificationScreen(
                role = "hospital",
                token = token,
                onVerified = {
                    navController.navigate("hospital_main/$token") {
                        popUpTo(Screen.HospitalPortal.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.HospitalVerify.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            VerificationScreen(
                role = "hospital",
                token = token,
                onVerified = {
                    navController.navigate("hospital_main/$token") {
                        popUpTo(Screen.HospitalVerify.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "hospital_main/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            HospitalMainScreen(
                token = token,
                onBack = {
                    navController.navigate("portal_select") {
                        popUpTo("hospital_main/{token}") { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

private fun navigateToPortal(navController: NavHostController, role: String, token: String) {
    val route = when (role.lowercase()) {
        "family" -> Screen.Main.route
        "community" -> Screen.CommunityPortal.createRoute(token)
        "hospital" -> Screen.HospitalPortal.createRoute(token)
        else -> Screen.Main.route
    }
    navController.navigate(route) {
        popUpTo("portal_select") { inclusive = true }
    }
}
