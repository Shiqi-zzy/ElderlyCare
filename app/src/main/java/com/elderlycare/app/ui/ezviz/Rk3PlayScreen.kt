package com.elderlycare.app.ui.ezviz

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.model.AudioCategory
import com.elderlycare.app.data.ezviz.model.AudioTrack
import com.elderlycare.app.data.ezviz.model.MediaPlayState
import com.elderlycare.app.ui.theme.*

/**
 * RK3 点播页（设备端音频播放，不做音频下载到手机）。
 *
 * 参照萤石云视频 App 点播 UI：分类 Tab + 音频卡片列表 + 底部播放状态栏。
 * 播放状态正常链路靠 webhook 回调（Rk3MediaStateHub）；Mock 开关打开时
 * 本地模拟播放状态切换（演示答辩用）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Rk3PlayScreen(
    deviceSerial: String,
    onBackClick: () -> Unit,
    viewModel: Rk3PlayViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // toast 提示（一次性消费）
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            msg?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.consumeToast()
            }
        }
    }

    LaunchedEffect(deviceSerial) {
        viewModel.initialize(deviceSerial)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RK3 点播", fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.rk3_play_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.rk3_play_title))
                    }
                },
                actions = {
                    // Mock 演示开关（网络层占位期间默认开；真实接口开通后关闭走真实请求）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.rk3_mock_switch),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = uiState.mockEnabled,
                            onCheckedChange = { viewModel.setMockEnabled(it) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        bottomBar = {
            PlayStateBar(
                currentTrack = uiState.currentTrack,
                playState = uiState.playState,
                onPlayPauseClick = viewModel::onPlayPauseClick,
                onStopClick = viewModel::onStopClick
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 分类 Tab：推荐｜音乐｜戏曲｜童话故事｜诗词跟学
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedCategory.ordinal,
                containerColor = Surface,
                edgePadding = 8.dp
            ) {
                AudioCategory.entries.forEach { category ->
                    Tab(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        text = {
                            Text(
                                categoryLabel(category),
                                fontWeight = if (uiState.selectedCategory == category)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            if (uiState.tracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (uiState.mockEnabled) "暂无音频内容" else stringResource(R.string.rk3_api_pending),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tracks, key = { it.contentId }) { track ->
                        AudioTrackCard(
                            track = track,
                            isPlaying = uiState.currentTrack?.contentId == track.contentId &&
                                uiState.playState == MediaPlayState.PLAYING,
                            onClick = { viewModel.onTrackClick(track) }
                        )
                    }
                }
            }
        }
    }

    // 「部分音频需要智控畅享服务」弹窗（接口权限不足时同样弹此文案）
    if (uiState.showPremiumDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPremiumDialog,
            title = { Text(stringResource(R.string.rk3_play_title)) },
            text = { Text(stringResource(R.string.rk3_premium_required)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPremiumDialog) {
                    Text("知道了")
                }
            }
        )
    }

    // 「请先绑定RK3设备」弹窗（设备校验）
    if (uiState.showBindDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBindDialog,
            title = { Text(stringResource(R.string.rk3_play_title)) },
            text = { Text(stringResource(R.string.rk3_bind_required)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissBindDialog) {
                    Text("知道了")
                }
            }
        )
    }
}

/** 底部播放状态栏：当前播放项 + 播放/暂停/停止 */
@Composable
private fun PlayStateBar(
    currentTrack: AudioTrack?,
    playState: MediaPlayState,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Surface(
        color = Surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    currentTrack?.title ?: stringResource(R.string.rk3_play_state_idle),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when (playState) {
                        MediaPlayState.IDLE -> stringResource(R.string.rk3_play_state_idle)
                        MediaPlayState.PLAYING -> stringResource(R.string.rk3_play_state_playing)
                        MediaPlayState.PAUSED -> stringResource(R.string.rk3_play_state_paused)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (playState == MediaPlayState.PLAYING) Primary else TextSecondary
                )
            }
            TextButton(
                onClick = onPlayPauseClick,
                enabled = currentTrack != null
            ) {
                Text(
                    if (playState == MediaPlayState.PLAYING) stringResource(R.string.rk3_pause)
                    else stringResource(R.string.rk3_play),
                    color = if (currentTrack != null) Primary else TextHint
                )
            }
            TextButton(
                onClick = onStopClick,
                enabled = currentTrack != null
            ) {
                Text(stringResource(R.string.rk3_stop), color = if (currentTrack != null) Primary else TextHint)
            }
        }
    }
}

/** 音频卡片：图标 + 标题/副标题 + 畅享角标/时长 */
@Composable
private fun AudioTrackCard(
    track: AudioTrack,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Primary.copy(alpha = 0.08f) else Surface
        ),
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(
                    color = if (isPlaying) Primary else Primary.copy(alpha = 0.10f),
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPlaying) Surface else Primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        track.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (track.premium) {
                    // 智控畅享角标（点击卡片时弹「部分音频需要智控畅享服务」）
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Secondary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                stringResource(R.string.rk3_premium_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = Secondary
                            )
                        }
                    }
                }
                if (track.durationSec > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${track.durationSec / 60}:${(track.durationSec % 60).toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryLabel(category: AudioCategory): String = when (category) {
    AudioCategory.RECOMMEND -> stringResource(R.string.rk3_tab_recommend)
    AudioCategory.MUSIC -> stringResource(R.string.rk3_tab_music)
    AudioCategory.OPERA -> stringResource(R.string.rk3_tab_opera)
    AudioCategory.FAIRY_TALE -> stringResource(R.string.rk3_tab_fairy_tale)
    AudioCategory.POETRY -> stringResource(R.string.rk3_tab_poetry)
}
