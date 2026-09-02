"""四端协同事件相关请求模型"""
from typing import Optional
from pydantic import BaseModel, Field


class SimulateFallReq(BaseModel):
    elderly_id: str
    elderly_name: str = ""
    family_user_id: Optional[str] = None
    family_phone: str = ""
    community_org_id: Optional[str] = None
    community_staff_id: Optional[str] = None
    building_no: str = ""
    unit_no: str = ""
    room_no: str = ""
    alarm_level: str = "HIGH"


class ContactFamilyReq(BaseModel):
    note: str = ""


class DispatchReq(BaseModel):
    hospital_org_id: Optional[str] = None


class HospitalCompleteReq(BaseModel):
    treatment: str = Field(..., min_length=1, description="处置措施必填")


class CommunityCompleteReq(BaseModel):
    note: str = ""


class SelfCloseReq(BaseModel):
    note: str = ""


class ShiftReq(BaseModel):
    role: str = "hospital"
    title: str = ""
    start_time: str
    end_time: str
    location: str = ""
    schedule_mode: int = 0  # 0 周循环 / 1 指定日期
    weekday: int = 0        # 周一=1..周日=7（周循环）
    schedule_date: Optional[int] = None


class HcBindingReq(BaseModel):
    community_org_id: str
    hospital_org_id: Optional[str] = None
    note: str = ""


class HcReviewReq(BaseModel):
    approved: bool
    note: str = ""
