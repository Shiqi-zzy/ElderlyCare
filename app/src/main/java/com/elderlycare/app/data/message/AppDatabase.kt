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
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.data.reminder.RemindPlanDao

/**
 * 应用数据库（留言模块新增）。
 *
 * 版本历史：
 * - v1：message 表（留言）
 * - v2：新增 remind_plan 表（提醒计划），见 AppMigrations.MIGRATION_1_2
 * - v3：message 表新增 localVideoPath/videoCloudUrl/thumbUrl/messageCategory 四列
 *   （消息分类汇总），见 AppMigrations.MIGRATION_2_3
 * - v4：移除 v3 初版创建的 remoteId 部分唯一索引（Room 不支持声明部分索引，
 *   升级校验索引集合不一致会崩溃），见 AppMigrations.MIGRATION_3_4
 * - v5：医院端业务——新增 medical_follow_up_record（医疗随访）、health_advice
 *   （健康建议）两张表；remind_plan 表新增 source 列（0=家属/1=医院本地提醒/
 *   2=医院设备播报），见 AppMigrations.MIGRATION_4_5
 * - v6：社区端业务——新增 community_follow_up（社区随访）、staff_schedule（排班）、
 *   service_record（服务记录）、todo_item（待办事项）四张表，见 AppMigrations.MIGRATION_5_6
 * 后续新增表/字段时在此处 +1 并写 Migration。
 * 注意：只有降级兜底（fallbackToDestructiveMigrationOnDowngrade），
 * 升级必须写 Migration，否则旧数据升级崩溃。
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
        TodoItem::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    abstract fun remindPlanDao(): RemindPlanDao

    abstract fun medicalFollowUpDao(): MedicalFollowUpDao

    abstract fun healthAdviceDao(): HealthAdviceDao

    abstract fun communityDao(): CommunityDao

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
                    // 升级迁移（v1→v2 remind_plan 表；v2→v3 消息分类 4 列；
                    // v3→v4 移除部分唯一索引；v4→v5 医院端随访/建议表 + remind_plan.source）
                    .addMigrations(
                        AppMigrations.MIGRATION_1_2,
                        AppMigrations.MIGRATION_2_3,
                        AppMigrations.MIGRATION_3_4,
                        AppMigrations.MIGRATION_4_5,
                        AppMigrations.MIGRATION_5_6
                    )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
