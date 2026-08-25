package com.elderlycare.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elderlycare.app.data.model.UserRole
import com.elderlycare.app.ui.community.*
import com.elderlycare.app.ui.shared.QualificationGate
import com.elderlycare.app.ui.shared.StaffAlarmScreen
import com.elderlycare.app.ui.shared.StaffBindingApplyScreen
import com.elderlycare.app.ui.shared.StaffBindingManageScreen
import com.elderlycare.app.ui.theme.*

@Composable
fun CommunityMainScreen(navController: NavHostController, onLogout: () -> Unit) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            // 欢迎页隐藏底部导航栏
            val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "community_welcome") {
                NavigationBar {
                    communityBottomNavItems.forEach { item ->
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
        }
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = "community_welcome",
            modifier = Modifier.padding(paddingValues)
        ) {
            // 社区端欢迎页（点击进入工作台）
            composable("community_welcome") {
                CommunityWelcomeScreen(
                    onEnter = { innerNavController.navigate("community_dashboard") }
                )
            }
            composable("community_dashboard") {
                QualificationGate {
                    CommunityDashboardScreen(
                        onLogout = onLogout,
                        onUserClick = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) }
                    )
                }
            }
            composable("community_roster") {
                QualificationGate {
                    CommunityRosterScreen(
                        onNavigateToDetail = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) }
                    )
                }
            }
            composable("community_alarm") {
                QualificationGate {
                    StaffAlarmScreen()
                }
            }
            composable("community_qual") {
                CommunityQualificationScreen()
            }
            composable("community_my") {
                CommunityMyScreen(
                    onLogout = onLogout,
                    onNavigateToBinding = { innerNavController.navigate("community_binding_manage") },
                    onNavigateToFollowUp = { innerNavController.navigate("community_followup") },
                    onNavigateToSchedule = { innerNavController.navigate("community_schedule") },
                    onNavigateToServiceRecord = { innerNavController.navigate("community_service_record") }
                )
            }
            // 随访计划
            composable("community_followup") {
                FollowUpPlanScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 我的排班
            composable("community_schedule") {
                StaffScheduleScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 服务记录
            composable("community_service_record") {
                ServiceRecordScreen(onNavigateBack = { innerNavController.popBackStack() })
            }
            // 社区发起绑定申请（内层全屏页，底部栏保持可见，返回 = popBackStack）
            composable("community_binding_apply") {
                StaffBindingApplyScreen(
                    role = UserRole.COMMUNITY,
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
            // 绑定管理页（三Tab：绑定申请/我的申请/已绑定用户），从「我的」→「绑定用户」进入
            composable("community_binding_manage") {
                StaffBindingManageScreen(
                    role = UserRole.COMMUNITY,
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
        }
    }
}
