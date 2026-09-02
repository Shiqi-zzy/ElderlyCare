"""设备验证码 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class GenerateCodeRequest(BaseModel):
    """生成验证码请求（device_id 从路径获取）"""


class GenerateCodeResponse(BaseModel):
    """生成验证码响应"""
    code_id: str
    code: str
    expires_at: str
    device_id: str
    elderly_id: str
    elderly_name: str = ""
    device_name: str = ""


class DeviceBindRequest(BaseModel):
    """绑定设备请求 — 输入验证码"""
    code: str = Field(..., min_length=6, max_length=6, description="6位数字验证码")


class DeviceBindResponse(BaseModel):
    """绑定设备响应"""
    message: str
    device_id: str
    elderly_id: str
    elderly_name: str = ""
    authorization_id: str = ""
    already_bound: bool = False


class RevokeCodeRequest(BaseModel):
    """撤销验证码（code_id 从路径获取）"""


class VerificationCodeItem(BaseModel):
    """验证码列表项"""
    id: str
    device_id: str
    elderly_id: str
    code: str
    expires_at: str
    use_count: int = 0
    max_uses: int = 1
    status: str = "active"
    device_name: str = ""
    elderly_name: str = ""
    created_at: str = ""


class VerificationCodeListResponse(BaseModel):
    """验证码列表响应"""
    total: int
    items: list
