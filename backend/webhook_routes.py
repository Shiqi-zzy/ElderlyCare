"""萤石消息推送 webhook 接收路由

两条消息类型（萤石控制台「消息推送」订阅）：
1. ys.open.rtc.call —— 设备呼叫 App 时，萤石把实时音视频通话事件 POST 到这里，
   后端解析后通过 WebSocket 广播推给 App，App 弹出来电接听界面。
2. 设备告警（ys.open.alarm，兼容 ys.open.device.alarm / ys.alarm）——
   解析 → 按客户端订阅（clientId→授权SN映射）精准推送给有权限客户端 →
   立即异步入库（db.alarm_events，messageId 幂等，capture_type=auto）→
   快速回执 → 异步下载/解密抓拍图片落盘 media → 回填路径 + WS 补推 captureUpdated。

回调地址需在萤石控制台「消息推送」里配置为公网 URL。

消息结构（ys.open.rtc.call，实测）：
    header: type / deviceId / messageId / messageTime / channelNo
    body:   action / callId / strRoomId / appId / version / timestamp
    action: request=设备发起呼叫(来电)  cancel=取消  reject=拒接  busy=忙  bellTimeout=响铃超时  answer=接听

告警消息：header 同上（type=deviceId=设备SN/messageId/messageTime），
body 含 alarmId / alarmName / alarmType / alarmTime / channelNo / alarmPicUrl /
alarmVideoUrl 等（字段随告警类型略有差异，解析做防御性兜底，
deviceSerial 优先 body.deviceSerial 其次 header.deviceId）。
图片 URL 兼容 picUrl / alarmPicUrl / pictureList[0].url 三种形态；
isEncrypted 兼容 body 字段（isEncrypted / crypt=1）与图片 URL 查询参数。

萤石要求应答里回传 header.messageId，否则提示「messageId 缺失」；2 秒内
未回 200 会重推，因此图片下载/解密全部放在回执之后的异步任务里，
任何一步失败仅记日志，不阻断告警文字入库与 WS 推送。
"""
import asyncio
import json
import logging
import os
import tempfile
import time
import urllib.parse
from datetime import datetime

from fastapi import APIRouter, Request

import capture_routes
import db
import ertc_service
from ws import ws_manager

logger = logging.getLogger("webhook")
logging.basicConfig(level=logging.INFO)

router = APIRouter(prefix="/api/ezviz", tags=["萤石回调"])

# 告警消息类型（控制台实际订阅类型可能有出入，三个别名全兼容）
_ALARM_TYPES = ("ys.open.alarm", "ys.open.device.alarm", "ys.alarm")


def _extract_alarm_pic(body: dict):
    """宽容解析告警图片 URL 与加密标记。返回 (图片URL, 是否加密)。

    URL 兼容：body.picUrl / body.alarmPicUrl / body.pictureList[0].url(或picUrl)
    isEncrypted 兼容：body.isEncrypted、body.crypt=1、URL 查询参数 isEncrypted=1
    """
    pic = str(body.get("picUrl") or body.get("alarmPicUrl") or "").strip()
    if not pic:
        picture_list = body.get("pictureList")
        if isinstance(picture_list, list) and picture_list:
            first = picture_list[0] or {}
            pic = str(first.get("url") or first.get("picUrl") or "").strip()
    if not pic:
        return "", False

    encrypted = False
    if body.get("isEncrypted") in (1, "1", True, "true", "True"):
        encrypted = True
    if body.get("crypt") in (1, "1"):
        encrypted = True
    # URL 查询参数兜底：...&isEncrypted=1
    try:
        query = urllib.parse.parse_qs(urllib.parse.urlparse(pic).query)
        if query.get("isEncrypted") and query["isEncrypted"][0] in ("1", "true", "True"):
            encrypted = True
    except Exception:
        pass
    return pic, encrypted


def _alarm_event_time(header: dict, alarm_time: str) -> int:
    """抓拍时间（毫秒）：header.messageTime 优先；否则 alarmTime 字符串解析；再兜底当前。"""
    ts = header.get("messageTime") or 0
    if ts:
        try:
            return int(ts)
        except (TypeError, ValueError):
            pass
    if alarm_time:
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S"):
            try:
                return int(datetime.strptime(alarm_time, fmt).timestamp() * 1000)
            except ValueError:
                continue
    return int(time.time() * 1000)


def _read_bytes(path: str) -> bytes:
    try:
        with open(path, "rb") as f:
            return f.read()
    except OSError:
        return b""


def _cleanup(path: str):
    try:
        if path and os.path.exists(path):
            os.remove(path)
    except OSError:
        pass


