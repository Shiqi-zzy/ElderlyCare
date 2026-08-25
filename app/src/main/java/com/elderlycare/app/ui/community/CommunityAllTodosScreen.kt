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
import com.elderlycare.app.data.community.TodoItem
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** 全部待办页色值（薄荷绿主题） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)
private val MetricRed = Color(0xFFF24848)
private val MetricOrange = Color(0xFFFF9F38)

/**
 * 社区端「全部待办事项」页面：显示所有待办（待处理+已完成），按老人分组，可设置完成状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityAllTodosScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0=待处理, 1=已完成

    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }
    val staffPhone = staff?.phone ?: ""

    val pendingTodos by remember(staffPhone) {
        if (staffPhone.isNotBlank()) ServiceLocator.communityRepository.observePendingTodos(staffPhone)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val allTodos by remember(staffPhone) {
        if (staffPhone.isNotBlank()) ServiceLocator.communityRepository.observeAllTodos(staffPhone)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val doneTodos = allTodos.filter { it.status == TodoItem.STATUS_DONE }
    val displayList = if (selectedTab == 0) pendingTodos else doneTodos

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部待办", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg)
                .padding(paddingValues)
        ) {
            // Tab 切换
            TabRow(selectedTabIndex = selectedTab, containerColor = CardWhite) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("待处理 (${pendingTodos.size})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("已完成 (${doneTodos.size})", fontSize = 13.sp) }
                )
            }

            if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Inbox, null, tint = TextHint, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(if (selectedTab == 0) "暂无待处理事项" else "暂无已完成事项", color = TextHint, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 按老人分组
                    val grouped = displayList.groupBy { it.elderlyId }
                    grouped.forEach { (elderlyId, elderTodos) ->
                        val elderName = elderTodos.first().elderlyName
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(MintGreen))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(elderName, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${elderTodos.size}项", color = TextHint, fontSize = 11.sp)
                            }
                        }
                        items(elderTodos, key = { it.id }) { todo ->
                            TodoDetailCard(
                                todo = todo,
                                showCompleteButton = selectedTab == 0,
                                onComplete = {
                                    scope.launch {
                                        staff?.let { s ->
                                            ServiceLocator.communityRepository.completeTodo(
                                                id = todo.id,
                                                staffId = s.phone,
                                                elderlyId = todo.elderlyId,
                                                elderlyName = todo.elderlyName,
                                                todoType = todo.todoType,
                                                content = todo.content
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }
                }
            }
        }
    }
}

/** 待办详情卡片 */
@Composable
private fun TodoDetailCard(todo: TodoItem, showCompleteButton: Boolean, onComplete: () -> Unit) {
    val isDone = todo.status == TodoItem.STATUS_DONE
    val iconBg = when (todo.priority) {
        TodoItem.PRIORITY_HIGH -> MetricRed
        else -> MetricOrange
    }
    val icon = when (todo.todoType) {
        "上门随访" -> Icons.Filled.DirectionsWalk
        "电话随访" -> Icons.Filled.Phone
        "健康随访" -> Icons.Filled.Favorite
        "告警消息" -> Icons.Filled.Warning
        else -> Icons.Filled.Assignment
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(iconBg.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconBg, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(todo.title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${todo.todoType} · ${formatTimestamp(todo.createdAt)}", color = TextGray, fontSize = 11.sp)
                }
                if (isDone) {
                    Surface(shape = RoundedCornerShape(8.dp), color = StatusGreen.copy(alpha = 0.1f)) {
                        Text("已完成", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                } else {
                    Surface(shape = RoundedCornerShape(8.dp), color = StatusYellow.copy(alpha = 0.1f)) {
                        Text("待处理", color = Color(0xFFD48806), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(todo.content, color = TextGray, fontSize = 12.sp, lineHeight = 18.sp)
            if (isDone && todo.completedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("完成时间：${formatTimestamp(todo.completedAt)}", color = TextHint, fontSize = 11.sp)
            }
            if (showCompleteButton) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("标记完成", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
