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

    /**
     * v2→v3：message 表新增消息分类相关 4 列（历史数据 messageCategory 默认 1=留言，行为不变）。
     * 注意：v3 初版曾在此创建 remoteId 部分唯一索引，但 Room 实体不支持声明部分索引
     * （@Entity indices 无 WHERE 语法），升级校验索引集合必然不一致而崩溃——v4 已移除，
     * 幂等去重改由仓库层 dedupMutex 串行化 + getByRemoteId 预查 + insertIgnore 兜底。
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `message` ADD COLUMN `localVideoPath` TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE `message` ADD COLUMN `videoCloudUrl` TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE `message` ADD COLUMN `thumbUrl` TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE `message` ADD COLUMN `messageCategory` INTEGER NOT NULL DEFAULT 1"
            )
        }
    }

    /**
     * v3→v4：移除 v3 初版创建的 remoteId 部分唯一索引（仅早期测试机短暂存在该索引）。
     * Room 升级校验要求实体声明与实际索引集合完全一致，而 @Entity 不支持部分索引
     * （无 WHERE 语法），必须删掉该索引才能通过校验；幂等去重由仓库层保证：
     * dedupMutex 串行化拉取落库 + getByRemoteId 预查 + insertIgnore 兜底。
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_message_remoteId_unique`")
        }
    }

    /**
     * v4→v5：医院端业务——新增医疗随访记录表 medical_follow_up_record、
     * 健康建议表 health_advice；remind_plan 表新增 source 列
     * （0=家属/1=医院本地提醒/2=医院设备播报，默认 0 老数据行为不变）。
     * 表结构必须与 MedicalFollowUpRecord / HealthAdvice 实体声明完全一致
     * （列名/类型/NOT NULL/默认值），否则 Room 升级校验崩溃。
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `medical_follow_up_record` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `elderlyId` TEXT NOT NULL,
                    `followUpTime` INTEGER NOT NULL,
                    `content` TEXT NOT NULL,
                    `status` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_medical_follow_up_record_elderlyId` " +
                    "ON `medical_follow_up_record` (`elderlyId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_medical_follow_up_record_followUpTime` " +
                    "ON `medical_follow_up_record` (`followUpTime`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `health_advice` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `elderlyId` TEXT NOT NULL,
                    `adviceTime` INTEGER NOT NULL,
                    `adviceContent` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_health_advice_elderlyId` " +
                    "ON `health_advice` (`elderlyId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_health_advice_adviceTime` " +
                    "ON `health_advice` (`adviceTime`)"
            )
            db.execSQL(
                "ALTER TABLE `remind_plan` ADD COLUMN `source` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
}
