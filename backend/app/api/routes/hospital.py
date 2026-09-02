"""医院端 API 路由"""
from fastapi import APIRouter, Depends, HTTPException
from ...models.authorization import AuthorizationGrant
from ...models.emergency import EmergencyRequest
from ..middleware.auth_mw import get_current_user, require_role

router = APIRouter(prefix="/api/hospital", tags=["医院端"])


@router.get("/dashboard")
async def dashboard(current_user: dict = Depends(require_role("hospital", "admin"))):
    """医院工作台概览"""
    from ...core.database import get_db
    db = get_db()
    bound_count = db.execute(
        """SELECT COUNT(*) FROM authorizations
           WHERE grantee_user_id = ? AND status = 'active'
           AND datetime(effective_until) > datetime('now','localtime')""",
        (current_user["user_id"],)
    ).fetchone()[0]
    return {
        "bound_elderly_count": bound_count,
        "message": "医院端工作台"
    }


@router.get("/elderly/list")
async def elderly_list(current_user: dict = Depends(require_role("hospital"))):
    """绑定老人列表（仅医疗数据）"""
    from ...core.database import get_db
    db = get_db()
    rows = db.execute(
        """SELECT e.id, e.name, e.gender, e.birth_date, e.medical_history, e.care_level
           FROM elderly e
           JOIN authorizations a ON e.id = a.elderly_id
           WHERE a.grantee_user_id = ? AND a.status = 'active'
           AND datetime(a.effective_until) > datetime('now','localtime')""",
        (current_user["user_id"],)
    ).fetchall()
    return {"total": len(rows), "items": [dict(r) for r in rows]}


# ═══════════════════════════════════════════════════════
# 授权申请（医院 → 家属审批）
# ═══════════════════════════════════════════════════════

