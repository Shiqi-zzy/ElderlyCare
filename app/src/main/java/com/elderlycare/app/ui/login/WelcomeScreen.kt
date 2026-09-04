package com.elderlycare.app.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.R
import com.elderlycare.app.ui.theme.TextSecondary

/**
 * 启动欢迎页（未登录时展示）：与三端「进入加载页」统一风格——
 * 浅灰白底 + 品牌标识行 + 写实照片 + 简洁商务布局 + 底部入口按钮。
 */
@Composable
fun WelcomeScreen(onEnter: () -> Unit) {
    val BgColor = Color(0xFFF5F7FA)
    val TextDark = Color(0xFF1A2E25)
    val TextGray = Color(0xFF6B7C74)
    val Accent = Color(0xFF4A7FE8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 32.dp)
    ) {
        // ===== 顶部：品牌标识行 =====
        Spacer(modifier = Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("银龄心语", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        // ===== 主标题 + 副标题 =====
        Spacer(modifier = Modifier.height(28.dp))
        Text("银龄心语", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("用科技守护  让陪伴更安心", color = TextGray, fontSize = 13.sp)

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
                painter = painterResource(R.drawable.family_welcome_photo),
                contentDescription = "银龄心语",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // ===== 底部：标语 + 按钮 + 提示 =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "看见关心  看见爱 ♡",
                fontSize = 14.sp,
                color = Color(0xFF90A0B5),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4A7FE8), Color(0xFF6BA5FF))
                        )
                    )
                    .clickable(onClick = onEnter),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "开启安心守护",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击进入",
                fontSize = 14.sp,
                color = TextSecondary.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
