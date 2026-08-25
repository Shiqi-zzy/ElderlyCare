"""JWT 认证中间件"""
from typing import Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from ...core.security import decode_access_token
from ...core.database import get_db

security = HTTPBearer(auto_error=False)


async def get_current_user(credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)) -> dict:
    """
    从 JWT token 中提取当前用户信息。

    用法（路由中）:
        @router.get("/xxx")
        async def xxx(current_user: dict = Depends(get_current_user)):
            ...
    """
    if not credentials:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="请先登录")

    payload = decode_access_token(credentials.credentials)
    if not payload:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token 无效或已过期")

    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token 格式错误")

    # 验证用户仍然活跃
    db = get_db()
    row = db.execute("SELECT id, client_id, role, is_active FROM users WHERE id=?", (user_id,)).fetchone()
    if not row or not row["is_active"]:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="用户不存在或已禁用")
    if not row["role"]:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="请先选择角色")

    return {"user_id": row["id"], "client_id": row["client_id"], "role": row["role"]}


def require_role(*allowed_roles: str):
    """
    角色权限限制装饰器工厂。

    用法:
        @router.get("/xxx")
        async def xxx(current_user: dict = Depends(require_role("family", "admin"))):
            ...
    """
    async def role_checker(current_user: dict = Depends(get_current_user)) -> dict:
        if current_user["role"] not in allowed_roles:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN,
                              detail=f"角色 {current_user['role']} 无权访问此接口")
        return current_user
    return role_checker
