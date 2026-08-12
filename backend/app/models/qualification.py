"""资质审核 Pydantic 模型"""
from pydantic import BaseModel, Field
from typing import Optional


class QualificationReviewRequest(BaseModel):
    """管理员审批资质"""
    result: str = Field(..., description="approved or rejected")
    note: Optional[str] = Field(None, description="审核备注")
