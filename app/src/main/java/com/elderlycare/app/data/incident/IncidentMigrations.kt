package com.elderlycare.app.data.incident

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * elderly_care.db v7→v8：四端协同紧急事件处置。
 *
 * - 旧表加列：service_record(+6)、todo_item(+2)、staff_schedule(+3)
 * - 新表：incident（事件状态机 + 11 时间戳）、doctor_penalty（值班漏接处罚）
 *
 * 列名/类型/NOT NULL/默认值/索引必须与 IncidentEntity、DoctorPenaltyEntity
 * 及扩展后的 ServiceRecord / TodoItem / StaffScheduleRecord 完全一致，否则 Room 升级校验崩溃。
 */
object IncidentMigrations {

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // service_record +6
            db.execSQL("ALTER TABLE `service_record` ADD COLUMN `side` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `service_record` ADD COLUMN `incidentId` INTEGER")
            db.execSQL("ALTER TABLE `service_record` ADD COLUMN `startedAt` INTEGER")
            db.execSQL("ALTER TABLE `service_record` ADD COLUMN `finishedAt` INTEGER")
            db.execSQL("ALTER TABLE `service_record` ADD COLUMN `staffName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `service_record` ADD COLUMN `treatment` TEXT NOT NULL DEFAULT ''")
            // todo_item +2
            db.execSQL("ALTER TABLE `todo_item` ADD COLUMN `incidentId` INTEGER")
            db.execSQL("ALTER TABLE `todo_item` ADD COLUMN `todoSubType` TEXT NOT NULL DEFAULT ''")
            // staff_schedule +3
            db.execSQL("ALTER TABLE `staff_schedule` ADD COLUMN `scheduleMode` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `staff_schedule` ADD COLUMN `weekday` INTEGER NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE `staff_schedule` ADD COLUMN `role` TEXT NOT NULL DEFAULT 'community'")

            // incident
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `incident` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `incidentNo` TEXT NOT NULL,
                    `alarmId` TEXT NOT NULL DEFAULT '',
                    `elderlyId` TEXT NOT NULL,
                    `elderlyName` TEXT NOT NULL,
                    `buildingNo` TEXT NOT NULL,
                    `unitNo` TEXT NOT NULL DEFAULT '',
                    `roomNo` TEXT NOT NULL DEFAULT '',
                    `level` TEXT NOT NULL DEFAULT 'HIGH',
                    `communityOrgId` TEXT NOT NULL,
                    `communityStaffId` TEXT NOT NULL,
                    `hospitalOrgId` TEXT,
                    `onDutyDoctorIds` TEXT NOT NULL DEFAULT '[]',
                    `missedDoctorIds` TEXT NOT NULL DEFAULT '[]',
                    `hospitalDoctorId` TEXT,
                    `escalatedToDoctorId` TEXT,
                    `status` TEXT NOT NULL,
                    `urgentCount` INTEGER NOT NULL DEFAULT 0,
                    `triggeredAt` INTEGER NOT NULL,
                    `communityReceivedAt` INTEGER,
                    `familyContactedAt` INTEGER,
                    `dispatchRequestedAt` INTEGER,
                    `hospitalReceivedAt` INTEGER,
                    `urgentLastAt` INTEGER,
                    `escalatedAt` INTEGER,
                    `hospitalAcceptedAt` INTEGER,
                    `hospitalDoneAt` INTEGER,
                    `communityDoneAt` INTEGER,
                    `closedAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `familyNote` TEXT NOT NULL DEFAULT '',
                    `hospitalTreatment` TEXT NOT NULL DEFAULT '',
                    `communityNote` TEXT NOT NULL DEFAULT '',
                    `communityRecordId` INTEGER,
                    `hospitalRecordId` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_incident_communityStaffId` ON `incident` (`communityStaffId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_incident_hospitalOrgId` ON `incident` (`hospitalOrgId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_incident_elderlyId` ON `incident` (`elderlyId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_incident_buildingNo` ON `incident` (`buildingNo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_incident_status` ON `incident` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_incident_hospitalOrgId_status` ON `incident` (`hospitalOrgId`, `status`)")

            // doctor_penalty
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `doctor_penalty` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `incidentId` INTEGER NOT NULL,
                    `doctorId` TEXT NOT NULL,
                    `doctorName` TEXT NOT NULL,
                    `hospitalOrgId` TEXT NOT NULL,
                    `penaltyType` TEXT NOT NULL,
                    `urgentCountAtMiss` INTEGER NOT NULL,
                    `level` TEXT NOT NULL,
                    `scoreDelta` INTEGER NOT NULL,
                    `status` TEXT NOT NULL DEFAULT 'active',
                    `createdAt` INTEGER NOT NULL,
                    `revokedAt` INTEGER,
                    `revokeReason` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_doctor_penalty_doctorId` ON `doctor_penalty` (`doctorId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_doctor_penalty_incidentId` ON `doctor_penalty` (`incidentId`)")
        }
    }
}
