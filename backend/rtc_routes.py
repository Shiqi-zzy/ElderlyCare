"""云通话(ERTC) 客户端路由 — 供 ElderlyCare App 调用

双向通话信令：
  - App 呼叫设备: 客户端拿 token 入会 + EZOpenSDK.inviteDeviceEnterMeeting 呼叫设备
  - 设备呼叫 App: 萤石 webhook → /api/ezviz/webhook → WebSocket 推给 App
"""
import time

from fastapi import APIRouter
from pydantic import BaseModel

from config import EZVIZ_RTC_APP_ID, EZVIZ_RTC_DEVICE_SERIAL
import ertc_service

router = APIRouter(prefix="/api/rtc", tags=["云通话"])


class SetupRequest(BaseModel):
    device_serial: str = ""
    account: str = "family001"
    contact_name: str = "家属"


class TokenRequest(BaseModel):
    room_id: str = ""        # 不传则后端生成
    custom_id: str = "family001"
    device_serial: str = ""


class CallRequest(BaseModel):
    device_serial: str = ""
    account: str = "family001"
    room_id: str = ""


class CallIdRequest(BaseModel):
    call_id: str = ""


def _summarize(result: dict) -> dict:
    """把萤石返回压成 {code, message, data}，便于客户端读取"""
    meta = result.get("meta") or {}
    return {"code": meta.get("code"), "message": meta.get("message"), "data": result.get("data")}


@router.post("/setup")
async def setup(req: SetupRequest):
    """一次性开通（幂等）：开通设备通话能力 + 创建联系人 + 设备关联联系人。"""
    device_serial = req.device_serial or EZVIZ_RTC_DEVICE_SERIAL
    steps = {
        "activate": _summarize(ertc_service.activate_device(device_serial)),
        "contact": _summarize(ertc_service.create_contact(req.account, req.contact_name)),
        "associate": _summarize(ertc_service.associate_device_contact(device_serial, req.account)),
    }
    return {"app_id": EZVIZ_RTC_APP_ID, "device_serial": device_serial, "account": req.account, "steps": steps}


@router.post("/token")
async def get_token(req: TokenRequest):
    """获取客户端 + 设备 RTC token（同一房间）。App 入会 + 邀请设备都靠它。"""
    device_serial = req.device_serial or EZVIZ_RTC_DEVICE_SERIAL
    room_id = req.room_id or f"room{int(time.time() * 1000)}"
    tokens = ertc_service.get_call_tokens(room_id, req.custom_id, device_serial)
    ok = bool(tokens["client_token"] and tokens["device_token"])
    return {
        "code": 200 if ok else -1,
        "message": "ok" if ok else "token 获取失败",
        "data": {
            "app_id": EZVIZ_RTC_APP_ID,
            "room_id": room_id,
            "device_serial": device_serial,
            "user_id": req.custom_id,
            "client_token": tokens["client_token"],
            "device_token": tokens["device_token"],
        },
    }


@router.post("/call")
async def call(req: CallRequest):
    """App 呼叫设备（HTTP 方式，后端代发）。

    客户端也可用 EZOpenSDK.inviteDeviceEnterMeeting 直接呼叫，无需此接口。
    """
    device_serial = req.device_serial or EZVIZ_RTC_DEVICE_SERIAL
    room_id = req.room_id or f"room{int(time.time() * 1000)}"
    tokens = ertc_service.get_call_tokens(room_id, req.account, device_serial)
    if not tokens["device_token"]:
        return {"code": -1, "message": "设备 token 获取失败", "data": None}
    result = ertc_service.call_device(device_serial, req.account, room_id, tokens["device_token"])
    return _summarize(result)


@router.post("/reject")
async def reject(req: CallIdRequest):
    """客户端拒绝来电。参数待实测确认。"""
    return _summarize(ertc_service.reject_call(req.call_id))


@router.post("/cancel")
async def cancel(req: CallIdRequest):
    """客户端取消呼叫。参数待实测确认。"""
    return _summarize(ertc_service.cancel_call(req.call_id))
