package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.components.RiskLevel
import com.elderlycare.app.ui.components.RiskLevelIndicator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.components.charts.ChartLegend
import com.elderlycare.app.ui.components.charts.DonutChart
import com.elderlycare.app.ui.components.charts.PieSlice
import com.elderlycare.app.ui.shared.HealthCategory
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.StatusRed
import com.elderlycare.app.ui.theme.StatusYellow
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import java.util.Locale

/**
 * 医院端「本地版健康报告」（仅前端）。
 *
 * 进入先校验 ACTIVE 医院授权：未授权 → 占位「未获得家属授权，无法查看健康报告」；
 * 已授权 → 读本地 Room（告警事件 + 档案体征）本地聚合渲染：
 * - 健康概况：复用 ElderlyHealthStatus.healthCategory() 统计分类 + RiskLevelIndicator；
 * - 告警统计：message 表 category=2 按类型聚合 → 复用 DonutChart/ChartLegend；
 * - 随访/建议计数 + 本地聚合评估文本。
 * 本阶段不做跨端后端版（页面顶部有本地聚合提示条）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalReportScreen(
    elderlyId: String,
    onBack: () -> Unit
) {
    val viewModel: HospitalReportViewModel = viewModel()
    LaunchedEffect(elderlyId) { viewModel.start(elderlyId) }

    val authorized by viewModel.authorized.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val followUpCount by viewModel.followUpCount.collectAsStateWithLifecycle()
    val adviceCount by viewModel.adviceCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.hospital_report_title), fontWeight = FontWeight.SemiBold)
                        if (profile != null) {
                            Text(
                                profile!!.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHint
                            )
                        }
                    }
                },
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
        when {
            // 校验中
            authorized == null -> CenterHint("加载中…", paddingValues, loading = true)
            // 未获得家属授权 → 占位
            authorized == false -> CenterHint(
                stringResource(R.string.hospital_report_no_auth), paddingValues
            )
            // 已授权但档案尚未读出
            profile == null -> CenterHint("加载中…", paddingValues, loading = true)
            else -> ReportContent(
                profile = profile!!,
                alerts = alerts,
                followUpCount = followUpCount,
                adviceCount = adviceCount,
                paddingValues = paddingValues
            )
        }
    }
}

@Composable
private fun CenterHint(text: String, paddingValues: androidx.compose.foundation.layout.PaddingValues, loading: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = Primary)
                Spacer(Modifier.height(12.dp))
            }
            Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 报告主体：概况（体征聚合）+ 告警统计（类型分布）+ 评估文本 */
@Composable
private fun ReportContent(
    profile: ElderlyProfile,
    alerts: List<com.elderlycare.app.data.message.MessageEntity>,
    followUpCount: Int,
    adviceCount: Int,
    paddingValues: androidx.compose.foundation.layout.PaddingValues
) {
    val category = profile.healthCategory()
    val riskLevel = category.riskLevel()
    val bmi = calcBmi(profile)
    val bpAvailable = profile.bloodPressureHigh.isNotBlank() && profile.bloodPressureLow.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 本地聚合提示条
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = Primary.copy(alpha = 0.06f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.hospital_report_local_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Primary
            )
        }

        // ===== 健康概况 =====
        ReportCard(title = stringResource(R.string.hospital_report_overview)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(text = category.label, color = category.color())
                Spacer(Modifier.width(8.dp))
                RiskLevelIndicator(level = riskLevel)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${profile.age}岁 · ${profile.gender.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            if (bmi != null) {
                InfoRow("BMI", "${String.format(Locale.US, "%.1f", bmi)}（${bmiLabel(bmi)}）")
            }
            if (bpAvailable) {
                InfoRow("血压", "${profile.bloodPressureHigh}/${profile.bloodPressureLow} mmHg")
            }
            if (profile.physicalExam.heartRate.isNotBlank()) {
                InfoRow("心率", "${profile.physicalExam.heartRate} 次/分")
            }
            InfoRow("慢病", profile.chronicDiseases.joinToString("、").ifEmpty { "无" }, showDivider = false)
            InfoRow(
                stringResource(R.string.hospital_report_follow_up_count, followUpCount),
                stringResource(R.string.hospital_report_advice_count, adviceCount),
                showDivider = false
            )
        }

        // ===== 告警统计（本地 Room 告警事件按类型聚合） =====
        ReportCard(title = stringResource(R.string.hospital_report_alerts)) {
            if (alerts.isEmpty()) {
                Text(
                    stringResource(R.string.hospital_report_alerts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                val slices = alertSlices(alerts)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        slices = slices,
                        modifier = Modifier.size(140.dp),
                        centerLabel = alerts.size.toString()
                    )
                    Spacer(Modifier.width(20.dp))
                    ChartLegend(slices = slices, modifier = Modifier.weight(1f))
                }
            }
        }

        // ===== 本地聚合评估 =====
        ReportCard(title = stringResource(R.string.hospital_report_assessment)) {
            Text(
                buildAssessment(profile, category, alerts, bmi, bpAvailable, followUpCount, adviceCount),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }
    }
}

