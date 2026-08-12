package com.elderlycare.app.ui.hospital

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
fun HospitalQualificationScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("资质管理", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QualCard("医师执业资格证", "已认证", StatusGreen, "有效期至 2025-06-30", "到期年审: 2025-05-01前")
            QualCard("科室在岗证明", "已认证", StatusGreen, "有效期至 2024-12-31", "")
            QualCard("急救协作授权函", "审核中", StatusYellow, "提交日期 2024-07-01", "预计5个工作日")

            Spacer(modifier = Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("资质更新提醒", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("医师执业资格证将于 2025-05-01 到期年审，请提前准备年审材料", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {}, shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.End)) { Text("更新资质") }
                }
            }
        }
    }
}

@Composable
private fun QualCard(title: String, statusText: String, statusColor: androidx.compose.ui.graphics.Color, validity: String, note: String) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.SemiBold); StatusBadge(text = statusText, color = statusColor) }
            Spacer(modifier = Modifier.height(4.dp))
            Text(validity, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (note.isNotBlank()) { Spacer(modifier = Modifier.height(2.dp)); Text(note, style = MaterialTheme.typography.labelSmall, color = TextHint) }
        }
    }
}
