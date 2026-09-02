"""健康档案 Pydantic 模型（阶段3主要使用，阶段1预留）"""
from pydantic import BaseModel, Field
from typing import Optional


class HealthRecordCreate(BaseModel):
    elderly_id: str
    record_type: str = Field(..., pattern="^(vital_sign|diagnosis|prescription|lab_report|medication)$")
    record_date: str
    doctor_name: Optional[str] = None
    hospital_name: Optional[str] = None
    content_json: str  # JSON: 结构化医疗数据
    attachment_urls: Optional[str] = None
    visibility: str = Field(default="family", pattern="^(family|hospital|both)$")


class HealthRecordResponse(BaseModel):
    id: str
    elderly_id: str
    record_type: str
    record_date: str
    doctor_name: str = ""
    hospital_name: str = ""
    content_json: str
    attachment_urls: Optional[str] = None
    visibility: str = "family"
    created_by: str = ""
    created_at: str = ""


class HealthRecordsResponse(BaseModel):
    total: int
    items: list[HealthRecordResponse]
