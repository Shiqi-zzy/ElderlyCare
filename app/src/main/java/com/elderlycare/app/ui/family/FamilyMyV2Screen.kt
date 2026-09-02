package com.elderlycare.app.ui.family

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.incident.IncidentStatus
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.FamilyUser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

// ==================== 家属端「我的 V2」浅蓝政务风色值 ====================
private val PageBg = Color(0xFFF5F8FD)
private val BlueMain = Color(0xFF3F8FE0)
private val BlueLight = Color(0xFF7DB8F2)
private val CardWhite = Color.White
private val TextDark = Color(0xFF16243A)
private val TextGray = Color(0xFF5E6B80)
private val TextHint = Color(0xFF9AA7BA)
private val AlarmRed = Color(0xFFE85A5A)

private val MetricBlue = Color(0xFF3F8FE0)
private val MetricTeal = Color(0xFF33B5B0)
private val MetricOrange = Color(0xFFF2994A)
private val MetricPurple = Color(0xFF8B7AE8)

/**
 * 家属端「我的」V2：仿社区/医院端结构——蓝色信息头 + 指标卡 + 方形宫格 + 条状列表。
 *
 * 保留既有已实现功能（编辑档案 / 我的设备 / 授权管理），新增：
 * - 我的社区 / 我的医院（基础信息 + 对应服务记录，方便联系）
 * - 处置记录（本次跌倒事件四端进度时间线）
 * - 模拟跌倒告警（演示 RK3 触发 → 四端联动全流程）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMyV2Screen(
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToDevice: () -> Unit,
    onNavigateToAuthorizationMgmt: () -> Unit,
    onNavigateToCareCommunity: () -> Unit,
    onNavigateToCareHospital: () -> Unit,
    onNavigateToIncidents: () -> Unit,
    onOpenMessagesTab: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<FamilyUser?>(null) }
    var profiles by remember { mutableStateOf<List<ElderlyProfile>>(emptyList()) }
    LaunchedEffect(Unit) {
        user = ServiceLocator.userStore.getCurrentUser()
        val uid = ServiceLocator.userStore.getCurrentUserId() ?: ""
        profiles = ServiceLocator.profileStore.getProfilesByUser(uid)
    }
    val elderlyIds = profiles.map { it.userId }.ifEmpty { listOf(user?.phone ?: "") }

    // 指标：事件数 / 服务记录数（实时流）
    val incidents by remember(elderlyIds) {
        ServiceLocator.incidentRepository.observeByFamily(elderlyIds)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val serviceRecords by remember(elderlyIds.first()) {
        if (elderlyIds.first().isNotBlank())
            ServiceLocator.incidentRepository.observeElderlyServiceRecords(elderlyIds.first())
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var showSimDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
    ) {
        // ===== 蓝色渐变信息头 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(BlueMain, BlueLight)))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Filled.Notifications, null, tint = Color.White,
                    modifier = Modifier.size(22.dp).clickable { onOpenMessagesTab() }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text((user?.name ?: "家").take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.name ?: "未登录", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    val elder = profiles.firstOrNull()
                    Text(
                        elder?.let { "关爱老人：${it.name} · ${it.buildingNo.ifBlank { "未填楼栋" }}栋${it.roomNo}" }
                            ?: "尚未建立老人档案",
                        color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("家属端", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        // ===== 指标卡上浮 =====
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-12).dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                FamMetric(Icons.Filled.FavoriteBorder, MetricRed(MetricOrange), "${incidents.size}", "关爱事件", Modifier.weight(1f))
                FamMetric(Icons.Filled.Assignment, MetricRed(MetricTeal), "${serviceRecords.size}", "服务记录", Modifier.weight(1f))
                val active = incidents.count { !IncidentStatus.isTerminal(it.status) }
                FamMetric(Icons.Filled.Schedule, MetricRed(MetricBlue), "$active", "处置中", Modifier.weight(1f))
                FamMetric(Icons.Filled.CheckCircle, MetricRed(MetricPurple),
                    "${incidents.count { IncidentStatus.isTerminal(it.status) }}", "已闭环", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(4.dp))

        // ===== 模拟跌倒告警（演示入口，醒目条状）=====
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable {
                    if (profiles.isEmpty()) toast("请先为老人建立档案并填写居住楼栋")
                    else showSimDialog = true
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC)),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(AlarmRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Sensors, "模拟跌倒", tint = AlarmRed, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("模拟 RK3 跌倒告警", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("一键触发，演示家属/社区/医院四端联动", color = TextGray, fontSize = 12.sp)
                }
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = AlarmRed)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 方形宫格（我的服务）=====
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("我的服务", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FamGrid(Icons.Filled.Badge, "编辑档案", BlueMain, onNavigateToProfileEdit)
                    FamGrid(Icons.Filled.Videocam, "我的设备", BlueMain, onNavigateToDevice)
                    FamGrid(Icons.Filled.VerifiedUser, "授权管理", BlueMain, onNavigateToAuthorizationMgmt)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FamGrid(Icons.Filled.AccountBalance, "我的社区", MetricTeal, onNavigateToCareCommunity)
                    FamGrid(Icons.Filled.LocalHospital, "我的医院", MetricRed(MetricPurple), onNavigateToCareHospital)
                    FamGrid(Icons.Filled.Timeline, "处置记录", MetricOrange, onNavigateToIncidents)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 条状列表 =====
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column {
                FamRow(Icons.Filled.HelpOutline, "帮助中心", isLast = false) { toast("帮助中心") }
                FamRow(Icons.Filled.Info, "关于我们", isLast = true) { toast("萤视 Pro 智慧养老 · 四端协同演示") }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AlarmRed, contentColor = Color.White)
        ) { Text("退出登录", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(24.dp))
    }

    // ===== 模拟跌倒：选择老人 + 确认 =====
    if (showSimDialog) {
        var picked by remember { mutableStateOf(profiles.firstOrNull()) }
        AlertDialog(
            onDismissRequest = { showSimDialog = false },
            title = { Text("模拟 RK3 跌倒告警") },
            text = {
                Column {
                    Text("选择要触发告警的老人：", color = TextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    profiles.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { picked = p }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = picked?.userId == p.userId, onClick = { picked = p })
                            Spacer(Modifier.width(4.dp))
                            Text("${p.name}（${p.buildingNo}栋${p.roomNo}）", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = picked ?: return@TextButton
                    scope.launch {
                        val id = ServiceLocator.incidentRepository.simulateFall(target)
                        toast("已触发跌倒事件 #$id，社区网格员与合作医院将同步收到")
                        showSimDialog = false
                        onNavigateToIncidents()
                    }
                }) { Text("触发告警", color = AlarmRed) }
            },
            dismissButton = { TextButton(onClick = { showSimDialog = false }) { Text("取消") } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前家属账号吗？") },
            confirmButton = { TextButton(onClick = { showLogoutDialog = false; onLogout() }) { Text("退出", color = AlarmRed) } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun FamMetric(icon: ImageVector, iconBg: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(iconBg.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconBg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextGray, fontSize = 11.sp)
    }
}

/** Color 透传辅助（保持调用处书写统一） */
private fun MetricRed(c: Color): Color = c

@Composable
private fun FamGrid(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp).clickable(onClick = onClick)
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = TextDark, fontSize = 12.sp)
    }
}

@Composable
private fun FamRow(icon: ImageVector, label: String, isLast: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = CardWhite) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(BlueMain.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = BlueMain, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Text(label, color = TextDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint, modifier = Modifier.size(20.dp))
            }
            if (!isLast) HorizontalDivider(color = Color(0xFFEDF1F7), thickness = 0.5.dp, modifier = Modifier.padding(start = 60.dp))
        }
    }
}
