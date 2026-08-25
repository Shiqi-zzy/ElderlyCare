"""设备管理服务"""
import json
import time
from typing import Optional
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso
from ..models.device import DeviceRegister


class DeviceService:

    @staticmethod
    def register(req: DeviceRegister) -> Optional[str]:
        """注册设备"""
        db = get_db()
        device_id = generate_uuid()
        ts = now_iso()

        db.execute(
            """INSERT INTO devices(id, device_name, device_type, manufacturer, model, elderly_id,
               location, stream_url, status, capabilities, install_date, created_at)
               VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""",
            (device_id, req.device_name, req.device_type, req.manufacturer, req.model,
             req.elderly_id, req.location, req.stream_url,
             "online", req.capabilities, ts, ts)
        )
        db.commit()
        return device_id

    @staticmethod
    def heartbeat(device_id: str, timestamp: int) -> bool:
        """设备心跳上报"""
        db = get_db()
        db.execute(
            "UPDATE devices SET last_heartbeat=?, status='online' WHERE id=?",
            (timestamp, device_id)
        )
        db.commit()
        return True

    @staticmethod
    def list_by_elderly(elderly_id: str) -> list:
        """获取老人的所有设备"""
        db = get_db()
        rows = db.execute(
            "SELECT * FROM devices WHERE elderly_id=? ORDER BY created_at DESC",
            (elderly_id,)
        ).fetchall()
        return [dict(r) for r in rows]

    @staticmethod
    def get(device_id: str) -> Optional[dict]:
        """获取设备详情"""
        db = get_db()
        row = db.execute("SELECT * FROM devices WHERE id=?", (device_id,)).fetchone()
        return dict(row) if row else None

    @staticmethod
    def update_status(device_id: str, status: str) -> bool:
        """更新设备状态"""
        db = get_db()
        db.execute("UPDATE devices SET status=? WHERE id=?", (status, device_id))
        db.commit()
        return True

    @staticmethod
    def update_stream_url(device_id: str, stream_url: str) -> bool:
        """更新设备的流地址"""
        db = get_db()
        db.execute("UPDATE devices SET stream_url=? WHERE id=?", (stream_url, device_id))
        db.commit()
        return True

    @staticmethod
    def check_offline_devices(timeout_seconds: int = 120) -> list:
        """检查心跳超时的设备，标记为离线"""
        db = get_db()
        threshold = int((time.time() - timeout_seconds) * 1000)
        db.execute(
            "UPDATE devices SET status='offline' WHERE status!='offline' AND last_heartbeat IS NOT NULL AND last_heartbeat < ?",
            (threshold,)
        )
        db.commit()

        # 返回刚被标记为离线的设备
        rows = db.execute(
            "SELECT * FROM devices WHERE status='offline' AND last_heartbeat IS NOT NULL AND last_heartbeat < ?",
            (threshold,)
        ).fetchall()
        return [dict(r) for r in rows]
