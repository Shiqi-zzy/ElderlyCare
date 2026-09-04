package com.elderlycare.app.ui.hospital

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
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
 * 医护管理端「进入加载页」：
 * 显示角色名 + 写实照片 + 转圈，真实预热工作人员数据后自动进入急救大屏。
 */
@Composable
fun HospitalWelcomeScreen(onEnter: () -> Unit) {
    LaunchedEffect(Unit) {
        // 真实加载当前工作人员（数据预热），转圈时长跟随实际加载
        withContext(Dispatchers.IO) {
            ServiceLocator.staffUserStore.getCurrentStaffUser()
        }
        delay(1200)
        onEnter()
    }
    WelcomeLoadingScreen(
        roleName = "医护管理",
        tagline = "健康监测与医疗联动，专业守护",
        photoRes = R.drawable.hospital_welcome_photo,
        accent = Color(0xFF2A9D8F),
        icon = Icons.Filled.LocalHospital,
        loadingText = "正在加载急救大屏数据…"
    )
}
