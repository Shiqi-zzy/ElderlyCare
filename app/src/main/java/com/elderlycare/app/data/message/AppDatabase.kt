package com.elderlycare.app.data.message

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elderlycare.app.data.community.CommunityDao
import com.elderlycare.app.data.community.CommunityFollowUpRecord
import com.elderlycare.app.data.community.ServiceRecord
import com.elderlycare.app.data.community.StaffScheduleRecord
import com.elderlycare.app.data.community.TodoItem
import com.elderlycare.app.data.hospital.HealthAdvice
import com.elderlycare.app.data.hospital.HealthAdviceDao
import com.elderlycare.app.data.hospital.MedicalFollowUpDao
import com.elderlycare.app.data.hospital.MedicalFollowUpRecord
import com.elderlycare.app.data.incident.DoctorPenaltyEntity
import com.elderlycare.app.data.incident.IncidentDao
import com.elderlycare.app.data.incident.IncidentEntity
import com.elderlycare.app.data.incident.IncidentMigrations
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.data.reminder.RemindPlanDao

/**
 * 应用数据库（留言模块新增，文件名 elderly_care.db）。
 *
 * 版本历史：
 * - v1：message 表（留言）
 * - v2：新增 remind_plan 表（提醒计划），见 AppMigrations.MIGRATION_1_2
 * - v3：message 表新增 localVideoPath/videoCloudUrl/thumbUrl/messageCategory 四列
 * - v4：移除 v3 初版创建的 remoteId 部分唯一索引
 * - v5：医院端业务——medical_follow_up_record、health_advice；remind_plan.source
 * - v6：remind_plan.confirmStatus（复诊双重确认）
 * - v7：社区端业务——community_follow_up、staff_schedule、service_record、todo_item
 * - v8：四端协同——incident、doctor_penalty；service_record/todo_item/staff_schedule 扩展列，
 *   见 IncidentMigrations.MIGRATION_7_8
 * 后续新增表/字段时在此处 +1 并写 Migration。
 * 注意：只有降级兜底（fallbackToDestructiveMigrationOnDowngrade），升级必须写 Migration。
 */
@Database(
    entities = [
        MessageEntity::class,
        RemindPlanEntity::class,
        MedicalFollowUpRecord::class,
        HealthAdvice::class,
        CommunityFollowUpRecord::class,
        StaffScheduleRecord::class,
        ServiceRecord::class,
        TodoItem::class,
        IncidentEntity::class,
        DoctorPenaltyEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    abstract fun remindPlanDao(): RemindPlanDao

    abstract fun medicalFollowUpDao(): MedicalFollowUpDao

    abstract fun healthAdviceDao(): HealthAdviceDao

    abstract fun communityDao(): CommunityDao

    abstract fun incidentDao(): IncidentDao

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
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addMigrations(
                        AppMigrations.MIGRATION_1_2,
                        AppMigrations.MIGRATION_2_3,
                        AppMigrations.MIGRATION_3_4,
                        AppMigrations.MIGRATION_4_5,
                        AppMigrations.MIGRATION_5_6,
                        AppMigrations.MIGRATION_6_7,
                        IncidentMigrations.MIGRATION_7_8
                    )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
