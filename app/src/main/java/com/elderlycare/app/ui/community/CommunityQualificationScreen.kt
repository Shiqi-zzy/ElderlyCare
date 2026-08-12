package com.elderlycare.app.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun CommunityQualificationScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("资质管理", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("网格员工作证", fontWeight = FontWeight.SemiBold); StatusBadge(text = "已认证", color = StatusGreen) }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("有效期至 2024-12-31 (剩余148天)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {}, shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.End)) { Text("续期申请") }
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("社区养老服务授权函", fontWeight = FontWeight.SemiBold); StatusBadge(text = "审核中", color = StatusYellow) }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("提交日期 2024-07-01 · 预计3个工作日内完成", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Secondary)) { Text("发起新绑定申请") }
            Button(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("查看绑定历史") }
        }
    }
}
