"""ElderlyCare 云通话(ERTC) 信令后端

启动:
  cd backend
  pip install -r requirements.txt
  uvicorn main:app --host 0.0.0.0 --port 8000 --reload

说明:
  这是 ElderlyCare 的云通话信令后端，负责打通 App ↔ 萤石设备(RK3) 的双向音视频通话：
  - App 呼叫设备 / 拒接 / 取消 / 取通话 token
  - 接收萤石消息推送(webhook)，实时推送给 App（WebSocket）
  - 告警/手动抓拍：图片下载落盘 media/ 并挂载 /media 静态目录供 App 加载
"""
import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

import capture_routes
import leave_message_routes
import rtc_routes
import webhook_routes
import ws

app = FastAPI(
    title="ElderlyCare 云通话信令",
    description="ElderlyCare App ↔ 萤石 RK3 设备双向音视频通话信令后端",
    version="0.1.0",
)

# CORS（开发期放开，生产收紧为 App 域名）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(rtc_routes.router)
app.include_router(webhook_routes.router)
app.include_router(ws.router)
app.include_router(leave_message_routes.router)
app.include_router(capture_routes.router)

# 抓拍图片静态目录（目录不存在 StaticFiles 会启动报错，先建好）
os.makedirs(capture_routes.MEDIA_ROOT, exist_ok=True)
app.mount("/media", StaticFiles(directory=capture_routes.MEDIA_ROOT), name="media")


@app.get("/")
async def root():
    return {"name": "ElderlyCare 云通话信令", "version": "0.1.0", "status": "running"}


@app.get("/api/health")
async def health():
    return {"status": "healthy", "websocket_online": ws.ws_manager.online_count()}
