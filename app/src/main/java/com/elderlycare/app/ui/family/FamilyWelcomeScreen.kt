package com.elderlycare.app.ui.family

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
 * 居家用户端「进入加载页」：
 * 显示角色名 + 写实照片 + 转圈，真实读取当前绑定设备后自动进入家庭主页。
 */
@Composable
fun FamilyWelcomeScreen(onEnter: () -> Unit) {
    LaunchedEffect(Unit) {
        // 真实加载当前绑定设备（设备链路预热），转圈时长跟随实际加载
        withContext(Dispatchers.IO) {
            ServiceLocator.bindingRepository.getCurrentUserDevice()
        }
        delay(1200)
        onEnter()
    }
    WelcomeLoadingScreen(
        roleName = "居家用户",
        tagline = "远程看护与亲情互动，温暖相伴",
        photoRes = R.drawable.family_welcome_photo,
        accent = Color(0xFF4A7FE8),
        icon = Icons.Filled.Home,
        loadingText = "正在加载家庭看护数据…"
    )
}
