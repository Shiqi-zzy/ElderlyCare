"""家属端 API 路由"""
from fastapi import APIRouter, HTTPException, status, Depends, Query
from typing import Optional
from ...models.elderly import ElderlyCreate, ElderlyUpdate, ElderlyResponse
from ...models.device import DeviceRegister, DeviceResponse
from ...models.alarm import AlarmAcknowledge, AlarmResolve, AlarmResponse, AlarmsResponse
from ...models.authorization import AuthorizationGrant, AuthorizationRevoke, AuthorizationResponse, AuthorizationsResponse
from ...services.elderly_service import ElderlyService
from ...services.device_service import DeviceService
from ...services.alarm_service import AlarmService
from ...services.permission_service import PermissionService
from ...services.desensitize_service import DesensitizeService
from ...services.audit_service import AuditService
from ..middleware.auth_mw import get_current_user, require_role
from ..middleware.perm_mw import ElderlyAccessChecker

router = APIRouter(prefix="/api/family", tags=["家属端"])


# ──────────────────── 老人档案 ────────────────────

@router.post("/elderly", response_model=ElderlyResponse)
async def create_elderly(req: ElderlyCreate, current_user: dict = Depends(require_role("family"))):
    """创建老人档案"""
    req.binding_family_user_id = current_user["user_id"]
    elderly_id = ElderlyService.create(req)
    if not elderly_id:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="创建失败")

    AuditService.log("elderly_create", current_user["user_id"], "elderly", elderly_id)
    elderly = DesensitizeService.get_elderly_for_role(elderly_id, current_user["user_id"])
    return ElderlyResponse(**elderly)


@router.get("/elderly/list")
async def list_my_elderly(current_user: dict = Depends(require_role("family"))):
    """我的老人列表（家属）"""
    items = ElderlyService.list_by_family(current_user["user_id"])
    # 家属角色全量数据
    return {"total": len(items), "items": items}


@router.get("/elderly/{elderly_id}")
async def get_elderly(
    elderly_id: str,
    current_user: dict = Depends(require_role("family")),
    access: dict = Depends(ElderlyAccessChecker("view_alarm"))
):
    """获取老人详情（权限检查+脱敏）"""
    elderly = DesensitizeService.get_elderly_for_role(elderly_id, current_user["user_id"])
    if not elderly:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="老人不存在")
    return elderly


@router.put("/elderly/{elderly_id}")
async def update_elderly(
    elderly_id: str, req: ElderlyUpdate,
    current_user: dict = Depends(require_role("family")),
    access: dict = Depends(ElderlyAccessChecker("view_alarm"))
):
    """更新老人档案"""
    success = ElderlyService.update(elderly_id, req)
    if not success:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="老人不存在")
    AuditService.log("elderly_update", current_user["user_id"], "elderly", elderly_id)
    return {"message": "更新成功"}


# ──────────────────── 设备管理 ────────────────────

@router.get("/devices/{elderly_id}")
async def get_devices(
    elderly_id: str,
    current_user: dict = Depends(require_role("family")),
    access: dict = Depends(ElderlyAccessChecker("device_status"))
):
    """获取老人设备列表"""
    devices = DeviceService.list_by_elderly(elderly_id)
    return {"total": len(devices), "items": devices}


@router.put("/devices/{device_id}/stream-url")
async def update_device_stream_url(
    device_id: str,
    stream_url: str = Query(..., min_length=1),
    current_user: dict = Depends(require_role("family"))
):
    """家属为设备设置流地址（如 RTMP/HLS 拉流地址）"""
    device = DeviceService.get(device_id)
    if not device:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="设备不存在")
    # 确认设备属于该家属的老人
    elderly = ElderlyService.get(device["elderly_id"])
    if not elderly or elderly.get("binding_family_user_id") != current_user["user_id"]:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="无权操作该设备")
    DeviceService.update_stream_url(device_id, stream_url)
    AuditService.log("device_stream_url_update", current_user["user_id"], "device", device_id)
    return {"message": "流地址已更新", "stream_url": stream_url}


# ──────────────────── 告警 ────────────────────

