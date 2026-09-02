"""统计相关 Pydantic 模型（Phase 4）"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict


class FallRiskRequest(BaseModel):
    elderly_id: str


class HealthTrendRequest(BaseModel):
    elderly_id: str
    days: int = Field(default=30, ge=7, le=365)


class ElderlyTrendRequest(BaseModel):
    elderly_id: str
    metric: str = Field(default="alarms", pattern=r"^(alarms|health_records|device_status)$")
    days: int = Field(default=30, ge=7, le=365)


class RegionalStatsRequest(BaseModel):
    institution_id: Optional[str] = None


class AlarmTrendsRequest(BaseModel):
    days: int = Field(default=30, ge=7, le=365)
