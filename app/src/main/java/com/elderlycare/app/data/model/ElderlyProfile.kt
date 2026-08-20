package com.elderlycare.app.data.model

/**
 * 用户完整档案数据模型 — Wizard 6步共享状态
 */
data class ElderlyProfile(
    // 绑定家属用户（userId = 家属手机号），用于 1:N 关联
    val userId: String = "",
    // ① 基础身份与身体测量
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val age: String = "",
    val examDate: String = "",
    val archiveNumber: String = "",
    val height: String = "",
    val weight: String = "",
    val waist: String = "",
    val temperature: String = "",
    val pulseRate: String = "",
    val respiration: String = "",
    val bloodPressureHigh: String = "",
    val bloodPressureLow: String = "",
    val phone: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",

    // ② 生活习惯与作息
    val exerciseFrequency: ExerciseFrequency = ExerciseFrequency.NONE,
    val exerciseDuration: String = "",
    val exerciseYears: String = "",
    val exerciseTypes: List<String> = emptyList(),
    val dietType: DietType = DietType.BALANCED,
    val dietPreferences: List<String> = emptyList(), // 饮食偏好多选标签
    val smokingStatus: SmokingStatus = SmokingStatus.NEVER,
    val dailyCigarettes: String = "",
    val smokingStartAge: String = "",
    val quitSmokingAge: String = "",
    val drinkingFrequency: DrinkingFrequency = DrinkingFrequency.NEVER,
    val dailyAlcoholAmount: String = "",
    val drinkingStartAge: String = "",
    val hasQuitAlcohol: Boolean = false,
    val quitAlcoholAge: String = "",
    val drunkInPastYear: Boolean = false,
    val alcoholTypes: List<String> = emptyList(),
    val occupationalExposures: List<OccupationalExposure> = emptyList(),

    // ③ 既往症状与疾病史
    val privacyConsentGiven: Boolean = false, // 隐私授权
    val currentSymptoms: List<String> = emptyList(), // 当下躯体症状（一月内）
    val chronicDiseases: List<String> = emptyList(),
    val allergyHistory: String = "",
    val mentalHealthHistory: String = "", // 精神情绪倾向病史
    val cognitiveDeclineRecord: String = "",
    val medicalImages: List<String> = emptyList(), // 图片路径列表

    // ④ 身心认知抑郁评估
    val healthSelfAssessment: HealthSelfAssessment? = null,
    val selfCareLevel: SelfCareLevel? = null,
    val cognitiveScreening: ScreeningResult? = null,
    val cognitiveScore: String = "",
    val depressionScreening: ScreeningResult? = null,
    val depressionScore: String = "",

    // ⑤ 全身体检脏器记录
    val physicalExam: PhysicalExam = PhysicalExam(),

    // ⑥ 兴趣爱好 & RK3设备绑定
    val hobbies: List<String> = emptyList(),
    val deviceSn: String = "",
    val deviceValidateCode: String = "", // 设备验证码（6 位大写字母，真绑定用）
    val deviceBound: Boolean = false,
    val snInputMode: SnInputMode = SnInputMode.SCAN // SN 绑定方式
)

// ===== 枚举类型 =====

enum class Gender(val label: String) { MALE("男"), FEMALE("女") }

enum class ExerciseFrequency(val label: String) {
    EVERYDAY("每天"), WEEKLY_PLUS("每周1次以上"), OCCASIONALLY("偶尔"), NONE("不锻炼")
}

enum class DietType(val label: String) {
    BALANCED("荤素均衡"), MEAT_HEAVY("荤食为主"), VEGGIE_HEAVY("素食为主")
}

enum class SmokingStatus(val label: String) {
    NEVER("从不吸烟"), QUIT("已戒烟"), STILL("仍吸烟")
}

enum class DrinkingFrequency(val label: String) {
    NEVER("从不"), OCCASIONALLY("偶尔"), OFTEN("经常"), EVERYDAY("每天")
}

enum class HealthSelfAssessment(val label: String) {
    SATISFIED("满意"), BASICALLY_SATISFIED("基本满意"), UNCLEAR("说不清"),
    NOT_VERY_SATISFIED("不太满意"), DISSATISFIED("不满意")
}

