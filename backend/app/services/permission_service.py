"""权限管理服务：授权授予、撤销、查询、过期自动处理"""
import json
from typing import Optional
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso
from ..core.config import AUTH_STATUS_ACTIVE, AUTH_STATUS_REVOKED, AUTH_STATUS_EXPIRED
from ..services.audit_service import AuditService


class PermissionService:

    @staticmethod
    def grant(
        elderly_id: str,
        grantor_user_id: str,
        grantee_user_id: str,
        permission_type: str,
        data_scope: str,
        effective_until: str
    ) -> Optional[str]:
        """家属授予数据访问权限给社区/医院人员"""
        db = get_db()

        # 获取被授权人的机构ID
        grantee = db.execute("SELECT institution_id, role FROM users WHERE id=?", (grantee_user_id,)).fetchone()
        if not grantee:
            return None
        grantee_institution_id = grantee["institution_id"]

        auth_id = generate_uuid()
        ts = now_iso()

        db.execute(
            """INSERT INTO authorizations(id, elderly_id, grantor_user_id, grantee_user_id, grantee_institution_id,
               permission_type, data_scope, effective_from, effective_until, status, created_at)
               VALUES(?,?,?,?,?,?,?,?,?,?,?)""",
            (auth_id, elderly_id, grantor_user_id, grantee_user_id, grantee_institution_id,
             permission_type, data_scope, ts, effective_until, AUTH_STATUS_ACTIVE, ts)
        )
        db.commit()

        AuditService.log(
            event_type="auth_grant",
            operator=grantor_user_id,
            target_type="authorization",
            target_id=auth_id,
            detail={
                "elderly_id": elderly_id,
                "grantee_user_id": grantee_user_id,
                "permission_type": permission_type,
                "effective_until": effective_until
            }
        )

        return auth_id

    @staticmethod
    def revoke(auth_id: str, revoked_by: str, reason: Optional[str] = None) -> bool:
        """家属一键撤销授权"""
        db = get_db()
        row = db.execute("SELECT * FROM authorizations WHERE id=? AND status=?", (auth_id, AUTH_STATUS_ACTIVE)).fetchone()
        if not row:
            return False

        ts = now_iso()
        db.execute(
            """UPDATE authorizations SET status=?, revoked_by=?, revoked_at=?, revoke_reason=? WHERE id=?""",
            (AUTH_STATUS_REVOKED, revoked_by, ts, reason, auth_id)
        )
        db.commit()

        AuditService.log(
            event_type="auth_revoke",
            operator=revoked_by,
            target_type="authorization",
            target_id=auth_id,
            detail={"reason": reason, "revoked_at": ts}
        )
        return True

    @staticmethod
    def list_by_elderly(elderly_id: str) -> list:
        """查看指定老人的所有授权"""
        db = get_db()
        rows = db.execute("""
            SELECT a.*, u.real_name as grantee_name, u.phone as grantee_phone,
                   i.name as institution_name
            FROM authorizations a
            LEFT JOIN users u ON a.grantee_user_id = u.id
            LEFT JOIN institutions i ON a.grantee_institution_id = i.id
            WHERE a.elderly_id = ?
            ORDER BY a.created_at DESC
        """, (elderly_id,)).fetchall()
        return [dict(r) for r in rows]

    @staticmethod
    def check_and_expire() -> int:
        """定时任务：检查过期的授权，自动标记为过期"""
        db = get_db()
        ts = now_iso()
        cursor = db.execute(
            """UPDATE authorizations SET status=?
               WHERE status=? AND datetime(effective_until) <= datetime(?)""",
            (AUTH_STATUS_EXPIRED, AUTH_STATUS_ACTIVE, ts)
        )
        db.commit()
        count = cursor.rowcount

        if count > 0:
            AuditService.log(
                event_type="auth_auto_expire",
                operator="system",
                target_type="authorization",
                detail={"count": count, "expired_at": ts}
            )

        return count

    @staticmethod
    def get_active_auth_for_user(user_id: str, elderly_id: str) -> Optional[dict]:
        """获取用户对特定老人的有效授权"""
        db = get_db()
        row = db.execute(
            """SELECT * FROM authorizations
               WHERE grantee_user_id=? AND elderly_id=? AND status=? AND datetime(effective_until) > datetime('now','localtime')
               ORDER BY created_at DESC LIMIT 1""",
            (user_id, elderly_id, AUTH_STATUS_ACTIVE)
        ).fetchone()
        return dict(row) if row else None
