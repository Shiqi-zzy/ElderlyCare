"""设备运维巡检台账服务"""
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso


class MaintenanceService:
    """设备巡检记录管理"""

    @staticmethod
    def log_inspection(
        device_id: str,
        inspector_id: str,
        maintenance_type: str,
        status: str,
        findings: str = "",
        photos: str = "",
        next_inspection_date: str = ""
    ) -> str:
        """
        记录一次设备巡检。

        Args:
            device_id: 设备ID
            inspector_id: 巡检人用户ID
            maintenance_type: 巡检类型 (routine/repair/replace/emergency)
            status: 设备状态 (normal/needs_repair/replaced/fault)
            findings: 巡检发现
            photos: 照片URL（逗号分隔）
            next_inspection_date: 下次巡检日期

        Returns:
            巡检记录ID
        """
        db = get_db()

        # 验证设备存在
        device = db.execute("SELECT * FROM devices WHERE id = ?", (device_id,)).fetchone()
        if not device:
            raise ValueError("设备不存在")

        inspection_id = generate_uuid()
        ts = now_iso()

        db.execute(
            """INSERT INTO device_maintenance(
                   id, device_id, maintenance_type, inspector_id,
                   inspection_date, status, findings, photos,
                   next_inspection_date, created_at)
               VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (inspection_id, device_id, maintenance_type, inspector_id,
             ts, status, findings, photos, next_inspection_date or "", ts)
        )

        # 如果设备状态为 fault，同时更新 devices 表状态
        if status == "fault":
            db.execute(
                "UPDATE devices SET status='fault' WHERE id=?",
                (device_id,)
            )
        elif status == "normal":
            db.execute(
                "UPDATE devices SET status='online' WHERE id=?",
                (device_id,)
            )

        db.commit()

        from .audit_service import AuditService
        AuditService.log(
            event_type="device_inspection",
            operator=inspector_id,
            target_type="device_maintenance",
            target_id=inspection_id,
            detail={"device_id": device_id, "status": status, "type": maintenance_type}
        )

        return inspection_id

    @staticmethod
    def list_by_device(device_id: str, limit: int = 50, offset: int = 0) -> tuple:
        """获取某设备的维护历史"""
        db = get_db()
        count = db.execute(
            "SELECT COUNT(*) FROM device_maintenance WHERE device_id=?",
            (device_id,)
        ).fetchone()[0]
        rows = db.execute(
            """SELECT dm.*, u.real_name as inspector_name
               FROM device_maintenance dm
               LEFT JOIN users u ON dm.inspector_id = u.id
               WHERE dm.device_id = ?
               ORDER BY dm.inspection_date DESC
               LIMIT ? OFFSET ?""",
            (device_id, limit, offset)
        ).fetchall()
        return count, [dict(r) for r in rows]

    @staticmethod
    def list_by_inspector(inspector_id: str, limit: int = 50, offset: int = 0) -> tuple:
        """获取某巡检员的所有记录"""
        db = get_db()
        count = db.execute(
            "SELECT COUNT(*) FROM device_maintenance WHERE inspector_id=?",
            (inspector_id,)
        ).fetchone()[0]
        rows = db.execute(
            """SELECT dm.*, d.device_name, d.device_type
               FROM device_maintenance dm
               JOIN devices d ON dm.device_id = d.id
               WHERE dm.inspector_id = ?
               ORDER BY dm.inspection_date DESC
               LIMIT ? OFFSET ?""",
            (inspector_id, limit, offset)
        ).fetchall()
        return count, [dict(r) for r in rows]
