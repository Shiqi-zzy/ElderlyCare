package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/**
 * 医院「健康档案」（第四阶段真实数据）。
 * 数据来源：bindingRepository.observeAccessibleElderly(当前医院工作人员)；
 * 患者列表 → 详情复用共享 UserDetailScreen（按 elderlyId 权限读取，展示真实 ElderlyProfile 字段）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalHealthRecordsScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToFollowUp: (String) -> Unit = {},
    onNavigateToAdvice: (String) -> Unit = {},
    onNavigateToReport: (String) -> Unit = {},
    onNavigateToMedicalRemind: () -> Unit = {}
) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        containerColor = Color(0xFFF8F7F2),
        topBar = {
            TopAppBar(
                title = { Text("健康档案", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    // 复诊提醒入口（照护对象页内选择，设备播报强制授权校验）
                    TextButton(onClick = onNavigateToMedicalRemind) {
                        Text(stringResource(R.string.hospital_records_remind), color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F7F2))
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 通栏 Banner 插画：铺满屏宽，仅裁切左右边缘，人物主体完整（仅装饰不承载数据）
            Image(
                painter = painterResource(R.drawable.img_hospital_measure),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("仅展示您已获授权访问的患者档案", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = Primary)
            }
            if (elderly.isEmpty()) {
                // 空状态：简约插画 + 引导文案
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.FolderOff,
                            contentDescription = null,
                            tint = TextHint.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("暂无可访问照护对象，可先在「资质管理」中发起绑定申请", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(elderly, key = { it.elderlyId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable(onClick = { onNavigateToDetail(item.elderlyId) }),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.profile.name, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${item.profile.age}岁 · ${item.profile.gender.label}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "慢病: ${item.profile.chronicDiseases.joinToString("、").ifEmpty { "暂无" }}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextHint
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "设备：${if (item.profile.hasDevice()) "已绑定" else "未绑定"} · 绑定时间 ${formatTimestamp(item.bindingCreatedAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHint
                                        )
                                    }
                                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextHint)
                                }
                                // 业务入口：随访 / 建议 / 报告（均按本照护对象 elderlyId 进入）
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { onNavigateToFollowUp(item.elderlyId) }) {
                                        Text(stringResource(R.string.hospital_records_follow_up), color = Primary)
                                    }
                                    TextButton(onClick = { onNavigateToAdvice(item.elderlyId) }) {
                                        Text(stringResource(R.string.hospital_records_advice), color = Primary)
                                    }
                                    TextButton(onClick = { onNavigateToReport(item.elderlyId) }) {
                                        Text(stringResource(R.string.hospital_records_report), color = Primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
