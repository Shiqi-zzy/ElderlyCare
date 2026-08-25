"""告警 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class AlarmCreate(BaseModel):
    """设备触发异常 → AI 引擎自动创建告警"""
    elderly_id: str
    device_id: Optional[str] = None
    alarm_type: str = Field(..., pattern="^(fall|stillness|smoke|gas|door_open|bed_absence|vital_sign)$")
    ai_score: Optional[float] = Field(default=None, ge=0.0, le=1.0)
    raw_data_json: Optional[str] = None
    snapshot_url: Optional[str] = None
    video_clip_url: Optional[str] = None
    title: str
    description: Optional[str] = None


class AlarmAcknowledge(BaseModel):
    acknowledged_by: str


class AlarmResolve(BaseModel):
    resolved_by: str
    resolution_note: Optional[str] = None


class AlarmResponse(BaseModel):
    id: str
    elderly_id: str
    device_id: Optional[str] = None
    alarm_type: str
    alarm_level: str
    ai_score: Optional[float] = None
    ai_verified: int = 0
    snapshot_url: Optional[str] = None
    video_clip_url: Optional[str] = None
    title: str
    description: str = ""
    status: str = "active"
    push_family: bool = True
    push_community: bool = False
    push_hospital: bool = False
    related_work_order_id: Optional[str] = None
    acknowledged_by: Optional[str] = None
    acknowledged_at: Optional[str] = None
    resolved_by: Optional[str] = None
    resolved_at: Optional[str] = None
    resolution_note: Optional[str] = None
    created_at: str = ""


class AlarmsResponse(BaseModel):
    total: int
    items: list[AlarmResponse]
