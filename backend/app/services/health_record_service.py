"""健康档案服务（Phase 3）"""
from typing import Optional
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso


class HealthRecordService:

    @staticmethod
    def add_record(elderly_id: str, record_type: str, content_json: str,
                   created_by: str, doctor_name: str = "", hospital_name: str = "",
                   visibility: str = "both", record_date: str = "") -> Optional[str]:
        """录入健康档案"""
        db = get_db()
        record_id = generate_uuid()
        ts = now_iso()
        rd = record_date if record_date else ts

        db.execute(
            """INSERT INTO health_records(id, elderly_id, record_type, record_date,
               doctor_name, hospital_name, content_json, visibility, created_by, created_at)
               VALUES(?,?,?,?,?,?,?,?,?,?)""",
            (record_id, elderly_id, record_type, rd, doctor_name, hospital_name,
             content_json, visibility, created_by, ts)
        )
        db.commit()
        return record_id

    @staticmethod
    def list_by_elderly(elderly_id: str, viewer_user_id: str, viewer_role: str) -> list:
        """查看老人健康档案（权限校验：家属 or 授权医院）"""
        db = get_db()

        # 家属可直接查看
        if viewer_role == "family":
            elderly = db.execute(
                "SELECT binding_family_user_id FROM elderly WHERE id=?",
                (elderly_id,)
            ).fetchone()
            if not elderly or elderly["binding_family_user_id"] != viewer_user_id:
                return []

        # 医院需有活跃授权 + health_records scope
        if viewer_role == "hospital":
            auth = db.execute(
                """SELECT id FROM authorizations
                   WHERE elderly_id=? AND grantee_user_id=? AND status='active'
                   AND datetime(effective_until) > datetime('now','localtime')
                   AND (permission_type IN ('health_records','all')
                        OR data_scope LIKE '%"medical":true%')""",
                (elderly_id, viewer_user_id)
            ).fetchone()
            if not auth:
                return []

        rows = db.execute(
            """SELECT * FROM health_records WHERE elderly_id=?
               AND visibility IN ('family','hospital','both')
               ORDER BY record_date DESC""",
            (elderly_id,)
        ).fetchall()
        return [dict(r) for r in rows]

    @staticmethod
    def get_record(record_id: str, viewer_user_id: str, viewer_role: str) -> Optional[dict]:
        """获取单条健康档案（权限校验）"""
        db = get_db()
        row = db.execute("SELECT * FROM health_records WHERE id=?", (record_id,)).fetchone()
        if not row:
            return None
        record = dict(row)

        # 复用 list_by_elderly 的权限逻辑
        allowed = HealthRecordService.list_by_elderly(
            record["elderly_id"], viewer_user_id, viewer_role
        )
        # 如果 list 返回空列表说明无权限
        if not allowed and viewer_role != "family":
            return None
        # 家属还需确认绑定关系
        if viewer_role == "family":
            elderly = db.execute(
                "SELECT binding_family_user_id FROM elderly WHERE id=?",
                (record["elderly_id"],)
            ).fetchone()
            if not elderly or elderly["binding_family_user_id"] != viewer_user_id:
                return None

        return record
