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
        engine?.let { onReady(it); return }
        val config = RTCConstant.RTCEngineConfig()
        config.appId = appId
        config.context = context.applicationContext
        config.audioCodeType = RTCConstant.ErtcAudioCodeType.AAC // RK3 音频用 AAC（与 S10 一致）
        ERTCEngine.init(config, object : ERTCEngine.OnInitListener {
            override fun onInitialization(e: ERTCEngine) {
                engine = e
                onReady(e)
            }

            override fun onError(code: Int) = onError(code)
        })
    }

    fun setListener(listener: RTCListener?) = engine?.setRTCListener(listener)

    /** 进入房间（用 clientToken） */
    fun enterRoom(param: RoomParam) {
        val p = RTCConstant.EnterParam()
        p.appId = param.appId
        p.roomId = param.roomId
        p.userId = param.userId
        p.token = param.clientToken
        engine?.enterRoom(p, RTCConstant.Scene.VideoCall)
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

    /** 释放：退出房间 + 销毁引擎 */
    fun release() {
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
