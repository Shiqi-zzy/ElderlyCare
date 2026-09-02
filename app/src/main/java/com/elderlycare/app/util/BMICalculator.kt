package com.elderlycare.app.util

import kotlin.math.roundToInt

object BMICalculator {
    fun calculate(weightKg: Float, heightCm: Float): Float {
        if (weightKg <= 0 || heightCm <= 0) return 0f
        val heightM = heightCm / 100f
        return (weightKg / (heightM * heightM) * 10).roundToInt() / 10f
    }

    fun getBMICategory(bmi: Float): BMICategory {
        return when {
            bmi <= 0 -> BMICategory.UNKNOWN
            bmi < 18.5 -> BMICategory.UNDERWEIGHT
            bmi < 24.0 -> BMICategory.NORMAL
            bmi < 28.0 -> BMICategory.OVERWEIGHT
            else -> BMICategory.OBESE
        }
    }

    enum class BMICategory(val label: String, val color: Long) {
        UNKNOWN("--", 0xFF999999),
        UNDERWEIGHT("偏瘦", 0xFFF0C75E),
        NORMAL("正常", 0xFF6BBF7A),
        OVERWEIGHT("偏胖", 0xFFF0C75E),
        OBESE("肥胖", 0xFFE8857C)
    }
}
