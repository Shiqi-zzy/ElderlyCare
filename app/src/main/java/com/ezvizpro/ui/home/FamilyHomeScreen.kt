package com.ezvizpro.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ezvizpro.ui.home.components.*

/**
 * 家庭互动首页内容（不含 Scaffold，由父级提供）
 */
@Composable
fun FamilyHomeContent(
    onVideoCallClick: () -> Unit,
    onFamilyMessageClick: () -> Unit,
    onLifeReminderClick: () -> Unit,
    onWechatAuthClick: () -> Unit,
    onPlaybackClick: (String, Int) -> Unit,
    viewModel: FamilyHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. 隐私遮蔽横幅
        PrivacyBanner(
            isVisible = uiState.isPrivacyShieldOn,
            onDismiss = { /* 页面内处理 */ }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. 快捷功能按钮组
        Text(
            "快捷功能",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        QuickActions(
            onVideoCall = onVideoCallClick,
            onFamilyMessage = onFamilyMessageClick,
            onLifeReminder = onLifeReminderClick,
            hasMedicineReminder = uiState.hasMedicineReminder
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 增值服务
        Text(
            "增值服务",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        ServiceCards(onWechatAuthorize = onWechatAuthClick)

        Spacer(modifier = Modifier.height(8.dp))

        // 4. 家庭时光
        Text(
            "家庭时光",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        FamilyMoments(
            albumUsedMB = uiState.albumUsedMB,
            albumTotalMB = uiState.albumTotalMB,
            faceCaptures = uiState.faceCapturesToday
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. 底部播放控制栏（嵌入内容区）
        PlaybackControlBar(
            isPlaying = uiState.isPlaying,
            isMuted = uiState.isMuted,
            deviceName = uiState.currentDeviceName,
            onSeekBack = {},
            onPlayPause = { viewModel.onPlayPause() },
            onSeekForward = {},
            onMuteToggle = { viewModel.onMuteToggle() },
            onVoiceSkill = {},
            onMore = {
                if (uiState.deviceSerial.isNotBlank()) {
                    onPlaybackClick(uiState.deviceSerial, 1)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}
