package com.elderlycare.app.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
 * 启动欢迎页：浅蓝渐变背景 + Logo + 标题 + 温馨插画 + 渐变按钮。
 * 冷启动的系统 splash（splash_bg.xml）使用同色系渐变背景，衔接无跳变。
 */
@Composable
fun WelcomeScreen(onEnter: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F1FB),
                        Color(0xFFF2F7FC),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        // 上半部分：Logo + 标题 + 副标题 + 插画
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo — 圆角白色卡片 + 四叶草彩色图标
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(82.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    FourLeafLogo()
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 主标题
            Text(
                text = "萤石养老看护",
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C2B4A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 副标题
            Text(
                text = "用科技守护  让陪伴更安心",
                fontSize = 14.sp,
                color = Color(0xFF7C8BA0),
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 主视觉插画（顶部对齐，避免偏下）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(R.drawable.welcome_illustration),
                    contentDescription = "养老看护温馨插画",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // 底部区域：标语 + 指示点 + 渐变按钮 + 提示文字
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 情感标语
            Text(
                text = "看见关心  看见爱 ♡",
                fontSize = 13.sp,
                color = Color(0xFF90A0B5),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 轮播指示点（3 页，当前第 1 页高亮）
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotIndicator(isActive = true)
                DotIndicator(isActive = false)
                DotIndicator(isActive = false)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 渐变圆角按钮 — 开启安心守护
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4A7FE8),
                                Color(0xFF6BA5FF)
                            )
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
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 四叶草 Logo — 四个彩色圆形（蓝 / 绿 / 红 / 橙）组成，
 * 呼应萤石品牌的四色花瓣标识。
 */
@Composable
private fun FourLeafLogo() {
    Canvas(modifier = Modifier.size(46.dp)) {
        val radius = size.width * 0.21f
        val offset = size.width * 0.17f
        val cx = size.width / 2
        val cy = size.height / 2
        // 左上 — 蓝
        drawCircle(
            color = Color(0xFF4A90D9),
            radius = radius,
            center = Offset(cx - offset, cy - offset)
        )
        // 右上 — 绿
        drawCircle(
            color = Color(0xFF6BBF7A),
            radius = radius,
            center = Offset(cx + offset, cy - offset)
        )
        // 左下 — 红
        drawCircle(
            color = Color(0xFFE8857C),
            radius = radius,
            center = Offset(cx - offset, cy + offset)
        )
        // 右下 — 橙
        drawCircle(
            color = Color(0xFFF5A623),
            radius = radius,
            center = Offset(cx + offset, cy + offset)
        )
    }
}

/** 轮播指示点 — 激活态为蓝色实心，非激活态为浅灰小圆。 */
@Composable
private fun DotIndicator(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(if (isActive) 9.dp else 7.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isActive) Color(0xFF4A7FE8)
                else Color(0xFFD1D9E6)
            )
    )
}
