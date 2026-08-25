"""老人档案服务"""
from typing import Optional
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso
from ..models.elderly import ElderlyCreate, ElderlyUpdate


class ElderlyService:

    @staticmethod
    def create(req: ElderlyCreate) -> Optional[str]:
        """创建老人档案"""
        db = get_db()
        elderly_id = generate_uuid()
        ts = now_iso()

        db.execute(
            """INSERT INTO elderly(id, name, gender, birth_date, id_card, phone, address,
               emergency_contact, medical_history, care_level, binding_family_user_id, created_at, updated_at)
               VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (elderly_id, req.name, req.gender or "", req.birth_date, req.id_card, req.phone,
             req.address, req.emergency_contact, req.medical_history, req.care_level,
             req.binding_family_user_id, ts, ts)
        )
        db.commit()
        return elderly_id

    @staticmethod
    def get(elderly_id: str) -> Optional[dict]:
        """获取老人档案（原始数据，脱敏在服务层外处理）"""
        db = get_db()
        row = db.execute("SELECT * FROM elderly WHERE id=? AND is_active=1", (elderly_id,)).fetchone()
        return dict(row) if row else None

    @staticmethod
    def list_by_family(family_user_id: str) -> list:
        """家属获取自己绑定的所有老人"""
        db = get_db()
        rows = db.execute(
            "SELECT * FROM elderly WHERE binding_family_user_id=? AND is_active=1 ORDER BY created_at DESC",
            (family_user_id,)
        ).fetchall()
        return [dict(r) for r in rows]

    @staticmethod
    def update(elderly_id: str, req: ElderlyUpdate) -> bool:
        """更新老人档案"""
        db = get_db()
        updates = {}
        if req.name is not None:
            updates["name"] = req.name
        if req.gender is not None:
            updates["gender"] = req.gender
        if req.birth_date is not None:
            updates["birth_date"] = req.birth_date
        if req.phone is not None:
            updates["phone"] = req.phone
        if req.address is not None:
            updates["address"] = req.address
        if req.emergency_contact is not None:
            updates["emergency_contact"] = req.emergency_contact
        if req.medical_history is not None:
            updates["medical_history"] = req.medical_history
        if req.care_level is not None:
            updates["care_level"] = req.care_level

        if not updates:
            return True

        updates["updated_at"] = now_iso()
        set_clause = ", ".join(f"{k}=?" for k in updates)
        values = list(updates.values()) + [elderly_id]

        db.execute(f"UPDATE elderly SET {set_clause} WHERE id=?", values)
        db.commit()
        return True

    @staticmethod
    def set_privacy_pause(elderly_id: str, paused: bool) -> bool:
        """老人暂停/恢复监控（隐私控制）"""
        db = get_db()
        db.execute(
            "UPDATE elderly SET privacy_paused=?, updated_at=? WHERE id=?",
            (1 if paused else 0, now_iso(), elderly_id)
        )
        db.commit()
        return True

    @staticmethod
    def get_privacy_status(elderly_id: str) -> bool:
        """获取老人监控暂停状态"""
        db = get_db()
        row = db.execute("SELECT privacy_paused FROM elderly WHERE id=?", (elderly_id,)).fetchone()
        return bool(row["privacy_paused"]) if row else False
