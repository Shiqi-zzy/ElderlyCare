"""通知推送服务：WebSocket 实时推送 + 短信/微信模板(预留)"""
import json
from typing import Optional
from ..core.database import get_db


class NotificationService:
    """
    消息通知服务。

    阶段 1：WebSocket 实时推送至在线客户端
    阶段 2+：SMS/微信模板消息推送至离线用户

    使用方式：
      - 设置 ws_manager 回调（由 ws.py 模块注入）
      - 告警/工单状态变更时调用对应方法
    """

    # WebSocket 连接管理器（由 routes/ws.py 注入）
    _ws_manager = None

    @classmethod
    def set_ws_manager(cls, manager):
        """注入 WebSocket 管理器"""
        cls._ws_manager = manager

    @classmethod
    async def notify_alarm(cls, alarm: dict):
        """
        新告警通知 → 推送给相关人员。

        推送规则：
          - push_family → 推送给老人的绑定家属
          - push_community → 推送给已授权的社区人员
          - push_hospital → 推送给已授权的医院人员
        """
        if not cls._ws_manager:
            return

        db = get_db()
        elderly_id = alarm.get("elderly_id")
        message = json.dumps({
            "type": "alarm_new",
            "data": {
                "alarm_id": alarm.get("id"),
                "elderly_id": elderly_id,
                "alarm_type": alarm.get("alarm_type"),
                "alarm_level": alarm.get("alarm_level"),
                "title": alarm.get("title"),
                "description": alarm.get("description"),
                "created_at": alarm.get("created_at")
            }
        }, ensure_ascii=False)

        # 推送给家属
        if alarm.get("push_family"):
            family_row = db.execute(
                "SELECT binding_family_user_id FROM elderly WHERE id=?",
                (elderly_id,)
            ).fetchone()
            if family_row:
                await cls._ws_manager.send_to_user(family_row["binding_family_user_id"], message)

        # 推送给已授权的社区人员
        if alarm.get("push_community"):
            community_users = db.execute(
                """SELECT u.id FROM users u
                   JOIN authorizations a ON u.id = a.grantee_user_id
                   WHERE a.elderly_id = ? AND u.role = 'community'
                   AND a.status = 'active' AND datetime(a.effective_until) > datetime('now','localtime')""",
                (elderly_id,)
            ).fetchall()
            for user_row in community_users:
                await cls._ws_manager.send_to_user(user_row["id"], message)

        # 推送给已授权的医院人员
        if alarm.get("push_hospital"):
            hospital_users = db.execute(
                """SELECT u.id FROM users u
                   JOIN authorizations a ON u.id = a.grantee_user_id
                   WHERE a.elderly_id = ? AND u.role = 'hospital'
                   AND a.status = 'active' AND datetime(a.effective_until) > datetime('now','localtime')""",
                (elderly_id,)
            ).fetchall()
            for user_row in hospital_users:
                await cls._ws_manager.send_to_user(user_row["id"], message)

    @classmethod
    async def notify_work_order_update(cls, work_order: dict, user_id: str):
        """工单状态变更通知"""
        if not cls._ws_manager:
            return

        message = json.dumps({
            "type": "work_order_update",
            "data": {
                "work_order_id": work_order.get("id"),
                "status": work_order.get("status"),
                "title": work_order.get("title"),
                "elderly_id": work_order.get("elderly_id")
            }
        }, ensure_ascii=False)

        await cls._ws_manager.send_to_user(user_id, message)

    @classmethod
    async def notify_device_status(cls, device: dict, elderly_id: str):
        """设备状态变更通知"""
        if not cls._ws_manager:
            return

        db = get_db()
        family_row = db.execute(
            "SELECT binding_family_user_id FROM elderly WHERE id=?",
            (elderly_id,)
        ).fetchone()
        if not family_row:
            return

        message = json.dumps({
            "type": "device_status",
            "data": {
                "device_id": device.get("id"),
                "device_name": device.get("device_name"),
                "status": device.get("status"),
                "elderly_id": elderly_id
            }
        }, ensure_ascii=False)

        await cls._ws_manager.send_to_user(family_row["binding_family_user_id"], message)

    @classmethod
    async def notify_monitoring_access(cls, elderly_id: str, viewer_name: str):
        """有人查看监控提示 → 通知家属"""
        if not cls._ws_manager:
            return

        db = get_db()
        family_row = db.execute(
            "SELECT binding_family_user_id FROM elderly WHERE id=?",
            (elderly_id,)
        ).fetchone()
        if not family_row:
            return

        message = json.dumps({
            "type": "monitoring_access",
            "data": {
                "elderly_id": elderly_id,
                "viewer": viewer_name,
                "message": f"{viewer_name} 正在查看监控画面"
            }
        }, ensure_ascii=False)

        await cls._ws_manager.send_to_user(family_row["binding_family_user_id"], message)
