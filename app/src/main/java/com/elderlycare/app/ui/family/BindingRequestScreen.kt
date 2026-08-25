package com.elderlycare.app.ui.family

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.R
import com.elderlycare.app.data.binding.BindingRepository.RequestUi
import com.elderlycare.app.data.binding.BindingStatus
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * 家属端「绑定申请」：查看社区/医院发起的绑定申请并审核（待处理 / 已同意 / 已拒绝 三栏）。
 * 数据经 BindingRepository.observeIncomingRequests 实时观察，审批后由 Room 失效自动刷新。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BindingRequestScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var familyUserId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        familyUserId = ServiceLocator.userStore.getCurrentUserId()
    }
    val uid = familyUserId
    val requestsFlow = remember(uid) {
        if (uid != null) ServiceLocator.bindingRepository.observeIncomingRequests(uid)
        else flowOf(emptyList())
    }
    val requests by requestsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("待处理", "已同意", "已拒绝")
    // 审批失败提示（同意/拒绝 出错时行内展示）
    var actionError by remember { mutableStateOf<String?>(null) }
    // 审批处理中（按申请 id 禁用该卡按钮 + 转圈，防重复提交）
    var decidingId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("绑定申请", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i; actionError = null },
                        text = { Text(t, fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                }
            }
            val filtered = when (tabs[selectedTab]) {
                "待处理" -> requests.filter { it.status == BindingStatus.PENDING.name }
                "已同意" -> requests.filter { it.status == BindingStatus.APPROVED.name }
                else -> requests.filter { it.status == BindingStatus.REJECTED.name }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (actionError != null) {
                    item {
                        Text(actionError!!, color = Error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        EmptyHint(
                            when (tabs[selectedTab]) {
                                "待处理" -> "暂无待处理的绑定申请"
                                "已同意" -> "暂无已同意的申请"
                                else -> "暂无已拒绝的申请"
                            }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { req ->
                        if (req.status == BindingStatus.PENDING.name) {
                            PendingRequestCard(req, deciding = decidingId == req.id) { approve ->
                                decidingId = req.id
                                scope.launch {
                                    actionError = if (approve) {
                                        ServiceLocator.bindingRepository.approve(req.id, uid ?: "")
                                    } else {
                                        ServiceLocator.bindingRepository.reject(req.id, uid ?: "")
                                    }
                                    decidingId = null
                                }
                            }
                        } else {
                            ReviewedRequestCard(req)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRequestCard(req: RequestUi, deciding: Boolean, onDecide: (approve: Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(req.orgName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                StatusBadge(text = BindingStatus.PENDING.label, color = StatusYellow)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("${req.orgTypeLabel} · ${req.requesterName} · ${req.requesterUserId}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("家人：${req.elderlyName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (req.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("申请说明：${req.message}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("申请时间：${formatTimestamp(req.createdAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onDecide(true) },
                    enabled = !deciding,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    if (deciding) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("同意")
                }
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedButton(
                    onClick = { onDecide(false) },
                    enabled = !deciding,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                ) { Text("拒绝") }
            }
        }
    }
}

/** 列表空态：插图 + 提示文案（与授权管理页统一） */
@Composable
private fun EmptyHint(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_state),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextHint)
    }
}

@Composable
private fun ReviewedRequestCard(req: RequestUi) {
    val approved = req.status == BindingStatus.APPROVED.name
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(req.orgName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                StatusBadge(text = if (approved) BindingStatus.APPROVED.label else BindingStatus.REJECTED.label, color = if (approved) StatusGreen else Error)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("${req.orgTypeLabel} · ${req.requesterName} · ${req.requesterUserId}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("家人：${req.elderlyName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("申请时间：${formatTimestamp(req.createdAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
            Text("审核时间：${formatTimestamp(req.reviewedAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
        }
    }
}
