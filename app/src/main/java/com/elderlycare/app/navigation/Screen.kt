package com.elderlycare.app.navigation

sealed class Screen(val route: String) {
    // 门户选择
    data object PortalSelection : Screen("portal_selection")

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
    data object ReportDetail : Screen("report_detail")
    data object VideoPlayer : Screen("video_player")
    data object AlertCenter : Screen("alert_center")
    data object AuthorizationMgmt : Screen("authorization_mgmt")

    // 家属端 — 萤石 RK3 设备（直播 / 回放 / 告警）
    data object LivePreview : Screen("live/{deviceSerial}") {
        fun createRoute(deviceSerial: String) = "live/$deviceSerial"
    }
    data object Playback : Screen("playback/{deviceSerial}") {
        fun createRoute(deviceSerial: String) = "playback/$deviceSerial"
    }
    data object EzvizAlarms : Screen("ezviz_alarms")

    // 家属端 — 萤石云通话（RK3 视频看护）
    data object VideoCall : Screen("video_call/{deviceSerial}?roomId={roomId}") {
        fun createRoute(deviceSerial: String, roomId: String = "") = "video_call/$deviceSerial?roomId=$roomId"
    }

    // 共享用户详情（社区/医院点击用户）
    data object UserDetail : Screen("user_detail/{userId}") {
        fun createRoute(userId: String) = "user_detail/$userId"
    }

    // 社区端详情
    data object EmergencyDispatch : Screen("emergency_dispatch")
    data object PatrolDetail : Screen("patrol_detail")

    // 医院端详情
    data object HealthRecordDetail : Screen("health_record_detail")
    data object FollowUpDetail : Screen("follow_up_detail")
}