enum class SelfCareLevel(val label: String, val score: String) {
    ABLE("可自理", "0~3分"),
    MILD_DEPENDENCE("轻度依赖", "4~8分"),
    MODERATE_DEPENDENCE("中度依赖", "9~18分"),
    UNABLE("完全不能自理", "≥19分")
}

enum class ScreeningResult(val label: String) { NEGATIVE("粗筛阴性"), POSITIVE("粗筛阳性") }

enum class SnInputMode(val label: String) { SCAN("扫码绑定"), MANUAL("手动输入") }

// ===== 饮食偏好预设标签 =====
val dietPreferenceOptions = listOf(
    "清淡饮食", "荤素均衡", "荤食为主", "素食为主", "杂粮为主",
    "果蔬充足", "嗜盐", "嗜油", "嗜糖", "嗜辣",
    "腌制食品偏好", "烧烤油炸偏好", "红肉过量", "加工肉制品偏好",
    "三餐规律", "三餐不规律", "暴饮暴食", "少食多餐", "爱吃夜宵",
    "软烂饮食", "流食为主", "常饮酒", "常饮甜饮料",
    "浓茶偏好", "咖啡偏好", "白开水为主"
)

// ===== 躯体症状预设（一月内） =====
val symptomOptions = listOf(
    "无症状", "头痛", "头晕", "心悸", "胸闷", "慢性咳嗽", "呼吸困难",
    "多饮多尿", "体重下降", "乏力", "关节肿痛", "视力模糊",
    "手脚麻木", "尿频", "便秘", "腹泻", "恶心呕吐", "眼花",
    "耳鸣", "乳房胀痛", "其他"
)

// ===== 慢性基础疾病预设 =====
val chronicDiseaseOptions = listOf(
    "高血压", "冠心病", "慢性阻塞性肺病", "糖尿病", "高血脂",
    "脑卒中", "溃疡病", "骨质疏松", "老年痴呆",
    "重度精神疾病", "肝炎", "其他慢病"
)

// ===== 兴趣爱好预设标签 =====
val presetHobbies = listOf(
    "戏曲", "书法", "国画", "广场舞", "太极", "八段锦",
    "养花", "种菜", "听书", "听广播", "正念冥想", "瑜伽",
    "钓鱼", "下棋", "麻将", "扑克", "慢跑散步", "骑行",
    "门球", "乒乓球", "合唱唱歌", "乐器演奏", "剪纸", "摄影",
    "养鸟养鱼", "短途旅游", "老友聚会", "茶艺品茶", "手工编织", "收藏",
    "追剧", "写随笔", "志愿活动", "徒步登山"
)

// ===== 职业病有害接触类型 =====
val exposureTypeOptions = listOf("粉尘", "放射物", "物理因素", "化学物质", "其他毒物")

// ===== 复杂数据类 =====

data class OccupationalExposure(
    val type: String = "",
    val hasContact: Boolean = false,
    val workYears: String = "",
    val hasProtection: Boolean = false
)

data class PhysicalExam(
    // 基础体表
    val consciousness: String = "",
    val consciousnessAbnormal: String = "",
    val skin: String = "",
    val skinAbnormal: String = "",
    val sclera: String = "",
    val scleraAbnormal: String = "",
    val lymphNodes: String = "",
    val lymphNodesAbnormal: String = "",
    // 五官 - 眼部
    val leftVision: String = "",
    val rightVision: String = "",
    val correctedVision: String = "",
    val fundus: String = "",
    val fundusAbnormal: String = "",
    // 五官 - 耳部
    val hearing: String = "",
    // 五官 - 口腔
    val lipStatus: String = "",
    val missingTeeth: String = "",
    val decayedTeeth: String = "",
    val dentures: String = "",
    val pharynx: String = "",
    // 胸肺心脏
    val chestShape: String = "",
    val breathSounds: String = "",
    val rales: String = "",
    val heartRate: String = "",
    val heartRhythm: String = "",
    val heartMurmur: String = "",
    // 腹部
    val abdominalTenderness: String = "",
    val abdominalMass: String = "",
    val liverEnlargement: String = "",
    // 运动功能
    val motorFunction: String = ""
)
