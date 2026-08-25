package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.shared.HealthCategory
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/** 全部急救事件页色值（深青绿主题） */
private val PageBg = Color(0xFFF5F9F8)
private val TealDark = Color(0xFF2A9D8F)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E2A)
private val TextGray = Color(0xFF6B7C78)
private val TextHint = Color(0xFF9AA8A4)

/**
 * 医院端「全部急救事件」页面：显示全部绑定患者，异常优先排序，点击进入详情。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalAllEventsScreen(onNavigateBack: () -> Unit, onUserClick: (String) -> Unit = {}) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // 异常老人优先排序
    val sortedElderly = elderly.sortedByDescending { it.profile.healthCategory() == HealthCategory.ABNORMAL }
    val abnormalCount = elderly.count { it.profile.healthCategory() == HealthCategory.ABNORMAL }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部急救事件", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("共 ${sortedElderly.size} 位患者", color = TextGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    if (abnormalCount > 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = StatusRed.copy(alpha = 0.1f)) {
                            Text("异常 $abnormalCount 人", color = StatusRed, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            if (sortedElderly.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Inbox, null, tint = TextHint, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("暂无可访问患者", color = TextHint, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            items(sortedElderly, key = { it.elderlyId }) { item ->
                EmergencyPatientCard(
                    name = item.profile.name,
                    age = item.profile.age,
                    gender = item.profile.gender.label,
                    orgName = item.orgName,
                    badgeText = item.profile.healthCategory().label,
                    badgeColor = item.profile.healthCategory().color(),
                    deviceText = "设备：${if (item.profile.hasDevice()) "已绑定" else "未绑定"} · ${formatTimestamp(item.bindingCreatedAt)}",
                    onClick = { onUserClick(item.elderlyId) }
                )
            }
        }
    }
}
