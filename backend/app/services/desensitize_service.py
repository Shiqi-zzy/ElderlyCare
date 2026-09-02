"""数据脱敏服务"""
import json
from typing import Optional
from ..core.database import get_db
from ..core.desensitize import (
    desensitize_elderly_record,
    desensitize_alarm_record,
    desensitize_health_record
)
from ..core.config import ROLE_FAMILY


class DesensitizeService:

    @staticmethod
    def get_elderly_for_role(elderly_id: str, user_id: str) -> Optional[dict]:
        """
        按用户角色获取经过脱敏处理的老人信息。

        家属：完整数据
        社区：身份证/手机/地址脱敏
        医院：仅医疗相关数据，无安防信息
        """
        db = get_db()
        row = db.execute("SELECT * FROM elderly WHERE id=? AND is_active=1", (elderly_id,)).fetchone()
        if not row:
            return None

        data = dict(row)

        # 获取查看者的角色
        user_row = db.execute("SELECT role FROM users WHERE id=?", (user_id,)).fetchone()
        role = user_row["role"] if user_row else ROLE_FAMILY

        return desensitize_elderly_record(data, role)

    @staticmethod
    def get_alarm_for_role(alarm_id: str, user_id: str) -> Optional[dict]:
        """按用户角色获取告警信息"""
        db = get_db()
        row = db.execute("SELECT * FROM alarms WHERE id=?", (alarm_id,)).fetchone()
        if not row:
            return None
        data = dict(row)

        user_row = db.execute("SELECT role FROM users WHERE id=?", (user_id,)).fetchone()
        role = user_row["role"] if user_row else ROLE_FAMILY

        return desensitize_alarm_record(data, role)

    @staticmethod
    def get_health_for_role(record_id: str, user_id: str) -> Optional[dict]:
        """按用户角色获取健康档案"""
        db = get_db()
        row = db.execute("SELECT * FROM health_records WHERE id=?", (record_id,)).fetchone()
        if not row:
            return None
        data = dict(row)

        user_row = db.execute("SELECT role FROM users WHERE id=?", (user_id,)).fetchone()
        role = user_row["role"] if user_row else ROLE_FAMILY

        return desensitize_health_record(data, role)
