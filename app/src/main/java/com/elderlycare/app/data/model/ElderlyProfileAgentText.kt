package com.elderlycare.app.data.model

/**
 * 6 步表单实体 → 格式化老人画像纯文本（供 RK3 智能体长期记忆读取）。
 *
 * 本代码仅完成档案写入智能体长期记忆；
 * SOP工作流侧必须开启【读取长期记忆】开关，才能读取这份画像，
 * SOP逻辑不由本模块实现。（现在还没开启）
 *
 * 输出规整自然语言文本（非 JSON），符合隐私安全条例；空字段自动跳过，
 * 整节为空时输出「未填写」占位。
 */
fun ElderlyProfile.buildAgentMemoryText(agentId: String): String = buildString {
    appendLine("====老人档案画像====")

    // 【1.基础信息】
    section("【1.基础信息】：") {
        kv("姓名", name)
        kv("性别", gender.label)
        kv("年龄", age)
        kv("建档编号", archiveNumber)
        kv("身高(cm)", height)
        kv("体重(kg)", weight)
        kv("腰围(cm)", waist)
        kv("体温(℃)", temperature)
        kv("脉搏(次/分)", pulseRate)
        kv("呼吸(次/分)", respiration)
        if (bloodPressureHigh.isNotBlank() || bloodPressureLow.isNotBlank()) {
            appendLine("血压：${bloodPressureHigh.ifBlank { "-" }}/${bloodPressureLow.ifBlank { "-" }} mmHg")
        }
        kv("建档日期", examDate)
        kv("联系电话", phone)
        if (emergencyContactName.isNotBlank()) {
            kv("紧急联系人", if (emergencyContactPhone.isNotBlank()) {
                "$emergencyContactName（$emergencyContactPhone）"
            } else emergencyContactName)
        }
    }

    // 【2.生活习惯】
    section("【2.生活习惯】：") {
        kv("锻炼频率", exerciseFrequency.label)
        kv("每次锻炼时长", exerciseDuration)
        kv("锻炼年限", exerciseYears)
        kvList("锻炼项目", exerciseTypes)
        kv("饮食类型", dietType.label)
        kvList("饮食偏好", dietPreferences)
        kv("吸烟状况", smokingStatus.label)
        kv("每天吸烟支数", dailyCigarettes)
        kv("开始吸烟年龄", smokingStartAge)
        kv("戒烟年龄", quitSmokingAge)
        kv("饮酒频率", drinkingFrequency.label)
        kv("每天饮酒量", dailyAlcoholAmount)
        kv("开始饮酒年龄", drinkingStartAge)
        kv("是否已戒酒", if (hasQuitAlcohol) "是" else "否")
        kv("戒酒年龄", quitAlcoholAge)
        kv("过去一年是否醉酒", if (drunkInPastYear) "是" else "否")
        kvList("常饮酒类", alcoholTypes)
        if (occupationalExposures.isNotEmpty()) {
            appendLine("职业病有害接触：")
            occupationalExposures.forEach { e ->
                appendLine(
                    "· ${e.type}：${if (e.hasContact) "有接触" else "无接触"}" +
                        (if (e.workYears.isNotBlank()) "，接触年限 ${e.workYears} 年" else "") +
                        (if (e.hasContact) (if (e.hasProtection) "，有防护" else "，无防护") else "")
                )
            }
        }
    }

    // 【3.疾病史】
    section("【3.疾病史】：") {
        kvList("一月内症状", currentSymptoms)
        kvList("慢性病", chronicDiseases)
        kv("过敏史", allergyHistory)
        kv("精神情绪倾向病史", mentalHealthHistory)
        kv("认知下降记录", cognitiveDeclineRecord)
    }

    // 【4.体检记录】
    section("【4.体检记录】：") {
        kv("健康自评", healthSelfAssessment?.label)
        kv("生活自理能力", selfCareLevel?.let { "${it.label}（${it.score}）" })
        kv("认知筛查结果", cognitiveScreening?.label)
        kv("认知评分", cognitiveScore)
        kv("抑郁筛查结果", depressionScreening?.label)
        kv("抑郁评分", depressionScore)
        val exam = physicalExam
        kv("意识", exam.consciousness)
        kv("意识异常", exam.consciousnessAbnormal)
        kv("皮肤", exam.skin)
        kv("皮肤异常", exam.skinAbnormal)
        kv("巩膜", exam.sclera)
        kv("巩膜异常", exam.scleraAbnormal)
        kv("淋巴结", exam.lymphNodes)
        kv("淋巴结异常", exam.lymphNodesAbnormal)
        kv("左眼视力", exam.leftVision)
        kv("右眼视力", exam.rightVision)
        kv("矫正视力", exam.correctedVision)
        kv("眼底", exam.fundus)
        kv("眼底异常", exam.fundusAbnormal)
        kv("听力", exam.hearing)
        kv("口唇", exam.lipStatus)
        kv("缺齿", exam.missingTeeth)
        kv("龋齿", exam.decayedTeeth)
        kv("义齿", exam.dentures)
        kv("咽部", exam.pharynx)
        kv("胸廓", exam.chestShape)
        kv("呼吸音", exam.breathSounds)
        kv("啰音", exam.rales)
        kv("心率", exam.heartRate)
        kv("心律", exam.heartRhythm)
        kv("心脏杂音", exam.heartMurmur)
        kv("腹部压痛", exam.abdominalTenderness)
        kv("腹部肿块", exam.abdominalMass)
        kv("肝大", exam.liverEnlargement)
        kv("运动功能", exam.motorFunction)
    }

    // 【5.兴趣爱好】
    section("【5.兴趣爱好】：") {
        kvList("兴趣爱好", hobbies)
    }

    // 【6.设备信息】
    section("【6.设备信息】：") {
        kv("RK3设备序列号", deviceSn)
        kv("对应智能体ID", agentId)
    }

    appendLine("================")
}

// ===== 纯文本拼装工具 =====

/** 追加「标签：值」行（值为空跳过） */
private fun StringBuilder.kv(label: String, value: String?) {
    if (!value.isNullOrBlank()) appendLine("$label：$value")
}

/** 追加「标签：值1、值2」行（列表为空跳过） */
private fun StringBuilder.kvList(label: String, values: List<String>) {
    if (values.isNotEmpty()) appendLine("$label：${values.joinToString("、")}")
}

/** 追加小节：标题恒输出；小节内容全空时输出「未填写」占位 */
private fun StringBuilder.section(title: String, body: StringBuilder.() -> Unit) {
    appendLine(title)
    val inner = StringBuilder()
    inner.body()
    if (inner.isEmpty()) appendLine("未填写") else append(inner)
}
