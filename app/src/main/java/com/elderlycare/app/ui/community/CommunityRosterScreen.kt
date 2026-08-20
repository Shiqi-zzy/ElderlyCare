package com.elderlycare.app.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/**
 * 社区「用户台账」（第四阶段真实数据）。
 * 数据来源：bindingRepository.observeAccessibleElderly(当前工作人员) —— 仅本人 ACTIVE 绑定的老人，
 * 点击传入真实 elderlyId（非脱敏名）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityRosterScreen(onNavigateToDetail: (String) -> Unit) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("用户台账", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface))
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(shape = RoundedCornerShape(0.dp), color = StatusYellow.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                Text("仅展示您已获授权访问的老人", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            if (elderly.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无可访问老人，可先在「资质管理」中发起绑定申请", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(elderly, key = { it.elderlyId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetail(item.elderlyId) },
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
                                        "设备：${if (item.profile.hasDevice()) "已绑定" else "未绑定"} · 绑定时间 ${formatTimestamp(item.bindingCreatedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextHint
                                    )
                                }
                                TextButton(onClick = { onNavigateToDetail(item.elderlyId) }) { Text("查看档案", color = Primary) }
                            }
                        }
                    }
                }
            }
        }
    }
}
