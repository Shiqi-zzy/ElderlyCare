"""设备巡检 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class MaintenanceCreate(BaseModel):
    """创建巡检记录"""
    maintenance_type: str = Field(..., description="巡检类型: routine/repair/replace/emergency")
    status: str = Field(..., description="设备状态: normal/needs_repair/replaced/fault")
    findings: str = ""
    photos: str = ""
    next_inspection_date: str = ""


class MaintenanceRecord(BaseModel):
    """巡检记录"""
    id: str
    device_id: str
    maintenance_type: str
    inspector_id: str = ""
    inspector_name: str = ""
    inspection_date: str
    status: str
    findings: str = ""
    photos: str = ""
    next_inspection_date: str = ""
    device_name: str = ""
    device_type: str = ""
    created_at: str = ""


class MaintenanceListResponse(BaseModel):
    """巡检记录列表"""
    total: int
    items: list
