"""授权 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class AuthorizationGrant(BaseModel):
    """家属授予社区/医院数据访问权限"""
    elderly_id: str
    grantee_user_id: str  # 被授权人ID
    permission_type: str = Field(..., pattern="^(monitoring|health_records|alarm_video|all)$")
    data_scope: str  # JSON: {"video":true,"medical":false,"alarm":true}
    effective_from: Optional[str] = None  # 默认立即生效
    effective_until: Optional[str] = None  # 授权截止时间，默认30天


class AuthorizationRevoke(BaseModel):
    revoke_reason: Optional[str] = None


class AuthorizationResponse(BaseModel):
    id: str
    elderly_id: str
    grantor_user_id: str
    grantee_user_id: str
    grantee_institution_id: Optional[str] = None
    permission_type: str
    data_scope: str
    effective_from: str
    effective_until: str
    status: str
    revoked_by: Optional[str] = None
    revoked_at: Optional[str] = None
    revoke_reason: Optional[str] = None
    created_at: str = ""
    # 关联显示
    grantee_name: str = ""
    grantee_phone: str = ""
    institution_name: str = ""


class AuthorizationsResponse(BaseModel):
    total: int
    items: list[AuthorizationResponse]
