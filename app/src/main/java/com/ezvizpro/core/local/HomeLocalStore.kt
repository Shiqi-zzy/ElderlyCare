package com.ezvizpro.core.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore(name = "home_prefs")

data class FamilyMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val sender: String = "家人",
    val timestamp: Long = System.currentTimeMillis()
)

data class LifeReminder(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val type: ReminderType = ReminderType.OTHER,
    val time: String = "",       // HH:mm
    val enabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ReminderType(val label: String) {
    MEDICINE("吃药提醒"),
    EXERCISE("运动提醒"),
    WATER("喝水提醒"),
    APPOINTMENT("预约提醒"),
    OTHER("其他提醒")
}

@Singleton
class HomeLocalStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val messagesKey = stringPreferencesKey("family_messages")
    private val remindersKey = stringPreferencesKey("life_reminders")

    // ===== 家人留言 =====

    val messagesFlow: Flow<List<FamilyMessage>> = context.homeDataStore.data.map { prefs ->
        val raw = prefs[messagesKey] ?: "[]"
        try {
            json.decodeFromString<List<FamilyMessage>>(raw)
        } catch (e: Exception) {
            Timber.e(e, "解析留言数据失败")
            emptyList()
        }
    }

    suspend fun addMessage(message: FamilyMessage) {
        context.homeDataStore.edit { prefs ->
            val raw = prefs[messagesKey] ?: "[]"
            val list = try {
                json.decodeFromString<List<FamilyMessage>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            list.add(0, message) // 最新在最前
            prefs[messagesKey] = json.encodeToString(list.take(100)) // 最多100条
        }
        Timber.d("留言已保存: ${message.content.take(20)}")
    }

    suspend fun deleteMessage(id: Long) {
        context.homeDataStore.edit { prefs ->
            val raw = prefs[messagesKey] ?: "[]"
            val list = try {
                json.decodeFromString<List<FamilyMessage>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            list.removeAll { it.id == id }
            prefs[messagesKey] = json.encodeToString(list)
        }
    }

    // ===== 生活提醒 =====

    val remindersFlow: Flow<List<LifeReminder>> = context.homeDataStore.data.map { prefs ->
        val raw = prefs[remindersKey] ?: "[]"
        try {
            json.decodeFromString<List<LifeReminder>>(raw)
        } catch (e: Exception) {
            Timber.e(e, "解析提醒数据失败")
            emptyList()
        }
    }

    val hasMedicineReminder: Flow<Boolean> = remindersFlow.map { reminders ->
        reminders.any { it.type == ReminderType.MEDICINE && it.enabled }
    }

    suspend fun addReminder(reminder: LifeReminder) {
        context.homeDataStore.edit { prefs ->
            val raw = prefs[remindersKey] ?: "[]"
            val list = try {
                json.decodeFromString<List<LifeReminder>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            list.add(0, reminder)
            prefs[remindersKey] = json.encodeToString(list.take(50))
        }
        Timber.d("提醒已保存: ${reminder.title}")
    }

    suspend fun updateReminder(reminder: LifeReminder) {
        context.homeDataStore.edit { prefs ->
            val raw = prefs[remindersKey] ?: "[]"
            val list = try {
                json.decodeFromString<List<LifeReminder>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            val idx = list.indexOfFirst { it.id == reminder.id }
            if (idx >= 0) list[idx] = reminder
            prefs[remindersKey] = json.encodeToString(list)
        }
    }

    suspend fun deleteReminder(id: Long) {
        context.homeDataStore.edit { prefs ->
            val raw = prefs[remindersKey] ?: "[]"
            val list = try {
                json.decodeFromString<List<LifeReminder>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            list.removeAll { it.id == id }
            prefs[remindersKey] = json.encodeToString(list)
        }
    }
}
