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

    /**
     * v5→v6：复诊双重确认——remind_plan 表新增 confirmStatus 列
     * （0=无确认流程/1=待家属确认/2=已同意/3=已拒绝，默认 0 老数据行为不变）。
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `remind_plan` ADD COLUMN `confirmStatus` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * v6→v7：社区端业务——新增 community_follow_up（社区随访）、staff_schedule（排班）、
     * service_record（服务记录）、todo_item（待办事项）四张表（SQL 来自 DEV 分支 v5→v6）。
     *
     * 防御处理：两支线在 v6 时 MIGRATION_5_6 语义不同（BASE=confirmStatus 列，DEV=本四张表），
     * 曾装过 DEV APK 的机子上 remind_plan 可能缺 confirmStatus——先 PRAGMA 检测再补列，
     * BASE v6 升上来的机子列已存在则跳过。表结构必须与对应实体声明完全一致。
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            var hasConfirmStatus = false
            db.query("PRAGMA table_info(`remind_plan`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "confirmStatus") {
                        hasConfirmStatus = true
                    }
                }
            }
            if (!hasConfirmStatus) {
                db.execSQL(
                    "ALTER TABLE `remind_plan` ADD COLUMN `confirmStatus` INTEGER NOT NULL DEFAULT 0"
                )
            }

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `community_follow_up` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `elderlyId` TEXT NOT NULL,
                    `elderlyName` TEXT NOT NULL,
                    `staffId` TEXT NOT NULL,
                    `followUpType` TEXT NOT NULL,
                    `scheduledTime` INTEGER NOT NULL,
                    `content` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_community_follow_up_elderlyId` ON `community_follow_up` (`elderlyId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_community_follow_up_staffId` ON `community_follow_up` (`staffId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_community_follow_up_scheduledTime` ON `community_follow_up` (`scheduledTime`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `staff_schedule` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `staffId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `scheduleDate` INTEGER NOT NULL,
                    `startTime` TEXT NOT NULL,
                    `endTime` TEXT NOT NULL,
                    `location` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_schedule_staffId` ON `staff_schedule` (`staffId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_schedule_scheduleDate` ON `staff_schedule` (`scheduleDate`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `service_record` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `staffId` TEXT NOT NULL,
                    `elderlyId` TEXT NOT NULL,
                    `elderlyName` TEXT NOT NULL,
                    `serviceType` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `durationMinutes` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_record_staffId` ON `service_record` (`staffId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_record_elderlyId` ON `service_record` (`elderlyId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_record_createdAt` ON `service_record` (`createdAt`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `todo_item` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `staffId` TEXT NOT NULL,
                    `elderlyId` TEXT NOT NULL,
                    `elderlyName` TEXT NOT NULL,
                    `todoType` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_item_staffId` ON `todo_item` (`staffId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_item_elderlyId` ON `todo_item` (`elderlyId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_item_status` ON `todo_item` (`status`)")
        }
    }
}
