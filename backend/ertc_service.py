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
import os
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


# ═══════════════════════════════════════════════════════
# 抓拍与图片（设备自动告警抓拍 / 手动云端抓拍共用）
# ═══════════════════════════════════════════════════════

def _resp_ok(result: dict) -> bool:
    """兼容两种返回形态：service/* 接口 meta.code、lapp/* 接口顶层 code（字符串）。"""
    code = (result.get("meta") or {}).get("code")
    if code is None:
        code = result.get("code")
    return code == 200 or str(code) == "200"


def capture_device(device_serial: str, channel_no: int = 1) -> dict:
    """云端抓拍：调萤石 /api/lapp/device/capture（form 携带 accessToken，
    与 leave_message_routes 的 voice/send 实测样式一致，不用 service/* 的 Header 约定）。

    返回完整 JSON（code/msg/data），由调用方判断 code。
    官方建议同设备两次抓拍间隔 ≥4 秒；错误码 10028/10029=频率超限、20008=设备响应超时。
    """
    token = _get_access_token()
    if not token:
        return {"code": -1, "msg": "获取 accessToken 失败", "data": None}

    url = f"{EZVIZ_OPEN_BASE_URL}/api/lapp/device/capture"
    body = urllib.parse.urlencode({
        "accessToken": token,
        "deviceSerial": device_serial.upper(),  # 官方要求字母大写
        "channelNo": channel_no,
    }).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    print(f"[ERTC] capture 请求: deviceSerial={device_serial.upper()} channelNo={channel_no}")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            result = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        print(f"[ERTC] capture HTTP {e.code}: {raw}")
        try:
            return json.loads(raw)
        except Exception:
            return {"code": e.code, "msg": raw, "data": None}
    except Exception as e:
        print(f"[ERTC] capture 请求异常: {e}")
        return {"code": -1, "msg": str(e), "data": None}
    print(f"[ERTC] capture 响应: {result}")
    return result


def _extract_pic_url_from_capture(result: dict) -> str:
    """宽容取抓拍结果图片 URL：data 可能为字符串 / dict.url / dict.picUrl。"""
    data = result.get("data")
    if not data:
        return ""
    if isinstance(data, str):
        return data
    if isinstance(data, dict):
        return str(data.get("url") or data.get("picUrl") or "")
    return ""


def download_file(url: str, target_path: str, timeout: int = 30,
                  max_bytes: int = 10 * 1024 * 1024) -> bool:
    """下载文件字节到 target_path（父目录自动创建）；超限/异常删残留返回 False。"""
    if not url:
        return False
    parent = os.path.dirname(target_path)
    if parent:
        os.makedirs(parent, exist_ok=True)
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = resp.read(max_bytes + 1)
        if len(data) > max_bytes:
            print(f"[ERTC] 下载超限 {len(data)} > {max_bytes}: {url}")
            return False
        with open(target_path, "wb") as f:
            f.write(data)
        return True
    except Exception as e:
        print(f"[ERTC] 下载文件失败: {url} -> {e}")
        try:
            if os.path.exists(target_path):
                os.remove(target_path)
        except OSError:
            pass
        return False


def try_decrypt_picture(url: str, device_serial: str, validate_code: str) -> Optional[str]:
    """尝试调萤石 REST 图片解密，返回解密后图片 URL。

    TODO 待真机验证：官方文档未收录该 REST 接口（官方实证路径只有 SDK 本地
    decryptData）。此函数为尝试性占位实现：任何失败仅记日志返回 None，
    绝不抛异常、绝不阻断告警流程（调用方保留原始 URL）。
    """
    token = _get_access_token()
    if not token or not validate_code:
        print("[ERTC] 解密前置条件不足（token/验证码），跳过解密")
        return None
    try:
        body = urllib.parse.urlencode({
            "accessToken": token,
            "deviceSerial": device_serial.upper(),
            "channelNo": 1,
            "picUrl": url,
            "validateCode": validate_code,
        }).encode("utf-8")
        req = urllib.request.Request(
            f"{EZVIZ_OPEN_BASE_URL}/api/lapp/device/encrypt/picture",
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=20) as resp:
            result = json.loads(resp.read().decode("utf-8"))
        print(f"[ERTC] 图片解密响应: {result}")
        if _resp_ok(result):
            data = result.get("data")
            if isinstance(data, str) and data:
                return data
            if isinstance(data, dict):
                decrypted = data.get("url") or data.get("picUrl")
                if decrypted:
                    return str(decrypted)
        print(f"[ERTC] 图片解密失败/无结果: {result}")
        return None
    except Exception as e:
        print(f"[ERTC] 图片解密异常（接口可能不存在，属预期降级）: {e}")
        return None
