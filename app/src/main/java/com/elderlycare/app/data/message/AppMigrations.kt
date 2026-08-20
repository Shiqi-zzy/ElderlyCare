package com.elderlycare.app.data.message

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 迁移集合。
 *
 * 注意：AppDatabase 只有 fallbackToDestructiveMigrationOnDowngrade（降级兜底），
 * 没有普通 fallbackToDestructiveMigration——升级必须写 Migration，否则旧版本数据
 * 升级时直接崩溃。
 */
object AppMigrations {

    /** v1→v2：新增 remind_plan 表（提醒计划）+ deviceSerial/clockId 索引；message 表不动，数据保留 */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `remind_plan` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `clockId` TEXT NOT NULL,
                    `tag` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `timeHour` INTEGER NOT NULL,
                    `timeMin` INTEGER NOT NULL,
                    `repeatType` INTEGER NOT NULL,
                    `weekdays` TEXT NOT NULL,
                    `year` INTEGER NOT NULL,
                    `month` INTEGER NOT NULL,
                    `day` INTEGER NOT NULL,
                    `enabled` INTEGER NOT NULL,
                    `executed` INTEGER NOT NULL,
                    `deviceSerial` TEXT NOT NULL,
                    `createTime` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_remind_plan_deviceSerial` " +
                    "ON `remind_plan` (`deviceSerial`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_remind_plan_clockId` " +
                    "ON `remind_plan` (`clockId`)"
            )
        }
    }
}
