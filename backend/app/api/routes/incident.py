"""四端协同应急事件 API 路由（云端对齐轨）

挂载前缀 /api/incident。状态机、加急升级、排班、处罚、服务记录逻辑见 services/incident_service。
"""
from fastapi import APIRouter, Depends, HTTPException

from ...models.incident import (
    SimulateFallReq, ContactFamilyReq, DispatchReq, HospitalCompleteReq,
    CommunityCompleteReq, SelfCloseReq, ShiftReq, HcBindingReq, HcReviewReq,
)
from ...services import incident_service as svc
from ..middleware.auth_mw import get_current_user, require_role
from ...core.database import get_db

router = APIRouter(prefix="/api/incident", tags=["四端协同-事件"])


def _staff(user: dict):
    """取当前员工姓名与所属机构"""
    db = get_db()
    row = db.execute("SELECT real_name, institution_id, role FROM users WHERE id=?",
                     (user["user_id"],)).fetchone()
    if not row:
        raise HTTPException(404, "当前用户不存在")
    return {"name": row["real_name"] or "", "org_id": row["institution_id"], "role": row["role"]}


def _ok(detail="操作成功", **extra):
    return {"ok": True, "detail": detail, **extra}


# ─────────────── 触发（演示：家属端模拟 RK3 跌倒） ───────────────
@router.post("/simulate-fall")
async def simulate_fall(req: SimulateFallReq, current_user: dict = Depends(require_role("family", "community", "admin"))):
    try:
        inc = svc.raise_incident(
            elderly_id=req.elderly_id, elderly_name=req.elderly_name,
            family_user_id=req.family_user_id or current_user["user_id"], family_phone=req.family_phone,
            community_org_id=req.community_org_id, community_staff_id=req.community_staff_id,
            building_no=req.building_no, unit_no=req.unit_no, room_no=req.room_no,
            alarm_level=req.alarm_level)
    except ValueError as e:
        raise HTTPException(400, str(e))
    return _ok("已模拟触发跌倒告警，三端同步接收", incident=inc)


# ─────────────── 社区处置 ───────────────
@router.post("/{incident_id}/contact-family")
async def contact_family(incident_id: str, req: ContactFamilyReq,
                         current_user: dict = Depends(require_role("community"))):
    try:
        return _ok("已登记联系家属", incident=svc.contact_family(incident_id, req.note))
    except ValueError as e:
        raise HTTPException(400, str(e))


@router.post("/{incident_id}/dispatch")
async def dispatch(incident_id: str, req: DispatchReq, current_user: dict = Depends(require_role("community"))):
    staff = _staff(current_user)
    try:
        return _ok("已向绑定医院推送急救告警", incident=svc.request_dispatch(incident_id, req.hospital_org_id))
    except ValueError as e:
        raise HTTPException(400, str(e))


@router.post("/{incident_id}/community-complete")
async def community_complete(incident_id: str, req: CommunityCompleteReq,
                             current_user: dict = Depends(require_role("community"))):
    staff = _staff(current_user)
    try:
        return _ok("社区已闭环", incident=svc.community_complete(
            incident_id, req.note, current_user["user_id"], staff["name"]))
    except ValueError as e:
        raise HTTPException(400, str(e))


@router.post("/{incident_id}/self-close")
async def self_close(incident_id: str, req: SelfCloseReq, current_user: dict = Depends(require_role("community"))):
    staff = _staff(current_user)
    try:
        return _ok("社区自行闭环", incident=svc.self_close(
            incident_id, req.note, current_user["user_id"], staff["name"]))
    except ValueError as e:
        raise HTTPException(400, str(e))


# ─────────────── 医院处置 ───────────────
@router.post("/{incident_id}/accept")
async def accept(incident_id: str, current_user: dict = Depends(require_role("hospital"))):
    staff = _staff(current_user)
    ok = svc.accept_by_doctor(incident_id, current_user["user_id"], staff["name"])
    if not ok:
        raise HTTPException(409, "该事件已被其他值班医生接走")
    return _ok("接单成功（先接先得）", incident=svc.get_incident(incident_id))


@router.post("/{incident_id}/hospital-complete")
async def hospital_complete(incident_id: str, req: HospitalCompleteReq,
                            current_user: dict = Depends(require_role("hospital"))):
    staff = _staff(current_user)
    try:
        return _ok("医院处置完成", incident=svc.hospital_complete(
            incident_id, req.treatment, current_user["user_id"], staff["name"]))
    except ValueError as e:
        raise HTTPException(400, str(e))


