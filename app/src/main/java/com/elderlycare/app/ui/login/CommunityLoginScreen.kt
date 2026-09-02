package com.elderlycare.app.ui.login

import androidx.compose.runtime.Composable
import com.elderlycare.app.data.model.UserRole
import com.elderlycare.app.ui.theme.Secondary

/**
 * 社区端登录/注册页（登录 + 注册切换，仅限 COMMUNITY 角色账号）。
 */
@Composable
fun CommunityLoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    StaffAuthScreen(
        role = UserRole.COMMUNITY,
        endName = "社区",
        accent = Secondary,
        onLoginSuccess = onLoginSuccess,
        onBack = onBack
    )
}
