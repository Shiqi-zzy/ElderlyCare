"""抓拍路由 — 手动云端抓拍 / 抓拍列表 / 已读 / 未读数 / 设备验证码上报

数据与流程（与留言模块完全隔离）：
  - alarm_events 仅存两类抓拍记录：capture_type=manual（手动）/ auto（设备自动），
    专供 App「全部抓拍」页；图片落盘 backend/media/captures/{SN}/{recordId}.jpg，
    由 main.py 以 /media 挂载 StaticFiles 对外提供（App 用 RTC_BACKEND_URL 拼接）。
  - 手动抓拍：App POST /api/ezviz/capture → 后端调萤石 /api/lapp/device/capture
    （同设备两次间隔 ≥4s，内存级限流）→ 下载图片落盘 → 写 alarm_events(manual)。
  - 验证码上报：App 绑定设备成功后 POST /api/device/auth → device_auth upsert，
    供 webhook 告警图片解密（isEncrypted=1）时按设备序列号取验证码。
"""
import asyncio
import json
import logging
import os
import tempfile
import threading
import time
import uuid

from fastapi import APIRouter, Query
from pydantic import BaseModel, Field

import db
import ertc_service

logger = logging.getLogger("capture")
logging.basicConfig(level=logging.INFO)

router = APIRouter(tags=["抓拍"])

# 媒体根目录：backend/media，由 main.py 以 /media 挂载 StaticFiles 对外提供
MEDIA_ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "media")

# 同设备两次抓拍最小间隔（秒）——萤石官方建议 ≥4s
CAPTURE_INTERVAL_S = 4.0
_capture_lock = threading.Lock()
_last_capture_ts: dict = {}  # device_serial(大写) → 上次抓拍登记时刻

# 单张图片下载上限（10MB）
MAX_PIC_BYTES = 10 * 1024 * 1024


def _try_acquire_capture(device_serial: str):
    """4s 限流：锁内原子「检查+登记」。返回 (是否允许, 还需等待秒数)。

    保守策略：登记即消耗窗口（即使后续萤石失败也需等 4s，防频率超限）。
    内存级限流：当前单 worker 部署成立（uvicorn 多 worker 各进程独立，需换 Redis）。
    """
    with _capture_lock:
        now = time.time()
        last = _last_capture_ts.get(device_serial, 0.0)
        wait = CAPTURE_INTERVAL_S - (now - last)
        if wait > 0:
            return False, round(wait, 1)
        _last_capture_ts[device_serial] = now
        return True, 0.0


def save_picture_to_media(device_serial: str, record_id: str, file_bytes: bytes):
    """图片落盘 media/captures/{SN}/{recordId}.jpg，返回相对 URL；失败返回 None。

    webhook_routes 的告警图片异步落盘也复用此函数。
    """
    rel_dir = os.path.join("captures", device_serial.upper())
    rel_path = f"/media/{rel_dir}/{record_id}.jpg".replace("\\", "/")
    abs_path = os.path.join(MEDIA_ROOT, rel_dir, f"{record_id}.jpg")
    try:
        os.makedirs(os.path.dirname(abs_path), exist_ok=True)
        with open(abs_path, "wb") as f:
            f.write(file_bytes)
        return rel_path
    except Exception as e:
        logger.warning("[抓拍] 图片落盘失败 %s: %s", rel_path, e)
        return None


# ---------- 请求体（camelCase 别名 + snake_case 兼容，populate_by_name） ----------

class CaptureRequest(BaseModel):
    device_serial: str = Field("", alias="deviceSerial")
    channel_no: int = Field(1, alias="channelNo")

    model_config = {"populate_by_name": True}


class MarkReadRequest(BaseModel):
    device_serial: str = Field("", alias="deviceSerial")

    model_config = {"populate_by_name": True}


class DeviceAuthRequest(BaseModel):
    device_serial: str = Field("", alias="deviceSerial")
    validate_code: str = Field("", alias="validateCode")

    model_config = {"populate_by_name": True}


# ---------- 统一信封 ----------

def _ok(data=None, **extra):
    resp = {"code": 200, "message": "ok"}
    if data is not None:
        resp["data"] = data
    resp.update(extra)
    return resp


def _err(code, message, **extra):
    resp = {"code": code, "message": message}
    resp.update(extra)
    return resp


# ---------- 手动抓拍 ----------

