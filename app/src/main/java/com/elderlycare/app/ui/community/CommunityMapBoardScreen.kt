package com.elderlycare.app.ui.community

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.incident.BuildingCell
import com.elderlycare.app.data.model.AppUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val BoardBg = Color(0xFFF2F6FC)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)
private val Blue = Color(0xFF3F8FE0)
private val Green = Color(0xFF52C41A)
private val Red = Color(0xFFEA4E4E)

/**
 * 社区网格大屏（虚拟 8 栋，非真实地图）：
 * 每栋显示责任网格员 / 老人数 / 未闭环事件数；该栋有进行中告警时方块红色闪烁。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityMapBoardScreen(
    onNavigateBack: () -> Unit,
    onOpenIncidents: () -> Unit
) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var grid by remember { mutableStateOf<List<BuildingCell>>(emptyList()) }

    LaunchedEffect(Unit) {
        staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
        val orgId = staff?.organizationId ?: "org_community_01"
        // 每 1.5s 重新聚合一次（事件状态变化即时反映到方块颜色）
        while (isActive) {
            val profiles = ServiceLocator.profileStore.getAllProfiles()
            grid = ServiceLocator.incidentRepository.buildBuildingGrid(orgId, profiles)
            delay(1500)
        }
    }

    // 全局闪烁相位（告警方块共用，节奏一致）
    val transition = rememberInfiniteTransition(label = "alarm")
    val phase by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "phase"
    )

    Scaffold(
        containerColor = BoardBg,
        topBar = {
            TopAppBar(
                title = { Text("社区网格大屏", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") }
                },
                colors = TopAppDefaults()
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            val alarmBuildings = grid.count { it.hasAlarm }
            // 概览条
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("幸福社区 · 8 栋网格", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            if (alarmBuildings > 0) "当前 $alarmBuildings 栋有进行中告警" else "全域运行正常",
                            color = if (alarmBuildings > 0) Red else Green, fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = onOpenIncidents) { Text("事件处置中心") }
                }
            }
            Spacer(Modifier.height(14.dp))

            // 4 行 × 2 列虚拟网格
            grid.chunked(2).forEach { rowCells ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowCells.forEach { cell ->
                        BuildingBlock(cell, phase, Modifier.weight(1f), onOpenIncidents)
                    }
                    if (rowCells.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            // 图例
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(Green, "正常")
                LegendDot(Blue, "已分配网格员")
                LegendDot(Red, "告警闪烁")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BuildingBlock(cell: BuildingCell, alarmPhase: Float, modifier: Modifier, onClick: () -> Unit) {
    val bg = when {
        cell.hasAlarm -> Red.copy(alpha = 0.18f + 0.5f * alarmPhase)
        cell.assigned -> Blue.copy(alpha = 0.08f)
        else -> Color(0xFFF0F2F6)
    }
    val borderColor = when {
        cell.hasAlarm -> Red.copy(alpha = alarmPhase)
        cell.assigned -> Blue.copy(alpha = 0.35f)
        else -> Color(0xFFDDE3EC)
    }
    Card(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = cell.hasAlarm, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${cell.buildingNo} 栋", color = Dark, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                if (cell.hasAlarm) Icon(Icons.Filled.Sensors, "告警", tint = Red, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, tint = Gray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(cell.staffName ?: "未分配", color = Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text("老人 ${cell.elderlyCount} 人", color = Gray, fontSize = 12.sp)
            if (cell.hasAlarm) {
                Spacer(Modifier.height(2.dp))
                Text("告警 ${cell.activeIncidentCount} 起 · 点击处置", color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LegendDot(c: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Gray, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppDefaults() = TopAppBarDefaults.topAppBarColors(
    containerColor = White,
    titleContentColor = Dark,
    navigationIconContentColor = Dark
)

/**
 * 工作台内嵌迷你网格大屏（2 行 × 4 列，紧凑版）：直接展示 8 栋状态，告警栋红色呼吸闪烁。
 * 点击卡片右上角「全屏」进入 [CommunityMapBoardScreen]，点击告警栋进入事件处置中心。
 */
@Composable
fun CommunityMiniGrid(onExpand: () -> Unit, onAlarmClick: () -> Unit) {
    var grid by remember { mutableStateOf<List<BuildingCell>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val s = ServiceLocator.staffUserStore.getCurrentStaffUser()
            val orgId = s?.organizationId ?: "org_community_01"
            val profiles = ServiceLocator.profileStore.getAllProfiles()
            grid = ServiceLocator.incidentRepository.buildBuildingGrid(orgId, profiles)
            delay(1500)
        }
    }
    val transition = rememberInfiniteTransition(label = "mini")
    val phase by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "minip")
    val alarmCount = grid.count { it.hasAlarm }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sensors, null, tint = Blue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("社区网格大屏", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    if (alarmCount > 0) "$alarmCount 栋告警" else "全域正常",
                    color = if (alarmCount > 0) Red else Green, fontSize = 11.sp
                )
                Spacer(Modifier.width(4.dp))
                Text("全屏 ›", color = Blue, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onExpand))
            }
            Spacer(Modifier.height(10.dp))
            grid.chunked(4).forEach { rowCells ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowCells.forEach { cell -> MiniBuilding(cell, phase, Modifier.weight(1f), onAlarmClick) }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun MiniBuilding(cell: BuildingCell, alarmPhase: Float, modifier: Modifier, onAlarmClick: () -> Unit) {
    val bg = when {
        cell.hasAlarm -> Red.copy(alpha = 0.16f + 0.5f * alarmPhase)
        cell.assigned -> Blue.copy(alpha = 0.08f)
        else -> Color(0xFFF0F2F6)
    }
    val bd = when {
        cell.hasAlarm -> Red.copy(alpha = alarmPhase)
        cell.assigned -> Blue.copy(alpha = 0.3f)
        else -> Color(0xFFDDE3EC)
    }
    Box(
        modifier
            .height(56.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.5.dp, bd, RoundedCornerShape(12.dp))
            .clickable(enabled = cell.hasAlarm, onClick = onAlarmClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text("${cell.buildingNo}栋", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                if (cell.hasAlarm) "告警${cell.activeIncidentCount}" else "${cell.elderlyCount}人",
                color = if (cell.hasAlarm) Red else Gray, fontSize = 10.sp
            )
        }
    }
}
