package com.elderlycare.app.data.local

import android.content.Context
import android.util.Log
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
 *
 * 1:N 设计：每个档案带 userId 绑定家属（当前流程一人一档，列表结构已支持多档）。
 * 合并说明：原基础版本为 SharedPreferences 单档 save/load（commit 同步落盘，返回值是
 * 「本地保存成功才上报智能体记忆 / 才进首页」的门控）；合并后保留该语义——save()
 * 返回是否成功，调用方在协程中调用（FamilyWizardScreen 最终提交处）。
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

    /**
     * 保存档案（供 Wizard 最终提交用）；返回是否保存成功。
     * 保存成功之后调用方才执行智能体长期记忆上报 / 进入首页（保存失败不上报、不进首页）。
     */
    suspend fun save(profile: ElderlyProfile): Boolean = try {
        saveProfile(profile)
        true
    } catch (t: Throwable) {
        Log.e(TAG, "档案本地保存失败", t)
        false
    }

    companion object {
        private const val TAG = "ElderlyProfileStore"
    }
}
