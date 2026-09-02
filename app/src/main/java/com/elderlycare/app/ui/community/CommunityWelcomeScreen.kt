package com.elderlycare.app.ui.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.R

/** 社区端欢迎页色值（薄荷绿主题，与参考图统一） */
private val WelcomeBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val MintGreenLight = Color(0xFF6BC9A8)
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)

/**
 * 社区端欢迎页（参考图风格）：
 * 顶部 ElderlyCare Logo + 「智慧养老·用心守护」
 * 中部 老人公园散步插画
 * 底部 「科技守护健康·服务温暖生活」+ 轮播点 + 「进入工作台」按钮
 */
@Composable
fun CommunityWelcomeScreen(onEnter: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WelcomeBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部 Logo 区域
            Spacer(modifier = Modifier.height(60.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo 图标：绿色心形手托
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MintGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "ElderlyCare",
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        "智慧养老 · 用心守护",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }

            // 中部插画
            Spacer(modifier = Modifier.height(40.dp))
            Image(
                painter = painterResource(R.drawable.community_welcome_illustration),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            // 底部文案
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "科技守护健康 · 服务温暖生活",
                color = TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Community Care · Heart to Heart",
                color = TextGray,
                fontSize = 12.sp
            )

            // 轮播指示点
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MintGreen)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0D8D4))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0D8D4))
                )
            }

            // 进入工作台按钮
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onEnter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "进入工作台",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
