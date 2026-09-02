"""急救权限 Pydantic 模型（Phase 3）"""
from pydantic import BaseModel, Field
from typing import Optional


class EmergencyRequest(BaseModel):
    elderly_id: str
    reason: str = Field(default="急救需要临时监控权限")


class EmergencyStatusResponse(BaseModel):
    active: bool
    expires_at: str = ""
    elderly_name: str = ""
    elderly_id: str = ""
    authorization_id: str = ""
