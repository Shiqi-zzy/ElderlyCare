"""管理员 API 路由"""
from fastapi import APIRouter, Depends, Query, HTTPException
from typing import Optional
from ...services.audit_service import AuditService
from ...services.permission_service import PermissionService
from ...services.statistics_service import StatisticsService
from ...ai.health_predictor import HealthPredictor
from ...ai.behavior_analyzer import BehaviorAnalyzer
from ...models.statistics import (
    FallRiskRequest, HealthTrendRequest, ElderlyTrendRequest,
    RegionalStatsRequest, AlarmTrendsRequest
)
from ..middleware.auth_mw import get_current_user, require_role

router = APIRouter(prefix="/api/admin", tags=["管理端"])


@router.get("/audit/logs")
async def get_audit_logs(
    event_type: Optional[str] = Query(None),
    operator: Optional[str] = Query(None),
    target_type: Optional[str] = Query(None),
    limit: int = Query(50, ge=1, le=500),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("admin"))
):
    """查询审计日志"""
    total, items = AuditService.query(event_type, operator, target_type, limit, offset)
    return {"total": total, "items": items}


@router.get("/audit/access_records")
async def get_access_records(
    limit: int = Query(50, ge=1, le=500),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("admin"))
):
    """查询数据访问记录"""
    from ...core.database import get_db
    db = get_db()
    count_row = db.execute("SELECT COUNT(*) FROM access_records").fetchone()
    total = count_row[0] if count_row else 0
    rows = db.execute(
        "SELECT * FROM access_records ORDER BY created_at DESC LIMIT ? OFFSET ?",
        (limit, offset)
    ).fetchall()
    return {"total": total, "items": [dict(r) for r in rows]}


@router.post("/permissions/expire")
async def trigger_expire_check(current_user: dict = Depends(require_role("admin"))):
    """手动触发授权过期检查（平时由定时任务处理）"""
    count = PermissionService.check_and_expire()
    return {"message": f"过期检查完成，{count} 条授权已标记为过期"}


@router.get("/stats")
async def get_stats(current_user: dict = Depends(require_role("admin"))):
    """系统概览统计"""
    from ...core.database import get_db
    db = get_db()

    total_users = db.execute("SELECT COUNT(*) FROM users WHERE is_active=1").fetchone()[0]
    total_elderly = db.execute("SELECT COUNT(*) FROM elderly WHERE is_active=1").fetchone()[0]
    total_devices = db.execute("SELECT COUNT(*) FROM devices WHERE status='online'").fetchone()[0]
    active_alarms = db.execute("SELECT COUNT(*) FROM alarms WHERE status IN ('active','acknowledged','processing')").fetchone()[0]
    pending_orders = db.execute("SELECT COUNT(*) FROM work_orders WHERE status IN ('pending','accepted','in_progress')").fetchone()[0]

    return {
        "total_users": total_users,
        "total_elderly": total_elderly,
        "online_devices": total_devices,
        "active_alarms": active_alarms,
        "pending_work_orders": pending_orders
    }


# ═══════════════════════════════════════════════════════
# 资质审核（管理员审批社区/医院人员资质）
# ═══════════════════════════════════════════════════════

@router.get("/qualification/reviews")
async def qualification_reviews(
    status_filter: Optional[str] = Query(None, alias="status"),
    limit: int = Query(50, ge=1, le=500),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("admin"))
):
    """管理员查看待审核资质列表"""
    from ...core.database import get_db
    db = get_db()

    where = ""
    params = []
    if status_filter:
        where = "WHERE qr.status = ?"
        params.append(status_filter)

    count = db.execute(f"SELECT COUNT(*) FROM qualification_reviews qr {where}", params).fetchone()[0]
    rows = db.execute(
        f"""SELECT qr.*, u.real_name as applicant_name, u.phone as applicant_phone,
                   i.name as institution_name
            FROM qualification_reviews qr
            JOIN users u ON qr.applicant_user_id = u.id
            JOIN institutions i ON qr.institution_id = i.id
            {where}
            ORDER BY qr.created_at DESC
            LIMIT ? OFFSET ?""",
        params + [limit, offset]
    ).fetchall()
    return {"total": count, "items": [dict(r) for r in rows]}


