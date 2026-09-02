package com.elderlycare.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 社区/医院工作人员账号存储（DataStore + Gson JSON，独立 `staff_data`）。
 *
 * 仅管理 COMMUNITY / HOSPITAL 角色账号；家属账号归 [FamilyUserStore]（`family_data`）。
 * 手机号唯一；退出登录只清当前员工登录态，不删除账号数据。
 */
class UserStore(private val context: Context) {

    private val gson = Gson()
    private val staffUsersKey = stringPreferencesKey("staff_users_json")
    private val currentStaffIdKey = stringPreferencesKey("current_staff_id")
    private val staffUsersType = object : TypeToken<List<AppUser>>() {}.type

    /** 当前登录员工 id（手机号），供启动会话判断与实时刷新 */
    val currentStaffId: Flow<String?> = context.staffDataStore.data.map { it[currentStaffIdKey] }

    private suspend fun readStaff(): List<AppUser> {
        val json = context.staffDataStore.data.first()[staffUsersKey] ?: return emptyList()
        return runCatching { gson.fromJson<List<AppUser>>(json, staffUsersType) }
            .getOrElse { emptyList() }
    }

    private suspend fun writeStaff(users: List<AppUser>) {
        context.staffDataStore.edit { it[staffUsersKey] = gson.toJson(users) }
    }

    suspend fun getCurrentStaffId(): String? =
        context.staffDataStore.data.first()[currentStaffIdKey]

    /** 当前登录员工账号 */
    suspend fun getCurrentStaffUser(): AppUser? {
        val id = getCurrentStaffId() ?: return null
        return readStaff().firstOrNull { it.phone == id }
    }

    suspend fun getStaffByPhone(phone: String): AppUser? =
        readStaff().firstOrNull { it.phone == phone }

    suspend fun getUsersByRole(role: UserRole): List<AppUser> =
        readStaff().filter { it.role == role }

    /**
     * 注册工作人员。
     * 仅接受 COMMUNITY / HOSPITAL（FAMILY 走 FamilyUserStore，不允许写入 staff_data）；
     * 手机号已存在返回 false。
     */
    suspend fun register(user: AppUser): Boolean {
        if (user.role == UserRole.FAMILY) return false
        val users = readStaff().toMutableList()
        if (users.any { it.phone == user.phone }) return false
        users.add(user)
        writeStaff(users)
        return true
    }

    /**
     * 登录。手机号 + 密码匹配返回用户，否则 null。
     * @param role 限定角色（社区登录传 COMMUNITY、医院传 HOSPITAL），null 则不限制。
     */
    suspend fun login(phone: String, password: String, role: UserRole? = null): AppUser? =
        readStaff().firstOrNull {
            it.phone == phone && it.password == password &&
                (role == null || it.role == role)
        }

    /** 设置当前登录员工（手机号）。 */
    suspend fun setCurrentStaff(phone: String) {
        context.staffDataStore.edit { it[currentStaffIdKey] = phone }
    }

    /** 退出登录：只清登录态，不删除账号数据。 */
    suspend fun clearCurrentStaff() {
        context.staffDataStore.edit { it.remove(currentStaffIdKey) }
    }

    /** 更新工作人员信息（按手机号匹配）。 */
    suspend fun updateUser(user: AppUser) {
        val users = readStaff().toMutableList()
        val idx = users.indexOfFirst { it.phone == user.phone }
        if (idx >= 0) users[idx] = user
        writeStaff(users)
    }
}
