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
import com.elderlycare.app.ui.community.FollowUpPlanScreen
import com.elderlycare.app.ui.community.ServiceRecordScreen
import com.elderlycare.app.ui.community.StaffScheduleScreen
import com.elderlycare.app.ui.hospital.*
import com.elderlycare.app.ui.shared.QualificationGate
import com.elderlycare.app.ui.shared.StaffAlarmScreen
import com.elderlycare.app.ui.shared.StaffBindingApplyScreen
import com.elderlycare.app.ui.shared.StaffBindingManageScreen

@Composable
fun HospitalMainScreen(navController: NavHostController, onLogout: () -> Unit) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            // 欢迎页隐藏底部导航栏
            val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "hospital_welcome") {
                NavigationBar {
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
                                        popUpTo("hospital_emergency") { saveState = true }
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
            startDestination = "hospital_welcome",
            modifier = Modifier.padding(paddingValues)
        ) {
            // 医院端欢迎页
            composable("hospital_welcome") {
                HospitalWelcomeScreen(
                    onEnter = {
                        innerNavController.navigate("hospital_emergency") {
                            popUpTo("hospital_welcome") { inclusive = true }
                        }
                    }
                )
            }
            // 急救大屏 Tab：绑定社区网格闪红 + 一键处警/处置完成
            composable("hospital_emergency") {
                QualificationGate {
                    HospitalEmergencyBoardScreen(
                        onNavigateToBinding = { innerNavController.navigate("hospital_hc_binding") },
                        onNavigateToShift = { innerNavController.navigate("hospital_doctor_shift") },
                        onNavigateToPerformance = { innerNavController.navigate("hospital_performance") },
                        onUserClick = { elderlyId ->
                            if (elderlyId.isNotBlank()) {
                                navController.navigate(Screen.UserDetail.createRoute(elderlyId))
                            }
                        }
                    )
                }
            }
            composable("hospital_records") {
                QualificationGate {
                    HospitalHealthRecordsScreen(
                        onNavigateToDetail = { elderlyId ->
                            if (elderlyId.isNotBlank()) {
                                navController.navigate(Screen.UserDetail.createRoute(elderlyId))
                            }
                        },
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
                HospitalQualificationScreen()
            }
            composable("hospital_my") {
                HospitalMyScreen(
                    onLogout = onLogout,
                    onNavigateToBinding = { innerNavController.navigate("hospital_hc_binding") },
                    onNavigateToFollowUp = { innerNavController.navigate("hospital_followup_plan") },
                    onNavigateToSchedule = { innerNavController.navigate("hospital_schedule") },
                    onNavigateToServiceRecord = { innerNavController.navigate("hospital_service_record") }
                )
            }
            // 医院-社区绑定（申请 + 管理端审批虚拟实现）
            composable("hospital_hc_binding") {
                HospitalCommunityBindingScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 医生值班排班
            composable("hospital_doctor_shift") {
                DoctorShiftScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 医生值班绩效/处罚
            composable("hospital_performance") {
                DoctorPerformanceScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            composable("hospital_followup_plan") {
                FollowUpPlanScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            composable("hospital_schedule") {
                StaffScheduleScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            composable("hospital_service_record") {
                ServiceRecordScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            composable("hospital_all_events") {
                HospitalAllEventsScreen(
                    onNavigateBack = { innerNavController.popBackStack() },
                    onUserClick = { elderlyId ->
                        if (elderlyId.isNotBlank()) {
                            navController.navigate(Screen.UserDetail.createRoute(elderlyId))
                        }
                    }
                )
            }
            composable("hospital_binding_apply") {
                StaffBindingApplyScreen(
                    role = UserRole.HOSPITAL,
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
            composable("hospital_binding_manage") {
                StaffBindingManageScreen(
                    role = UserRole.HOSPITAL,
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
            composable(
                route = Screen.HospitalFollowUp.route,
                arguments = listOf(navArgument("elderlyId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                MedicalFollowUpScreen(
                    elderlyId = entry.arguments?.getString("elderlyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() }
                )
            }
            composable(
                route = Screen.HospitalAdvice.route,
                arguments = listOf(navArgument("elderlyId") { type = NavType.StringType })
            ) { entry ->
                HealthAdviceScreen(
                    elderlyId = entry.arguments?.getString("elderlyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() }
                )
            }
            composable(Screen.HospitalMedicalRemind.route) {
                HospitalMedicalRemindScreen(onBack = { innerNavController.popBackStack() })
            }
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
