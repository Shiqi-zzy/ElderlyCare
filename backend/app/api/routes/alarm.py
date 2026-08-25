"""告警通用路由（模拟设备触发 + 告警管理）"""
import json
from fastapi import APIRouter, HTTPException, status, Depends, Query
from typing import Optional
from ...models.alarm import AlarmCreate, AlarmAcknowledge, AlarmResolve, AlarmResponse, AlarmsResponse
from ...models.device import DeviceHeartbeat
from ...services.alarm_service import AlarmService
from ...services.ai_detect_service import AiDetectService
from ...services.device_service import DeviceService
from ...services.notification_service import NotificationService
from ...services.audit_service import AuditService
from ..middleware.auth_mw import get_current_user, require_role

router = APIRouter(prefix="/api/alarm", tags=["告警"])


@router.post("/simulate/trigger")
async def simulate_alarm_trigger(current_user: dict = Depends(require_role("family", "admin"))):
    """
    模拟设备数据上报并触发告警闭环（阶段 1 测试用）。

    流程：
      1. 获取当前用户绑定的老人和摄像头设备
      2. 模拟 AI 检测异常
      3. 经过防误报引擎 → 生成分级告警
      4. 如需要自动创建工单
      5. WebSocket 推送通知
    """
    from ...services.elderly_service import ElderlyService

    # 获取家属绑定的老人
    if current_user["role"] == "admin":
        # 管理员触发演示：使用第一个老人
        from ...core.database import get_db
        db = get_db()
        row = db.execute("SELECT id FROM elderly WHERE is_active=1 LIMIT 1").fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="系统中无老人数据，请先创建")
        elderly_id = row["id"]
    else:
        elderly_list = ElderlyService.list_by_family(current_user["user_id"])
        if not elderly_list:
            raise HTTPException(status_code=404, detail="请先创建老人档案")
        elderly_id = elderly_list[0]["id"]

    # 获取老人的摄像头设备
    devices = DeviceService.list_by_elderly(elderly_id)
    camera = next((d for d in devices if d["device_type"] == "camera"), None)
    device_id = camera["id"] if camera else (devices[0]["id"] if devices else None)

    # 模拟 AI 检测
    anomaly = AiDetectService._detect_from_camera({})
    if not anomaly:
        # 强制产生一次异常用于测试
        import random
        anomaly = {
            "type": "fall",
            "confidence": round(random.uniform(0.6, 0.92), 2),
            "snapshot_ref": f"test_snapshot_{random.randint(1000,9999)}.jpg",
            "description": "[模拟] 检测到疑似跌倒行为"
        }

    # 获取最近历史
    recent = AlarmService.get_recent_by_device(device_id) if device_id else []

    # 创建告警（经过防误报引擎）
    alarm = AlarmService.create_from_anomaly(elderly_id, device_id, anomaly, recent)
    if not alarm:
        raise HTTPException(status_code=500, detail="告警生成失败")

    # WebSocket 实时推送
    await NotificationService.notify_alarm(alarm)

    return {
        "message": "模拟告警已触发",
        "alarm": alarm,
        "anomaly": anomaly,
        "flow": "设备异常 → AI检测 → 防误报 → 分级 → 告警生成 → 工单创建(如需) → WS推送"
    }


@router.get("/list/{elderly_id}")
async def list_alarms(
    elderly_id: str,
    level: Optional[str] = Query(None),
    status_filter: Optional[str] = Query(None, alias="status"),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    current_user: dict = Depends(get_current_user)
):
    """获取告警列表（通用）"""
    total, items = AlarmService.list_by_elderly(elderly_id, level, status_filter, limit, offset)
    return {"total": total, "items": items}


@router.get("/{alarm_id}")
async def get_alarm(alarm_id: str, current_user: dict = Depends(get_current_user)):
    """获取告警详情"""
    alarm = AlarmService.get(alarm_id)
    if not alarm:
        raise HTTPException(status_code=404, detail="告警不存在")
    return alarm


@router.post("/{alarm_id}/resolve")
async def resolve_alarm(
    alarm_id: str, req: AlarmResolve,
    current_user: dict = Depends(get_current_user)
):
    """解决告警"""
    success = AlarmService.resolve(alarm_id, req.resolved_by, req.resolution_note)
    if not success:
        raise HTTPException(status_code=400, detail="告警状态不允许解决")
    return {"message": "告警已解决"}


# ──────────────────── 设备心跳（触发设备离线检测）────────────────────

@router.post("/device/heartbeat")
async def device_heartbeat(req: DeviceHeartbeat, current_user: dict = Depends(get_current_user)):
    """设备心跳上报"""
    DeviceService.heartbeat(req.device_id, req.timestamp)
    return {"message": "心跳已接收"}
