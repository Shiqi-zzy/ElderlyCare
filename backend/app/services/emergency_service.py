"""急救临时权限服务（Phase 3）"""
from typing import Optional
from datetime import datetime, timedelta
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso
from ..services.audit_service import AuditService
from ..services.notification_service import NotificationService


class EmergencyService:

    @staticmethod
    def request_emergency_access(elderly_id: str, hospital_user_id: str,
                                  reason: str, institution_id: str = "") -> dict:
        """医院发起急救临时24小时监控权限"""
        db = get_db()

        # 验证老人存在
        elderly = db.execute("SELECT * FROM elderly WHERE id=? AND is_active=1",
                            (elderly_id,)).fetchone()
        if not elderly:
            raise ValueError("老人档案不存在")

        # 检查是否已有活跃的急救权限
        existing = db.execute(
            """SELECT id FROM authorizations
               WHERE elderly_id=? AND grantee_user_id=? AND status='active'
               AND permission_type='emergency_monitoring'
               AND datetime(effective_until) > datetime('now','localtime')""",
            (elderly_id, hospital_user_id)
        ).fetchone()
        if existing:
            return {"message": "已有活跃的急救临时权限，无需重复申请",
                    "authorization_id": existing["id"], "status": "already_active"}

        # 创建24h临时授权
        auth_id = generate_uuid()
        ts = now_iso()
        expires = (datetime.now() + timedelta(hours=24)).strftime("%Y-%m-%d %H:%M:%S")

        db.execute(
            """INSERT INTO authorizations(
                   id, elderly_id, grantor_user_id, grantee_user_id, grantee_institution_id,
                   permission_type, data_scope, effective_from, effective_until, status, created_at)
               VALUES(?, ?, ?, ?, ?, 'emergency_monitoring',
                      '{"video":true,"alarm":true,"medical":false}',
                      ?, ?, 'active', ?)""",
            (auth_id, elderly_id, elderly["binding_family_user_id"], hospital_user_id,
             institution_id, ts, expires, ts)
        )
        db.commit()

        # 审计日志
        AuditService.log(
            event_type="emergency_access_granted",
            operator=hospital_user_id,
            target_type="authorization",
            target_id=auth_id,
            detail={"elderly_id": elderly_id, "reason": reason, "expires_at": expires}
        )

        # 推送通知给家属
        try:
            family_id = elderly["binding_family_user_id"]
            NotificationService.push_to_user(family_id, {
                "type": "emergency_access",
                "title": "急救临时权限已激活",
                "message": f"医院已发起{elderly['name']}的急救24小时临时监控权限，理由：{reason}",
                "elderly_id": elderly_id,
                "expires_at": expires
            })
        except Exception:
            pass  # 通知非关键路径

        return {
            "message": "急救临时权限已激活（24小时有效）",
            "authorization_id": auth_id,
            "status": "active",
            "expires_at": expires
        }

    @staticmethod
    def list_active_emergency(hospital_user_id: str) -> list:
        """查看当前有效的急救权限"""
        db = get_db()
        rows = db.execute(
            """SELECT a.*, e.name as elderly_name FROM authorizations a
               JOIN elderly e ON a.elderly_id = e.id
               WHERE a.grantee_user_id=? AND a.status='active'
               AND a.permission_type='emergency_monitoring'
               AND datetime(a.effective_until) > datetime('now','localtime')
               ORDER BY a.created_at DESC""",
            (hospital_user_id,)
        ).fetchall()
        return [dict(r) for r in rows]

    @staticmethod
    def get_emergency_status(hospital_user_id: str) -> dict:
        """获取急救权限当前状态"""
        actives = EmergencyService.list_active_emergency(hospital_user_id)
        if actives:
            latest = actives[0]
            return {
                "active": True,
                "expires_at": latest["effective_until"],
                "elderly_name": latest.get("elderly_name", ""),
                "elderly_id": latest["elderly_id"],
                "authorization_id": latest["id"]
            }
        return {"active": False, "expires_at": "", "elderly_name": ""}
