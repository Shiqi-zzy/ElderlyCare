package com.elderlycare.app.ui.message

import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elderlycare.app.ui.theme.TextHint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 设备视频留言缩略图（留言页 / 消息中心共用）。
 *
 * 优先加载云端缩略图 URL（EZLeaveMessage.msgPicUrl / 告警 alarmPicUrl，Coil 加载，
 * 失败时露出底层占位）；thumbUrl 为空（历史数据/云端未给）时改为本地提取视频首帧
 * （IO 线程 MediaMetadataRetriever，结果 remember 缓存，提帧失败显示占位）。
 */
@Composable
fun VideoThumbnail(
    thumbUrl: String,
    localVideoPath: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 底层占位：云端加载失败 / 本地提帧中，均露出灰底播放图标
        ThumbnailPlaceholder(modifier = Modifier.matchParentSize())
        if (thumbUrl.isNotBlank()) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 本地提取视频首帧（IO 线程，按路径缓存，路径变化自动重新提取）
            var frame by remember(localVideoPath) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(localVideoPath) {
                frame = if (localVideoPath.isBlank()) null
                else extractFirstFrame(localVideoPath)
            }
            frame?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/** 灰底占位（云端图加载失败 / 本地提帧失败时可见） */
@Composable
private fun ThumbnailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(TextHint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = TextHint.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp)
        )
    }
}

/** 本地视频首帧提取（MediaMetadataRetriever，失败返回 null 由占位兜底） */
private suspend fun extractFirstFrame(path: String): ImageBitmap? =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val bitmap: android.graphics.Bitmap? =
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            Log.w("VideoThumbnail", "提取视频首帧失败: $path", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
