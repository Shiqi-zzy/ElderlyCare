package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
fun HospitalFollowUpOrdersScreen(onUserClick: (String) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("待随访", "已完成", "已逾期")

    Scaffold(
        topBar = { TopAppBar(title = { Text("随访工单", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) },
        floatingActionButton = { FloatingActionButton(onClick = {}, containerColor = Primary, contentColor = OnPrimary, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Filled.Add, "新建随访") } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary) {
                tabs.forEachIndexed { i, t -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t, fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal) }) }
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockFollowUps.filter { it.status == tabs[selectedTab] }) { fu ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onUserClick(fu.patient.substringBefore(" (")) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(fu.type, fontWeight = FontWeight.SemiBold); StatusBadge(text = fu.status, color = if (fu.status == "已逾期") StatusRed else if (fu.status == "待随访") StatusYellow else StatusGreen) }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${fu.patient} · ${fu.date}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (fu.status == "待随访") Button(onClick = {}, shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.End), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { Text("开始随访") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalFollowUpDetailScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("随访详情", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("出院随访 - 张**", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("出院日期: 2024-06-15", style = MaterialTheme.typography.bodyMedium)
                    Text("随访计划: 出院后1周、1月、3月", style = MaterialTheme.typography.bodyMedium)
                    Text("当前状态: 第2次随访 (出院后1月)", style = MaterialTheme.typography.bodyMedium, color = Primary)
                }
            }
        }
    }
}

private data class FollowUpInfo(val type: String, val patient: String, val date: String, val status: String)
private val mockFollowUps = listOf(
    FollowUpInfo("出院随访", "张** (72岁)", "2024-07-10", "待随访"),
    FollowUpInfo("慢病随访", "李** (68岁)", "2024-07-08", "待随访"),
    FollowUpInfo("术后随访", "王** (75岁)", "2024-06-30", "已完成"),
    FollowUpInfo("出院随访", "赵** (81岁)", "2024-06-20", "已逾期")
)
