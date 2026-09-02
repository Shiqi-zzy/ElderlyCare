"""健康档案 API 路由（Phase 3）"""
from fastapi import APIRouter, Depends, HTTPException
from ...models.health_record import HealthRecordCreate
from ...services.health_record_service import HealthRecordService
from ...services.audit_service import AuditService
from ..middleware.auth_mw import get_current_user, require_role
from ..middleware.perm_mw import ElderlyAccessChecker

router = APIRouter(tags=["健康档案"])


# ═══════════════════════════════════════════════════════
# 医院端：健康档案录入与查看
# ═══════════════════════════════════════════════════════

@router.post("/api/hospital/health/{elderly_id}/add")
async def add_health_record(
    elderly_id: str,
    req: HealthRecordCreate,
    current_user: dict = Depends(require_role("hospital")),
    _access: dict = Depends(ElderlyAccessChecker("view_health"))
):
    """医院端录入健康档案（需诊疗绑定授权）"""
    record_id = HealthRecordService.add_record(
        elderly_id=elderly_id,
        record_type=req.record_type,
        content_json=req.content_json,
        created_by=current_user["user_id"],
        doctor_name=req.doctor_name or current_user.get("real_name", ""),
        hospital_name=req.hospital_name or "",
        visibility=req.visibility,
        record_date=req.record_date
    )
    if not record_id:
        raise HTTPException(status_code=500, detail="录入健康档案失败")

    AuditService.log(
        event_type="health_record_create",
        operator=current_user["user_id"],
        target_type="health_record",
        target_id=record_id,
        detail={"elderly_id": elderly_id, "record_type": req.record_type}
    )

    return {"message": "健康档案已录入", "record_id": record_id}


@router.get("/api/hospital/health/{elderly_id}")
async def get_hospital_health_records(
    elderly_id: str,
    current_user: dict = Depends(require_role("hospital", "admin")),
    _access: dict = Depends(ElderlyAccessChecker("view_health"))
):
    """医院端查看已授权老人的健康档案"""
    records = HealthRecordService.list_by_elderly(elderly_id, current_user["user_id"], "hospital")
    return {"total": len(records), "items": records}


# ═══════════════════════════════════════════════════════
# 家属端：查看老人健康档案
# ═══════════════════════════════════════════════════════

@router.get("/api/family/health/{elderly_id}")
async def get_family_health_records(
    elderly_id: str,
    current_user: dict = Depends(require_role("family")),
    _access: dict = Depends(ElderlyAccessChecker("view_health"))
):
    """家属端查看老人健康档案（完整记录）"""
    records = HealthRecordService.list_by_elderly(elderly_id, current_user["user_id"], "family")
    return {"total": len(records), "items": records}
