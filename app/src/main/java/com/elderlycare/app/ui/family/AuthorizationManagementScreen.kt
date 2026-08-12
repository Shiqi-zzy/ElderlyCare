package com.elderlycare.app.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizationManagementScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("授权管理", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("当前授权", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) }
            items(mockAuthorizations) { auth ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(auth.orgName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            StatusBadge(text = auth.status, color = if (auth.status == "生效中") StatusGreen else StatusYellow)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("授权范围: ${auth.scope}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("有效期: ${auth.startDate} 至 ${auth.expiryDate}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = {}, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Error), modifier = Modifier.align(Alignment.End)) { Text("一键撤销") }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text("授权历史", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = TextHint) }
            items(mockHistory) { h ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceVariant)) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) { Text(h.org, style = MaterialTheme.typography.bodyMedium); Text(h.date, style = MaterialTheme.typography.labelSmall, color = TextHint) }
                        Text(h.action, color = TextHint, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private data class AuthInfo(val orgName: String, val scope: String, val startDate: String, val expiryDate: String, val status: String)
private data class HistoryInfo(val org: String, val date: String, val action: String)

private val mockAuthorizations = listOf(
    AuthInfo("幸福社区养老驿站", "脱敏基础信息、告警通知、工单处理", "2024-01-01", "2024-12-31", "生效中"),
    AuthInfo("新华社区医院", "健康档案、用药记录、随访记录", "2024-03-15", "2024-09-15", "生效中")
)
private val mockHistory = listOf(
    HistoryInfo("阳光养老驿站", "2023-12-31", "已过期"),
    HistoryInfo("城北社区医院", "2023-06-30", "家属已撤销")
)