/** 报告分区卡片（标题 + 内容） */
@Composable
private fun ReportCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

// ==================== 聚合工具（本地计算，复用家属端统计语义） ====================

/** 健康分类 → 风险等级（复用 RiskLevelIndicator 组件） */
private fun HealthCategory.riskLevel(): RiskLevel = when (this) {
    HealthCategory.NORMAL -> RiskLevel.NORMAL
    HealthCategory.ATTENTION -> RiskLevel.ATTENTION
    HealthCategory.ABNORMAL -> RiskLevel.RISK
}

/** BMI = 体重(kg) / 身高(m)²；档案字段非数字/缺失返回 null */
private fun calcBmi(profile: ElderlyProfile): Double? {
    val h = profile.height.toDoubleOrNull() ?: return null
    val w = profile.weight.toDoubleOrNull() ?: return null
    if (h <= 0 || w <= 0) return null
    val m = h / 100
    return w / (m * m)
}

/** 中国成人 BMI 分级（照护对象健康档案常用区间） */
private fun bmiLabel(bmi: Double): String = when {
    bmi < 18.5 -> "偏瘦"
    bmi < 24 -> "正常"
    bmi < 28 -> "超重"
    else -> "肥胖"
}

/** 告警类型聚合 → 环形图切片（按数量降序，颜色轮换） */
private fun alertSlices(
    alerts: List<com.elderlycare.app.data.message.MessageEntity>
): List<PieSlice> {
    val palette = listOf(StatusRed, StatusYellow, Primary, StatusGreen, Color(0xFF9C6ADE), Color(0xFF4A9E8F))
    return alerts
        .groupingBy { it.content.ifBlank { "其他" } }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .mapIndexed { i, (label, count) ->
            PieSlice(label = label, value = count.toFloat(), color = palette[i % palette.size])
        }
}

/** 评估文本（本地聚合：分类 + 告警 + BMI/血压/心率 + 随访/建议计数） */
private fun buildAssessment(
    profile: ElderlyProfile,
    category: HealthCategory,
    alerts: List<com.elderlycare.app.data.message.MessageEntity>,
    bmi: Double?,
    bpAvailable: Boolean,
    followUpCount: Int,
    adviceCount: Int
): String = buildString {
    append("照护对象健康分类为「${category.label}」。")
    if (alerts.isEmpty()) {
        append("暂未同步到本地告警记录。")
    } else {
        val top = alerts.groupingBy { it.content.ifBlank { "其他" } }
            .eachCount().maxByOrNull { it.value }?.key.orEmpty()
        append("本地已同步告警 ${alerts.size} 条，最常见类型为「${top.ifBlank { "其他" }}」。")
    }
    bmi?.let { append(" BMI ${String.format(Locale.US, "%.1f", it)}，处于${bmiLabel(it)}范围。") }
    if (bpAvailable) {
        append(" 血压 ${profile.bloodPressureHigh}/${profile.bloodPressureLow} mmHg。")
    }
    if (profile.physicalExam.heartRate.isNotBlank()) {
        append(" 心率 ${profile.physicalExam.heartRate} 次/分。")
    }
    append(" 当前累计随访记录 ${followUpCount} 条、健康建议 ${adviceCount} 条。")
}
