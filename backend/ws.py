"""WebSocket 管理器 — 把萤石通话事件实时推给 ElderlyCare App"""
from typing import Dict
from fastapi import APIRouter, WebSocket, WebSocketDisconnect

router = APIRouter()


class WebSocketManager:
    """维护 client_id → [ws, ...] 映射，支持按 client 推送或广播。"""

    def __init__(self):
        self._conns: Dict[str, list] = {}

    async def connect(self, ws: WebSocket, client_id: str):
        await ws.accept()
        self._conns.setdefault(client_id, []).append(ws)
        print(f"[WS] {client_id} 已连接 (在线客户端数 {len(self._conns)})")

    def disconnect(self, ws: WebSocket, client_id: str):
        if client_id in self._conns:
            self._conns[client_id] = [c for c in self._conns[client_id] if c is not ws]
            if not self._conns[client_id]:
                del self._conns[client_id]
        print(f"[WS] {client_id} 已断开")

    async def send_to(self, client_id: str, message: str):
        for ws in self._conns.get(client_id, []):
            try:
                await ws.send_text(message)
            except Exception:
                pass

    async def broadcast(self, message: str):
        for client_id in list(self._conns.keys()):
            await self.send_to(client_id, message)

    def online_count(self) -> int:
        return len(self._conns)


ws_manager = WebSocketManager()


@router.websocket("/api/ws")
async def ws_endpoint(ws: WebSocket):
    """App 连接此端点接收通话事件。clientId 用于路由（默认用联系人 account）。"""
    client_id = ws.query_params.get("clientId", "unknown")
    await ws_manager.connect(ws, client_id)
    try:
        while True:
            data = await ws.receive_text()
            if data == "ping":
                await ws.send_text("pong")
    except WebSocketDisconnect:
        ws_manager.disconnect(ws, client_id)
    except Exception:
        ws_manager.disconnect(ws, client_id)
