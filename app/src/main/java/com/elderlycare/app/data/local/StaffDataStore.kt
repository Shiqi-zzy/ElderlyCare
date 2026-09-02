package com.elderlycare.app.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * 社区/医院工作人员共享 DataStore 实例（独立文件，与家属端 family_data 分离）。
 */
internal val Context.staffDataStore by preferencesDataStore(name = "staff_data")
