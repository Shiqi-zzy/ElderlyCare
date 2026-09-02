"""安全模块：Token 生成与验证（纯 stdlib 实现，无外部 JWT 依赖）"""
import uuid
import json
import hmac
import hashlib
import base64
from datetime import datetime, timedelta, timezone
from typing import Optional

from .config import SECRET_KEY, ACCESS_TOKEN_EXPIRE_MINUTES


def _base64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('ascii')


def _base64url_decode(s: str) -> bytes:
    # Add padding
    padding = 4 - len(s) % 4
    if padding != 4:
        s += '=' * padding
    return base64.urlsafe_b64decode(s)


def _sign(data: str) -> str:
    """HMAC-SHA256 签名"""
    return hmac.new(
        SECRET_KEY.encode('utf-8'),
        data.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """
    生成自签名的 access token（无需 jose 库）。

    格式: payload_base64.signature
    """
    expire = datetime.now(timezone.utc) + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    payload = {
        **data,
        "exp": expire.timestamp(),
        "iat": datetime.now(timezone.utc).timestamp()
    }
    payload_json = json.dumps(payload, separators=(',', ':'))
    payload_b64 = _base64url_encode(payload_json.encode('utf-8'))
    signature = _sign(payload_b64)
    return f"{payload_b64}.{signature}"


def decode_access_token(token: str) -> Optional[dict]:
    """
    解码并验证 token，返回 payload 或 None。
    """
    try:
        parts = token.split('.')
        if len(parts) != 2:
            return None

        payload_b64, signature = parts

        # 验证签名
        expected_sig = _sign(payload_b64)
        if not hmac.compare_digest(signature, expected_sig):
            return None

        # 解码 payload
        payload_json = _base64url_decode(payload_b64).decode('utf-8')
        payload = json.loads(payload_json)

        # 检查过期
        exp = payload.get("exp", 0)
        if datetime.now(timezone.utc).timestamp() > exp:
            return None

        return payload
    except Exception:
        return None


def generate_uuid() -> str:
    """生成 UUID 字符串"""
    return str(uuid.uuid4())


def now_iso() -> str:
    """当前时间 ISO 字符串"""
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
