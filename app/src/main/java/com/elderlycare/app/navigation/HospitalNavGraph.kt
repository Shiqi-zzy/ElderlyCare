package com.elderlycare.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elderlycare.app.data.model.UserRole
import com.elderlycare.app.ui.hospital.*
import com.elderlycare.app.ui.shared.QualificationGate
import com.elderlycare.app.ui.shared.StaffAlarmScreen
import com.elderlycare.app.ui.shared.StaffBindingApplyScreen

@Composable
fun HospitalMainScreen(navController: NavHostController, onLogout: () -> Unit) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                hospitalBottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == item.route)
                                    item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                innerNavController.navigate(item.route) {
                                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
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
            startDestination = "hospital_emergency",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("hospital_emergency") {
                QualificationGate {
                    HospitalEmergencyPanelScreen(
                        onLogout = onLogout,
                        onUserClick = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) }
                    )
                }
            }
            composable("hospital_records") {
                QualificationGate {
                    HospitalHealthRecordsScreen(
                        onNavigateToDetail = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) },
                        onNavigateToFollowUp = { elderlyId -> innerNavController.navigate(Screen.HospitalFollowUp.createRoute(elderlyId)) },
                        onNavigateToAdvice = { elderlyId -> innerNavController.navigate(Screen.HospitalAdvice.createRoute(elderlyId)) },
                        onNavigateToReport = { elderlyId -> innerNavController.navigate(Screen.HospitalReport.createRoute(elderlyId)) },
                        onNavigateToMedicalRemind = { innerNavController.navigate(Screen.HospitalMedicalRemind.route) }
                    )
                }
            }
            composable("hospital_alarm") {
                QualificationGate {
                    StaffAlarmScreen()
                }
            }
            composable("hospital_qual") {
                HospitalQualificationScreen(
                    onApplyBinding = { innerNavController.navigate("hospital_binding_apply") }
                )
            }
            composable("hospital_my") {
                HospitalMyScreen(onLogout = onLogout)
            }
            // 医院发起绑定申请（内层全屏页，底部栏保持可见，返回 = popBackStack）
            composable("hospital_binding_apply") {
                StaffBindingApplyScreen(
                    role = UserRole.HOSPITAL,
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
            // 医疗随访（elderlyId 空 = 全部随访记录只读视图，无录入入口）
            composable(
                route = Screen.HospitalFollowUp.route,
                arguments = listOf(
                    navArgument("elderlyId") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                MedicalFollowUpScreen(
                    elderlyId = entry.arguments?.getString("elderlyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() }
                )
            }
            // 健康建议录入（仅 App 消息模块查看，不走设备播报）
            composable(
                route = Screen.HospitalAdvice.route,
                arguments = listOf(navArgument("elderlyId") { type = NavType.StringType })
            ) { entry ->
                HealthAdviceScreen(
                    elderlyId = entry.arguments?.getString("elderlyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() }
                )
            }
            // 复诊提醒（老人页内选择；复用提醒计划萤石 v3 clock 能力）
            composable(Screen.HospitalMedicalRemind.route) {
                HospitalMedicalRemindScreen(onBack = { innerNavController.popBackStack() })
            }
            // 本地版健康报告（ACTIVE 授权校验 + 本地聚合）
            composable(
                route = Screen.HospitalReport.route,
                arguments = listOf(navArgument("elderlyId") { type = NavType.StringType })
            ) { entry ->
                HospitalReportScreen(
                    elderlyId = entry.arguments?.getString("elderlyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() }
                )
            }
        }
    }
}
