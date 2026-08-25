package com.elderlycare.app.ui.ezviz

/**
 * 把萤石 API / EZUIKit 播放器的原始错误信息翻译成用户可读的友好提示。
 * 直播与回放页共用：取流失败、设备离线、无录像、验证码错误等场景给出可操作的提示。
 */
object FriendlyEzError {

    fun message(raw: String): String = when {
        // 5402：设备该时段无录像（常见于未安装存储卡 / 设备未开启录像）
        raw.contains("5402") ->
            "该时段设备没有录像（可能未安装存储卡或设备未录像），请选择其他日期再试"
        // 60019：设备开启视频加密，验证码不对或未传
        raw.contains("60019") || raw.contains("加密") ->
            "设备已开启视频加密，请确认验证码正确（设备标签上的6位大写字母）"
        // 60020 及类似文案：设备离线
        raw.contains("60020") || raw.contains("离线") || raw.contains("不在线") ->
            "设备不在线，请确认设备已开机并连接网络后重试"
        // token 过期
        raw.contains("10002") || raw.contains("token", ignoreCase = true) ->
            "登录凭证已过期，请重新登录后再试"
        else -> raw
    }
}
