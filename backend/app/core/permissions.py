"""权限引擎：基于角色的访问控制 (RBAC)  + authorization 表检查"""
from typing import Optional
from .config import (
    ROLE_FAMILY, ROLE_COMMUNITY, ROLE_HOSPITAL, ROLE_ADMIN,
    AUTH_STATUS_ACTIVE, ALARM_LEVEL_LOW
)
from .database import get_db


# ────────────────────────────────────────────
# 角色 → 可访问的数据范围
# ────────────────────────────────────────────
ROLE_DATA_SCOPE = {
    ROLE_FAMILY:    {"video": True, "medical": True,  "alarm": True,  "desensitized": False},
    ROLE_COMMUNITY: {"video": False,"medical": False, "alarm": True,  "desensitized": True},
    ROLE_HOSPITAL:  {"video": False,"medical": True,  "alarm": False, "desensitized": True},
    ROLE_ADMIN:     {"video": False,"medical": False, "alarm": False, "desensitized": False},
}

# 管理员不可直接访问老人数据
ADMIN_NO_ELDERLY_ACCESS = True


def get_user_role(user_id: str) -> Optional[str]:
    """查询用户角色"""
    db = get_db()
    row = db.execute("SELECT role FROM users WHERE id=? AND is_active=1", (user_id,)).fetchone()
    return row["role"] if row else None


def check_elderly_access(user_id: str, elderly_id: str, access_type: str) -> dict:
    """
    检查用户是否有权访问指定老人的指定类型数据。

    返回:
      {"granted": bool, "reason": str, "data_scope": dict, "authorization_id": str|None}
    """
    db = get_db()
    role = get_user_role(user_id)

    # 管理员不直接访问老人数据
    if role == ROLE_ADMIN and ADMIN_NO_ELDERLY_ACCESS:
        return {"granted": False, "reason": "管理员无权直接访问老人数据，请通过审计日志查看", "data_scope": {}, "authorization_id": None}

    # 家属：检查是否是绑定家属
    if role == ROLE_FAMILY:
        row = db.execute(
            "SELECT binding_family_user_id FROM elderly WHERE id=? AND is_active=1",
            (elderly_id,)
        ).fetchone()
        if row and row["binding_family_user_id"] == user_id:
            return {
                "granted": True,
                "reason": "家属直接绑定关系",
                "data_scope": ROLE_DATA_SCOPE[ROLE_FAMILY],
                "authorization_id": None
            }
        return {"granted": False, "reason": "该老人未绑定到您的账户", "data_scope": {}, "authorization_id": None}

    # 社区/医院：检查 authorizations 表
    if role in (ROLE_COMMUNITY, ROLE_HOSPITAL):
        row = db.execute("""
            SELECT a.id, a.data_scope, a.effective_until, a.permission_type
            FROM authorizations a
            WHERE a.grantee_user_id = ?
              AND a.elderly_id = ?
              AND a.status = ?
              AND datetime(a.effective_until) > datetime('now','localtime')
            ORDER BY a.created_at DESC
            LIMIT 1
        """, (user_id, elderly_id, AUTH_STATUS_ACTIVE)).fetchone()

        if not row:
            return {"granted": False, "reason": "无有效授权，请先申请绑定或等待家属授权", "data_scope": {}, "authorization_id": None}

        import json
        data_scope = json.loads(row["data_scope"])

        # 检查授权 scope 是否包含请求的数据类型
        scope_key = _access_type_to_scope_key(access_type)
        if scope_key and not data_scope.get(scope_key, False):
            return {"granted": False, "reason": f"授权范围不包含 {access_type} 类型数据", "data_scope": data_scope, "authorization_id": row["id"]}

        return {
            "granted": True,
            "reason": "授权有效",
            "data_scope": data_scope,
            "authorization_id": row["id"]
        }

    return {"granted": False, "reason": "未知角色", "data_scope": {}, "authorization_id": None}


def check_self_only(user_id: str, target_user_id: str) -> bool:
    """检查是否操作自己的数据（家属只能操作自己的内容）"""
    return user_id == target_user_id


def _access_type_to_scope_key(access_type: str) -> Optional[str]:
    """将访问类型映射到 data_scope 的 JSON key"""
    mapping = {
        "view_live": "video",
        "view_recording": "video",
        "view_health": "medical",
        "view_alarm": "alarm",
        "monitoring": "video",
        "health_records": "medical",
        "alarm_video": "alarm",
        "device_status": "video",
    }
    return mapping.get(access_type)
