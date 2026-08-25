package com.elderlycare.app.data.reminder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 提醒计划实体（Room 表 remind_plan）。
 *
 * 业务链路：App 表单保存 → 萤石 REST api/v3/device/life/remind/clock 下发到 RK3
 * 设备本地（设备时钟 + 设备本地 TTS 到点播报），拿到的 clockId 与完整数据存本地；
 * 打开列表页/日程页时 clock/list 以设备为准覆盖同步；schedule/record 轮询识别
 * 「已播报完成」。
 *
 * 字段语义：
 * - clockId：萤石计划唯一标识（删除凭据、同步关联键）；空字符串 = 本地脏数据（未下发成功）
 * - repeatType：0=单次 1=每日 2=每周（App 本地语义；下发时映射为萤石 once/weekdays）
 * - weekdays：逗号分隔星期集合（0=周日…6=周六），如 "1,3,5"
 * - executed：客户端展示态（是否已播报完成），由 schedule/record 轮询标记；
 *   refreshFromDevice 覆盖同步后会重置，等下次轮询恢复
 * - enabled：恒 1，预留暂停能力（避免后续加字段再写 Migration）
 * - source：计划来源（v5 新增）0=家属端创建、1=医院端创建（仅 App 本地提醒，
 *   未下发设备）、2=医院端创建（已下发设备播报）。医院端计划以本地为准——
 *   差分同步清理脏行时跳过 source != 0（本地提醒行 clockId 为空，不随设备清删）
 */
@Entity(
    tableName = "remind_plan",
    indices = [Index(value = ["deviceSerial"]), Index(value = ["clockId"])]
)
data class RemindPlanEntity(
    /** 本地主键自增 */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 萤石计划 id（clockId），删除与同步凭据 */
    val clockId: String = "",

    /** 标题（萤石 tag，≤50 字） */
    val tag: String,

    /** 留言内容（设备播报文本，≤20 字含标点） */
    val content: String,

    /** 播报时间：时 0-23 */
    val timeHour: Int,

    /** 播报时间：分 0-59 */
    val timeMin: Int,

    /** 重复类型：0=单次 1=每日 2=每周 */
    val repeatType: Int,

    /** 星期集合（逗号分隔，0=周日…6=周六）；单次=日期对应星期，每日=全 7 天 */
    val weekdays: String,

    /** 单次计划的日期（重复计划为 0） */
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,

    /** 是否启用（恒 1，预留暂停能力） */
    val enabled: Int = 1,

    /** 是否已播报完成（0/1，客户端展示态，轮询标记） */
    val executed: Int = 0,

    /** 绑定设备序列号 */
    val deviceSerial: String,

    /** 本地创建时间戳（毫秒，列表/混合流排序用） */
    val createTime: Long,

    /**
     * 计划来源（v5 新增）：0=家属端创建、1=医院端创建（仅 App 本地提醒）、
     * 2=医院端创建（已下发设备播报）。
     * defaultValue 与 MIGRATION_4_5 的 ALTER 语句 DEFAULT 0 对齐。
     */
    @ColumnInfo(defaultValue = "0")
    val source: Int = SOURCE_FAMILY,

    /**
     * 复诊双重确认状态（v6 新增）：医院端设备播报计划（source=2）在
     * 家属确认前不下发设备。0=无确认流程（家属端计划/医院本地提醒）、
     * 1=待家属确认（已插本地闹钟兜底，未建 v3 clock）、2=已同意（clockId 回填，
     * RK3 到点播报）、3=已拒绝（到点仅 App 本地通知）。
     * defaultValue 与 MIGRATION_5_6 的 ALTER 语句 DEFAULT 0 对齐。
     */
    @ColumnInfo(defaultValue = "0")
    val confirmStatus: Int = CONFIRM_NONE
) {
    companion object {
        const val REPEAT_ONCE = 0    // 单次
        const val REPEAT_DAILY = 1   // 每日
        const val REPEAT_WEEKLY = 2  // 每周

        /** 是否已播报完成 */
        const val EXECUTED_NO = 0
        const val EXECUTED_YES = 1

        /** 计划来源：家属端创建（默认，行为不变） */
        const val SOURCE_FAMILY = 0
        /** 计划来源：医院端创建（仅 App 本地提醒，未下发设备） */
        const val SOURCE_HOSPITAL_LOCAL = 1
        /** 计划来源：医院端创建（已下发 RK3 设备播报） */
        const val SOURCE_HOSPITAL_DEVICE = 2

        /** 复诊确认状态：无确认流程（默认） */
        const val CONFIRM_NONE = 0
        /** 复诊确认状态：待家属确认（未下发设备，本地闹钟兜底） */
        const val CONFIRM_PENDING = 1
        /** 复诊确认状态：家属已同意（clockId 回填，RK3 到点播报） */
        const val CONFIRM_AGREED = 2
        /** 复诊确认状态：家属已拒绝（到点仅 App 本地通知） */
        const val CONFIRM_REJECTED = 3
    }
}
