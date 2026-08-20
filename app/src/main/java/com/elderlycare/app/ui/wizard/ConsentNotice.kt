package com.elderlycare.app.ui.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.theme.TextSecondary

/**
 * 三端统一的「告知与授权」弹窗。
 * 信息录入完成后、进入门户前展示，需勾选同意后才能进入。
 */
@Composable
fun ConsentNotice(
    onAgree: () -> Unit,
    onDismiss: () -> Unit
) {
    var agreed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("告知与授权", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                NoticeItem("隐私合规", "姓名、证件号、联系方式等个人及健康信息仅加密存储于自研后端，不传输至第三方；监控画面按授权范围脱敏展示。")
                NoticeItem("数据用途", "采集的数据仅用于看护、告警处置与健康服务，不用于 AI 模型训练，不向无关机构完整推送。")
                NoticeItem("授权与提醒", "社区/医院人员仅在获授权范围内查看数据；急救等特殊场景按需开放临时权限。")
                NoticeItem("随时撤回", "您可随时在授权管理中撤回任一机构授权；删除档案后相关数据将按规销毁。")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                    Text(
                        "我已阅读并同意上述告知",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree, enabled = agreed) {
                Text("同意并进入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun NoticeItem(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(content, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
