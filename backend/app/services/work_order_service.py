"""工单服务：自动生成、分配、处理、完成"""
import json
from typing import Optional
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso
from ..core.config import (
    ORDER_STATUS_PENDING, ORDER_STATUS_ACCEPTED,
    ORDER_STATUS_IN_PROGRESS, ORDER_STATUS_COMPLETED, ORDER_STATUS_CANCELLED,
    ALARM_LEVEL_HIGH, ALARM_LEVEL_EMERGENCY
)
from ..services.audit_service import AuditService


class WorkOrderService:

    @staticmethod
    def auto_create_from_alarm(alarm: dict) -> Optional[str]:
        """
        HIGH/EMERGENCY 告警触发时自动生成处置工单。

        分配逻辑（阶段 1）：
        - 查询是否有社区人员已授权绑定该老人
        - 如果有，分配给第一个授权机构
        - 如果没有，工单状态为 pending（等待管理员手动分配）
        """
        db = get_db()
        order_id = generate_uuid()
        ts = now_iso()

        elderly_id = alarm.get("elderly_id")
        alarm_id = alarm.get("id")
        alarm_level = alarm.get("alarm_level", "")
        alarm_type = alarm.get("alarm_type", "")

        # 查找已授权该老人的社区人员
        auth_row = db.execute(
            """SELECT a.grantee_user_id, a.grantee_institution_id, u.real_name
               FROM authorizations a
               JOIN users u ON a.grantee_user_id = u.id
               WHERE a.elderly_id = ? AND a.status = 'active'
               AND u.role = 'community'
               AND datetime(a.effective_until) > datetime('now','localtime')
               LIMIT 1""",
            (elderly_id,)
        ).fetchone()

        assigned_to = auth_row["grantee_user_id"] if auth_row else None
        assigned_inst = auth_row["grantee_institution_id"] if auth_row else None

        # 优先级映射
        priority = "urgent" if alarm_level == ALARM_LEVEL_EMERGENCY else "high"

        # 工单标题和描述
        type_labels = {
            "fall": "跌倒告警处置",
            "stillness": "静止异常处置",
            "smoke": "烟雾告警处置",
            "gas": "燃气泄漏处置",
            "bed_absence": "夜间离床异常处置",
            "vital_sign": "生命体征异常处置"
        }
        title = type_labels.get(alarm_type, f"异常告警处置 - {alarm_type}")
        description = alarm.get("description", f"系统自动生成工单，关联告警 {alarm_id}")

        db.execute(
            """INSERT INTO work_orders(id, alarm_id, elderly_id, order_type, title, description,
               priority, assigned_to, assigned_institution_id, status, created_at)
               VALUES(?,?,?,?,?,?,?,?,?,?,?)""",
            (order_id, alarm_id, elderly_id, "alarm_handling", title, description,
             priority, assigned_to, assigned_inst, ORDER_STATUS_PENDING, ts)
        )
        db.commit()

        AuditService.log(
            event_type="work_order_auto_create",
            operator="system",
            target_type="work_order",
            target_id=order_id,
            detail={
                "alarm_id": alarm_id,
                "elderly_id": elderly_id,
                "assigned_to": assigned_to,
                "priority": priority
            }
        )

        return order_id

    @staticmethod
    def get(order_id: str) -> Optional[dict]:
        """获取工单详情"""
        db = get_db()
        row = db.execute("SELECT * FROM work_orders WHERE id=?", (order_id,)).fetchone()
        return dict(row) if row else None

    @staticmethod
    def list_by_assignee(user_id: str, status: Optional[str] = None,
                         limit: int = 50, offset: int = 0) -> tuple:
        """获取分配给某人的工单列表"""
        db = get_db()
        conditions = ["assigned_to = ?"]
        params = [user_id]

        if status:
            conditions.append("status = ?")
            params.append(status)

        where = "WHERE " + " AND ".join(conditions)
        count_row = db.execute(f"SELECT COUNT(*) FROM work_orders {where}", params).fetchone()
        total = count_row[0] if count_row else 0

        rows = db.execute(
            f"SELECT * FROM work_orders {where} ORDER BY created_at DESC LIMIT ? OFFSET ?",
            params + [limit, offset]
        ).fetchall()
        return total, [dict(r) for r in rows]

    @staticmethod
    def list_by_elderly(elderly_id: str, limit: int = 20, offset: int = 0) -> tuple:
        """获取老人的工单列表"""
        db = get_db()
        count_row = db.execute("SELECT COUNT(*) FROM work_orders WHERE elderly_id=?", (elderly_id,)).fetchone()
        total = count_row[0] if count_row else 0

        rows = db.execute(
            "SELECT * FROM work_orders WHERE elderly_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?",
            (elderly_id, limit, offset)
        ).fetchall()
        return total, [dict(r) for r in rows]

    @staticmethod
    def list_by_institution(institution_id: str, status: Optional[str] = None,
                            limit: int = 50, offset: int = 0) -> tuple:
        """获取机构的工单列表"""
        db = get_db()
        conditions = ["assigned_institution_id = ?"]
        params = [institution_id]

        if status:
            conditions.append("status = ?")
            params.append(status)

        where = "WHERE " + " AND ".join(conditions)
        count_row = db.execute(f"SELECT COUNT(*) FROM work_orders {where}", params).fetchone()
        total = count_row[0] if count_row else 0

        rows = db.execute(
            f"SELECT * FROM work_orders {where} ORDER BY created_at DESC LIMIT ? OFFSET ?",
            params + [limit, offset]
        ).fetchall()
        return total, [dict(r) for r in rows]

    @staticmethod
    def accept(order_id: str, user_id: str) -> bool:
        """接单"""
        db = get_db()
        row = db.execute("SELECT * FROM work_orders WHERE id=? AND status=?", (order_id, ORDER_STATUS_PENDING)).fetchone()
        if not row:
            return False

        db.execute(
            "UPDATE work_orders SET status=? WHERE id=?",
            (ORDER_STATUS_ACCEPTED, order_id)
        )
        db.commit()

        AuditService.log(
            event_type="work_order_accept",
            operator=user_id,
            target_type="work_order",
            target_id=order_id
        )
        return True

    @staticmethod
    def start(order_id: str, user_id: str) -> bool:
        """开始处理"""
        db = get_db()
        db.execute(
            "UPDATE work_orders SET status=? WHERE id=? AND status=?",
            (ORDER_STATUS_IN_PROGRESS, order_id, ORDER_STATUS_ACCEPTED)
        )
        db.commit()
        return True

    @staticmethod
    def complete(order_id: str, user_id: str, result_json: Optional[str] = None,
                 result_photos: Optional[str] = None) -> bool:
        """完成工单并上传处置结果"""
        db = get_db()
        row = db.execute(
            "SELECT * FROM work_orders WHERE id=? AND status IN (?,?,?)",
            (order_id, ORDER_STATUS_PENDING, ORDER_STATUS_ACCEPTED, ORDER_STATUS_IN_PROGRESS)
        ).fetchone()
        if not row:
            return False

        ts = now_iso()
        db.execute(
            "UPDATE work_orders SET status=?, result_json=?, result_photos=?, completed_at=? WHERE id=?",
            (ORDER_STATUS_COMPLETED, result_json, result_photos, ts, order_id)
        )
        db.commit()

        # 同时解决关联的告警
        if row["alarm_id"]:
            from ..services.alarm_service import AlarmService
            AlarmService.resolve(row["alarm_id"], user_id, f"工单 {order_id} 已完成")

        AuditService.log(
            event_type="work_order_complete",
            operator=user_id,
            target_type="work_order",
            target_id=order_id,
            detail={"result_json": result_json, "completed_at": ts}
        )
        return True
