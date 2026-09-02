"""权限检查依赖注入"""
from fastapi import Depends, HTTPException, status
from ...core.permissions import check_elderly_access
from ...core.database import get_db
from ...services.audit_service import AuditService
from .auth_mw import get_current_user


class ElderlyAccessChecker:
    """
    老人数据访问权限检查器。

    用法（路由中）:
        @router.get("/family/elderly/{elderly_id}")
        async def get_elderly(
            elderly_id: str,
            current_user: dict = Depends(get_current_user),
            access: dict = Depends(ElderlyAccessChecker("view_alarm"))
        ):
            # access 包含 {"granted": True, "data_scope": ..., "authorization_id": ...}
            ...

    检查逻辑：
      1. 家属：检查是否是绑定家属
      2. 社区/医院：检查 authorizations 表中是否有有效授权
      3. 管理员：禁止直接访问老人数据
    """

    def __init__(self, access_type: str = "view_alarm"):
        self.access_type = access_type

    async def __call__(self, elderly_id: str, current_user: dict = Depends(get_current_user)) -> dict:
        user_id = current_user["user_id"]

        # 检查权限
        result = check_elderly_access(user_id, elderly_id, self.access_type)

        # 记录访问日志
        AuditService.log_access(
            user_id=user_id,
            accessed_elderly_id=elderly_id,
            access_type=self.access_type,
            data_scope=str(result.get("data_scope", {})),
            authorized_by=result.get("authorization_id"),
            access_result="granted" if result["granted"] else "denied",
            deny_reason=result.get("reason") if not result["granted"] else None
        )

        if not result["granted"]:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=result["reason"])

        return result
