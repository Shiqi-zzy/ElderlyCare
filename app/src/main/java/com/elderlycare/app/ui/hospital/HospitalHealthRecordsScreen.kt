package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
fun HospitalHealthRecordsScreen(onNavigateToDetail: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("健康档案", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(shape = RoundedCornerShape(0.dp), color = Primary.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
                Text("医疗数据独立存储，与监控数据物理隔离", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = Primary)
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockPatients) { patient ->
                    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToDetail), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Text(patient.name, fontWeight = FontWeight.SemiBold); Spacer(modifier = Modifier.width(8.dp)); Text("${patient.age}岁 · ${patient.gender}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("慢病: ${patient.chronic}", style = MaterialTheme.typography.labelMedium, color = TextHint)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("上次随访: ${patient.lastVisit}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalHealthRecordDetailScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("健康档案详情", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface))
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("既往病史", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("高血压 (10年) · 高血脂 (5年) · 冠心病 (2年)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("过敏史: 青霉素过敏", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前用药", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("硝苯地平 30mg qd · 阿托伐他汀 20mg qn · 阿司匹林 100mg qd", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("随访记录", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("2024-07-01: 血压135/85，心率72，用药依从性良好", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("2024-04-01: 血压140/90，调整硝苯地平剂量至30mg", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("2024-01-05: 年度体检，总胆固醇5.2，低密度脂蛋白3.1", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Primary.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
                Text("医疗数据仅用于诊疗用途，不用于AI模型训练", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, color = TextHint)
            }
        }
    }
}

private data class PatientInfo(val name: String, val gender: String, val age: String, val chronic: String, val lastVisit: String)
private val mockPatients = listOf(
    PatientInfo("张**", "男", "72", "高血压、高血脂、冠心病", "2024-07-01"),
    PatientInfo("李**", "女", "68", "糖尿病、骨质疏松", "2024-06-15"),
    PatientInfo("王**", "男", "75", "冠心病、脑卒中后", "2024-05-20")
)
