package com.elderlycare.app.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun CommunityWorkOrdersScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("待处理", "处理中", "已完成")

    Scaffold(
        topBar = { TopAppBar(title = { Text("工单中心", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) },
        floatingActionButton = { FloatingActionButton(onClick = {}, containerColor = Secondary, contentColor = OnSecondary, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Filled.Add, "新建工单") } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary) {
                tabs.forEachIndexed { i, t -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t, fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal) }) }
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockOrders.filter { it.status == tabs[selectedTab] }) { order ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(order.type, fontWeight = FontWeight.SemiBold); StatusBadge(text = order.priority, color = if (order.priority == "紧急") StatusRed else StatusYellow) }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${order.elderly} · ${order.address}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(order.time, style = MaterialTheme.typography.labelSmall, color = TextHint)
                                when (order.status) {
                                    "待处理" -> Button(onClick = {}, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { Text("接单") }
                                    "处理中" -> OutlinedButton(onClick = {}, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { Text("完成流转") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class OrderInfo(val type: String, val elderly: String, val address: String, val time: String, val priority: String, val status: String)
private val mockOrders = listOf(
    OrderInfo("跌倒告警上门", "张**", "3号楼", "15:30", "紧急", "待处理"),
    OrderInfo("摄像头故障检修", "李**", "5号楼", "10:00", "普通", "处理中"),
    OrderInfo("高龄老人巡检", "王**", "1号楼", "14:00", "普通", "待处理"),
    OrderInfo("跌倒告警上门", "赵**", "2号楼", "09:15", "紧急", "已完成")
)
