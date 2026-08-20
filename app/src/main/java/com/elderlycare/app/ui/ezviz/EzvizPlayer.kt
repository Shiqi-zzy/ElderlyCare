package com.elderlycare.app.ui.ezviz

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * 播放器状态
 */
sealed class PlayerState {
    data object Idle : PlayerState()
    data object Buffering : PlayerState()
    data object Playing : PlayerState()
    data object Paused : PlayerState()
    data class Error(val message: String) : PlayerState()
}

/**
 * ExoPlayer 封装
 */
class EzvizPlayer(private val context: Context) {

    private val TAG = "EzvizPlayer"

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setLiveTargetOffsetMs(2000)
        )
        .build()

    var currentState: PlayerState = PlayerState.Idle
        private set

    private var stateListener: ((PlayerState) -> Unit)? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                currentState = when (state) {
                    Player.STATE_BUFFERING -> PlayerState.Buffering
                    Player.STATE_READY -> {
                        if (exoPlayer.playWhenReady) PlayerState.Playing
                        else PlayerState.Paused
                    }
                    Player.STATE_IDLE -> PlayerState.Idle
                    else -> PlayerState.Idle
                }
                stateListener?.invoke(currentState)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                currentState = PlayerState.Error(error.localizedMessage ?: "播放失败")
                stateListener?.invoke(currentState)
                Log.e(TAG, "播放器错误", error)
            }
        })
    }

    fun setOnStateChangeListener(listener: (PlayerState) -> Unit) {
        stateListener = listener
    }

    fun play(url: String) {
        Log.d(TAG, "开始播放: $url")
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        currentState = PlayerState.Paused
    }

    fun resume() {
        exoPlayer.playWhenReady = true
        currentState = PlayerState.Playing
    }

    fun stop() {
        exoPlayer.stop()
        currentState = PlayerState.Idle
    }

    fun release() {
        exoPlayer.release()
        Log.d(TAG, "播放器已释放")
    }

    fun setMuted(muted: Boolean) {
        exoPlayer.volume = if (muted) 0f else 1f
    }
}

/**
 * Compose 版播放器视图
 */
@Composable
fun rememberEzvizPlayer(context: Context = LocalContext.current): EzvizPlayer {
    val player = remember { EzvizPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    return player
}

@Composable
fun EzvizPlayerView(
    player: EzvizPlayer,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player.exoPlayer
                useController = false
                this.resizeMode = resizeMode
                keepScreenOn = true
            }
        },
        update = { view ->
            view.resizeMode = resizeMode
        }
    )
}
