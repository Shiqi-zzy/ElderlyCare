package com.elderlycare.app.ui.wizard.steps

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.SnInputMode
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/** 后端局域网同步状态（萤石云绑定成功 ≠ 后端同步成功，两状态分开展示）。 */
private enum class SyncState { IDLE, SYNCING, SUCCESS, FAILED }

/**
 * 设备绑定（档案录入第 6 步）。
 *
 * 绑定成功（SDK 校验通过）后必须调后端上报设备验证码（device_auth），
 * 上报成功才置 [onBackendSynced]（向导【完成】按钮据此放行）；同步请求 8s 超时，
 * 超时自动结束「同步中…」并展示失败文案，已绑定卡内提供「重试同步」。
 */
@Composable
fun Step7DeviceBinding(
    profile: ElderlyProfile,
    onUpdate: (ElderlyProfile) -> Unit,
    backendSynced: Boolean,
    onBackendSynced: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isBinding by remember { mutableStateOf(false) }
    var bindError by remember { mutableStateOf<String?>(null) }
    var syncState by remember { mutableStateOf(SyncState.IDLE) }
    var syncError by remember { mutableStateOf<String?>(null) }

    // 后端局域网同步（8s 超时由仓库层保证，超时自动结束「同步中…」并展示失败文案）
    suspend fun doSync(sn: String, code: String) {
        syncState = SyncState.SYNCING
        syncError = null
        ServiceLocator.captureRepository.uploadDeviceAuth(sn, code)
            .onSuccess {
                syncState = SyncState.SUCCESS
                onBackendSynced(true)
            }
            .onFailure { e ->
                syncState = SyncState.FAILED
                syncError = e.message
                onBackendSynced(false)
            }
    }

    // 已绑定但未同步：进入本页时静默补传一次（存量已绑定设备兜底，幂等 upsert；
    // 仅首次组合触发，避免与「绑定成功」分支的 doSync 重复请求）
    LaunchedEffect(Unit) {
        if (profile.deviceBound && !backendSynced &&
            profile.deviceSn.isNotBlank() && profile.deviceValidateCode.isNotBlank()
        ) {
            doSync(profile.deviceSn, profile.deviceValidateCode)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // === SN 绑定方式 ===
        SectionTitle("SN 绑定方式（二选一，必填）")

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "绑定萤石 RK3 适老桌面机器人，一台机器人对应一份档案",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (profile.deviceBound) {
                    // 已绑定状态：萤石云绑定成功 + 后端局域网同步，两状态分开展示
                    Column {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusGreen.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "SN: ${profile.deviceSn}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(text = "已绑定", color = StatusGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "RK3 适老桌面机器人",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        ServiceLocator.deviceBindingStore.clear()
                                        onBackendSynced(false)
                                        syncState = SyncState.IDLE
                                        syncError = null
                                        onUpdate(
                                            profile.copy(
                                                deviceSn = "",
                                                deviceValidateCode = "",
                                                deviceBound = false
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                                ) {
                                    Text("解绑")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // 双状态明细：萤石云绑定 ≠ 后端局域网同步
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "萤石云绑定",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    StatusBadge(text = "已绑定", color = StatusGreen)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "后端同步",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    when (syncState) {
                                        SyncState.SUCCESS -> StatusBadge(text = "已同步", color = StatusGreen)
                                        SyncState.SYNCING -> StatusBadge(text = "同步中…", color = StatusYellow)
                                        else -> StatusBadge(text = "未同步", color = Error)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "注意：手机热点可能存在网络隔离，优先使用普通路由器WiFi完成档案同步",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint
                        )

                        // 验证码尚未同步到后端（device_auth）→ 阻断向导，提供失败文案 + 重试（只重传不重调 addDevice）
                        if (!backendSynced) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (syncState == SyncState.FAILED) {
                                Text(
                                    text = if (syncError?.contains("超时") == true) {
                                        "同步失败，手机热点可能开启设备隔离，建议更换普通路由器WiFi重试"
                                    } else {
                                        "设备同步失败，请确认手机与RK3设备连接同一局域网WiFi后重试"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch { doSync(profile.deviceSn, profile.deviceValidateCode) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                enabled = syncState != SyncState.SYNCING
                            ) {
                                if (syncState == SyncState.SYNCING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("同步中…")
                                } else {
                                    Text("重试同步")
                                }
                            }
                        }
                    }
                } else {
                    // SN 绑定方式选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnInputMode.entries.forEach { mode ->
                            FilterChip(
                                selected = profile.snInputMode == mode,
                                onClick = { onUpdate(profile.copy(snInputMode = mode)) },
                                label = { Text(mode.label) },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (profile.snInputMode) {
                        SnInputMode.SCAN -> {
                            Button(
                                onClick = {
                                    // TODO: 调用相机扫描二维码（需引入 ZXing/MLKit）
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("扫描设备二维码", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "扫描 RK3 机身/包装盒二维码，自动解析填充 SN 码",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextHint
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = {
                                onUpdate(profile.copy(snInputMode = SnInputMode.MANUAL))
                            }) {
                                Text("扫码失败？切换手动输入", color = Primary)
                            }
                        }
                        SnInputMode.MANUAL -> {
                            OutlinedTextField(
                                value = profile.deviceSn,
                                onValueChange = { v ->
                                    val filtered = v.filter { it.isDigit() || it in 'A'..'Z' }
                                    if (filtered.length <= 20) {
                                        onUpdate(profile.copy(deviceSn = filtered))
                                    }
                                },
                                label = { Text("设备 SN 码") },
                                placeholder = { Text("填写设备序列号（机身/包装）") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = {
                                    Text(
                                        "仅允许数字 + 大写字母",
                                        color = TextHint
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = profile.deviceValidateCode,
                                onValueChange = { v ->
                                    val filtered = v.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(6)
                                    onUpdate(profile.copy(deviceValidateCode = filtered))
                                },
                                label = { Text("设备验证码") },
                                placeholder = { Text("6 位大写字母/数字（设备标签上）") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    keyboardType = KeyboardType.Ascii
                                ),
                                supportingText = {
                                    Text("设备标签上的 6 位大写字母或数字", color = TextHint)
                                }
                            )

                            if (bindError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bindError!!,
                                    color = Error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val sn = profile.deviceSn.trim()
                                    val code = profile.deviceValidateCode.trim()
                                    scope.launch {
                                        isBinding = true
                                        bindError = null
                                        when (val result = ServiceLocator.repository.addDevice(sn, code)) {
                                            is NetworkResult.Success -> {
                                                ServiceLocator.deviceBindingStore.save(sn, code, "RK3 设备")
                                                onUpdate(
                                                    profile.copy(
                                                        deviceSn = sn,
                                                        deviceValidateCode = code,
                                                        deviceBound = true
                                                    )
                                                )
                                                isBinding = false
                                                // SDK 校验成功 → 上报后端 device_auth（8s 超时）；成功才放行向导【完成】按钮
                                                doSync(sn, code)
                                            }
                                            is NetworkResult.Error -> {
                                                bindError = result.message
                                            }
                                        }
                                        isBinding = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = profile.deviceSn.length in 6..20 && profile.deviceValidateCode.length == 6 && !isBinding
                            ) {
                                if (isBinding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = OnPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("绑定中…")
                                } else {
                                    Text("绑定设备", modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = {
                                onUpdate(profile.copy(snInputMode = SnInputMode.SCAN))
                            }) {
                                Text("切换扫码绑定", color = Primary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // === 档案管理功能 ===
        SectionTitle("档案管理")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedButton(
                    onClick = {
                        // TODO: 导航到完整编辑页
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("修改全部档案信息", modifier = Modifier.padding(vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onUpdate(profile.copy(medicalImages = emptyList()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusYellow)
                ) {
                    Text("清空所有病历图片", modifier = Modifier.padding(vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        // TODO: 二次确认弹窗
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                ) {
                    Text("删除本用户全部档案", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // === 隐私合规说明 ===
        SectionTitle("隐私合规说明")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "1. 用户基础信息、病史、情绪倾向评估数据仅存储于自研后端加密数据库，不传输至萤石云端；萤石平台仅接收设备 AI 输出的情绪分值、活动统计脱敏数值；",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "2. 所有健康数据仅用于家属查看情绪倾向报告、生成正念提醒日程，不用于 AI 模型训练、不向社区/第三方完整推送病历原图；",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "3. 用户随时可撤回授权、删除全部用户档案，删除后 7 日内彻底销毁所有本地、云端数据。",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
