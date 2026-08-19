package com.elderlycare.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elderlycare.app.data.model.ElderlyProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 老人档案存储（DataStore + Gson JSON）。
 * 1:N 设计：每个档案带 userId 绑定家属（当前流程一人一档，列表结构已支持多档）。
 */
class ElderlyProfileStore(private val context: Context) {

    private val gson = Gson()
    private val profilesKey = stringPreferencesKey("profiles_json")
    private val profilesType = object : TypeToken<List<ElderlyProfile>>() {}.type

    private suspend fun readProfiles(): List<ElderlyProfile> {
        val json = context.familyDataStore.data.first()[profilesKey] ?: return emptyList()
        return runCatching { gson.fromJson<List<ElderlyProfile>>(json, profilesType) }
            .getOrElse { emptyList() }
    }

    private suspend fun writeProfiles(profiles: List<ElderlyProfile>) {
        context.familyDataStore.edit { it[profilesKey] = gson.toJson(profiles) }
    }

    suspend fun getProfilesByUser(userId: String): List<ElderlyProfile> =
        readProfiles().filter { it.userId == userId }

    suspend fun getPrimaryProfile(userId: String): ElderlyProfile? =
        getProfilesByUser(userId).firstOrNull()

    /** 全部老人档案（社区/医院枚举可申请绑定老人用）。 */
    suspend fun getAllProfiles(): List<ElderlyProfile> = readProfiles()

    /**
     * 老人档案实时流（DataStore 变更自动重发）。
     * 供社区/医院与绑定关系组合（第四阶段权限过滤）：家属修改档案后，观察方自动显示最新值。
     */
    fun observeProfiles(): Flow<List<ElderlyProfile>> =
        context.familyDataStore.data
            .map { prefs ->
                prefs[profilesKey]?.let { json ->
                    runCatching { gson.fromJson<List<ElderlyProfile>>(json, profilesType) }
                        .getOrElse { emptyList() }
                } ?: emptyList()
            }
            .distinctUntilChanged()

    /** 按 userId upsert（当前一人一档；列表结构已支持 1:N）。 */
    suspend fun saveProfile(profile: ElderlyProfile) {
        val profiles = readProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.userId == profile.userId }
        if (idx >= 0) profiles[idx] = profile else profiles.add(profile)
        writeProfiles(profiles)
    }

    suspend fun deleteProfile(userId: String) {
        writeProfiles(readProfiles().filter { it.userId != userId })
    }
}
