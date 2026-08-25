"""老人档案 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class ElderlyCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)
    gender: Optional[str] = Field(default="", pattern="^(M|F|)?$")
    birth_date: Optional[str] = None
    id_card: Optional[str] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    emergency_contact: Optional[str] = None
    medical_history: Optional[str] = None
    care_level: str = Field(default="自理", pattern="^(自理|半自理|全护理)$")
    binding_family_user_id: str = ""  # 由后端从 token 中自动填充


class ElderlyUpdate(BaseModel):
    name: Optional[str] = None
    gender: Optional[str] = None
    birth_date: Optional[str] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    emergency_contact: Optional[str] = None
    medical_history: Optional[str] = None
    care_level: Optional[str] = None


class ElderlyResponse(BaseModel):
    id: str
    name: str
    gender: str = ""
    birth_date: str = ""
    id_card: str = ""
    phone: str = ""
    address: str = ""
    emergency_contact: str = ""
    medical_history: str = ""
    care_level: str = "自理"
    binding_family_user_id: str = ""
    is_active: bool = True
    privacy_paused: bool = False
    created_at: str = ""
    updated_at: str = ""
