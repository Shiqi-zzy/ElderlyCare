"""用户相关 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class SyncRequest(BaseModel):
    """萤石登录后同步：App 发送 client_id + ezviz_access_token"""
    client_id: str = Field(..., min_length=8, max_length=64,
                           description="App 本地持久化的唯一客户端标识")
    ezviz_access_token: Optional[str] = Field(default=None,
                                               description="萤石平台 accessToken")


class SendCodeRequest(BaseModel):
    """发送手机验证码"""
    phone: str = Field(..., min_length=11, max_length=11, pattern=r"^\d{11}$")


class LoginRequest(BaseModel):
    """手机验证码登录"""
    phone: str = Field(..., min_length=11, max_length=11, pattern=r"^\d{11}$")
    code: str = Field(..., min_length=4, max_length=6)
    role: str = Field(..., pattern="^(family|community|hospital)$")


class SelectRoleRequest(BaseModel):
    """新用户选择角色"""
    client_id: str = Field(..., min_length=8, max_length=64)
    role: str = Field(..., pattern="^(family|community|hospital)$")
    real_name: str = Field(default="", max_length=50)
    phone: str = Field(default="", max_length=20)


class SyncResponse(BaseModel):
    """sync 接口返回"""
    access_token: str  # 自建后端 JWT
    token_type: str = "bearer"
    user: Optional["UserResponse"] = None
    need_select_role: bool = False


class UserResponse(BaseModel):
    id: str
    client_id: str = ""
    real_name: str
    phone: str
    role: str
    institution_id: Optional[str] = None
    is_active: bool = True
    qualification_status: str = "none"  # none / pending / approved / rejected
    created_at: str = ""


class QualificationApplyRequest(BaseModel):
    """社区/医院人员提交资质审核申请"""
    applicant_user_id: str = Field(..., description="申请人 user id")
    institution_name: str = Field(default="", max_length=100)
    institution_type: str = Field(..., pattern="^(community|hospital)$")
    document_urls: str = Field(default="", description="上传的资质文件URL列表(JSON数组)")


class QualificationStatusResponse(BaseModel):
    qualification_status: str  # none / pending / approved / rejected
    review_note: Optional[str] = None
    valid_until: Optional[str] = None
    submitted_at: Optional[str] = None
