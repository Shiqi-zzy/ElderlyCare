package com.elderlycare.app.ui.community

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.shared.WelcomeLoadingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 社区管护端「进入加载页」：
 * 显示角色名 + 写实照片 + 转圈，真实预热工作人员数据后自动进入工作台。
 */
@Composable
fun CommunityWelcomeScreen(onEnter: () -> Unit) {
    LaunchedEffect(Unit) {
        // 真实加载当前工作人员（数据预热），转圈时长跟随实际加载
        withContext(Dispatchers.IO) {
            ServiceLocator.staffUserStore.getCurrentStaffUser()
        }
        delay(1200)
        onEnter()
    }
    WelcomeLoadingScreen(
        roleName = "社区管护",
        tagline = "社区巡访与照护工单，一站管理",
        photoRes = R.drawable.community_welcome_photo,
        accent = Color(0xFF4CAF8A),
        icon = Icons.Filled.Favorite,
        loadingText = "正在加载工作台数据…"
    )
}
