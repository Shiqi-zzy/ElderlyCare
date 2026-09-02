package com.elderlycare.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elderlycare.app.data.model.FamilyUser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 家属用户账号存储（DataStore + Gson JSON）。
 * 手机号唯一；保存当前登录态（退出登录仅清除登录态，不删除用户数据）。
 */
class FamilyUserStore(private val context: Context) {

    private val gson = Gson()
    private val usersKey = stringPreferencesKey("users_json")
    private val currentUserIdKey = stringPreferencesKey("current_user_id")
    private val usersType = object : TypeToken<List<FamilyUser>>() {}.type

    /** 当前登录用户 id（手机号），供启动会话判断 */
    val currentUserId: Flow<String?> = context.familyDataStore.data.map { it[currentUserIdKey] }

    private suspend fun readUsers(): List<FamilyUser> {
        val json = context.familyDataStore.data.first()[usersKey] ?: return emptyList()
        return runCatching { gson.fromJson<List<FamilyUser>>(json, usersType) }
            .getOrElse { emptyList() }
    }

    private suspend fun writeUsers(users: List<FamilyUser>) {
        context.familyDataStore.edit { it[usersKey] = gson.toJson(users) }
    }

    suspend fun getCurrentUserId(): String? =
        context.familyDataStore.data.first()[currentUserIdKey]

    suspend fun getCurrentUser(): FamilyUser? {
        val uid = getCurrentUserId() ?: return null
        return readUsers().firstOrNull { it.phone == uid }
    }

    suspend fun phoneExists(phone: String): Boolean =
        readUsers().any { it.phone == phone }

    /** 注册。手机号已存在返回 false。 */
    suspend fun register(user: FamilyUser): Boolean {
        val users = readUsers().toMutableList()
        if (users.any { it.phone == user.phone }) return false
        users.add(user)
        writeUsers(users)
        return true
    }

    /** 登录。手机号 + 密码匹配返回用户，否则 null。 */
    suspend fun login(phone: String, password: String): FamilyUser? =
        readUsers().firstOrNull { it.phone == phone && it.password == password }

    suspend fun setCurrentUser(phone: String) {
        context.familyDataStore.edit { it[currentUserIdKey] = phone }
    }

    suspend fun clearCurrentUser() {
        context.familyDataStore.edit { it.remove(currentUserIdKey) }
    }

    /** 更新用户信息（按手机号匹配）。 */
    suspend fun updateUser(user: FamilyUser) {
        val users = readUsers().toMutableList()
        val idx = users.indexOfFirst { it.phone == user.phone }
        if (idx >= 0) users[idx] = user
        writeUsers(users)
    }
}