@router.get("/alarms/{elderly_id}")
async def get_alarms(
    elderly_id: str,
    level: Optional[str] = Query(None),
    status_filter: Optional[str] = Query(None, alias="status"),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("family")),
    access: dict = Depends(ElderlyAccessChecker("view_alarm"))
):
    """获取老人告警列表"""
    total, items = AlarmService.list_by_elderly(elderly_id, level, status_filter, limit, offset)
    return {"total": total, "items": items}


@router.get("/alarm/{alarm_id}")
async def get_alarm_detail(
    alarm_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """获取告警详情"""
    alarm = AlarmService.get(alarm_id)
    if not alarm:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="告警不存在")
    # 确认家属权限
    access = ElderlyAccessChecker("view_alarm")
    await access(alarm["elderly_id"], current_user)
    # 按角色脱敏
    result = DesensitizeService.get_alarm_for_role(alarm_id, current_user["user_id"])
    return result


@router.post("/alarm/{alarm_id}/acknowledge")
async def acknowledge_alarm(
    alarm_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """确认告警"""
    alarm = AlarmService.get(alarm_id)
    if not alarm:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="告警不存在")

    success = AlarmService.acknowledge(alarm_id, current_user["user_id"])
    if not success:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="告警状态不允许确认")
    return {"message": "已确认告警"}


# ──────────────────── 授权管理 ────────────────────

@router.post("/authorization/grant")
async def grant_authorization(
    req: AuthorizationGrant,
    current_user: dict = Depends(require_role("family"))
):
    """家属授予数据访问权限"""
    auth_id = PermissionService.grant(
        elderly_id=req.elderly_id,
        grantor_user_id=current_user["user_id"],
        grantee_user_id=req.grantee_user_id,
        permission_type=req.permission_type,
        data_scope=req.data_scope,
        effective_until=req.effective_until
    )
    if not auth_id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="授权失败，请检查被授权人信息")
    return {"message": "授权成功", "authorization_id": auth_id}


@router.post("/authorization/revoke/{auth_id}")
async def revoke_authorization(
    auth_id: str,
    req: AuthorizationRevoke = AuthorizationRevoke(),
    current_user: dict = Depends(require_role("family"))
):
    """家属一键撤销授权"""
    success = PermissionService.revoke(auth_id, current_user["user_id"], req.revoke_reason)
    if not success:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="授权不存在或已失效")
    return {"message": "授权已撤销"}


