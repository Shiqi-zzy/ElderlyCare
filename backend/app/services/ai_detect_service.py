"""AI 检测服务（阶段1：规则引擎 stub，阶段3接入真实模型）"""
import random
from typing import Optional
from ..core.config import (
    ALARM_TYPE_FALL, ALARM_TYPE_STILLNESS, ALARM_TYPE_SMOKE,
    ALARM_TYPE_GAS, ALARM_TYPE_VITAL_SIGN,
    FALL_DETECTION_MIN_CONFIDENCE, STILLNESS_TIMEOUT_SECONDS
)


class AiDetectService:
    """
    AI 异常检测引擎。

    阶段 1：基于规则的 stub 实现（用于打通告警闭环链路）
    阶段 3：替换为真实 CV/ML 模型

    模拟行为：
    - 约 15% 概率产生"可疑异常"
    - 其中跌倒检测置信度在 0.5-0.95 之间分布
    - 静止检测基于时间阈值
    """

    @staticmethod
    def detect_anomaly(device_type: str, raw_data: dict) -> Optional[dict]:
        """
        检测异常。

        Args:
            device_type: 设备类型 (camera/wearable/bed_sensor/smoke_sensor/gas_sensor)
            raw_data: 设备上报的原始数据

        Returns:
            如果检测到异常: {"type": str, "confidence": float, "snapshot_ref": str|None}
            如果正常: None
        """
        if device_type == "camera":
            return AiDetectService._detect_from_camera(raw_data)
        elif device_type == "wearable":
            return AiDetectService._detect_from_wearable(raw_data)
        elif device_type == "bed_sensor":
            return AiDetectService._detect_from_bed_sensor(raw_data)
        elif device_type == "smoke_sensor":
            return AiDetectService._detect_smoke(raw_data)
        elif device_type == "gas_sensor":
            return AiDetectService._detect_gas(raw_data)

        return None

    @staticmethod
    def _detect_from_camera(raw_data: dict) -> Optional[dict]:
        """摄像头：检测跌倒/静止"""
        # 阶段 1 模拟：15% 概率产生异常
        if random.random() < 0.15:
            # 70% 跌倒, 30% 静止
            if random.random() < 0.7:
                confidence = round(random.uniform(0.5, 0.95), 2)
                return {
                    "type": ALARM_TYPE_FALL,
                    "confidence": confidence,
                    "snapshot_ref": f"snapshot_{random.randint(1000,9999)}.jpg",
                    "description": "检测到疑似跌倒行为"
                }
            else:
                return {
                    "type": ALARM_TYPE_STILLNESS,
                    "confidence": 0.85,
                    "snapshot_ref": f"snapshot_{random.randint(1000,9999)}.jpg",
                    "description": f"检测到超过{STILLNESS_TIMEOUT_SECONDS}秒静止状态"
                }

        return None

    @staticmethod
    def _detect_from_wearable(raw_data: dict) -> Optional[dict]:
        """穿戴设备：检测跌倒/生命体征异常"""
        heart_rate = raw_data.get("heart_rate", 75)

        # 心率异常检测
        if heart_rate < 40 or heart_rate > 150:
            return {
                "type": ALARM_TYPE_VITAL_SIGN,
                "confidence": 0.8,
                "snapshot_ref": None,
                "description": f"心率异常: {heart_rate} bpm"
            }

        # 模拟跌倒检测
        if random.random() < 0.08:
            confidence = round(random.uniform(0.6, 0.9), 2)
            return {
                "type": ALARM_TYPE_FALL,
                "confidence": confidence,
                "snapshot_ref": None,
                "description": "穿戴设备检测到疑似跌倒冲击"
            }

        return None

    @staticmethod
    def _detect_from_bed_sensor(raw_data: dict) -> Optional[dict]:
        """床垫传感器：检测离床异常"""
        absence_minutes = raw_data.get("absence_minutes", 0)

        # 夜间（22:00-06:00）离床超过 30 分钟
        if absence_minutes > 30:
            return {
                "type": "bed_absence",
                "confidence": 0.75,
                "snapshot_ref": None,
                "description": f"夜间离床超过{absence_minutes}分钟"
            }

        return None

    @staticmethod
    def _detect_smoke(raw_data: dict) -> Optional[dict]:
        """烟感传感器"""
        smoke_level = raw_data.get("smoke_level", 0)
        if smoke_level > 80:
            return {
                "type": ALARM_TYPE_SMOKE,
                "confidence": 0.9,
                "snapshot_ref": None,
                "description": f"烟雾浓度超标: {smoke_level}"
            }
        return None

    @staticmethod
    def _detect_gas(raw_data: dict) -> Optional[dict]:
        """燃气传感器"""
        gas_level = raw_data.get("gas_level", 0)
        if gas_level > 60:
            return {
                "type": ALARM_TYPE_GAS,
                "confidence": 0.95,
                "snapshot_ref": None,
                "description": f"燃气浓度异常: {gas_level}"
            }
        return None
