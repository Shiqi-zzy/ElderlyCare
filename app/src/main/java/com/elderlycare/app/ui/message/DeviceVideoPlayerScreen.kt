package com.elderlycare.app.ui.message

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.data.message.MessageFiles
import com.elderlycare.app.ui.ezviz.EzvizPlayerView
import com.elderlycare.app.ui.ezviz.PlayerState
import com.elderlycare.app.ui.ezviz.rememberEzvizPlayer
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 设备视频留言播放页。
 *
 * 路由参数 messageId 为本机 message 表主键（非设备串号，无需设备授权闸门）。
 * 播放源优先级：本地缓存（localVideoPath，断网可播）→ 云端 URL（videoCloudUrl）。
 * 打开即标记已读（仓库层对 msgType=3 同步云端已读）；
 * 本地缓存播放失败时重试按钮自动删除损坏缓存并降级直连云端 URL。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceVideoPlayerScreen(messageId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = rememberEzvizPlayer(context)

    var message by remember { mutableStateOf<MessageEntity?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var playUrl by remember { mutableStateOf<String?>(null) }
    var playerState by remember { mutableStateOf<PlayerState>(PlayerState.Idle) }

    // 加载留言记录：不存在 → 空态；未读 → 标记已读；选定播放源（本地优先）
    LaunchedEffect(messageId) {
        val msg = withContext(Dispatchers.IO) {
            ServiceLocator.messageRepository.getMessageById(messageId)
        }
        if (msg == null) {
            notFound = true
            return@LaunchedEffect
        }
        message = msg
        if (!msg.isRead) {
            withContext(Dispatchers.IO) {
                ServiceLocator.messageRepository.markMessageRead(msg)
            }
        }
        playUrl = msg.localVideoPath.takeIf { it.isNotBlank() && File(it).exists() }
            ?: msg.videoCloudUrl.takeIf { it.isNotBlank() }
    }

    // 播放器状态透传（Buffering / Error 覆盖层）；播放器释放由 rememberEzvizPlayer 内部负责
    LaunchedEffect(player) {
        player.setOnStateChangeListener { playerState = it }
    }

    // 播放源就绪后开始播放（本地文件转 file:// URI；云端 URL 直传）
    LaunchedEffect(playUrl) {
        playUrl?.let { url ->
            val uri = if (url.startsWith("http")) url
            else Uri.fromFile(File(url)).toString()
            player.play(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.message_video_label), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.message_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                notFound -> CenterHint(stringResource(R.string.message_video_not_found))
                playUrl == null -> CenterHint(stringResource(R.string.message_video_not_found))
                else -> {
                    EzvizPlayerView(player = player, modifier = Modifier.fillMaxSize())

                    // 缓冲中 / 未就绪
                    if (playerState == PlayerState.Buffering || playerState == PlayerState.Idle) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(40.dp),
                            color = Primary
                        )
                    }

                    // 播放失败 + 重试：本地缓存损坏时删缓存降级云端 URL，否则原源重试
                    if (playerState is PlayerState.Error) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(R.string.message_video_play_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                val current = playUrl
                                val msg = message
                                if (current != null && !current.startsWith("http") &&
                                    msg != null && msg.videoCloudUrl.isNotBlank()
                                ) {
                                    // 本地缓存损坏：删缓存文件，降级直连云端 URL
                                    MessageFiles.deleteQuietly(File(current))
                                    Log.w("DeviceVideoPlayer", "本地视频缓存播放失败，删除并降级云端: ${msg.videoCloudUrl}")
                                    playUrl = msg.videoCloudUrl
                                } else {
                                    // 云端播放失败：重试原源
                                    playUrl?.let { url ->
                                        val uri = if (url.startsWith("http")) url
                                        else Uri.fromFile(File(url)).toString()
                                        player.play(uri)
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.message_video_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 居中提示文案（视频不存在/已删除） */
@Composable
private fun CenterHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
