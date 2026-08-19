package com.elderlycare.app.data.ezviz

import android.content.Context
import android.util.Log
import com.ez.basertc.RTCConstant
import com.ez.basertc.RTCListener
import com.ez.basertc.view.VideoCanvasView
import com.ez.ertcengine.ERTCEngine
import com.videogo.openapi.EZOpenSDK

/**
 * 云通话(ERTC) 管理器。
 *
 * 媒体走 [ERTCEngine]，信令（邀请/取消/拒接设备）走 [EZOpenSDK]。
 * 参考萤石官方 demo：ErtcHelper / ErtcRoomActivity。
 */
object ErtcManager {

    private const val TAG = "ErtcManager"

    var engine: ERTCEngine? = null
        private set

    // ── 第九阶段：状态保护（防重复 init / 防重复 enterRoom / 失败可复位）──
    @Volatile private var initState = InitState.Idle
    @Volatile private var roomState = RoomState.Idle

    /** init 进行中排队等待的 onReady/onError（防御并发调用方，完成后补发）。 */
    private var pendingReady: ((ERTCEngine) -> Unit)? = null
    private var pendingError: ((Int) -> Unit)? = null

    private enum class InitState { Idle, Initializing, Ready, Failed }
    private enum class RoomState { Idle, Entering, InRoom }

    /**
     * 进入房间所需参数。
     * clientToken / deviceToken / roomId 由后端 /api/rtc/token 下发（联调时确认字段）。
     */
    data class RoomParam(
        val appId: String,
        val roomId: String,
        val userId: String,
        val clientToken: String,
        val deviceToken: String,
        val deviceSerial: String,
        val cameraNo: Int = 1,
    )

    /** 初始化 ERTCEngine（用云通话 RTC AppId） */
    fun init(
        context: Context,
        appId: String,
        onReady: (ERTCEngine) -> Unit,
        onError: (Int) -> Unit,
    ) {
        when (initState) {
            // 已初始化完成：直接回调
            InitState.Ready -> engine?.let { onReady(it) }
            // 初始化进行中：排队等待，完成后补发（同一时刻至多一个等待方，防并发重复 init）
            InitState.Initializing -> {
                pendingReady = onReady
                pendingError = onError
            }
            // 空闲 / 上次失败：发起初始化
            InitState.Idle, InitState.Failed -> {
                initState = InitState.Initializing
                val config = RTCConstant.RTCEngineConfig()
                config.appId = appId
                config.context = context.applicationContext
                config.audioCodeType = RTCConstant.ErtcAudioCodeType.AAC // RK3 音频用 AAC（与 S10 一致）
                ERTCEngine.init(config, object : ERTCEngine.OnInitListener {
                    override fun onInitialization(e: ERTCEngine) {
                        engine = e
                        initState = InitState.Ready
                        val waiting = pendingReady
                        pendingReady = null
                        pendingError = null
                        waiting?.invoke(e)
                        onReady(e)
                    }

                    override fun onError(code: Int) {
                        initState = InitState.Failed
                        val waiting = pendingError
                        pendingReady = null
                        pendingError = null
                        waiting?.invoke(code)
                        onError(code)
                    }
                })
            }
        }
    }

    fun setListener(listener: RTCListener?) = engine?.setRTCListener(listener)

    /**
     * 进入房间（用 clientToken）。
     * 幂等：进入中/已在房间时忽略后续调用（返回 false），保证同一时间只发生一次入会。
     * 失败恢复：Entering 状态下入会失败 → RTCListener.onError → endCall → release() 复位为 Idle，可再次尝试。
     */
    fun enterRoom(param: RoomParam): Boolean {
        if (roomState != RoomState.Idle) return false
        roomState = RoomState.Entering
        val e = engine
        if (e == null) {
            roomState = RoomState.Idle
            return false
        }
        val p = RTCConstant.EnterParam()
        p.appId = param.appId
        p.roomId = param.roomId
        p.userId = param.userId
        p.token = param.clientToken
        e.enterRoom(p, RTCConstant.Scene.VideoCall)
        return true
    }

    /** 本地成功入会后由 ViewModel 回调（onEnterRoomSuccess），标记已在房间。 */
    fun markRoomEntered() {
        if (roomState == RoomState.Entering) roomState = RoomState.InRoom
    }

    // ── 视图绑定 ──
    fun setLocalView(view: VideoCanvasView?) = engine?.setLocalView(view)

    fun setRemoteView(userId: String, view: VideoCanvasView?) =
        engine?.setRemoteView(userId, RTCConstant.ERTC_VIDEO_STREAM_TYPE_BIG, view)

    // ── 音视频控制 ──
    fun enableLocalVideo(on: Boolean) = engine?.enableLocalVideo(on)
    fun enableLocalAudio(on: Boolean) = engine?.enableLocalAudio(on)
    fun setSpeakerPhoneOn(on: Boolean) = engine?.setSpeakerPhoneOn(on)
    fun switchCamera() = engine?.switchCamera()
    fun enableHardAec(on: Boolean) = engine?.enableHardAec(on)

    /** 设置本地视频编码参数（与官方 demo 一致，720p/10fps/500kbps） */
    fun setupVideo() {
        val param = RTCConstant.ERTCVideoEncParam()
        param.videoResolution = RTCConstant.ERTCVideoResolution.ERTCVideoResolution_1280_720
        param.videoFps = 10
        param.videoBitrate = 500 * 1024
        engine?.setVideoEncoderParam(param, true)
    }

    // ── 信令（EZOpenSDK）──
    fun inviteDevice(param: RoomParam) {
        try {
            EZOpenSDK.getInstance().inviteDeviceEnterMeeting(
                param.appId, param.deviceToken, param.roomId,
                param.deviceSerial, param.cameraNo, param.userId,
            )
        } catch (e: Exception) {
            Log.e(TAG, "邀请设备入会失败", e)
        }
    }

    fun cancelInviteDevice(param: RoomParam) {
        try {
            EZOpenSDK.getInstance().cancelInviteDeviceEnterMeeting(
                param.appId, param.deviceToken, param.roomId,
                param.deviceSerial, param.cameraNo, param.userId,
            )
        } catch (e: Exception) {
            Log.e(TAG, "取消邀请设备失败", e)
        }
    }

    fun rejectDeviceCall(param: RoomParam): Boolean = try {
        EZOpenSDK.getInstance().rejectVideoCallReqFromDevice(
            param.appId, param.deviceToken, param.roomId,
            param.deviceSerial, param.cameraNo, param.userId,
        )
    } catch (e: Exception) {
        Log.e(TAG, "拒接设备呼叫失败", e)
        false
    }

    /** 释放：退出房间 + 销毁引擎。先复位状态，任何路径（含 engine 为空）都恢复可重试。 */
    fun release() {
        initState = InitState.Idle
        roomState = RoomState.Idle
        pendingReady = null
        pendingError = null
        val e = engine ?: return
        try {
            e.setLocalView(null)
            e.setLocalViewChanged(null)
            e.enableLocalVideo(false)
            e.setRTCListener(null)
            e.exitRoom()
        } catch (ex: Exception) {
            Log.w(TAG, "release 异常", ex)
        }
        ERTCEngine.destroyEngine()
        engine = null
    }
}
