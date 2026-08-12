"""
智慧养老平台 — FastAPI 后端入口

启动方式:
  cd backend
  pip install -r requirements.txt
  uvicorn main:app --host 0.0.0.0 --port 8000 --reload

访问:
  API 文档: http://localhost:8000/docs
  种子数据已预置测试账号
"""
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.database import init_db
from app.core.config import DB_PATH
from app.api.routes import auth, family, alarm, work_order, community, hospital, admin as admin_routes
from app.api.routes import health_record as health_record_routes
from app.api.routes.ws import router as ws_router, ws_manager
from app.services.notification_service import NotificationService
from app.services.permission_service import PermissionService
from app.services.device_service import DeviceService


# ──────────────────── 定时后台任务 ────────────────────
async def periodic_auth_expire_check():
    """每 10 分钟检查一次授权过期"""
    while True:
        try:
            count = PermissionService.check_and_expire()
            if count > 0:
                print(f"[定时任务] {count} 条授权已自动过期")
        except Exception as e:
            print(f"[定时任务] 授权过期检查出错: {e}")
        await asyncio.sleep(600)  # 10分钟


async def periodic_device_offline_check():
    """每 2 分钟检查一次设备离线"""
    while True:
        try:
            offline_devices = DeviceService.check_offline_devices(timeout_seconds=120)
            if offline_devices:
                print(f"[定时任务] {len(offline_devices)} 台设备已标记为离线")
        except Exception as e:
            print(f"[定时任务] 设备离线检查出错: {e}")
        await asyncio.sleep(120)  # 2分钟


# ──────────────────── 生命周期 ────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时
    print("=" * 60)
    print("  智慧养老平台后端启动中...")
    print(f"  数据库: {DB_PATH}")
    print("=" * 60)

    # 初始化数据库
    init_db()

    # 注入 WebSocket 管理器到通知服务
    NotificationService.set_ws_manager(ws_manager)

    # 启动后台定时任务
    expire_task = asyncio.create_task(periodic_auth_expire_check())
    offline_task = asyncio.create_task(periodic_device_offline_check())

    print("[启动] 后台定时任务已启动 (授权过期检查 + 设备离线检测)")
    print("[启动] 服务已就绪，监听 http://0.0.0.0:8000")

    yield

    # 关闭时
    expire_task.cancel()
    offline_task.cancel()
    print("[关闭] 服务已停止")


# ──────────────────── 应用实例 ────────────────────
app = FastAPI(
    title="智慧养老平台 API",
    description="""
## 智慧养老平台（Smart Elderly Care Platform）

四端口权限体系：**家属端 / 社区端 / 医院端 / 管理端**

### 核心功能
- **告警闭环引擎**: 设备异常 → AI检测 → 防误报 → 分级告警 → 工单处置 → 闭环归档
- **权限引擎**: 最小授权原则，默认无访问权限，按需临时/时效授权，全操作审计
- **隐私脱敏**: 按角色分层展示数据，证件/手机/地址脱敏处理

### 测试账号 (种子数据)
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 家属 | family_test | family123 |
| 社区 | community_test | community123 |
| 医院 | hospital_test | hospital123 |
| 管理员 | admin | admin123 |
    """,
    version="1.0.0-Phase1",
    lifespan=lifespan
)

# CORS 配置（允许前端跨域访问）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应改为具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ──────────────────── 注册路由 ────────────────────
app.include_router(auth.router)
app.include_router(family.router)
app.include_router(alarm.router)
app.include_router(work_order.router)
app.include_router(community.router)
app.include_router(hospital.router)
app.include_router(admin_routes.router)
app.include_router(health_record_routes.router)
app.include_router(ws_router)


# ──────────────────── 健康检查 ────────────────────
@app.get("/")
async def root():
    return {
        "name": "智慧养老平台 API",
        "version": "1.0.0-Phase1",
        "status": "running",
        "docs": "/docs"
    }


@app.get("/api/health")
async def health_check():
    from app.core.database import get_db
    db = get_db()
    user_count = db.execute("SELECT COUNT(*) FROM users").fetchone()[0]
    return {
        "status": "healthy",
        "database": "connected",
        "users": user_count,
        "websocket_online": ws_manager.get_online_count()
    }


# ──────────────────── 直接运行入口 ────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
