package com.elderlycare.app.ui.hospital

import androidx.compose.runtime.Composable
import com.elderlycare.app.ui.shared.StaffMyScreen

/** 医院端「我的」Tab（薄封装，复用共享 [StaffMyScreen]）。 */
@Composable
fun HospitalMyScreen(onLogout: () -> Unit) {
    StaffMyScreen(onLogout)
}
