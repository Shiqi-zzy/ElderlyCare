package com.elderlycare.app.ui.ezviz

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.model.FmGroup
import com.elderlycare.app.data.ezviz.model.FmStation
import com.elderlycare.app.ui.theme.*

/**
 * RK3 广播FM页（网络电台，平台内置电台 ID，不支持自定义外部 URL）。
 *
 * 参照萤石云视频 App 广播 UI：顶部设备状态栏 + 推荐/国家广播/地方广播分组列表 + 停止按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Rk3FmScreen(
    deviceSerial: String,
    onBackClick: () -> Unit,
    viewModel: Rk3FmViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // toast 提示（一次性消费）
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            msg?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.consumeToast()
            }
        }
    }

    LaunchedEffect(deviceSerial) {
        viewModel.initialize(deviceSerial)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.rk3_fm_title), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.rk3_fm_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.rk3_fm_title))
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.rk3_mock_switch),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = uiState.mockEnabled,
                            onCheckedChange = { viewModel.setMockEnabled(it) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 顶部 RK3 设备状态栏
            item(key = "device_bar") {
                DeviceStatusBar(
                    deviceSerial = deviceSerial,
                    currentStation = uiState.currentStation,
                    isPlaying = uiState.isPlaying,
                    onStopClick = viewModel::onStopClick
                )
            }
            if (uiState.stations.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (uiState.mockEnabled) "暂无电台" else stringResource(R.string.rk3_api_pending),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // 分组：推荐 / 国家广播 / 地方广播
                FmGroup.entries.forEach { group ->
                    val stations = uiState.stations[group].orEmpty()
                    if (stations.isNotEmpty()) {
                        item(key = "header_${group.name}") {
                            Text(
                                groupLabel(group),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        // key 须含分组名：推荐组与国家级广播组存在相同 fmId（fm-cnr-01 等），
                        // 同一 LazyColumn 内 key 重复会直接崩溃（Key "fm_fm-cnr-01" was already used）
                        items(stations, key = { "fm_${group.name}_${it.fmId}" }) { station ->
                            FmStationRow(
                                station = station,
                                isPlaying = uiState.currentStation?.fmId == station.fmId && uiState.isPlaying,
                                onClick = { viewModel.onStationClick(station) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 「请先绑定RK3设备」弹窗（设备校验）
    if (uiState.showBindDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBindDialog,
            title = { Text(stringResource(R.string.rk3_fm_title)) },
            text = { Text(stringResource(R.string.rk3_bind_required)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissBindDialog) {
                    Text("知道了")
                }
            }
        )
    }
}

/** 顶部 RK3 设备状态栏：设备 SN + 播放状态 + 停止按钮 */
@Composable
private fun DeviceStatusBar(
    deviceSerial: String,
    currentStation: FmStation?,
    isPlaying: Boolean,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Surface)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.rk3_fm_device_bar, deviceSerial),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isPlaying && currentStation != null)
                        stringResource(R.string.rk3_fm_now_playing, currentStation.name)
                    else stringResource(R.string.rk3_play_state_idle),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) Primary else TextSecondary
                )
            }
            TextButton(onClick = onStopClick, enabled = isPlaying) {
                Text(
                    stringResource(R.string.rk3_stop),
                    color = if (isPlaying) Primary else TextHint
                )
            }
        }
    }
}

/** 电台行：图标 + 名称 + 播放中标识 */
@Composable
private fun FmStationRow(
    station: FmStation,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Primary.copy(alpha = 0.08f) else Surface
        ),
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = if (isPlaying) Primary else TextHint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                station.name,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isPlaying) {
                Surface(shape = RoundedCornerShape(6.dp), color = Primary) {
                    Text(
                        stringResource(R.string.rk3_play_state_playing),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Surface
                    )
                }
            }
        }
    }
}

@Composable
private fun groupLabel(group: FmGroup): String = when (group) {
    FmGroup.RECOMMEND -> stringResource(R.string.rk3_fm_group_recommend)
    FmGroup.NATIONAL -> stringResource(R.string.rk3_fm_group_national)
    FmGroup.LOCAL -> stringResource(R.string.rk3_fm_group_local)
}
