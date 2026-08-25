"""WebSocket 实时推送路由"""
import json
from typing import Dict
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from ...core.security import decode_access_token

router = APIRouter()


class WebSocketManager:
    """
    WebSocket 连接管理器。

    功能：
      - 维护 user_id → WebSocket 的映射
      - 支持向指定用户发送消息
      - 支持广播消息
    """

    def __init__(self):
        self._connections: Dict[str, list] = {}  # user_id → [ws1, ws2, ...]

    async def connect(self, websocket: WebSocket, user_id: str):
        """接受连接并注册"""
        await websocket.accept()
        if user_id not in self._connections:
            self._connections[user_id] = []
        self._connections[user_id].append(websocket)
        print(f"[WS] 用户 {user_id} 已连接 (当前连接数: {len(self._connections[user_id])})")

    def disconnect(self, websocket: WebSocket, user_id: str):
        """断开连接并清理"""
        if user_id in self._connections:
            self._connections[user_id] = [ws for ws in self._connections[user_id] if ws != websocket]
            if not self._connections[user_id]:
                del self._connections[user_id]
        print(f"[WS] 用户 {user_id} 已断开")

    async def send_to_user(self, user_id: str, message: str):
        """向指定用户发送消息"""
        if user_id in self._connections:
            for ws in self._connections[user_id]:
                try:
                    await ws.send_text(message)
                except Exception:
                    pass  # 忽略发送失败

    async def broadcast(self, message: str):
        """广播消息给所有连接的用户"""
        for user_id in list(self._connections.keys()):
            await self.send_to_user(user_id, message)

    def get_online_count(self) -> int:
        """获取在线用户数"""
        return len(self._connections)


# 全局单例
ws_manager = WebSocketManager()


@router.websocket("/api/ws/live")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket 实时推送端点"""
    # 从 query string 中获取 token
    token = websocket.query_params.get("token")
    if not token:
        await websocket.close(code=4001, reason="缺少 token")
        return

    # 解析 token 获取 user_id
    payload = decode_access_token(token)
    if not payload:
        await websocket.close(code=4002, reason="Token 无效")
        return

    user_id = payload.get("sub")
    if not user_id:
        await websocket.close(code=4003, reason="Token 格式错误")
        return

    # 建立连接
    await ws_manager.connect(websocket, user_id)

    try:
        # 发送欢迎消息
        await websocket.send_text(json.dumps({
            "type": "connected",
            "data": {"message": "实时推送已连接", "user_id": user_id}
        }, ensure_ascii=False))

        # 保持连接，接收客户端消息（如心跳）
        while True:
            data = await websocket.receive_text()
            # 回复心跳
            if data == "ping":
                await websocket.send_text("pong")

    except WebSocketDisconnect:
        ws_manager.disconnect(websocket, user_id)
    except Exception as e:
        ws_manager.disconnect(websocket, user_id)
