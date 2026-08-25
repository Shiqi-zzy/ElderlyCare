package com.elderlycare.app.ui.community

import androidx.compose.runtime.Composable
import com.elderlycare.app.ui.shared.StaffMyScreen

/** 社区端「我的」Tab（薄封装，复用共享 [StaffMyScreen]）。 */
@Composable
fun CommunityMyScreen(
    onLogout: () -> Unit,
    onNavigateToBinding: () -> Unit = {},
    onNavigateToFollowUp: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToServiceRecord: () -> Unit = {}
) {
    StaffMyScreen(
        onLogout = onLogout,
        onNavigateToBinding = onNavigateToBinding,
        onNavigateToFollowUp = onNavigateToFollowUp,
        onNavigateToSchedule = onNavigateToSchedule,
        onNavigateToServiceRecord = onNavigateToServiceRecord
    )
}
