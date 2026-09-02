"""智慧养老平台后端入口 — Web 四端口前端所对接的 API 服务

启动:
  cd backend
  uvicorn platform_main:app --host 0.0.0.0 --port 8002

说明:
  ElderlyCare4 有相互独立的两套 FastAPI 后端：
    - main.py          : ERTC 云通话信令后端(端口 8000)，供 Android App 调用
    - platform_main.py : 智慧养老平台后端(端口 8002)，供 Web 前端 web/ 调用
  本文件把 backend/app/api/routes/* 的路由全部挂载成一个可运行的 FastAPI app，
  并在导入时调用 init_db()(幂等建表 + 空库才插种子数据，对应 data/elderly_care.db)。

  注意：health_record 路由(无 prefix、用全路径)须先于 family 挂载，
  否则 /api/family/health/{elderly_id} 会被 family 路由的同路径抢先遮蔽。
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.database import init_db
from app.api.routes import (
    auth,
    family,
    community,
    hospital,
    health_record,
    admin,
    work_order,
    alarm,
    incident,
    ws,
)

# 幂等建表 + 种子数据（users/elderly/devices/authorizations 等 13 张表）
init_db()

app = FastAPI(
    title="智慧养老平台",
    description="萤视Pro · 四端口权限体系 Web 前端所对接的 API（家属/社区/医院/管理员）",
    version="1.0.0",
)

# CORS（开发期放开，生产收紧为 Web 域名）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_record.router)
app.include_router(auth.router)
app.include_router(family.router)
app.include_router(community.router)
app.include_router(hospital.router)
app.include_router(admin.router)
app.include_router(work_order.router)
app.include_router(alarm.router)
app.include_router(incident.router)
app.include_router(ws.router)


@app.get("/")
async def root():
    return {"name": "智慧养老平台", "version": "1.0.0", "status": "running"}


@app.get("/api/health")
async def health():
    return {"status": "healthy", "websocket_online": ws.ws_manager.get_online_count()}
