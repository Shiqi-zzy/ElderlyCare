"""WebSocket 管理器 — 把萤石通话事件实时推给 ElderlyCare App

两条推送通道：
- broadcast：云通话事件（保留，所有在线客户端）
- send_alarm：告警事件（按订阅精准推送——客户端连接后上报已授权
  deviceSn 集合 `{"type":"subscribe","sns":[...]}`，后端维护
  client_id → 订阅 SN 映射；医院端不上报订阅，只靠 App 60s 轮询兜底）
"""
import json
from typing import Dict
from fastapi import APIRouter, WebSocket, WebSocketDisconnect

router = APIRouter()


class WebSocketManager:
    """维护 client_id → [ws, ...] 连接映射与 client_id → 订阅 SN 集合。"""

    def __init__(self):
        self._conns: Dict[str, list] = {}
        self._subs: Dict[str, set] = {}  # client_id → 已授权 deviceSn 集合

    async def connect(self, ws: WebSocket, client_id: str):
        await ws.accept()
        self._conns.setdefault(client_id, []).append(ws)
        print(f"[WS] {client_id} 已连接 (在线客户端数 {len(self._conns)})")

    def disconnect(self, ws: WebSocket, client_id: str):
        if client_id in self._conns:
            self._conns[client_id] = [c for c in self._conns[client_id] if c is not ws]
            if not self._conns[client_id]:
                del self._conns[client_id]
                # 连接全部断开后清订阅（避免残留映射误推）
                self._subs.pop(client_id, None)
        print(f"[WS] {client_id} 已断开")

    def set_subscriptions(self, client_id: str, sns):
        """更新客户端订阅的设备 SN 集合（App 连接后上报 + 授权变更时重发）。"""
        self._subs[client_id] = {str(s) for s in (sns or []) if s}
        print(f"[WS] {client_id} 订阅更新: {len(self._subs[client_id])} 台设备")

    async def send_to(self, client_id: str, message: str):
        for ws in self._conns.get(client_id, []):
            try:
                await ws.send_text(message)
            except Exception:
                pass

    async def broadcast(self, message: str):
        for client_id in list(self._conns.keys()):
            await self.send_to(client_id, message)

    async def send_alarm(self, device_serial: str, message: str) -> int:
        """告警精准推送：只发给订阅了该设备的客户端，返回推送客户端数。"""
        sent = 0
        for client_id, sns in list(self._subs.items()):
            if device_serial in sns:
                await self.send_to(client_id, message)
                sent += 1
        return sent

    def online_count(self) -> int:
        return len(self._conns)


ws_manager = WebSocketManager()


@router.websocket("/api/ws")
async def ws_endpoint(ws: WebSocket):
    """App 连接此端点接收通话/告警事件。clientId 用于路由（默认用联系人 account）。

    客户端消息协议：
    - "ping" → "pong"（保活，现有）
    - {"type":"subscribe","sns":["deviceSn",...]} → 更新该 clientId 的告警订阅
      （家属端连接后上报已授权设备 SN；医院端不订阅发空列表）
    """
    client_id = ws.query_params.get("clientId", "unknown")
    await ws_manager.connect(ws, client_id)
    try:
        while True:
            data = await ws.receive_text()
            if data == "ping":
                await ws.send_text("pong")
                continue
            # 订阅上报：{"type":"subscribe","sns":[...]}
            try:
                obj = json.loads(data)
            except Exception:
                continue
            if isinstance(obj, dict) and obj.get("type") == "subscribe":
                ws_manager.set_subscriptions(client_id, obj.get("sns") or [])
    except WebSocketDisconnect:
        ws_manager.disconnect(ws, client_id)
    except Exception:
        ws_manager.disconnect(ws, client_id)
