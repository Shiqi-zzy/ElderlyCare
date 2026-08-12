package com.ezvizpro.domain.model

/**
 * 预置位领域模型
 */
data class Preset(
    val index: Int,
    val name: String
)

/**
 * 云台控制方向
 */
enum class PtzDirection(val value: Int, val label: String) {
    UP(0, "上"),
    DOWN(1, "下"),
    LEFT(2, "左"),
    RIGHT(3, "右"),
    UP_LEFT(4, "左上"),
    DOWN_LEFT(5, "左下"),
    UP_RIGHT(6, "右上"),
    DOWN_RIGHT(7, "右下")
}

/**
 * 云台控制速度
 */
enum class PtzSpeed(val value: Int, val label: String) {
    SLOW(0, "慢"),
    NORMAL(1, "适中"),
    FAST(2, "快")
}
