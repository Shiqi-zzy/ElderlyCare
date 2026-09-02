"""健康档案云同步路由 — 上传 / 拉取（自家后端，与萤石云无关）。

数据流：
  - 家属端保存档案后 fire-and-forget POST /api/health-profile/upload
    {userId, deviceSn, profileJson}（Gson 序列化，≤1MB；失败仅 App 日志，不阻断本地保存）；
  - 机构端授权后 GET /api/health-profile/{userId} → data 含 profileJson 原文
    + deviceSn/version/updatedAt（App 侧解析展示云端卡，失败静默回退本地）。

后端不做鉴权（无绑定数据，权限由 App 侧把关：UserDetailScreen 权限校验通过后才拉取）。
"""
import asyncio
import json
import logging

from fastapi import APIRouter
from pydantic import BaseModel, Field

import db
import knowledge_service

logger = logging.getLogger("profile")
logging.basicConfig(level=logging.INFO)

router = APIRouter(tags=["健康档案"])

# 档案 JSON 文本上限（1MB，远小于图片上传的 10MB）
MAX_PROFILE_BYTES = 1024 * 1024


def _ok(data=None):
    resp = {"code": 200, "message": "ok"}
    if data is not None:
        resp["data"] = data
    return resp


def _err(code, message):
    return {"code": code, "message": message}


class ProfileUploadRequest(BaseModel):
    user_id: str = Field("", alias="userId")
    device_sn: str = Field("", alias="deviceSn")
    profile_json: str = Field("", alias="profileJson")

    model_config = {"populate_by_name": True}


@router.post("/api/health-profile/upload")
async def upload_profile(req: ProfileUploadRequest):
    """健康档案上传（upsert，version 自增）。

    错误码：-1 参数缺失 / -2 体积超限 / -3 JSON 非法。
    """
    user_id = (req.user_id or "").strip()
    if not user_id:
        return _err(-1, "userId 不能为空")
    profile_json = req.profile_json or ""
    if not profile_json:
        return _err(-1, "profileJson 不能为空")
    if len(profile_json.encode("utf-8")) > MAX_PROFILE_BYTES:
        return _err(-2, "档案数据过大（上限 1MB）")
    try:
        json.loads(profile_json)
    except Exception:
        return _err(-3, "profileJson 不是合法 JSON")
    version = await asyncio.to_thread(
        db.upsert_health_profile, user_id, (req.device_sn or "").strip(), profile_json)
    logger.info("[健康档案] %s 上传成功 version=%d", user_id, version)

    # 业务库保存成功后 → 知识库同步（fire-and-forget）。
    # 知识库任何失败仅打印 error 日志，不返回错误给前端，不影响档案保存业务。
    device_serial = (req.device_sn or "").strip()
    if device_serial:
        asyncio.create_task(_sync_knowledge_async(user_id, device_serial, json.loads(profile_json)))
    else:
        logger.info("[知识库] %s deviceSn 为空，跳过知识库同步（metadata 禁止缺失）", user_id)
    return _ok({"version": version})


async def _sync_knowledge_async(user_id: str, device_serial: str, profile: dict):
    """档案保存成功后异步同步知识库：删旧 → 传新（metadata.device_serial 严格携带）。

    全部异常吞掉只记日志——知识库 API 失败不阻断档案业务，不返回错误给前端。
    """
    try:
        result = await asyncio.to_thread(
            knowledge_service.sync_profile_to_knowledge, device_serial, profile)
        logger.info("[知识库] %s 档案同步结果: %s", user_id, result)
    except Exception:
        logger.exception("[知识库] %s 档案同步异常（已降级，不影响档案保存）", user_id)


@router.get("/api/health-profile/{user_id}")
async def get_profile(user_id: str):
    """按 userId 拉取云端健康档案；无记录返回 -4。"""
    row = await asyncio.to_thread(db.get_health_profile, (user_id or "").strip())
    if not row:
        return _err(-4, "云端暂无该用户档案")
    return _ok({
        "userId": row["user_id"],
        "deviceSn": row["device_sn"],
        "profileJson": row["profile_json"],
        "version": row["version"],
        "updatedAt": row["updated_at"],
    })
