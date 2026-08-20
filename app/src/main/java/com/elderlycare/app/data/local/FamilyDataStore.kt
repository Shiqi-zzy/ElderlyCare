package com.elderlycare.app.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * 家属端共享 DataStore 实例（家属账号 + 老人档案共用，避免多实例冲突）。
 */
internal val Context.familyDataStore by preferencesDataStore(name = "family_data")
