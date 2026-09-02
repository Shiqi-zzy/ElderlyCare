"""告警服务：告警生成、分级推送、状态流转、闭环归档"""
import json
from typing import Optional
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso
from ..core.config import (
    ALARM_STATUS_ACTIVE, ALARM_STATUS_ACKNOWLEDGED, ALARM_STATUS_PROCESSING,
    ALARM_STATUS_RESOLVED, ALARM_STATUS_ARCHIVED,
    ALARM_LEVEL_HIGH, ALARM_LEVEL_EMERGENCY
)
from ..services.audit_service import AuditService
from ..services.anti_false_positive import AntiFalsePositiveEngine


class AlarmService:

    @staticmethod
    def create_from_anomaly(
        elderly_id: str,
        device_id: Optional[str],
        anomaly: dict,
        recent_alarms: list = None
    ) -> Optional[dict]:
        """
        从 AI 检测异常创建告警。

        流程：AI 初判结果 → 防误报三级评估 → 分级 → 写入 DB → 生成工单(如需)
        """
        db = get_db()

        # 1. 防误报三级评估
        verified = AntiFalsePositiveEngine.evaluate(anomaly, recent_alarms)

        alarm_id = generate_uuid()
        ts = now_iso()

        # 2. 写入告警
        db.execute(
            """INSERT INTO alarms(id, elderly_id, device_id, alarm_type, alarm_level, ai_score,
               ai_verified, raw_data_json, snapshot_url, video_clip_url, title, description,
               status, push_family, push_community, push_hospital, created_at)
               VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (alarm_id, elderly_id, device_id, anomaly.get("type"),
             verified["level"], anomaly.get("confidence"), verified["verified"],
             json.dumps(anomaly, ensure_ascii=False),
             anomaly.get("snapshot_ref"), anomaly.get("video_clip_ref"),
             anomaly.get("description", anomaly.get("type")),
             anomaly.get("description", ""),
             ALARM_STATUS_ACTIVE,
             1 if verified["push_family"] else 0,
             1 if verified["push_community"] else 0,
             1 if verified["push_hospital"] else 0,
             ts)
        )
        db.commit()

        # 3. 审计日志
        AuditService.log(
            event_type="alarm_create",
            operator="ai_engine",
            target_type="alarm",
            target_id=alarm_id,
            detail={
                "elderly_id": elderly_id,
                "alarm_type": anomaly.get("type"),
                "alarm_level": verified["level"],
                "ai_score": anomaly.get("confidence"),
                "ai_verified": verified["verified"]
            }
        )

        # 4. HIGH/EMERGENCY 自动创建工单
        alarm_row = db.execute("SELECT * FROM alarms WHERE id=?", (alarm_id,)).fetchone()
        alarm_dict = dict(alarm_row)

        if verified["level"] in (ALARM_LEVEL_HIGH, ALARM_LEVEL_EMERGENCY):
            from ..services.work_order_service import WorkOrderService
            work_order_id = WorkOrderService.auto_create_from_alarm(alarm_dict)
            if work_order_id:
                db.execute(
                    "UPDATE alarms SET related_work_order_id=? WHERE id=?",
                    (work_order_id, alarm_id)
                )
                db.commit()
                alarm_dict["related_work_order_id"] = work_order_id

        return alarm_dict

    @staticmethod
    def get(alarm_id: str) -> Optional[dict]:
        """获取告警详情"""
        db = get_db()
        row = db.execute("SELECT * FROM alarms WHERE id=?", (alarm_id,)).fetchone()
        return dict(row) if row else None

    @staticmethod
    def list_by_elderly(elderly_id: str, level: Optional[str] = None,
                        status: Optional[str] = None, limit: int = 50, offset: int = 0) -> tuple:
        """获取老人的告警列表"""
        db = get_db()
        conditions = ["elderly_id = ?"]
        params = [elderly_id]

        if level:
            conditions.append("alarm_level = ?")
            params.append(level)
        if status:
            conditions.append("status = ?")
            params.append(status)

        where = "WHERE " + " AND ".join(conditions)

        count_row = db.execute(f"SELECT COUNT(*) FROM alarms {where}", params).fetchone()
        total = count_row[0] if count_row else 0

        rows = db.execute(
            f"SELECT * FROM alarms {where} ORDER BY created_at DESC LIMIT ? OFFSET ?",
            params + [limit, offset]
        ).fetchall()

        return total, [dict(r) for r in rows]

    @staticmethod
    def acknowledge(alarm_id: str, user_id: str) -> bool:
        """家属确认告警"""
        db = get_db()
        row = db.execute("SELECT * FROM alarms WHERE id=? AND status=?", (alarm_id, ALARM_STATUS_ACTIVE)).fetchone()
        if not row:
            return False

        ts = now_iso()
        db.execute(
            "UPDATE alarms SET status=?, acknowledged_by=?, acknowledged_at=? WHERE id=?",
            (ALARM_STATUS_ACKNOWLEDGED, user_id, ts, alarm_id)
        )
        db.commit()

        AuditService.log(
            event_type="alarm_acknowledge",
            operator=user_id,
            target_type="alarm",
            target_id=alarm_id,
            detail={"acknowledged_at": ts}
        )
        return True

    @staticmethod
    def resolve(alarm_id: str, user_id: str, note: Optional[str] = None) -> bool:
        """标记告警已解决 → 归档"""
        db = get_db()
        row = db.execute(
            "SELECT * FROM alarms WHERE id=? AND status IN (?,?,?)",
            (alarm_id, ALARM_STATUS_ACTIVE, ALARM_STATUS_ACKNOWLEDGED, ALARM_STATUS_PROCESSING)
        ).fetchone()
        if not row:
            return False

        ts = now_iso()
        db.execute(
            "UPDATE alarms SET status=?, resolved_by=?, resolved_at=?, resolution_note=? WHERE id=?",
            (ALARM_STATUS_RESOLVED, user_id, ts, note, alarm_id)
        )
        db.commit()

        AuditService.log(
            event_type="alarm_resolve",
            operator=user_id,
            target_type="alarm",
            target_id=alarm_id,
            detail={"resolution_note": note, "resolved_at": ts}
        )
        return True

    @staticmethod
    def archive(alarm_id: str) -> bool:
        """告警归档"""
        db = get_db()
        db.execute(
            "UPDATE alarms SET status=? WHERE id=? AND status=?",
            (ALARM_STATUS_ARCHIVED, alarm_id, ALARM_STATUS_RESOLVED)
        )
        db.commit()
        return True

    @staticmethod
    def get_recent_by_device(device_id: str, limit: int = 5) -> list:
        """获取设备最近告警（用于防误报二级校验）"""
        db = get_db()
        rows = db.execute(
            "SELECT * FROM alarms WHERE device_id=? ORDER BY created_at DESC LIMIT ?",
            (device_id, limit)
        ).fetchall()
        return [dict(r) for r in rows]