@router.get("/authorization/list/{elderly_id}")
async def list_authorizations(
    elderly_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """查看老人所有授权"""
    items = PermissionService.list_by_elderly(elderly_id)
    return {"total": len(items), "items": items}


# ──────────────────── 授权审批（家属处理社区/医院申请）────────────────────

@router.get("/authorization/requests")
async def pending_authorization_requests(current_user: dict = Depends(require_role("family"))):
    """家属端查看待审批的授权申请（社区/医院发来的）"""
    from ...core.database import get_db
    db = get_db()
    rows = db.execute(
        """SELECT a.*, e.name as elderly_name,
                  u.real_name as grantee_name, u.phone as grantee_phone,
                  i.name as institution_name
           FROM authorizations a
           JOIN elderly e ON a.elderly_id = e.id
           JOIN users u ON a.grantee_user_id = u.id
           LEFT JOIN institutions i ON a.grantee_institution_id = i.id
           WHERE a.grantor_user_id = ? AND a.status = 'pending'
           ORDER BY a.created_at DESC""",
        (current_user["user_id"],)
    ).fetchall()
    return {"total": len(rows), "items": [dict(r) for r in rows]}


@router.post("/authorization/requests/{auth_id}/approve")
async def approve_authorization_request(
    auth_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """家属审批通过授权申请"""
    from ...core.database import get_db
    from ...services.audit_service import AuditService

    db = get_db()
    row = db.execute(
        "SELECT * FROM authorizations WHERE id = ? AND grantor_user_id = ? AND status = 'pending'",
        (auth_id, current_user["user_id"])
    ).fetchone()
    if not row:
        raise HTTPException(status_code=404, detail="申请不存在或已处理")

    db.execute("UPDATE authorizations SET status = 'active' WHERE id = ?", (auth_id,))
    db.commit()

    AuditService.log(
        event_type="auth_request_approved",
        operator=current_user["user_id"],
        target_type="authorization",
        target_id=auth_id,
        detail={"action": "家属审批通过"}
    )

    return {"message": "已审批通过，授权生效"}


@router.post("/authorization/requests/{auth_id}/reject")
async def reject_authorization_request(
    auth_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """家属拒绝授权申请"""
    from ...core.database import get_db
    from ...services.audit_service import AuditService

    db = get_db()
    row = db.execute(
        "SELECT * FROM authorizations WHERE id = ? AND grantor_user_id = ? AND status = 'pending'",
        (auth_id, current_user["user_id"])
    ).fetchone()
    if not row:
        raise HTTPException(status_code=404, detail="申请不存在或已处理")

    db.execute("UPDATE authorizations SET status = 'rejected' WHERE id = ?", (auth_id,))
    db.commit()

    AuditService.log(
        event_type="auth_request_rejected",
        operator=current_user["user_id"],
        target_type="authorization",
        target_id=auth_id,
        detail={"action": "家属拒绝"}
    )

    return {"message": "已拒绝授权申请"}


# ──────────────────── 隐私控制 ────────────────────

@router.post("/monitoring/pause/{elderly_id}")
async def pause_monitoring(
    elderly_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """暂停监控（老人隐私控制）"""
    success = ElderlyService.set_privacy_pause(elderly_id, True)
    if not success:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="老人不存在")
    AuditService.log("monitoring_pause", current_user["user_id"], "elderly", elderly_id)
    return {"message": "监控已暂停"}


@router.post("/monitoring/resume/{elderly_id}")
async def resume_monitoring(
    elderly_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """恢复监控"""
    success = ElderlyService.set_privacy_pause(elderly_id, False)
    if not success:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="老人不存在")
    AuditService.log("monitoring_resume", current_user["user_id"], "elderly", elderly_id)
    return {"message": "监控已恢复"}


@router.get("/privacy/status/{elderly_id}")
async def privacy_status(
    elderly_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """查询监控暂停状态"""
    paused = ElderlyService.get_privacy_status(elderly_id)
    return {"elderly_id": elderly_id, "privacy_paused": paused}


# ──────────────────── 设备验证码（三端联动）────────────────────

@router.post("/device/{device_id}/generate-code")
async def generate_device_code(
    device_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """家属为设备生成验证码（供社区/医院绑定）"""
    from ...services.device_code_service import DeviceCodeService
    try:
        result = DeviceCodeService.generate_code(device_id, current_user["user_id"])
        AuditService.log(
            "device_code_generated", current_user["user_id"],
            "device_verification_code", result["code_id"],
            {"device_id": device_id}
        )
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.get("/device/codes")
async def list_device_codes(current_user: dict = Depends(require_role("family"))):
    """查看活跃的设备验证码列表"""
    from ...services.device_code_service import DeviceCodeService
    items = DeviceCodeService.list_active_codes(current_user["user_id"])
    return {"total": len(items), "items": items}


@router.post("/device/code/{code_id}/revoke")
async def revoke_device_code(
    code_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """撤销未使用的设备验证码"""
    from ...services.device_code_service import DeviceCodeService
    success = DeviceCodeService.revoke_code(code_id, current_user["user_id"])
    if not success:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="验证码不存在或已失效")
    AuditService.log(
        "device_code_revoked", current_user["user_id"],
        "device_verification_code", code_id
    )
    return {"message": "验证码已撤销"}


# ──────────────────── 健康档案查看（Phase 3）────────────────────

@router.get("/health/{elderly_id}")
async def family_health_records(
    elderly_id: str,
    current_user: dict = Depends(require_role("family"))
):
    """家属查看老人健康档案"""
    from ...services.health_record_service import HealthRecordService
    records = HealthRecordService.list_by_elderly(elderly_id, current_user["user_id"], "family")
    return {"total": len(records), "items": records}
