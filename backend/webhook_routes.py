"""萤石消息推送 webhook 接收路由

设备呼叫 App 时，萤石把实时音视频通话事件 POST 到这里的回调地址，
后端解析后通过 WebSocket 实时推给 App，App 再弹出来电接听界面。

回调地址需在萤石控制台「消息推送」里配置为公网 URL，并订阅「实时音视频」消息类型。

消息结构（ys.open.rtc.call，实测）：
    header: type / deviceId / messageId / messageTime / channelNo
    body:   action / callId / strRoomId / appId / version / timestamp
    action: request=设备发起呼叫(来电)  cancel=取消  reject=拒接  busy=忙  bellTimeout=响铃超时  answer=接听

萤石要求应答里回传 header.messageId，否则提示「messageId 缺失」。
"""
import json
from fastapi import APIRouter, Request

from ws import ws_manager

router = APIRouter(prefix="/api/ezviz", tags=["萤石回调"])


@router.post("/webhook")
async def webhook(request: Request):
    """接收萤石消息推送（实时音视频通话事件）。"""
    try:
        raw = (await request.body()).decode("utf-8")
    except Exception as e:
        print(f"[WEBHOOK] 读取请求体失败: {e}")
        raw = ""

    print(f"[WEBHOOK] 收到回调: {raw}")

    try:
        payload = json.loads(raw) if raw else {}
    except Exception:
        payload = {"raw": raw}

    header = payload.get("header") or {}
    body = payload.get("body") or {}

    msg_type = header.get("type", "")
    message_id = header.get("messageId", "")
    device_serial = header.get("deviceId", "")
    action = body.get("action", "")
    call_id = body.get("callId", "")
    room_id = body.get("strRoomId", "")

    # action=request → 设备主动呼叫 App（来电）；其余状态 → 通话状态变化
    is_incoming = msg_type == "ys.open.rtc.call" and action == "request"
    event = {
        "type": "incoming_call" if is_incoming else "call_state",
        "data": {
            "action": action,
            "deviceSerial": device_serial,
            "callingId": call_id,
            "roomId": room_id,
            "raw": payload,  # 原始 payload 保留，便于调试
        },
    }
    # 推送给所有在线客户端（单家庭 demo）。
    # TODO(多家庭): 按 header.deviceId 映射到具体 clientId，用 send_to 精准路由。
    await ws_manager.broadcast(json.dumps(event, ensure_ascii=False))

    # 必须回传 messageId，萤石才会认为推送成功
    return {"code": "200", "msg": "success", "messageId": message_id}
