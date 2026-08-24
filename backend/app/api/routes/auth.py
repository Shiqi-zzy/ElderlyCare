"""认证路由"""
from fastapi import APIRouter, HTTPException, status, Depends
from ...models.user import SyncRequest, SelectRoleRequest, SyncResponse, UserResponse, \
    QualificationApplyRequest, QualificationStatusResponse, SendCodeRequest, LoginRequest
from ...services.auth_service import AuthService
from ...services.audit_service import AuditService
from ...core.database import get_db
from ...core.security import generate_uuid, now_iso
from ..middleware.auth_mw import get_current_user

router = APIRouter(prefix="/api/auth", tags=["认证"])


# ═══════════════════════════════════════════════════════
# 手机验证码登录（主要登录方式）
# ═══════════════════════════════════════════════════════

@router.post("/send-code")
async def send_code(req: SendCodeRequest):
    """发送手机验证码。P1 固定为 123456。"""
    return AuthService.send_code(req.phone)


@router.post("/login", response_model=SyncResponse)
async def login(req: LoginRequest):
    """手机验证码登录：验证码校验 → 查找/创建用户 → 返回 JWT"""
    if not req.role or req.role not in ("family", "community", "hospital", "admin"):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="无效角色")

    result = AuthService.login_with_phone(req.phone, req.code, req.role)

    AuditService.log(
        event_type="user_phone_login",
        operator=result.user.id,
        target_type="user",
        target_id=result.user.id,
        detail={"role": result.user.role, "phone": req.phone}
    )

    return result


# ═══════════════════════════════════════════════════════
# 设备同步（萤石 AppKey 初始化后调用）
# ═══════════════════════════════════════════════════════

@router.post("/sync", response_model=SyncResponse)
async def sync(req: SyncRequest):
    """萤石 AppKey 初始化后同步设备标识：更新 ezviz_access_token"""
    result = AuthService.sync(req)

    if result.user:
        AuditService.log(
            event_type="user_sync_login",
            operator=result.user.id,
            target_type="user",
            target_id=result.user.id,
            detail={"role": result.user.role}
        )

    return result


@router.post("/select-role", response_model=SyncResponse)
async def select_role(req: SelectRoleRequest):
    """新用户选择角色"""
    if not req.role or req.role not in ("family", "community", "hospital", "admin"):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="无效角色")

    result = AuthService.select_role(req)

    AuditService.log(
        event_type="user_select_role",
        operator=result.user.id,
        target_type="user",
        target_id=result.user.id,
        detail={"role": result.user.role}
    )

    return result


@router.get("/me", response_model=UserResponse)
async def get_me(current_user: dict = Depends(get_current_user)):
    """获取当前用户信息"""
    user = AuthService.get_user(current_user["user_id"])
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")
    return user


# ═══════════════════════════════════════════════════════
# 资质验证（社区/医院 二次认证 · AI 自动审核）
# ═══════════════════════════════════════════════════════

@router.get("/verification/status", response_model=QualificationStatusResponse)
async def verification_status(current_user: dict = Depends(get_current_user)):
    """查询当前用户的资质审核状态"""
    db = get_db()
    row = db.execute(
        "SELECT * FROM qualification_reviews WHERE applicant_user_id = ? ORDER BY created_at DESC LIMIT 1",
        (current_user["user_id"],)
    ).fetchone()

    if not row:
        return QualificationStatusResponse(qualification_status="none")

    return QualificationStatusResponse(
        qualification_status=row["status"],
        review_note=row["manual_review_note"] if row["manual_review_note"] else None,
        valid_until=None,
        submitted_at=row["created_at"]
    )


@router.post("/verification/apply")
async def verification_apply(
    req: QualificationApplyRequest,
    current_user: dict = Depends(get_current_user)
):
    """社区/医院提交资质 → AI 自动审核 → 通过后通知家属端授权"""
    if current_user["role"] not in ("community", "hospital"):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="仅社区/医院人员需要资质审核")

    db = get_db()

    # 检查是否已有进行中的申请
    existing = db.execute(
        "SELECT id FROM qualification_reviews WHERE applicant_user_id = ? AND status IN ('pending','approved')",
        (current_user["user_id"],)
    ).fetchone()
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="已有提交记录，请等待审核或使用已有资质")

    # 确保机构存在
    inst_id = generate_uuid()
    db.execute(
        """INSERT OR IGNORE INTO institutions(id, name, institution_type, is_verified)
           VALUES(?, ?, ?, 0)""",
        (inst_id, req.institution_name, req.institution_type)
    )
    db.execute(
        "UPDATE users SET institution_id = ? WHERE id = ?",
        (inst_id, current_user["user_id"])
    )

    # ── AI 自动审核 ──
    ai_result = _ai_review_qualification(req, current_user)

    review_id = generate_uuid()
    ts = now_iso()
    db.execute(
        """INSERT INTO qualification_reviews(
               id, applicant_user_id, elderly_id, institution_id,
               review_type, documents_json, status, auto_review_result, manual_review_note, created_at)
           VALUES(?, ?, NULL, ?, 'initial', ?, ?, ?, ?, ?)""",
        (review_id, current_user["user_id"], inst_id,
         req.document_urls, ai_result["status"], ai_result["status"], ai_result["note"], ts)
    )
    db.commit()

    # 如果 AI 通过 → 通知家属端有新授权请求
    if ai_result["status"] == "approved":
        AuditService.log(
            event_type="qualification_ai_approved",
            operator=current_user["user_id"],
            target_type="qualification_review",
            target_id=review_id,
            detail={
                "institution": req.institution_name,
                "type": req.institution_type,
                "ai_note": ai_result["note"],
                "note": "已通过AI审核，等待家属授权"
            }
        )

    AuditService.log(
        event_type="qualification_submitted",
        operator=current_user["user_id"],
        target_type="qualification_review",
        target_id=review_id,
        detail={"institution": req.institution_name, "type": req.institution_type,
                "ai_status": ai_result["status"]}
    )

    return {
        "message": ai_result["message"],
        "review_id": review_id,
        "status": ai_result["status"],
        "ai_note": ai_result["note"]
    }


def _ai_review_qualification(req: QualificationApplyRequest, user: dict) -> dict:
    """AI 自动审核资质（Phase 1: 统一放通 → Phase 2: 接入 OCR + 执业证核验）"""
    return {
        "status": "approved",
        "note": "P1统一认证通过（P2: OCR+执业证核验）",
        "message": "认证已通过"
    }
