package com.elderlycare.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                        onNavigateToDetail = { elderlyId -> navController.navigate(Screen.UserDetail.createRoute(elderlyId)) }
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
        }
    }
}
