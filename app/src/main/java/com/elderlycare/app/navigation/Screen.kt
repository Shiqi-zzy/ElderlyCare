package com.elderlycare.app.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    // 启动欢迎页（点击「开启安心守护」后按登录态进入真实首屏）
    data object Welcome : Screen("welcome")

    // 门户选择
    data object PortalSelection : Screen("portal_selection")

    // 三端登录/注册（多端账号体系）
    data object FamilyLogin : Screen("family_login")

    // 社区/医院端登录/注册（统一认证，按角色限定入口）
    data object CommunityLogin : Screen("community_login")
    data object HospitalLogin : Screen("hospital_login")

    // Wizard
    data object FamilyWizard : Screen("family_wizard")
    data object CommunityWizard : Screen("community_wizard")
    data object HospitalWizard : Screen("hospital_wizard")

    // 主界面
    data object FamilyMain : Screen("family_main")
    data object CommunityMain : Screen("community_main")
    data object HospitalMain : Screen("hospital_main")

    // 家属端详情
    data object ProfileDetail : Screen("profile_detail")
    data object ProfileEdit : Screen("profile_edit")
    data object ReportDetail : Screen("report_detail")
    data object VideoPlayer : Screen("video_player")
    data object AlertCenter : Screen("alert_center")
    data object AuthorizationMgmt : Screen("authorization_mgmt")
    // 家属端 — 绑定申请审核（社区/医院发起的绑定申请）
    data object BindingRequest : Screen("binding_request")

    // 家属端 — 留言（音频收发模块）
    data object Message : Screen("message")

    // 家属端 — 设备视频留言播放页（messageId = 本机 message 表主键，非设备串号，无需授权闸门）
    data object DeviceVideo : Screen("device_video/{messageId}") {
        fun createRoute(messageId: Long) = "device_video/$messageId"
    }

    // 家属端 — 聊天对话页（会话键 = 内存分组键：设备消息「RK3(SN)」/「我」/人员名称，URL 编码传递）
    data object Conversation : Screen("conversation/{senderName}") {
        fun createRoute(senderName: String) = "conversation/${Uri.encode(senderName)}"
    }

    // 家属端 — 提醒计划（RK3 设备本地闹铃；留言页入口 + 日程 Tab 共用）
    data object RemindPlanList : Screen("remind_plan_list")
    data object RemindPlanForm : Screen("remind_plan_form/{templateKey}") {
        fun createRoute(templateKey: String) = "remind_plan_form/$templateKey"
    }
    data object VoiceSelect : Screen("remind_voice_select")
    data object RemindPlanDetail : Screen("remind_plan_detail/{planId}") {
        fun createRoute(planId: Long) = "remind_plan_detail/$planId"
    }

    // 家属端 — 萤石 RK3 设备（直播 / 回放 / 告警）
    data object LivePreview : Screen("live/{deviceSerial}") {
        fun createRoute(deviceSerial: String) = "live/$deviceSerial"
    }
    // 家属端 — RK3 点播 / 广播FM（设备媒体播放，与 sendOnce 留言广播完全独立）
    data object Rk3Play : Screen("rk3_play/{deviceSerial}") {
        fun createRoute(deviceSerial: String) = "rk3_play/$deviceSerial"
    }
    data object Rk3Fm : Screen("rk3_fm/{deviceSerial}") {
        fun createRoute(deviceSerial: String) = "rk3_fm/$deviceSerial"
    }
    data object Playback : Screen("playback/{deviceSerial}?startTime={startTime}") {
        // startTime 可选：告警发生时间（yyyy-MM-dd HH:mm:ss），回放自动定位到该时刻附近
        fun createRoute(deviceSerial: String, startTime: String = "") =
            "playback/$deviceSerial?startTime=${Uri.encode(startTime)}"
    }
    data object EzvizAlarms : Screen("ezviz_alarms")

    // 家属端 — 全部抓拍（手动抓拍 + 设备告警自动抓拍快照唯一查看入口；数据源=后端 alarm_events）
    data object Captures : Screen("captures")

    // 家属端 — 云通话（RK3 视频通话；roomId 空 = App 主动呼叫，非空 = 设备呼叫加入房间）
    data object VideoCall : Screen("video_call/{deviceSerial}?roomId={roomId}") {
        fun createRoute(deviceSerial: String, roomId: String = "") = "video_call/$deviceSerial?roomId=$roomId"
    }

    // 共享用户详情（社区/医院点击用户；elderlyId = profile.userId，权限由 BindingRepository 校验）
    data object UserDetail : Screen("user_detail/{elderlyId}") {
        fun createRoute(elderlyId: String) = "user_detail/$elderlyId"
    }

    // 社区端详情
    data object EmergencyDispatch : Screen("emergency_dispatch")
    data object PatrolDetail : Screen("patrol_detail")

    // 医院端详情
    data object HealthRecordDetail : Screen("health_record_detail")
    data object FollowUpDetail : Screen("follow_up_detail")

    // 医院端 — 医疗随访（elderlyId 空 = 全部随访记录只读视图，无录入入口）
    data object HospitalFollowUp : Screen("hospital_follow_up?elderlyId={elderlyId}") {
        fun createRoute(elderlyId: String = "") = "hospital_follow_up?elderlyId=$elderlyId"
    }

    // 医院端 — 健康建议录入（仅 App 消息模块查看，不走设备播报）
    data object HospitalAdvice : Screen("hospital_advice/{elderlyId}") {
        fun createRoute(elderlyId: String) = "hospital_advice/$elderlyId"
    }

    // 医院端 — 复诊提醒（复用提醒计划萤石 v3 clock 能力；无需老人参数，页内选择）
    data object HospitalMedicalRemind : Screen("hospital_medical_remind")

    // 医院端 — 本地版健康报告（ACTIVE 授权校验 + 本地聚合，非跨端后端版）
    data object HospitalReport : Screen("hospital_report/{elderlyId}") {
        fun createRoute(elderlyId: String) = "hospital_report/$elderlyId"
    }
}