# ─────────────── 调度（系统/管理员触发加急升级） ───────────────
@router.post("/tick")
async def tick(current_user: dict = Depends(require_role("admin"))):
    return _ok("调度完成", processed=svc.tick())


# ─────────────── 列表 / 详情 ───────────────
@router.get("/{incident_id}")
async def detail(incident_id: str, current_user: dict = Depends(get_current_user)):
    try:
        return svc.get_incident(incident_id)
    except ValueError as e:
        raise HTTPException(404, str(e))


@router.get("/community/list/{org_id}")
async def community_list(org_id: str, active_only: bool = False,
                         current_user: dict = Depends(require_role("community", "admin"))):
    return {"items": svc.list_incidents("community", org_id, active_only)}


@router.get("/hospital/list/{org_id}")
async def hospital_list(org_id: str, active_only: bool = False,
                        current_user: dict = Depends(require_role("hospital", "admin"))):
    return {"items": svc.list_incidents("hospital", org_id, active_only)}


@router.get("/family/list/{user_id}")
async def family_list(user_id: str, current_user: dict = Depends(require_role("family", "admin"))):
    return {"items": svc.list_incidents("family", user_id)}


@router.get("/hospital-grid/{org_id}")
async def hospital_grid(org_id: str, current_user: dict = Depends(require_role("hospital", "admin"))):
    return {"items": svc.hospital_grid(org_id)}


# ─────────────── 排班 ───────────────
@router.post("/shift")
async def create_shift(req: ShiftReq, current_user: dict = Depends(require_role("hospital", "community", "admin"))):
    staff = _staff(current_user)
    return _ok("班次已添加", **svc.create_shift(
        current_user["user_id"], staff["name"], req.role, req.title,
        req.start_time, req.end_time, req.location, req.schedule_mode, req.weekday, req.schedule_date))


@router.get("/shifts")
async def shifts(role: str = "hospital", current_user: dict = Depends(get_current_user)):
    return {"items": svc.list_shifts(role=role)}


# ─────────────── 医院-社区绑定 ───────────────
@router.post("/hc-binding")
async def apply_hc(req: HcBindingReq, current_user: dict = Depends(require_role("hospital", "admin"))):
    staff = _staff(current_user)
    hosp = req.hospital_org_id or staff["org_id"]
    try:
        return _ok("已提交绑定申请，等待管理端审批", **svc.apply_hc_binding(hosp, req.community_org_id, req.note))
    except ValueError as e:
        raise HTTPException(400, str(e))


@router.post("/hc-binding/{binding_id}/review")
async def review_hc(binding_id: str, req: HcReviewReq, current_user: dict = Depends(require_role("admin"))):
    try:
        return _ok("审批完成", **svc.review_hc_binding(binding_id, req.approved, current_user["user_id"], req.note))
    except ValueError as e:
        raise HTTPException(400, str(e))


@router.get("/hc-binding/list")
async def hc_list(current_user: dict = Depends(get_current_user)):
    staff = _staff(current_user)
    if staff["role"] == "hospital":
        return {"items": svc.list_hc_bindings(hospital_org_id=staff["org_id"])}
    return {"items": svc.list_hc_bindings()}


# ─────────────── 服务记录 / 绩效 / 处罚 ───────────────
@router.get("/service-records")
async def records(elderly_id: str | None = None, side: str | None = None,
                  current_user: dict = Depends(get_current_user)):
    return {"items": svc.service_records(elderly_id, side)}


@router.get("/performance/{doctor_id}")
async def performance(doctor_id: str, current_user: dict = Depends(get_current_user)):
    return svc.doctor_performance(doctor_id)


@router.get("/penalties")
async def penalties(current_user: dict = Depends(get_current_user)):
    staff = _staff(current_user)
    target = None if staff["role"] == "admin" else current_user["user_id"]
    return {"items": svc.list_penalties(target)}


@router.post("/penalty/{penalty_id}/revoke")
async def revoke_penalty(penalty_id: str, current_user: dict = Depends(require_role("admin"))):
    return _ok("已撤销处罚", **svc.revoke_penalty(penalty_id, current_user["user_id"]))
