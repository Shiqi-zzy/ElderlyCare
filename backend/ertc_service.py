"""萤石云通话(ERTC) 服务 — 对接 open.ys7.com /api/service/* 接口

信令链路（双向通话）：
  App 呼叫设备: 客户端 → 后端 call_device → 萤石 → 设备振铃 → 双方 SDK 入会
  设备呼叫 App: 设备 → 萤石消息推送(webhook) → 后端 → WebSocket → App → 双方 SDK 入会

接口约定（2026-08-16 实测确认）：
  - base: https://open.ys7.com
  - 均为 POST，accessToken 放 HTTP Header
  - activate / contact / device(关联) 用 form-urlencoded
  - device/contact 的 deviceSerial 放 Header（不是 body）

参考: https://icnopen.ezviz.com/help/4919
"""
import json
import time
import threading
import urllib.request
import urllib.error
import urllib.parse
from typing import Optional

from config import (
    EZVIZ_OPEN_BASE_URL,
    EZVIZ_RTC_APP_ID,
    EZVIZ_RTC_APP_KEY,
    EZVIZ_RTC_APP_SECRET,
)

# accessToken 内存缓存（过期自动刷新）
_token_cache = {"token": None, "expire_ms": 0}
_token_lock = threading.Lock()


def _get_access_token() -> Optional[str]:
    """获取萤石 accessToken（内存缓存，过期自动用 AppKey/AppSecret 刷新）"""
    with _token_lock:
        if _token_cache["token"] and time.time() * 1000 < _token_cache["expire_ms"]:
            return _token_cache["token"]

        if not EZVIZ_RTC_APP_KEY or not EZVIZ_RTC_APP_SECRET:
            print("[ERTC] RTC AppKey/Secret 未配置")
            return None

        body = urllib.parse.urlencode({
            "appKey": EZVIZ_RTC_APP_KEY,
            "appSecret": EZVIZ_RTC_APP_SECRET,
        }).encode("utf-8")
        req = urllib.request.Request(
            f"{EZVIZ_OPEN_BASE_URL}/api/lapp/token/get",
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                result = json.loads(resp.read().decode("utf-8"))
        except Exception as e:
            print(f"[ERTC] 获取 accessToken 异常: {e}")
            return None

        data = result.get("data") or {}
        token = data.get("accessToken")
        if not token:
            print(f"[ERTC] 获取 accessToken 失败: {result.get('msg')}")
            return None

        expire_ms = data.get("expireTime") or 0
        _token_cache["token"] = token
        # expireTime 为毫秒时间戳；无效则默认 7 天
        _token_cache["expire_ms"] = (
            expire_ms if expire_ms > 10 ** 11 else int(time.time() * 1000) + 7 * 24 * 3600 * 1000
        )
        return token


def _post_service(path: str, form: Optional[dict] = None, headers: Optional[dict] = None) -> dict:
    """POST 到 open.ys7.com/api/service/*，form-urlencoded + Header accessToken。

    返回完整 JSON（含 meta 与 data），由调用方判断 meta.code。
    """
    token = _get_access_token()
    if not token:
        return {"meta": {"code": -1, "message": "获取 accessToken 失败"}, "data": None}

    url = f"{EZVIZ_OPEN_BASE_URL}{path}"
    req_headers = {
        "Content-Type": "application/x-www-form-urlencoded",
        "accessToken": token,
    }
    if headers:
        req_headers.update(headers)

    body = urllib.parse.urlencode(form or {}).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=req_headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        print(f"[ERTC] {path} HTTP {e.code}: {raw}")
        try:
            return json.loads(raw)
        except Exception:
            return {"meta": {"code": e.code, "message": raw}, "data": None}
    except Exception as e:
        print(f"[ERTC] {path} 请求异常: {e}")
        return {"meta": {"code": -1, "message": str(e)}, "data": None}


def _post_json(path: str, payload: dict) -> dict:
    """POST JSON 到 open.ys7.com，header accessToken（media/token/rtc 用 JSON）。"""
    token = _get_access_token()
    if not token:
        return {"meta": {"code": -1, "message": "获取 accessToken 失败"}, "data": None}

    url = f"{EZVIZ_OPEN_BASE_URL}{path}"
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", "accessToken": token},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        print(f"[ERTC] {path} HTTP {e.code}: {raw}")
        try:
            return json.loads(raw)
        except Exception:
            return {"meta": {"code": e.code, "message": raw}, "data": None}
    except Exception as e:
        print(f"[ERTC] {path} 请求异常: {e}")
        return {"meta": {"code": -1, "message": str(e)}, "data": None}


# ═══════════════════════════════════════════════════════
# 已实测确认的接口
# ═══════════════════════════════════════════════════════

def activate_device(device_serial: str) -> dict:
    """开通设备视频通话能力（必须调用，否则设备呼叫到不了开放平台）"""
    return _post_service(
        "/api/service/rtc/call/activate",
        {"appId": EZVIZ_RTC_APP_ID, "deviceSerial": device_serial},
    )


def create_contact(account: str, name: str) -> dict:
    """添加设备联系人。account 为联系人唯一标识（客户端自定义，如 family001）"""
    return _post_service(
        "/api/service/rtc/call/contact",
        {"appId": EZVIZ_RTC_APP_ID, "account": account, "name": name},
    )


def associate_device_contact(device_serial: str, account: str) -> dict:
    """设备关联联系人（deviceSerial 放 Header）。需设备在线，否则返回 20007"""
    return _post_service(
        "/api/service/rtc/call/device/contact",
        {"appId": EZVIZ_RTC_APP_ID, "account": account},
        headers={"deviceSerial": device_serial},
    )


# ═══════════════════════════════════════════════════════
# 待设备上线实测确认的接口（参数为最佳猜测，联调时按实测修正）
# ═══════════════════════════════════════════════════════

def get_rtc_token(room_id: str, custom_id: str) -> Optional[str]:
    """获取 RTC 通话 token（资源 token）。

    实测（2026-08）: media/token/rtc 用 JSON body + 嵌套 params，
    expireTime 单位秒，返回 data.token。
    """
    result = _post_json("/api/service/media/token/rtc", {
        "expireTime": "3600",
        "appId": EZVIZ_RTC_APP_ID,
        "params": {"strRoomId": room_id, "customId": custom_id},
    })
    if (result.get("meta") or {}).get("code") == 200:
        return (result.get("data") or {}).get("token")
    print(f"[ERTC] 获取 token 失败: {result}")
    return None


def get_call_tokens(room_id: str, client_custom_id: str, device_serial: str) -> dict:
    """同一房间下同时获取客户端 token 与设备 token。"""
    client_token = get_rtc_token(room_id, client_custom_id)
    device_token = get_rtc_token(room_id, device_serial)
    return {"client_token": client_token, "device_token": device_token}


def call_device(device_serial: str, account: str, room_id: str, resource_token: str,
                call_id: Optional[str] = None) -> dict:
    """客户端呼叫设备（HTTP 方式）。resourceToken 传设备 token。

    注意：客户端 SDK 也可用 EZOpenSDK.inviteDeviceEnterMeeting 直接呼叫，
    本接口作为后端信令备用。deviceSerial 放 header。
    """
    form = {
        "appId": EZVIZ_RTC_APP_ID,
        "resourceToken": resource_token,
        "strRoomId": room_id,
        "account": account,
    }
    if call_id:
        form["callId"] = call_id
    return _post_service(
        "/api/service/rtc/call/request",
        form,
        headers={"deviceSerial": device_serial},
    )


def reject_call(call_id: str) -> dict:
    """客户端拒绝呼叫。参数待实测确认。"""
    return _post_service(
        "/api/service/rtc/call/reject",
        {"appId": EZVIZ_RTC_APP_ID, "callId": call_id},
    )


def cancel_call(call_id: str) -> dict:
    """客户端取消通话。参数待实测确认。"""
    return _post_service(
        "/api/service/rtc/call/cancel",
        {"appId": EZVIZ_RTC_APP_ID, "callId": call_id},
    )
