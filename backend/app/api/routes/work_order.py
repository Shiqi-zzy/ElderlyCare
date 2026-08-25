"""工单路由"""
from fastapi import APIRouter, HTTPException, Depends, Query
from typing import Optional
from ...models.work_order import WorkOrderComplete, WorkOrderResponse, WorkOrdersResponse
from ...services.work_order_service import WorkOrderService
from ...services.notification_service import NotificationService
from ..middleware.auth_mw import get_current_user, require_role

router = APIRouter(prefix="/api/work_order", tags=["工单"])


@router.get("/my")
async def my_work_orders(
    status_filter: Optional[str] = Query(None, alias="status"),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("community", "admin"))
):
    """我的工单列表"""
    total, items = WorkOrderService.list_by_assignee(current_user["user_id"], status_filter, limit, offset)
    return {"total": total, "items": items}


@router.get("/institution/{institution_id}")
async def institution_work_orders(
    institution_id: str,
    status_filter: Optional[str] = Query(None, alias="status"),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(require_role("community", "admin"))
):
    """机构的工单列表"""
    total, items = WorkOrderService.list_by_institution(institution_id, status_filter, limit, offset)
    return {"total": total, "items": items}


@router.get("/{order_id}")
async def get_work_order(order_id: str, current_user: dict = Depends(get_current_user)):
    """获取工单详情"""
    order = WorkOrderService.get(order_id)
    if not order:
        raise HTTPException(status_code=404, detail="工单不存在")
    return order


@router.post("/{order_id}/accept")
async def accept_work_order(
    order_id: str,
    current_user: dict = Depends(require_role("community"))
):
    """接单"""
    success = WorkOrderService.accept(order_id, current_user["user_id"])
    if not success:
        raise HTTPException(status_code=400, detail="工单状态不允许接单")
    return {"message": "已接单"}


@router.post("/{order_id}/start")
async def start_work_order(
    order_id: str,
    current_user: dict = Depends(require_role("community"))
):
    """开始处理"""
    success = WorkOrderService.start(order_id, current_user["user_id"])
    if not success:
        raise HTTPException(status_code=400, detail="工单状态不允许开始处理")
    return {"message": "已开始处理"}


@router.post("/{order_id}/complete")
async def complete_work_order(
    order_id: str,
    req: WorkOrderComplete,
    current_user: dict = Depends(require_role("community", "admin"))
):
    """完成工单并上传处置结果"""
    success = WorkOrderService.complete(
        order_id, current_user["user_id"],
        req.result_json, req.result_photos
    )
    if not success:
        raise HTTPException(status_code=400, detail="工单状态不允许完成")

    # 通知工单状态更新
    order = WorkOrderService.get(order_id)
    if order:
        await NotificationService.notify_work_order_update(order, current_user["user_id"])

    return {"message": "工单已完成，关联告警已解决"}
