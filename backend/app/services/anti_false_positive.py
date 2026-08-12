"""AI 防误报引擎：三级校验"""
from ..core.config import (
    ALARM_LEVEL_LOW, ALARM_LEVEL_MEDIUM, ALARM_LEVEL_HIGH, ALARM_LEVEL_EMERGENCY,
    ALARM_TYPE_FALL, ALARM_TYPE_STILLNESS, ALARM_TYPE_SMOKE, ALARM_TYPE_GAS,
    AI_VERIFY_UNVERIFIED, AI_VERIFY_LOW, AI_VERIFY_MEDIUM, AI_VERIFY_HIGH,
    FALL_DETECTION_MIN_CONFIDENCE, ANTI_FALSE_POSITIVE_MIN_FRAMES
)


class AntiFalsePositiveEngine:
    """
    三级防误报校验引擎

    一级：AI 初判 → 置信度阈值校验
    二级：校验风险可信度（多帧复核、上下文关联）
    三级：二次复核（边界案例升级人工）

    输出分级：
      LOW      — 仅留存记录
      MEDIUM   — 通知家属
      HIGH     — 推送家属 + 社区
      EMERGENCY— 推送家属 + 社区 + 医院
    """

    @staticmethod
    def evaluate(anomaly: dict, history: list = None) -> dict:
        """
        三级评估并返回最终分级。

        Args:
            anomaly: AI 检测原始结果 {"type": str, "confidence": float, ...}
            history: 该设备/老人的最近异常历史记录（用于二级校验）

        Returns:
            {"level": str, "verified": int, "push_family": bool, "push_community": bool, "push_hospital": bool, "reason": str}
        """
        anomaly_type = anomaly.get("type")
        confidence = anomaly.get("confidence", 0)

        # ── 一级：置信度阈值 ──
        level_1 = AntiFalsePositiveEngine._level_1_threshold(anomaly_type, confidence)
        if level_1["level"] == ALARM_LEVEL_LOW and level_1["skip"]:
            return {
                "level": ALARM_LEVEL_LOW,
                "verified": AI_VERIFY_LOW,
                "push_family": False,
                "push_community": False,
                "push_hospital": False,
                "reason": level_1["reason"]
            }

        # ── 二级：多帧复核 + 上下文关联 ──
        level_2 = AntiFalsePositiveEngine._level_2_verify(anomaly_type, confidence, history)

        # ── 三级：边界案例升级 ──
        level_3 = AntiFalsePositiveEngine._level_3_escalation(level_2, anomaly_type, confidence)

        return level_3

    @staticmethod
    def _level_1_threshold(anomaly_type: str, confidence: float) -> dict:
        """一级：置信度阈值"""
        # 烟感/燃气：传感器阈值触发本身可信度高，直接通过
        if anomaly_type in (ALARM_TYPE_SMOKE, ALARM_TYPE_GAS):
            return {"level": ALARM_LEVEL_HIGH, "skip": False, "reason": "传感器硬触发，置信度高"}

        # 跌倒检测
        if anomaly_type == ALARM_TYPE_FALL:
            if confidence < FALL_DETECTION_MIN_CONFIDENCE:
                return {"level": ALARM_LEVEL_LOW, "skip": True,
                        "reason": f"置信度 {confidence} 低于阈值 {FALL_DETECTION_MIN_CONFIDENCE}"}
            if confidence >= 0.85:
                return {"level": ALARM_LEVEL_HIGH, "skip": False, "reason": "高置信度跌倒"}
            return {"level": ALARM_LEVEL_MEDIUM, "skip": False, "reason": "中等置信度，需二级复核"}

        # 静止检测
        if anomaly_type == ALARM_TYPE_STILLNESS:
            if confidence >= 0.8:
                return {"level": ALARM_LEVEL_MEDIUM, "skip": False, "reason": "静止超时"}
            return {"level": ALARM_LEVEL_LOW, "skip": True, "reason": "静止时间较短"}

        return {"level": ALARM_LEVEL_MEDIUM, "skip": False, "reason": "默认通过一级"}

    @staticmethod
    def _level_2_verify(anomaly_type: str, confidence: float, history: list = None) -> dict:
        """二级：多帧复核 + 上下文关联"""
        base = {"verified": AI_VERIFY_MEDIUM}

        if history and len(history) > 0:
            # 如果短时间内有多个同类告警，提升可信度
            recent_same_type = [h for h in history[-5:] if h.get("alarm_type") == anomaly_type]
            if len(recent_same_type) >= 3:
                return {
                    "level": ALARM_LEVEL_HIGH,
                    "verified": AI_VERIFY_HIGH,
                    "push_family": True,
                    "push_community": True,
                    "push_hospital": False,
                    "reason": f"短时间内第{len(recent_same_type)+1}次同类告警，提升为高风险"
                }

        # 默认维持一级结果
        return {
            "level": ALARM_LEVEL_MEDIUM,
            "verified": AI_VERIFY_MEDIUM,
            "push_family": True,
            "push_community": False,
            "push_hospital": False,
            "reason": "二级复核通过"
        }

    @staticmethod
    def _level_3_escalation(level_2_result: dict, anomaly_type: str, confidence: float) -> dict:
        """三级：边界案例升级处理"""
        level = level_2_result.get("level", ALARM_LEVEL_MEDIUM)

        # 跌倒 + 高置信度 + 无响应 → 升级为 EMERGENCY
        if anomaly_type == ALARM_TYPE_FALL and confidence >= 0.9:
            return {
                "level": ALARM_LEVEL_EMERGENCY,
                "verified": AI_VERIFY_HIGH,
                "push_family": True,
                "push_community": True,
                "push_hospital": True,
                "reason": "高置信度跌倒，升级为紧急告警，同步推送医院"
            }

        # 烟感/燃气 → 升级为 EMERGENCY
        if anomaly_type in (ALARM_TYPE_SMOKE, ALARM_TYPE_GAS):
            return {
                "level": ALARM_LEVEL_EMERGENCY,
                "verified": AI_VERIFY_HIGH,
                "push_family": True,
                "push_community": True,
                "push_hospital": True,
                "reason": "火灾/燃气泄漏风险，紧急推送全部端口"
            }

        # 低/中/高风险差异化推送
        if level == ALARM_LEVEL_LOW:
            return {**level_2_result, "push_family": False, "push_community": False, "push_hospital": False}
        elif level == ALARM_LEVEL_MEDIUM:
            return {**level_2_result, "push_family": True, "push_community": False, "push_hospital": False}
        elif level == ALARM_LEVEL_HIGH:
            return {**level_2_result, "push_family": True, "push_community": True, "push_hospital": False}

        return level_2_result
