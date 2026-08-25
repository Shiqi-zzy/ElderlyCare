"""工单 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class WorkOrderCreate(BaseModel):
    """自动生成或手动创建工单"""
    alarm_id: Optional[str] = None
    elderly_id: str
    order_type: str = Field(..., pattern="^(alarm_handling|device_maintenance|inspection|emergency|binding_review)$")
    title: str
    description: Optional[str] = None
    priority: str = Field(default="normal", pattern="^(low|normal|high|urgent)$")
    assigned_to: Optional[str] = None
    assigned_institution_id: Optional[str] = None
    deadline: Optional[str] = None


class WorkOrderComplete(BaseModel):
    result_json: Optional[str] = None  # JSON: 处理结果
    result_photos: Optional[str] = None  # 逗号分隔的照片URL


class WorkOrderResponse(BaseModel):
    id: str
    alarm_id: Optional[str] = None
    elderly_id: str
    order_type: str
    title: str
    description: str = ""
    priority: str = "normal"
    assigned_to: Optional[str] = None
    assigned_institution_id: Optional[str] = None
    status: str = "pending"
    result_json: Optional[str] = None
    result_photos: Optional[str] = None
    deadline: Optional[str] = None
    completed_at: Optional[str] = None
    created_at: str = ""


class WorkOrdersResponse(BaseModel):
    total: int
    items: list[WorkOrderResponse]
