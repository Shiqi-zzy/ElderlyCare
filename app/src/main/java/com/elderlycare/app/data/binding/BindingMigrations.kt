package com.elderlycare.app.data.binding

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * elderly_binding.db 迁移集合。
 * 升级必须写 Migration（BindingDatabase 只有降级兜底，无 destructive 升级）。
 */
object BindingMigrations {

    /**
     * v1→v2：organization 补机构展示 5 列；新增医院-社区绑定表 hospital_community_binding。
     * 列名/类型/NOT NULL/默认值必须与实体声明完全一致，否则 Room 升级校验崩溃。
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `organization` ADD COLUMN `contactPerson` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `organization` ADD COLUMN `contactPhone` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `organization` ADD COLUMN `address` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `organization` ADD COLUMN `serviceArea` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `organization` ADD COLUMN `intro` TEXT NOT NULL DEFAULT ''")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `hospital_community_binding` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `hospitalOrgId` TEXT NOT NULL,
                    `communityOrgId` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `applyReason` TEXT NOT NULL DEFAULT '',
                    `reviewedBy` TEXT,
                    `reviewNote` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL,
                    `reviewedAt` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hospital_community_binding_hospitalOrgId` " +
                    "ON `hospital_community_binding` (`hospitalOrgId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hospital_community_binding_communityOrgId` " +
                    "ON `hospital_community_binding` (`communityOrgId`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_hospital_community_binding_hospitalOrgId_communityOrgId` " +
                    "ON `hospital_community_binding` (`hospitalOrgId`, `communityOrgId`)"
            )
        }
    }
}