@router.post("/qualification/review/{review_id}")
async def review_qualification(
    review_id: str,
    req: "QualificationReviewRequest",
    current_user: dict = Depends(require_role("admin"))
):
    """管理员审批资质（通过/驳回）"""
    from ...core.database import get_db
    from ...models.qualification import QualificationReviewRequest
    from ...core.security import now_iso

    if req.result not in ("approved", "rejected"):
        raise HTTPException(status_code=400, detail="审批结果必须为 approved 或 rejected")

    db = get_db()
    row = db.execute(
        "SELECT * FROM qualification_reviews WHERE id=? AND status='pending'",
        (review_id,)
    ).fetchone()
    if not row:
        raise HTTPException(status_code=404, detail="审核记录不存在或已处理")

    ts = now_iso()
    db.execute(
        """UPDATE qualification_reviews
           SET manual_reviewer_id=?, manual_review_result=?, manual_review_note=?,
               status=?, reviewed_at=?
           WHERE id=?""",
        (current_user["user_id"], req.result, req.note or "", req.result, ts, review_id)
    )
    db.commit()

    AuditService.log(
        event_type=f"qualification_{req.result}",
        operator=current_user["user_id"],
        target_type="qualification_review",
        target_id=review_id,
        detail={"result": req.result, "note": req.note}
    )

    return {"message": f"资质审核已{req.result == 'approved' and '通过' or '驳回'}"}


# ═══════════════════════════════════════════════════════
# Phase 4: 大数据统计
# ═══════════════════════════════════════════════════════

@router.get("/statistics/overview")
async def statistics_overview(current_user: dict = Depends(require_role("admin"))):
    """全平台增强统计概览（Phase 4）"""
    return StatisticsService.platform_overview()


@router.get("/statistics/regional")
async def statistics_regional(
    institution_id: Optional[str] = Query(None),
    current_user: dict = Depends(require_role("admin"))
):
    """区域健康统计（Phase 4）"""
    return StatisticsService.regional_stats(institution_id)


@router.get("/statistics/elderly/{elderly_id}")
async def statistics_elderly_trends(
    elderly_id: str,
    metric: str = Query("alarms", pattern=r"^(alarms|health_records|device_status)$"),
    days: int = Query(30, ge=7, le=365),
    current_user: dict = Depends(require_role("admin"))
):
    """个人健康趋势（Phase 4）"""
    return StatisticsService.elderly_trends(elderly_id, metric, days)


@router.get("/statistics/alarms")
async def statistics_alarm_trends(
    days: int = Query(30, ge=7, le=365),
    current_user: dict = Depends(require_role("admin"))
):
    """告警趋势分析（Phase 4）"""
    return StatisticsService.alarm_trends(days)


# ═══════════════════════════════════════════════════════
# Phase 4: AI 健康预测 & 行为分析
# ═══════════════════════════════════════════════════════

@router.post("/ai/fall-risk")
async def ai_fall_risk(
    req: FallRiskRequest,
    current_user: dict = Depends(require_role("admin", "hospital"))
):
    """AI 跌倒风险评估"""
    return HealthPredictor.assess_fall_risk(req.elderly_id)


@router.post("/ai/health-trend")
async def ai_health_trend(
    req: HealthTrendRequest,
    current_user: dict = Depends(require_role("admin", "hospital"))
):
    """AI 健康趋势分析"""
    return HealthPredictor.assess_health_trend(req.elderly_id, req.days)


@router.get("/ai/health-report/{elderly_id}")
async def ai_health_report(
    elderly_id: str,
    current_user: dict = Depends(require_role("admin", "hospital", "family"))
):
    """AI 健康综合报告"""
    return HealthPredictor.generate_health_report(elderly_id)


@router.post("/ai/behavior/sleep")
async def ai_sleep_pattern(
    req: HealthTrendRequest,
    current_user: dict = Depends(require_role("admin", "hospital"))
):
    """AI 睡眠规律分析"""
    return BehaviorAnalyzer.analyze_sleep_pattern(req.elderly_id, req.days)


@router.post("/ai/behavior/activity")
async def ai_activity_level(
    req: HealthTrendRequest,
    current_user: dict = Depends(require_role("admin", "hospital"))
):
    """AI 活动量分析"""
    return BehaviorAnalyzer.analyze_activity_level(req.elderly_id, req.days)


@router.get("/ai/behavior/anomaly/{elderly_id}")
async def ai_anomaly_detect(
    elderly_id: str,
    current_user: dict = Depends(require_role("admin", "hospital", "family"))
):
    """AI 异常模式检测"""
    return BehaviorAnalyzer.detect_anomaly_pattern(elderly_id)
