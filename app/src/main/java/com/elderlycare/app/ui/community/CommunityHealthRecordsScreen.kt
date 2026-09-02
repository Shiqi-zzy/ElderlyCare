package com.elderlycare.app.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.binding.BindingRepository.AccessibleElderlyUi
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/**
 * 社区「健康档案」（第四阶段新建）。
 * 数据来源：bindingRepository.observeAccessibleElderly(当前工作人员)；
 * 结构：照护对象列表 → 选择 → 档案详情（展示 ElderlyProfile 模型已有字段，不虚构）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHealthRecordsScreen() {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val selected = elderly.firstOrNull { it.elderlyId == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("健康档案", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface))
        }
    ) { paddingValues ->
        if (selected == null) {
            HealthRecordList(elderly, paddingValues) { selectedId = it }
        } else {
            HealthRecordDetail(selected.profile, paddingValues) { selectedId = null }
        }
    }
}

@Composable
private fun HealthRecordList(
    elderly: List<AccessibleElderlyUi>,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        if (elderly.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无可访问照护对象，可先在「资质管理」中发起绑定申请", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(elderly, key = { it.elderlyId }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item.elderlyId) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.profile.name, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(text = item.profile.healthCategory().label, color = item.profile.healthCategory().color())
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${item.profile.gender.label} · ${item.profile.age}岁 · ${item.profile.phone.ifEmpty { "未留电话" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "设备：${if (item.profile.hasDevice()) "已绑定" else "未绑定"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextHint
                                )
                            }
                            TextButton(onClick = { onSelect(item.elderlyId) }) { Text("查看档案", color = Primary) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthRecordDetail(profile: ElderlyProfile, paddingValues: androidx.compose.foundation.layout.PaddingValues, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("← 返回列表", color = Primary) }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("基本信息", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("姓名", profile.name)
                InfoRow("性别", profile.gender.label)
                InfoRow("年龄", profile.age)
                InfoRow("身高", profile.height)
                InfoRow("体重", profile.weight)
                InfoRow("联系电话", profile.phone)
                InfoRow("紧急联系人", listOf(profile.emergencyContactName, profile.emergencyContactPhone).filter { it.isNotBlank() }.joinToString(" · "))
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("健康档案", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("慢病", profile.chronicDiseases.joinToString("、").ifEmpty { "无" })
                InfoRow("过敏史", profile.allergyHistory.ifEmpty { "无" })
                InfoRow("当前症状", profile.currentSymptoms.joinToString("、").ifEmpty { "无" })
                if (profile.bloodPressureHigh.isNotBlank() && profile.bloodPressureLow.isNotBlank()) {
                    InfoRow("血压", "${profile.bloodPressureHigh}/${profile.bloodPressureLow} mmHg")
                }
                InfoRow("吸烟", profile.smokingStatus.label)
                InfoRow("饮酒", profile.drinkingFrequency.label)
                InfoRow("自理能力", profile.selfCareLevel?.label ?: "-")
                InfoRow("认知筛查", "${profile.cognitiveScreening?.label ?: "-"}" +
                    (if (profile.cognitiveScore.isNotBlank()) "（${profile.cognitiveScore}分）" else ""))
                InfoRow("抑郁筛查", "${profile.depressionScreening?.label ?: "-"}" +
                    (if (profile.depressionScore.isNotBlank()) "（${profile.depressionScore}分）" else ""))
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("设备状态", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("设备SN", profile.deviceSn.ifEmpty { "未绑定设备" })
                InfoRow("绑定状态", if (profile.deviceBound) "已绑定" else "未绑定")
            }
        }
    }
}
