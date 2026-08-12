package com.elderlycare.app.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elderlycare.app.ui.family.AlertCenterScreen
import com.elderlycare.app.ui.family.AuthorizationManagementScreen
import com.elderlycare.app.ui.home.ProfileDetailScreen
import com.elderlycare.app.ui.login.PortalSelectionScreen
import com.elderlycare.app.ui.reports.ReportDetailScreen
import com.elderlycare.app.ui.wizard.FamilyWizardScreen
import com.elderlycare.app.ui.wizard.CommunityWizardScreen
import com.elderlycare.app.ui.wizard.HospitalWizardScreen
import com.elderlycare.app.ui.hospital.HospitalHealthRecordDetailScreen
import com.elderlycare.app.ui.hospital.HospitalFollowUpDetailScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.PortalSelection.route
    ) {
        // ===== 门户选择 =====
        composable(Screen.PortalSelection.route) {
            PortalSelectionScreen(
                onFamilyLogin = {
                    navController.navigate(Screen.FamilyWizard.route)
                },
                onCommunityLogin = {
                    navController.navigate(Screen.CommunityWizard.route)
                },
                onHospitalLogin = {
                    navController.navigate(Screen.HospitalWizard.route)
                }
            )
        }

        // ===== 家属端 Wizard =====
        composable(Screen.FamilyWizard.route) {
            FamilyWizardScreen(
                onWizardComplete = {
                    navController.navigate(Screen.FamilyMain.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
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

        // ===== 医院端详情页 =====
        composable(Screen.HealthRecordDetail.route) {
            HospitalHealthRecordDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
