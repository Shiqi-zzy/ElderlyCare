"""认证服务：手机验证码登录 + 角色选择 + Token 管理"""
import random
import time
from ..core.database import get_db
from ..core.security import create_access_token, generate_uuid, now_iso
from ..models.user import SyncRequest, SelectRoleRequest, UserResponse, SyncResponse
from .ezviz_sms import send_sms_code

# 内存验证码存储: {phone: (code, expire_timestamp)}
_CODE_STORE: dict[str, tuple[str, float]] = {}
_CODE_TTL = 300  # 5分钟有效
_FIXED_CODE = "123456"  # EZVIZ 短信不可用时的兜底


def _generate_code() -> str:
    return f"{random.randint(100000, 999999)}"


class AuthService:

    @staticmethod
    def send_code(phone: str) -> dict:
        """发送手机验证码。优先萤石云信令，失败则固定码兜底。"""
        code = _generate_code()
        sent = send_sms_code(phone, code)

        if sent:
            _CODE_STORE[phone] = (code, time.time() + _CODE_TTL)
            return {"message": "验证码已发送", "phone": phone}
        else:
            # 萤石短信不可用，用固定码兜底（开发测试用）
            _CODE_STORE[phone] = (_FIXED_CODE, time.time() + _CODE_TTL)
            print(f"[SMS] 萤石短信发送失败，使用固定验证码: {_FIXED_CODE}")
            return {"message": "验证码已发送", "phone": phone}

    @staticmethod
    def login_with_phone(phone: str, code: str, role: str) -> SyncResponse:
        """手机验证码登录：验证 → 查找或创建用户 → 返回 JWT"""
        # 验证码校验
        stored = _CODE_STORE.get(phone)
        valid = False
        if stored:
            stored_code, expires = stored
            if time.time() < expires and code == stored_code:
                valid = True
                del _CODE_STORE[phone]  # 一次性使用
            elif time.time() >= expires:
                del _CODE_STORE[phone]  # 过期清理

        if not valid:
            from fastapi import HTTPException
            from fastapi import status
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="验证码错误或已过期")
            from fastapi import HTTPException
            from fastapi import status
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="验证码错误")

        db = get_db()

        # 按手机号 + 角色查找用户
        row = db.execute(
            "SELECT * FROM users WHERE phone = ? AND role = ? AND is_active = 1",
            (phone, role)
        ).fetchone()

        if row:
            # 已有用户 → 更新登录时间
            db.execute(
                "UPDATE users SET last_login_at = ? WHERE id = ?",
                (now_iso(), row["id"])
            )
            db.commit()
            user = _row_to_user(dict(row))
            token = _make_token(user)
            return SyncResponse(access_token=token, user=user, need_select_role=False)

        # 同一手机号不同角色允许（一人多角色），每条独立 client_id
        client_id = generate_uuid()

        # 新用户（该角色下）→ 创建
        user_id = generate_uuid()
        ts = now_iso()
        db.execute(
            """INSERT INTO users(id, client_id, phone, role, real_name, created_at, updated_at)
               VALUES(?,?,?,?,?,?,?)""",
            (user_id, client_id, phone, role, "", ts, ts)
        )
        db.commit()

        row2 = db.execute("SELECT * FROM users WHERE id = ?", (user_id,)).fetchone()
        user = _row_to_user(dict(row2))
        token = _make_token(user)
        return SyncResponse(access_token=token, user=user, need_select_role=False)

    @staticmethod
    def sync(req: SyncRequest) -> SyncResponse:
        """萤石 AppKey 初始化后同步：更新或创建用户的 ezviz_access_token"""
        db = get_db()
        row = db.execute(
            "SELECT * FROM users WHERE client_id = ? AND is_active = 1",
            (req.client_id,)
        ).fetchone()

        if row and row["role"]:
            db.execute(
                "UPDATE users SET last_login_at = ?, ezviz_access_token = ? WHERE id = ?",
                (now_iso(), req.ezviz_access_token, row["id"])
            )
            db.commit()
            user = _row_to_user(dict(row))
            token = _make_token(user)
            return SyncResponse(access_token=token, user=user, need_select_role=False)

        if row and row["role"] is None:
            db.execute(
                "UPDATE users SET last_login_at = ?, ezviz_access_token = ? WHERE id = ?",
                (now_iso(), req.ezviz_access_token, row["id"])
            )
            db.commit()
            return SyncResponse(access_token="", need_select_role=True)

        # 全新设备 → 创建空白用户记录（role 为空，待手机登录）
        user_id = generate_uuid()
        ts = now_iso()
        db.execute(
            """INSERT INTO users(id, client_id, ezviz_access_token, real_name, phone, role, created_at, updated_at)
               VALUES(?,?,?,?,?,NULL,?,?)""",
            (user_id, req.client_id, req.ezviz_access_token, "", "", ts, ts)
        )
        db.commit()
        return SyncResponse(access_token="", need_select_role=True)

    @staticmethod
    def select_role(req: SelectRoleRequest) -> SyncResponse:
        """新用户选择角色"""
        db = get_db()
        row = db.execute(
            "SELECT * FROM users WHERE client_id = ? AND is_active = 1",
            (req.client_id,)
        ).fetchone()

        if not row:
            # 理论上不会发生：先 sync 再 select_role
            user_id = generate_uuid()
            ts = now_iso()
            db.execute(
                """INSERT INTO users(id, client_id, real_name, phone, role, created_at, updated_at)
                   VALUES(?,?,?,?,?,?,?)""",
                (user_id, req.client_id, req.real_name, req.phone, req.role, ts, ts)
            )
            db.commit()
            user = UserResponse(id=user_id, client_id=req.client_id,
                                real_name=req.real_name, phone=req.phone,
                                role=req.role)
            token = _make_token(user)
            return SyncResponse(access_token=token, user=user, need_select_role=False)

        db.execute(
            "UPDATE users SET role = ?, real_name = ?, phone = ?, updated_at = ? WHERE id = ?",
            (req.role, req.real_name or row["real_name"] or "",
             req.phone or row["phone"] or "", now_iso(), row["id"])
        )
        db.commit()

        row = db.execute("SELECT * FROM users WHERE id = ?", (row["id"],)).fetchone()
        user = _row_to_user(dict(row))
        token = _make_token(user)
        return SyncResponse(access_token=token, user=user, need_select_role=False)

    @staticmethod
    def get_user(user_id: str) -> UserResponse | None:
        """获取用户信息"""
        db = get_db()
        row = db.execute("SELECT * FROM users WHERE id = ? AND is_active = 1", (user_id,)).fetchone()
        if not row:
            return None
        return _row_to_user(dict(row))


def _row_to_user(r: dict) -> UserResponse:
    db = get_db()
    # 查询资质审核状态
    q_status = "none"
    q_row = db.execute(
        "SELECT status FROM qualification_reviews WHERE applicant_user_id = ? ORDER BY created_at DESC LIMIT 1",
        (r["id"],)
    ).fetchone()
    if q_row:
        q_status = q_row["status"]  # pending / approved / rejected

    return UserResponse(
        id=r["id"],
        client_id=r.get("client_id", ""),
        real_name=r.get("real_name", ""),
        phone=r.get("phone", ""),
        role=r["role"],
        institution_id=r.get("institution_id"),
        is_active=bool(r.get("is_active", 1)),
        qualification_status=q_status,
        created_at=r.get("created_at", "")
    )


def _make_token(user: UserResponse) -> str:
    return create_access_token({
        "sub": user.id,
        "client_id": user.client_id,
        "role": user.role
    })
