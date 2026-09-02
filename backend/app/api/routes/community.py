"""社区端 API 路由"""
from fastapi import APIRouter, Depends, HTTPException, Query
from ...models.authorization import AuthorizationGrant
from ..middleware.auth_mw import get_current_user, require_role

router = APIRouter(prefix="/api/community", tags=["社区端"])


@router.get("/dashboard")
async def dashboard(current_user: dict = Depends(require_role("community", "admin"))):
    """社区工作台概览"""
    from ...core.database import get_db
    db = get_db()
    pending_orders = db.execute(
        "SELECT COUNT(*) FROM work_orders WHERE assigned_to=? AND status IN ('pending','accepted','in_progress')",
        (current_user["user_id"],)
    ).fetchone()[0]
    return {
        "pending_work_orders": pending_orders,
        "message": "社区端工作台"
    }


@router.get("/elderly/list")
async def elderly_list(current_user: dict = Depends(require_role("community"))):
    """绑定老人台账（脱敏）"""
    from ...core.database import get_db
    db = get_db()
    rows = db.execute(
        """SELECT e.* FROM elderly e
           JOIN authorizations a ON e.id = a.elderly_id
           WHERE a.grantee_user_id = ? AND a.status = 'active'
           AND datetime(a.effective_until) > datetime('now','localtime')""",
        (current_user["user_id"],)
    ).fetchall()
    from ...core.desensitize import desensitize_elderly_record
    items = [desensitize_elderly_record(dict(r), "community") for r in rows]
    return {"total": len(items), "items": items}


# ═══════════════════════════════════════════════════════
# 授权申请（社区 → 家属审批）
# ═══════════════════════════════════════════════════════

@router.post("/authorization/request")
async def request_authorization(req: AuthorizationGrant, current_user: dict = Depends(require_role("community"))):
    """社区端发起授权申请 → 创建 pending 状态授权记录 → 家属审批"""
    from ...core.database import get_db
    from ...core.security import generate_uuid, now_iso
    from ...services.audit_service import AuditService

    db = get_db()

    # 检查老人是否存在
    elderly = db.execute("SELECT * FROM elderly WHERE id = ?", (req.elderly_id,)).fetchone()
    if not elderly:
        raise HTTPException(status_code=404, detail="老人档案不存在")

    # 检查是否已有有效/待审批的授权
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
        detail={"elderly_id": req.elderly_id, "permission_type": req.permission_type, "role": "community"}
    )

    return {"message": "授权申请已提交，等待家属审批", "authorization_id": auth_id, "status": "pending"}


@router.get("/authorization/requests")
async def my_authorization_requests(current_user: dict = Depends(require_role("community"))):
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
# 设备管理（设备验证码绑定 + 巡检台账）
# ═══════════════════════════════════════════════════════

@router.post("/device/bind")
async def bind_device_by_code(
    req: "DeviceBindRequest",
    current_user: dict = Depends(require_role("community"))
):
    """输入设备验证码绑定设备（社区端）"""
    from ...services.device_code_service import DeviceCodeService
    from ...models.device_code import DeviceBindRequest
    try:
        result = DeviceCodeService.verify_and_bind(req.code, current_user["user_id"])
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/devices/{elderly_id}")
async def community_devices(
    elderly_id: str,
    current_user: dict = Depends(require_role("community"))
):
    """查看已授权设备的老人设备列表"""
    from ...core.database import get_db
    from ...core.desensitize import desensitize_elderly_record
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


@router.post("/device/{device_id}/inspection")
async def log_device_inspection(
    device_id: str,
    req: "MaintenanceCreate",
    current_user: dict = Depends(require_role("community"))
):
    """记录设备巡检"""
    from ...services.maintenance_service import MaintenanceService
    from ...models.maintenance import MaintenanceCreate
    try:
        inspection_id = MaintenanceService.log_inspection(
            device_id=device_id,
            inspector_id=current_user["user_id"],
            maintenance_type=req.maintenance_type,
            status=req.status,
            findings=req.findings,
            photos=req.photos,
            next_inspection_date=req.next_inspection_date
        )
        return {"message": "巡检记录已保存", "inspection_id": inspection_id}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/device/{device_id}/maintenance")
async def device_maintenance_history(
    device_id: str,
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("community"))
):
    """查看设备巡检历史"""
    from ...services.maintenance_service import MaintenanceService
    total, items = MaintenanceService.list_by_device(device_id, limit, offset)
    return {"total": total, "items": items}


@router.get("/maintenance/my")
async def my_maintenance_records(
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("community"))
):
    """我的巡检记录"""
    from ...services.maintenance_service import MaintenanceService
    total, items = MaintenanceService.list_by_inspector(current_user["user_id"], limit, offset)
    return {"total": total, "items": items}
