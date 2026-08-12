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
import com.elderlycare.app.ui.hospital.*

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
            composable("hospital_emergency") { HospitalEmergencyPanelScreen(onLogout = onLogout) }
            composable("hospital_records") {
                HospitalHealthRecordsScreen(
                    onNavigateToDetail = { navController.navigate(Screen.HealthRecordDetail.route) }
                )
            }
            composable("hospital_video") { HospitalEmergencyVideoScreen() }
            composable("hospital_followup") { HospitalFollowUpOrdersScreen() }
            composable("hospital_qual") { HospitalQualificationScreen() }
        }
    }
}
