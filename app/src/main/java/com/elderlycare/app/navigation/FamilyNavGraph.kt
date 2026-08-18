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
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.calendar.CalendarScreen
import com.elderlycare.app.ui.ezviz.AlarmListScreen
import com.elderlycare.app.ui.family.AuthorizationManagementScreen
import com.elderlycare.app.ui.family.FamilyHomeScreen
import com.elderlycare.app.ui.family.MyScreen
import com.elderlycare.app.ui.reports.ReportsScreen

@Composable
fun FamilyMainScreen(navController: NavHostController, onLogout: () -> Unit) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                familyBottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == item.route)
                                    item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                innerNavController.navigate(item.route) {
                                    popUpTo(innerNavController.graph.startDestinationId) {
                                        saveState = true
                                    }
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
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                FamilyHomeScreen(
                    onNavigateToProfile = {
                        navController.navigate(Screen.ProfileDetail.route)
                    },
                    onNavigateToReport = {
                        innerNavController.navigate("reports") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCalendar = {
                        innerNavController.navigate("calendar") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToVideo = {
                        val serial = ServiceLocator.deviceBindingStore.load()?.deviceSerial
                        if (serial != null) {
                            navController.navigate(Screen.LivePreview.createRoute(serial))
                        }
                    },
                    onNavigateToEmergencyCall = {
                        val serial = ServiceLocator.deviceBindingStore.load()?.deviceSerial
                        if (serial != null) {
                            navController.navigate(Screen.VideoCall.createRoute(serial))
                        }
                    },
                    onNavigateToAlertCenter = {
                        navController.navigate(Screen.EzvizAlarms.route)
                    },
                    onNavigateToAuthorizationMgmt = {
                        navController.navigate(Screen.AuthorizationMgmt.route)
                    }
                )
            }
            composable("calendar") {
                CalendarScreen()
            }
            composable("reports") {
                ReportsScreen(
                    onNavigateToDetail = {
                        navController.navigate(Screen.ReportDetail.route)
                    }
                )
            }
            composable("messages") {
                AlarmListScreen(
                    onViewPlayback = { message ->
                        navController.navigate(Screen.Playback.createRoute(message.deviceSerial))
                    }
                )
            }
            composable("my") {
                MyScreen(
                    onNavigateToProfile = {
                        navController.navigate(Screen.ProfileDetail.route)
                    },
                    onNavigateToAuthorizationMgmt = {
                        navController.navigate(Screen.AuthorizationMgmt.route)
                    },
                    onLogout = onLogout
                )
            }
        }
    }
}
