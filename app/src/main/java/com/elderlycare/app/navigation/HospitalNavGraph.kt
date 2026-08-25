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
            // 医院端欢迎页（点击进入急救大屏，清空欢迎页栈）
            composable("hospital_welcome") {
                HospitalWelcomeScreen(
                    onEnter = {
                        innerNavController.navigate("hospital_emergency") {
                            popUpTo("hospital_welcome") { inclusive = true }
                        }
                    }
                )
            }
            composable("hospital_emergency") {
                QualificationGate {
                    HospitalEmergencyPanelScreen(
                        onLogout = onLogout,
                        onUserClick = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) },
                        onNavigateToAlarm = { innerNavController.navigate("hospital_alarm") },
                        onNavigateToAllEvents = { innerNavController.navigate("hospital_all_events") }
                    )
                }
            }
            composable("hospital_records") {
                QualificationGate {
                    HospitalHealthRecordsScreen(
                        onNavigateToDetail = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) },
                        onNavigateToFollowUp = { innerNavController.navigate("hospital_followup_plan") },
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
                    onNavigateToBinding = { innerNavController.navigate("hospital_binding_manage") },
                    onNavigateToFollowUp = { innerNavController.navigate("hospital_followup_plan") },
                    onNavigateToSchedule = { innerNavController.navigate("hospital_schedule") },
                    onNavigateToServiceRecord = { innerNavController.navigate("hospital_service_record") }
                )
            }
            // 随访计划
            composable("hospital_followup_plan") {
                FollowUpPlanScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 我的排班
            composable("hospital_schedule") {
                StaffScheduleScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 服务记录
            composable("hospital_service_record") {
                ServiceRecordScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 全部急救事件
            composable("hospital_all_events") {
                HospitalAllEventsScreen(
                    onNavigateBack = { innerNavController.popBackStack() },
                    onUserClick = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) }
                )
            }
            // 医院发起绑定申请（内层全屏页，底部栏保持可见，返回 = popBackStack）
            composable("hospital_binding_apply") {
                StaffBindingApplyScreen(
                    role = UserRole.HOSPITAL,
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
            // 绑定管理页（三Tab：绑定申请/我的申请/已绑定用户），从「我的」→「绑定用户」进入
            composable("hospital_binding_manage") {
                StaffBindingManageScreen(
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