@router.post("/authorization/request")
async def request_authorization(req: AuthorizationGrant, current_user: dict = Depends(require_role("hospital"))):
    """医院端发起授权申请 → 创建 pending 状态授权记录 → 家属审批"""
    from ...core.database import get_db
    from ...core.security import generate_uuid, now_iso
    from ...services.audit_service import AuditService

    db = get_db()

    elderly = db.execute("SELECT * FROM elderly WHERE id = ?", (req.elderly_id,)).fetchone()
    if not elderly:
        raise HTTPException(status_code=404, detail="老人档案不存在")

    existing = db.execute(
        """SELECT id FROM authorizations
           WHERE elderly_id = ? AND grantee_user_id = ? AND status IN ('pending','active')""",
        (req.elderly_id, current_user["user_id"])
    ).fetchone()
    if existing:
        raise HTTPException(status_code=409, detail="已有待审批或已生效的授权申请")

    auth_id = generate_uuid()
    ts = now_iso()
    from datetime import datetime, timedelta
    default_until = (datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d %H:%M:%S")

    db.execute(
        """INSERT INTO authorizations(
               id, elderly_id, grantor_user_id, grantee_user_id, grantee_institution_id,
               permission_type, data_scope, effective_from, effective_until, status, created_at)
           VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?)""",
        (auth_id, req.elderly_id, elderly["binding_family_user_id"], current_user["user_id"],
         current_user.get("institution_id"), req.permission_type, req.data_scope,
         ts, req.effective_until if req.effective_until else default_until, ts)
    )
    db.commit()

    AuditService.log(
        event_type="auth_request_submitted",
        operator=current_user["user_id"],
        target_type="authorization",
        target_id=auth_id,
        detail={"elderly_id": req.elderly_id, "permission_type": req.permission_type, "role": "hospital"}
    )

    return {"message": "授权申请已提交，等待家属审批", "authorization_id": auth_id, "status": "pending"}


@router.get("/authorization/requests")
async def my_authorization_requests(current_user: dict = Depends(require_role("hospital"))):
    """查看我发起的授权申请列表"""
    from ...core.database import get_db
    db = get_db()
    rows = db.execute(
        """SELECT a.*, e.name as elderly_name FROM authorizations a
           JOIN elderly e ON a.elderly_id = e.id
           WHERE a.grantee_user_id = ? ORDER BY a.created_at DESC""",
        (current_user["user_id"],)
    ).fetchall()
    return {"total": len(rows), "items": [dict(r) for r in rows]}


# ═══════════════════════════════════════════════════════
# 设备管理（设备验证码绑定）
# ═══════════════════════════════════════════════════════

@router.post("/device/bind")
async def bind_device_by_code(
    req: "DeviceBindRequest",
    current_user: dict = Depends(require_role("hospital"))
):
    """输入设备验证码绑定设备（医院端）"""
    from ...services.device_code_service import DeviceCodeService
    from ...models.device_code import DeviceBindRequest
    try:
        result = DeviceCodeService.verify_and_bind(req.code, current_user["user_id"])
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/devices/{elderly_id}")
async def hospital_devices(
    elderly_id: str,
    current_user: dict = Depends(require_role("hospital"))
):
    """查看已授权设备的老人设备列表（医院端）"""
    from ...core.database import get_db
    db = get_db()

    # 确认有活跃授权
    auth = db.execute(
        """SELECT id FROM authorizations
           WHERE elderly_id=? AND grantee_user_id=? AND status='active'
           AND datetime(effective_until)>datetime('now','localtime')""",
        (elderly_id, current_user["user_id"])
    ).fetchone()
    if not auth:
        raise HTTPException(status_code=403, detail="无有效授权，请先通过验证码绑定设备")

    # 获取老人名下所有设备
    devices = db.execute(
        """SELECT * FROM devices WHERE elderly_id=? ORDER BY created_at DESC""",
        (elderly_id,)
    ).fetchall()

    return {
        "total": len(devices),
        "items": [dict(d) for d in devices],
        "elderly_id": elderly_id
    }


# ═══════════════════════════════════════════════════════
# 急救临时权限（Phase 3）
# ═══════════════════════════════════════════════════════

@router.post("/emergency/request")
async def request_emergency_access(
    req: "EmergencyRequest",
    current_user: dict = Depends(require_role("hospital"))
):
    """医院发起急救临时24小时监控权限"""
    from ...services.emergency_service import EmergencyService
    from ...models.emergency import EmergencyRequest  # noqa: F811
    try:
        result = EmergencyService.request_emergency_access(
            elderly_id=req.elderly_id,
            hospital_user_id=current_user["user_id"],
            reason=req.reason,
            institution_id=current_user.get("institution_id", "")
        )
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/emergency/status")
async def emergency_status(current_user: dict = Depends(require_role("hospital"))):
    """查看当前急救权限状态"""
    from ...services.emergency_service import EmergencyService
    return EmergencyService.get_emergency_status(current_user["user_id"])


# ═══════════════════════════════════════════════════════
# 执业证年审（Phase 3）
# ═══════════════════════════════════════════════════════

@router.put("/license/renew")
async def renew_license(current_user: dict = Depends(require_role("hospital"))):
    """在线年审执业证（延长有效期1年）"""
    from ...core.database import get_db
    from ...core.security import now_iso
    from datetime import datetime, timedelta
    db = get_db()

    inst_id = current_user.get("institution_id")
    if not inst_id:
        raise HTTPException(status_code=400, detail="未关联机构")

    new_expiry = (datetime.now() + timedelta(days=365)).strftime("%Y-%m-%d %H:%M:%S")
    db.execute(
        "UPDATE institutions SET license_expiry=? WHERE id=?",
        (new_expiry, inst_id)
    )
    db.commit()

    from ...services.audit_service import AuditService
    AuditService.log(
        event_type="license_renewal",
        operator=current_user["user_id"],
        target_type="institution",
        target_id=inst_id,
        detail={"new_expiry": new_expiry}
    )

    return {"message": "执业证年审成功", "new_expiry": new_expiry}
