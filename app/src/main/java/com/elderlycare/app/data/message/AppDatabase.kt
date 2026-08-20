package com.elderlycare.app.data.message

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.data.reminder.RemindPlanDao

/**
 * 应用数据库（留言模块新增）。
 *
 * 版本历史：
 * - v1：message 表（留言）
 * - v2：新增 remind_plan 表（提醒计划），见 AppMigrations.MIGRATION_1_2
 * 后续新增表/字段时在此处 +1 并写 Migration。
 * 注意：只有降级兜底（fallbackToDestructiveMigrationOnDowngrade），
 * 升级必须写 Migration，否则旧数据升级崩溃。
 */
@Database(
    entities = [MessageEntity::class, RemindPlanEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    abstract fun remindPlanDao(): RemindPlanDao

    companion object {
        private const val DB_NAME = "elderly_care.db"

        @Volatile
        private var instance: AppDatabase? = null

        /** 单例获取（数据库初始化也在调用线程完成，首次建议在子线程调用） */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // 留言操作全部为 suspend，允许主线程查询仅为降级保障
                    .fallbackToDestructiveMigrationOnDowngrade()
                    // 升级迁移（v1→v2 新增 remind_plan 表，保住 message 数据）
                    .addMigrations(AppMigrations.MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
