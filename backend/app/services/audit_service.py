"""审计日志服务"""
import json
from typing import Optional
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso


class AuditService:

    @staticmethod
    def log(
        event_type: str,
        operator: str = "system",
        target_type: Optional[str] = None,
        target_id: Optional[str] = None,
        detail: Optional[dict] = None,
        ip_address: Optional[str] = None,
        event_source: str = "system"
    ):
        """写入一条不可修改的审计日志"""
        db = get_db()
        db.execute(
            """INSERT INTO audit_logs(event_type, event_source, operator, target_type, target_id, detail_json, ip_address)
               VALUES(?,?,?,?,?,?,?)""",
            (event_type, event_source, operator, target_type, target_id,
             json.dumps(detail, ensure_ascii=False) if detail else None,
             ip_address)
        )
        db.commit()

    @staticmethod
    def log_access(
        user_id: str,
        accessed_elderly_id: Optional[str],
        access_type: str,
        data_scope: Optional[str],
        authorized_by: Optional[str],
        access_result: str,
        deny_reason: Optional[str] = None,
        ip_address: Optional[str] = None
    ):
        """写入数据访问记录（access_records 表）"""
        db = get_db()
        db.execute(
            """INSERT INTO access_records(user_id, accessed_elderly_id, access_type, data_scope, authorized_by, access_result, deny_reason, ip_address)
               VALUES(?,?,?,?,?,?,?,?)""",
            (user_id, accessed_elderly_id, access_type,
             data_scope, authorized_by, access_result, deny_reason, ip_address)
        )
        db.commit()

    @staticmethod
    def query(
        event_type: Optional[str] = None,
        operator: Optional[str] = None,
        target_type: Optional[str] = None,
        limit: int = 50,
        offset: int = 0
    ) -> tuple:
        """管理员查询审计日志"""
        db = get_db()
        conditions = []
        params = []

        if event_type:
            conditions.append("event_type = ?")
            params.append(event_type)
        if operator:
            conditions.append("operator = ?")
            params.append(operator)
        if target_type:
            conditions.append("target_type = ?")
            params.append(target_type)

        where = ("WHERE " + " AND ".join(conditions)) if conditions else ""

        count_row = db.execute(f"SELECT COUNT(*) FROM audit_logs {where}", params).fetchone()
        total = count_row[0] if count_row else 0

        rows = db.execute(
            f"SELECT * FROM audit_logs {where} ORDER BY created_at DESC LIMIT ? OFFSET ?",
            params + [limit, offset]
        ).fetchall()

        return total, [dict(r) for r in rows]
