package com.elderlycare.app.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 三端统一的「进入加载页」：
 * 浅灰白底 + 小图标 + 银龄心语 + 角色名 + 一句话定位 + 写实照片 + 底部转圈加载。
 * 由各端在 LaunchedEffect 中执行真实初始化后调用 onEnter() 自动进入主页。
 *
 * @param roleName  角色名（如"社区管护"）
 * @param tagline   一句话定位（如"社区巡访与照护工单，一站管理"）
 * @param photoRes  写实照片资源
 * @param accent    主题色（图标 / 转圈）
 * @param icon      角色图标
 * @param loadingText 加载提示（如"正在加载工作台数据…"）
 */
@Composable
fun WelcomeLoadingScreen(
    roleName: String,
    tagline: String,
    photoRes: Int,
    accent: Color,
    icon: ImageVector,
    loadingText: String
) {
    val BgColor = Color(0xFFF5F7FA)
    val TextDark = Color(0xFF1A2E25)
    val TextGray = Color(0xFF6B7C74)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 32.dp)
    ) {
        // ===== 顶部：小图标 + 银龄心语 =====
        Spacer(modifier = Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("银龄心语", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        // ===== 角色名 + 定位语 =====
        Spacer(modifier = Modifier.height(28.dp))
        Text(roleName, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(tagline, color = TextGray, fontSize = 13.sp)

        // ===== 中部写实照片 =====
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
        ) {
            Image(
                painter = painterResource(photoRes),
                contentDescription = roleName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }
        // 照片底部细边框分隔
        Spacer(modifier = Modifier.height(18.dp))

        // ===== 底部：转圈 + 加载提示 =====
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = accent,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(loadingText, color = TextGray, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
