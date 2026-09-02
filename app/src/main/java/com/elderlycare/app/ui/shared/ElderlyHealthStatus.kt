package com.elderlycare.app.ui.shared

import androidx.compose.ui.graphics.Color
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.ScreeningResult
import com.elderlycare.app.data.model.SelfCareLevel
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.StatusRed
import com.elderlycare.app.ui.theme.StatusYellow

/**
 * UI 健康分类（第四阶段）：由档案**现有评估字段**保守推导，仅用于看板/台账的展示统计，
 * 不是医学结论。后续阶段接入真实告警/设备数据后可替换。
 */
enum class HealthCategory(val label: String) {
    NORMAL("正常"),
    ATTENTION("关注"),
    ABNORMAL("异常")
}

/** 分类颜色（看板/台账/急救大屏通用）。 */
fun HealthCategory.color(): Color = when (this) {
    HealthCategory.NORMAL -> StatusGreen
    HealthCategory.ATTENTION -> StatusYellow
    HealthCategory.ABNORMAL -> StatusRed
}

/**
 * 现有字段推导规则：
 * 异常 = 认知/抑郁粗筛阳性，或中重度及以上依赖；
 * 关注 = 轻度依赖，或已患有慢病；
 * 其余 = 正常。
 */
fun ElderlyProfile.healthCategory(): HealthCategory = when {
    cognitiveScreening == ScreeningResult.POSITIVE ||
        depressionScreening == ScreeningResult.POSITIVE ||
        selfCareLevel == SelfCareLevel.UNABLE ||
        selfCareLevel == SelfCareLevel.MODERATE_DEPENDENCE -> HealthCategory.ABNORMAL
    selfCareLevel == SelfCareLevel.MILD_DEPENDENCE || chronicDiseases.isNotEmpty() -> HealthCategory.ATTENTION
    else -> HealthCategory.NORMAL
}

/** 设备是否已绑定（deviceSn 非空或 deviceBound 标记；不读全局 DeviceBindingStore）。 */
fun ElderlyProfile.hasDevice(): Boolean = deviceBound || deviceSn.isNotBlank()