@router.post("/api/ezviz/capture")
async def capture(req: CaptureRequest):
    """手动云端抓拍：后端调萤石 device/capture → 下载落盘 → 写 alarm_events(manual)。

    不推 WS（App 是发起方，自己拿到结果）。
    错误码：-2 4s 限流 / -3 萤石频率超限 / -4 设备超时离线 / -5 图片下载失败。
    """
    device_serial = (req.device_serial or "").strip().upper()
    if not device_serial:
        return _err(-1, "deviceSerial 不能为空")

    allowed, wait = _try_acquire_capture(device_serial)
    if not allowed:
        return _err(-2, f"操作太频繁，请 {wait:.0f} 秒后再试", data={"waitSeconds": wait})

    # 1) 萤石云端抓拍
    try:
        ezviz = await asyncio.to_thread(
            ertc_service.capture_device, device_serial, req.channel_no or 1)
    except Exception as e:
        logger.exception("[抓拍] 调萤石失败")
        return _err(-1, f"抓拍失败: {e}")

    if not ertc_service._resp_ok(ezviz):
        ezviz_code = str(ezviz.get("code") or (ezviz.get("meta") or {}).get("code") or "")
        ezviz_msg = str(ezviz.get("msg") or ezviz.get("message") or "")
        logger.warning("[抓拍] 萤石返回失败 code=%s msg=%s", ezviz_code, ezviz_msg)
        if ezviz_code in ("10007", "10028", "10029"):
            return _err(-3, "抓拍频率受限，请稍后再试", ezviz_code=ezviz_code, ezviz_msg=ezviz_msg)
        if ezviz_code in ("20008", "20016", "20017", "20018", "20032"):
            return _err(-4, "设备未响应（可能离线），请重试", ezviz_code=ezviz_code, ezviz_msg=ezviz_msg)
        return _err(-1, f"萤石抓拍失败（{ezviz_code}）", ezviz_code=ezviz_code, ezviz_msg=ezviz_msg)

    pic_url = ertc_service._extract_pic_url_from_capture(ezviz)
    if not pic_url:
        return _err(-1, "萤石未返回图片地址",
                    ezviz_code=str(ezviz.get("code") or ""),
                    ezviz_msg=json.dumps(ezviz, ensure_ascii=False)[:200])

    # 2) 下载图片落盘
    record_id = uuid.uuid4().hex
    try:
        tmp = os.path.join(tempfile.gettempdir(), f"capture_{record_id}.jpg")
        ok = await asyncio.to_thread(ertc_service.download_file, pic_url, tmp, 30, MAX_PIC_BYTES)
        file_bytes = b""
        if ok:
            with open(tmp, "rb") as f:
                file_bytes = f.read()
            try:
                os.remove(tmp)
            except OSError:
                pass
        if not file_bytes:
            return _err(-5, "图片下载失败")
        rel_path = await asyncio.to_thread(
            save_picture_to_media, device_serial, record_id, file_bytes)
        if not rel_path:
            return _err(-5, "图片保存失败")
    except Exception as e:
        logger.exception("[抓拍] 图片下载/落盘失败")
        return _err(-5, f"图片处理失败: {e}")

    # 3) 入库（manual）
    event_time = int(time.time() * 1000)
    await asyncio.to_thread(
        db.save_capture,
        record_id, "manual", device_serial, event_time,
        "", "手动抓拍", pic_url, rel_path,
        json.dumps(ezviz, ensure_ascii=False),
    )
    logger.info("[抓拍] 手动抓拍成功 recordId=%s 设备=%s 图片=%s", record_id, device_serial, rel_path)

    return _ok({
        "recordId": record_id,
        "localPicUrl": rel_path,
        "captureType": "manual",
        "eventTime": event_time,
    })


# ---------- 全部抓拍列表 / 已读 / 未读数 ----------

@router.get("/api/captures")
async def list_captures(
    device_serial: str = Query("", alias="deviceSerial"),
    page_start: int = 0,
    page_size: int = 100,
):
    """全部抓拍列表（manual+auto，新→旧）。"""
    sn = (device_serial or "").strip().upper()
    if not sn:
        return _err(-1, "deviceSerial 不能为空")
    page_size = max(1, min(page_size or 100, 200))
    offset = max(0, page_start or 0)
    total = await asyncio.to_thread(db.count_captures, sn)
    rows = await asyncio.to_thread(db.list_captures, sn, page_size, offset)
    items = [{
        "recordId": r["message_id"],
        "deviceSerial": r["device_serial"] or r["device_id"],
        "captureType": r["capture_type"] or "auto",
        "alarmName": r["alarm_name"],
        "eventTime": r["event_time"] or r["message_time"],
        "picUrl": r["pic_url"],
        "localPicUrl": r["local_save_path"],
        "isRead": bool(r["is_read"]),
    } for r in rows]
    return _ok({"list": items, "total": total})


@router.post("/api/captures/{record_id}/read")
async def mark_read(record_id: str, req: MarkReadRequest):
    """点击条目标记该条已读（限定设备防串读）。"""
    sn = (req.device_serial or "").strip().upper()
    if not sn or not record_id:
        return _err(-1, "参数不完整")
    hit = await asyncio.to_thread(db.mark_capture_read, record_id, sn)
    if not hit:
        return _err(-6, "记录不存在")
    return _ok()


@router.get("/api/captures/unread-count")
async def unread_count(device_serial: str = Query("", alias="deviceSerial")):
    """全部抓拍页未读数（首页告警消息图标角标数据源）。"""
    sn = (device_serial or "").strip().upper()
    if not sn:
        return _err(-1, "deviceSerial 不能为空")
    count = await asyncio.to_thread(db.unread_capture_count, sn)
    return _ok({"unreadCount": count})


# ---------- 设备验证码上报 ----------

@router.post("/api/device/auth")
async def upload_device_auth(req: DeviceAuthRequest):
    """App 绑定设备成功后上报验证码（upsert；更换设备重绑时更新）。

    后端解密告警图片（isEncrypted=1）时按 device_serial 取验证码。
    """
    sn = (req.device_serial or "").strip().upper()
    code = (req.validate_code or "").strip().upper()
    if not sn or not (6 <= len(sn) <= 20) or not sn.isalnum():
        return _err(-1, "deviceSerial 格式错误")
    if not code or len(code) != 6 or not code.isalnum():
        return _err(-1, "validateCode 格式错误（6 位字母数字）")
    await asyncio.to_thread(db.upsert_device_auth, sn, code)
    logger.info("[验证码] %s 上报成功", sn)
    return _ok()
