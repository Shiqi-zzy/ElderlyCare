package com.ezvizpro.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ezvizpro.data.local.room.dao.AlarmDao
import com.ezvizpro.data.local.room.dao.ElderlyDao
import com.ezvizpro.data.local.room.entity.AlarmEntity
import com.ezvizpro.data.local.room.entity.ElderlyEntity

/**
 * 智慧养老平台 Room 数据库
 *
 * 本地缓存：老人档案 + 告警记录（支持离线查看）
 */
@Database(
    entities = [ElderlyEntity::class, AlarmEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun elderlyDao(): ElderlyDao
    abstract fun alarmDao(): AlarmDao
}
