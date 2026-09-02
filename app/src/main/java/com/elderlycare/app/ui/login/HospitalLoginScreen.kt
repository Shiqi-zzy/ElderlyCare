package com.elderlycare.app.ui.login

import androidx.compose.runtime.Composable
import com.elderlycare.app.data.model.UserRole
import com.elderlycare.app.ui.theme.Error

/**
 * 医院端登录/注册页（登录 + 注册切换，仅限 HOSPITAL 角色账号）。
 */
@Composable
fun HospitalLoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    StaffAuthScreen(
        role = UserRole.HOSPITAL,
        endName = "医院",
        accent = Error,
        onLoginSuccess = onLoginSuccess,
        onBack = onBack
    )
}