async def _process_alarm_picture(record_id: str, device_serial: str,
                                 pic_url: str, is_encrypted: bool):
    """回执之后异步执行：下载告警抓拍图 → 必要时尝试解密 → 落盘 media →
    回填 alarm_events.local_save_path → WS 补推 captureUpdated。

    任何一步失败仅记日志不阻断（图片不完整不影响告警文字链路）。
    """
    if not pic_url:
        logger.warning("[WEBHOOK] 告警 %s 无图片 URL，跳过下载", record_id)
        return
    tmp = os.path.join(tempfile.gettempdir(), f"alarm_{record_id}.jpg")
    try:
        ok = await asyncio.to_thread(ertc_service.download_file, pic_url, tmp)
        if not ok:
            logger.warning("[WEBHOOK] 告警图片下载失败 recordId=%s url=%s", record_id, pic_url)
            return
        data = await asyncio.to_thread(_read_bytes, tmp)
        if not data:
            logger.warning("[WEBHOOK] 告警图片读取失败 recordId=%s", record_id)
            return

        # 非 JPEG 且加密 → 尝试解密（device_auth 验证码）
        if data[:3] != b"\xff\xd8\xff":
            if is_encrypted:
                code = await asyncio.to_thread(db.get_validate_code, device_serial)
                if not code:
                    logger.warning("[WEBHOOK] device_auth 无 %s 验证码，跳过解密", device_serial)
                else:
                    decrypted_url = await asyncio.to_thread(
                        ertc_service.try_decrypt_picture, pic_url, device_serial, code)
                    if decrypted_url:
                        ok2 = await asyncio.to_thread(
                            ertc_service.download_file, decrypted_url, tmp)
                        data = await asyncio.to_thread(_read_bytes, tmp) if ok2 else b""
                        if not (data and data[:3] == b"\xff\xd8\xff"):
                            logger.warning("[WEBHOOK] 解密后图片仍无效 recordId=%s", record_id)
                            data = b""
            if not data or data[:3] != b"\xff\xd8\xff":
                logger.warning("[WEBHOOK] 图片非 JPEG 且解密不可用，保留原始 URL 不落盘 recordId=%s",
                               record_id)
                _cleanup(tmp)
                return

        rel = await asyncio.to_thread(
            capture_routes.save_picture_to_media, device_serial, record_id, data)
        if not rel:
            logger.warning("[WEBHOOK] 图片落盘失败 recordId=%s", record_id)
            return
        _cleanup(tmp)

        await asyncio.to_thread(db.update_capture_pic, record_id, rel)
        await ws_manager.send_alarm(
            device_serial,
            json.dumps({
                "type": "captureUpdated",
                "data": {"recordId": record_id, "localPicUrl": rel, "captureType": "auto"},
            }, ensure_ascii=False),
        )
        logger.info("[WEBHOOK] 告警图片就绪 recordId=%s path=%s", record_id, rel)
    except Exception as e:
        logger.warning("[WEBHOOK] 告警图片处理异常 recordId=%s: %s", record_id, e)
        _cleanup(tmp)


@router.post("/webhook")
async def webhook(request: Request):
    """接收萤石消息推送（云通话事件 + 告警事件）。"""
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

    # ---------- 云通话事件（现有逻辑不变） ----------
    if msg_type == "ys.open.rtc.call":
        action = body.get("action", "")
        call_id = body.get("callId", "")
        room_id = body.get("strRoomId", "")

        # action=request → 设备主动呼叫 App（来电）；其余状态 → 通话状态变化
        is_incoming = action == "request"
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

    # ---------- 告警事件：立即入库 + 精准推送 + 快速回执，图片异步下载落盘 ----------
    if msg_type in _ALARM_TYPES:
        alarm_id = str(body.get("alarmId") or body.get("alarmMessageId") or "").strip() \
            or message_id  # alarmId 缺失时用 messageId 兜底，保证 App 端幂等去重有稳定键
        alarm_name = body.get("alarmName") or "设备报警"
        # 设备 SN：body.deviceSerial 优先（部分告警类型有），否则 header.deviceId
        alarm_sn = str(body.get("deviceSerial") or "").strip() or device_serial
        alarm_time = body.get("alarmTime") or ""
        alarm_type = body.get("alarmType") or 0
        channel_no = body.get("channelNo") or 1
        pic_url, is_encrypted = _extract_alarm_pic(body)
        event_time = _alarm_event_time(header, str(alarm_time))

        # 立即入库（图片路径留空，下载完成后回填）；messageId 幂等
        asyncio.create_task(
            asyncio.to_thread(
                db.save_capture,
                message_id, "auto", alarm_sn, event_time,
                alarm_id, alarm_name, pic_url, "",
                json.dumps(payload, ensure_ascii=False),
            )
        )

        # 推送字段与 App 端 AlarmMessage 同名对齐；图片只进后端，
        # App 侧仅落告警文字（localPicUrl 供全部抓拍页，图片就绪前为空）
        event = {
            "type": "alarm",
            "data": {
                "recordId": message_id,
                "alarmId": alarm_id,
                "deviceSerial": alarm_sn,
                "channelNo": channel_no,
                "alarmName": alarm_name,
                "alarmType": alarm_type,
                "alarmTime": alarm_time,
                "alarmPicUrl": pic_url,
                "alarmVideoUrl": body.get("alarmVideoUrl") or "",
                "captureType": "auto",
                "localPicUrl": "",
                "isRead": False,
                "isChecked": False,
                "deviceName": None,
            },
        }
        sent = await ws_manager.send_alarm(alarm_sn, json.dumps(event, ensure_ascii=False))

        # 快速回执（萤石 2s 未回 200 会重推）；图片处理放回执之后
        asyncio.create_task(
            _process_alarm_picture(message_id, alarm_sn, pic_url, is_encrypted)
        )

        logger.info("[WEBHOOK] 告警 messageId=%s alarmId=%s 设备=%s 加密=%s 推送客户端数=%d",
                    message_id, alarm_id, alarm_sn, is_encrypted, sent)

        return {"code": "200", "msg": "success", "messageId": message_id}

    # ---------- 其它消息类型：仅记录，不推送（不影响 App 业务） ----------
    print(f"[WEBHOOK] 未处理的消息类型: {msg_type}")
    return {"code": "200", "msg": "success", "messageId": message_id}
