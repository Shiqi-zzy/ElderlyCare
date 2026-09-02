"""审计日志 Pydantic 模型"""
from pydantic import BaseModel
from typing import Optional


class AuditLogCreate(BaseModel):
    event_type: str
    event_source: str = "system"
    operator: str = "system"
    target_type: Optional[str] = None
    target_id: Optional[str] = None
    detail_json: Optional[str] = None
    ip_address: Optional[str] = None


class AuditLogResponse(BaseModel):
    id: int
    event_type: str
    event_source: str = "system"
    operator: str = "system"
    target_type: Optional[str] = None
    target_id: Optional[str] = None
    detail_json: Optional[str] = None
    ip_address: Optional[str] = None
    created_at: str = ""


class AuditLogsResponse(BaseModel):
    total: int
    items: list[AuditLogResponse]
