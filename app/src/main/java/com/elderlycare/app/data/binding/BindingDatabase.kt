package com.elderlycare.app.data.binding

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 多端绑定关系数据库（与留言库 elderly_care.db 相互独立）。
 *
 * 只存关系数据四张表：organization / binding_request / user_elderly_binding / local_alert。
 * 账号不在此库（家属 FamilyUserStore、社区/医院 UserStore 均在 DataStore）。
 */
@Database(
    entities = [
        OrganizationEntity::class,
        BindingRequestEntity::class,
        UserElderlyBindingEntity::class,
        LocalAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BindingDatabase : RoomDatabase() {

    abstract fun bindingDao(): BindingDao

    companion object {
        private const val DB_NAME = "elderly_binding.db"

        @Volatile
        private var instance: BindingDatabase? = null

        /** 单例获取（首次建议在子线程调用） */
        fun getInstance(context: Context): BindingDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BindingDatabase::class.java,
                    DB_NAME
                )
                    // 关系数据均为 suspend/Flow 调用，降级保障主线程查询不崩
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
