package com.elderlycare.app.ui.community

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
import com.elderlycare.app.data.community.ServiceRecord
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/** 服务记录页色值（薄荷绿主题） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)

/** 社区端「服务记录」页面：永久记录的服务历史列表。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRecordScreen(onNavigateBack: () -> Unit) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val records by remember(staff?.phone) {
        if (staff != null) ServiceLocator.communityRepository.observeServiceRecords(staff!!.phone)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务记录", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(PageBg).padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("共 ${records.size} 条记录", color = TextGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("永久保留", color = TextHint, fontSize = 11.sp)
                }
            }
            if (records.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.History, null, tint = TextHint, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("暂无服务记录", color = TextHint, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("完成随访或告警处理后自动记录", color = TextHint, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            items(records, key = { it.id }) { record ->
                ServiceRecordCard(record = record)
            }
        }
    }
}

@Composable
private fun ServiceRecordCard(record: ServiceRecord) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(MintGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (record.serviceType) {
                            "上门随访" -> Icons.Filled.DirectionsWalk
                            "电话随访" -> Icons.Filled.Phone
                            "健康随访" -> Icons.Filled.Favorite
                            "告警消息" -> Icons.Filled.Warning
                            else -> Icons.Filled.Assignment
                        },
                        null,
                        tint = MintGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${record.serviceType} - ${record.elderlyName}", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(formatTimestamp(record.createdAt), color = TextGray, fontSize = 12.sp)
                }
                if (record.durationMinutes > 0) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MintGreen.copy(alpha = 0.1f)) {
                        Text("${record.durationMinutes}分钟", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(record.content, color = TextGray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
