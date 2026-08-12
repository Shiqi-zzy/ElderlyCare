package com.ezvizpro.ui.wechat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ezvizpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WechatAuthScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("微信视频通话") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = Green500.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.VideoCall,
                        null,
                        tint = Green500,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "绑定微信视频通话",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "授权后，家人可通过微信直接呼叫您的设备进行视频通话",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray600
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 授权步骤
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AuthStep("1", "打开微信扫一扫", "扫描设备二维码添加设备为微信好友")
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Gray600.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthStep("2", "关注萤石云视频公众号", "接收设备告警和视频通话通知")
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Gray600.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthStep("3", "完成设备绑定", "在公众号中绑定设备序列号即可使用")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* TODO: 跳转微信或显示二维码 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green500)
            ) {
                Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("前往微信授权")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "需要设备支持语音对讲功能（supportTalk）",
                style = MaterialTheme.typography.bodySmall,
                color = Gray600
            )
        }
    }
}

@Composable
private fun AuthStep(number: String, title: String, desc: String) {
    Row {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(14.dp),
            color = Green500
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Gray600)
        }
    }
}
