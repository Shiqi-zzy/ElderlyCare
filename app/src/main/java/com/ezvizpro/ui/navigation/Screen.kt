package com.ezvizpro.ui.navigation

/**
 * 路由定义 — 智慧养老平台（基于萤石）
 *
 * 家属端：MainScreen（底部导航四Tab）
 * 社区端：CommunityPortal（验证门控）
 * 医院端：HospitalPortal（验证门控）
 */
sealed class Screen(val route: String) {
    // 登录
    data object Login : Screen("login")

    // 家属主页 (底部导航容器: 首页/设备/消息/我的)
    data object Main : Screen("main")

    // 家庭互动页
    data object FamilyHome : Screen("family_home")

    // 家人留言
    data object FamilyMessage : Screen("family_message")

    // 生活提醒
    data object LifeReminder : Screen("life_reminder")

    // 微信授权
    data object WechatAuth : Screen("wechat_auth")

    // 视频通话
    data object VideoCall : Screen("video_call")

    // 实时预览页
    data object LivePlay : Screen("live/{deviceSerial}/{channelNo}") {
        fun createRoute(deviceSerial: String, channelNo: Int = 1): String =
            "live/$deviceSerial/$channelNo"
    }

    // 录像回放页
    data object Playback : Screen("playback/{deviceSerial}/{channelNo}") {
        fun createRoute(deviceSerial: String, channelNo: Int = 1): String =
            "playback/$deviceSerial/$channelNo"
    }

    // ── 角色选择页（新用户） ──
    data object RoleSelect : Screen("role_select/{clientId}") {
        fun createRoute(clientId: String): String = "role_select/$clientId"
    }

    // ── 智慧养老子页面（家属端内部导航） ──

    // 养老告警中心
    data object ElderlyAlarms : Screen("elderly_alarms")

    // 授权管理
    data object ElderlyAuthorizations : Screen("elderly_authorizations")

    // 隐私控制
    data object ElderlyPrivacy : Screen("elderly_privacy")

    // ── 社区端 ──
    data object CommunityPortal : Screen("community_portal/{token}") {
        fun createRoute(token: String): String = "community_portal/$token"
    }

    // 社区资质验证页
    data object CommunityVerify : Screen("community_verify/{token}") {
        fun createRoute(token: String): String = "community_verify/$token"
    }

    // ── 医院端 ──
    data object HospitalPortal : Screen("hospital_portal/{token}") {
        fun createRoute(token: String): String = "hospital_portal/$token"
    }

    // 医院资质验证页
    data object HospitalVerify : Screen("hospital_verify/{token}") {
        fun createRoute(token: String): String = "hospital_verify/$token"
    }
}

/**
 * 底部导航项
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: String,
    val unselectedIcon: String
) {
    HOME(
        route = "home",
        label = "首页",
        selectedIcon = "home",
        unselectedIcon = "home_outlined"
    ),
    DEVICES(
        route = "devices",
        label = "设备",
        selectedIcon = "videocam",
        unselectedIcon = "videocam_outlined"
    ),
    MESSAGES(
        route = "messages",
        label = "消息",
        selectedIcon = "notifications",
        unselectedIcon = "notifications_outlined"
    ),
    PROFILE(
        route = "profile",
        label = "我的",
        selectedIcon = "person",
        unselectedIcon = "person_outlined"
    )
}
