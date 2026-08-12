package com.elderlycare.app.ui.wizard.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.SnInputMode
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

@Composable
fun Step7DeviceBinding(profile: ElderlyProfile, onUpdate: (ElderlyProfile) -> Unit) {
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
                    // 已绑定状态
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
                                    StatusBadge(text = "在线", color = StatusGreen)
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
                                    onUpdate(profile.copy(deviceSn = "", deviceBound = false))
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                            ) {
                                Text("解绑")
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
                                    // TODO: 调用相机扫描二维码
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
                                    if (filtered.length <= 16) {
                                        onUpdate(profile.copy(deviceSn = filtered))
                                    }
                                },
                                label = { Text("设备 SN 码") },
                                placeholder = { Text("手动填写设备 16 位序列号") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = {
                                    Text(
                                        "仅允许数字 + 大写字母，长度 16 位",
                                        color = TextHint
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (profile.deviceSn.isNotBlank() && profile.deviceSn.length == 16) {
                                        onUpdate(profile.copy(deviceBound = true))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = profile.deviceSn.length == 16
                            ) {
                                Text("绑定设备", modifier = Modifier.padding(vertical = 4.dp))
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
                    Text("删除本长者全部档案", modifier = Modifier.padding(vertical = 4.dp))
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
                    "1. 长者基础信息、病史、情绪倾向评估数据仅存储于自研后端加密数据库，不传输至萤石云端；萤石平台仅接收设备 AI 输出的情绪分值、活动统计脱敏数值；",
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
                    "3. 用户随时可撤回授权、删除全部长者档案，删除后 7 日内彻底销毁所有本地、云端数据。",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
