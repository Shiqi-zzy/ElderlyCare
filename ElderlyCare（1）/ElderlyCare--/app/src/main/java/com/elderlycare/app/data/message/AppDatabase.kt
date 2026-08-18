package com.elderlycare.app.data.message

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 应用数据库（留言模块新增）。
 *
 * 注意：项目此前无任何数据库，本类为第一个 Room 数据库，version 从 1 开始。
 * 后续新增表/字段时在此处 +1 并写 Migration。
 */
@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

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
                    .build()
                    .also { instance = it }
            }
        }
    }
}
