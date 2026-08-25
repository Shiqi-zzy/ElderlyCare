"""文字留言路由 — App 提交文本 → 云端 TTS → 萤石云广播下发 RK3 播报

链路（2026-08-19 实测确认）：
  App POST /api/leave-message/text {device_serial, text}
    → edge-tts（微软云端合成，免费）合成 mp3
    → 萤石 POST /api/lapp/voice/upload（multipart）→ fileUrl（1 天有效）
    → 萤石 POST /api/lapp/voice/send（fileUrl + deviceSerial）→ 设备播报

设计要点：
  - App 端零 TTS 依赖（不碰手机本地 TTS 引擎）；
  - 萤石返回的原始 code/msg 透传给 App（UI 展示萤石原始错误码与错误信息）；
  - 每步请求/响应均打印日志（请求体、HTTP 状态码、完整返回 JSON）。

说明：萤石公开 API 无「文本→TTS→下发」一步接口（voice/upload、voice/send、
voice/task/create 均只收音频文件）；平台后台的文字任务依赖 Cookie 登录态，
AppKey/AppSecret 无法调用。故 TTS 在服务端完成后再走云广播文件链路。
后续若萤石开放智能体/云广播文本接口，只需替换本文件内部实现，App 无感知。
"""
import json
import logging
import tempfile
import time
import urllib.error
import urllib.request
import uuid

from fastapi import APIRouter
from fastapi.responses import Response
from pydantic import BaseModel

from config import EZVIZ_OPEN_BASE_URL
import ertc_service  # 复用 _get_access_token（AppKey/AppSecret → accessToken，带缓存）

logger = logging.getLogger("leave_message")
logging.basicConfig(level=logging.INFO)

router = APIRouter(prefix="/api/leave-message", tags=["文字留言"])

# 留言文本长度上限（与 App 端 80 字一致，留余量）
MAX_TEXT_LEN = 200


class TextRequest(BaseModel):
    device_serial: str = ""
    text: str = ""


def _http_json(method: str, url: str, headers: dict, body: bytes = None) -> tuple:
    """urllib 统一请求：返回 (http_status, json_body|None, raw_text)。

    raw_text 保留完整响应原文，便于日志与透传。
    """
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            try:
                return resp.status, json.loads(raw), raw
            except ValueError:
                return resp.status, None, raw
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(raw), raw
        except ValueError:
            return e.code, None, raw


def _multipart_body(token: str, voice_name: str, file_bytes: bytes, filename: str) -> tuple:
    """构造 voice/upload 的 multipart/form-data 请求体与 Content-Type"""
    boundary = "----ezviz" + uuid.uuid4().hex
    parts = []
    for name, value in (("accessToken", token), ("voiceName", voice_name)):
        parts.append(
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'
            f"{value}\r\n"
        )
    parts.append(
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="voiceFile"; filename="{filename}"\r\n'
        f"Content-Type: audio/mpeg\r\n\r\n"
    )
    body = "".join(parts).encode("utf-8")
    body += file_bytes
    body += f"\r\n--{boundary}--\r\n".encode("utf-8")
    return body, f"multipart/form-data; boundary={boundary}"


# 试听音色白名单（提醒计划表单手机试听用；edge-tts 中文音色）
VOICE_WHITELIST = {
    "zh-CN-XiaoxiaoNeural",  # 晓晓：标准中文女声
    "zh-CN-XiaoyiNeural",    # 晓伊：活泼女声
    "zh-CN-YunjianNeural",   # 云健：沉稳男声
    "zh-CN-YunxiNeural",     # 云希：阳光男声
    "zh-CN-YunxiaNeural",    # 云夏：男童
    "zh-CN-YunyangNeural",   # 云扬：新闻男声
}
DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"


async def _synthesize_tts(text: str, voice: str = DEFAULT_VOICE) -> bytes:
    """edge-tts 云端合成 mp3。失败抛异常。voice 见 VOICE_WHITELIST。"""
    import edge_tts

    with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as tmp:
        path = tmp.name
    communicate = edge_tts.Communicate(text, voice)
    await communicate.save(path)
    with open(path, "rb") as f:
        return f.read()


@router.post("/text")
async def send_text(req: TextRequest):
    """App 文字留言：文本 → 云端 TTS → 云广播下发设备播报。"""
    device_serial = (req.device_serial or "").strip()
    text = (req.text or "").strip()

    logger.info("[文字留言] 请求体: %s", json.dumps({"device_serial": device_serial, "text": text}, ensure_ascii=False))

    if not device_serial:
        return {"code": -1, "message": "device_serial 不能为空"}
    if not text:
        return {"code": -1, "message": "text 不能为空"}
    if len(text) > MAX_TEXT_LEN:
        return {"code": -1, "message": f"文本超过 {MAX_TEXT_LEN} 字上限"}

    # 1) 云端 TTS 合成（微软 edge-tts，免费）
    try:
        t0 = time.time()
        mp3 = await _synthesize_tts(text)
        logger.info("[文字留言] TTS 合成完成: %d 字节, 耗时 %.1fs", len(mp3), time.time() - t0)
    except Exception as e:
        logger.exception("[文字留言] TTS 合成失败")
        return {"code": -1, "message": f"云端语音合成失败: {e}"}

    # 2) 萤石 accessToken
    token = ertc_service._get_access_token()
    if not token:
        return {"code": -1, "message": "萤石 accessToken 获取失败"}

    # 3) 上传语音文件 → fileUrl
    voice_name = f"leave_{int(time.time())}"
    body, content_type = _multipart_body(token, voice_name, mp3, "leave.mp3")
    status, js, raw = _http_json(
        "POST",
        f"{EZVIZ_OPEN_BASE_URL}/api/lapp/voice/upload",
        {"Content-Type": content_type},
        body,
    )
    logger.info("[文字留言] voice/upload HTTP=%s 响应=%s", status, raw)
    if js is None or str(js.get("code")) != "200":
        ezviz_code = str(js.get("code", status)) if js else str(status)
        ezviz_msg = str(js.get("msg", raw[:200])) if js else raw[:200]
        return {"code": -1, "message": f"萤石语音上传失败（{ezviz_code}）", "ezviz_code": ezviz_code, "ezviz_msg": ezviz_msg}

    data = js.get("data")
    file_url = None
    if isinstance(data, dict):
        file_url = data.get("url")
    elif isinstance(data, list) and data:
        file_url = data[0].get("url")
    if not file_url:
        return {"code": -1, "message": "萤石语音上传响应缺少 fileUrl", "ezviz_code": "200", "ezviz_msg": raw[:200]}

    # 4) 下发设备播报
    import urllib.parse as up

    send_body = up.urlencode({
        "accessToken": token,
        "deviceSerial": device_serial,
        "channelNo": "1",
        "fileUrl": file_url,
    }).encode("utf-8")
    status2, js2, raw2 = _http_json(
        "POST",
        f"{EZVIZ_OPEN_BASE_URL}/api/lapp/voice/send",
        {"Content-Type": "application/x-www-form-urlencoded"},
        send_body,
    )
    logger.info("[文字留言] voice/send 请求体: deviceSerial=%s channelNo=1 fileUrl=%s", device_serial, file_url)
    logger.info("[文字留言] voice/send HTTP=%s 响应=%s", status2, raw2)

    if js2 is not None and str(js2.get("code")) == "200":
        return {"code": 200, "message": "ok", "ezviz_code": "200", "ezviz_msg": "操作成功"}

    ezviz_code = str(js2.get("code", status2)) if js2 else str(status2)
    ezviz_msg = str(js2.get("msg", raw2[:200])) if js2 else raw2[:200]
    return {
        "code": -1,
        "message": f"萤石下发失败：{ezviz_code} {ezviz_msg}",
        "ezviz_code": ezviz_code,
        "ezviz_msg": ezviz_msg,
    }


class TtsPreviewRequest(BaseModel):
    text: str = ""
    voice: str = DEFAULT_VOICE


@router.post("/tts-preview")
async def tts_preview(req: TtsPreviewRequest):
    """手机试听（提醒计划表单）：文本 + 音色 → edge-tts 合成 mp3 字节流。

    仅返回音频给 App 播放，不触碰萤石、不下发设备；
    音色仅在白名单内生效，非法值回落默认音色。
    """
    text = (req.text or "").strip()
    if not text or len(text) > MAX_TEXT_LEN:
        return {"code": -1, "message": "text 不能为空或超长"}
    voice = req.voice if (req.voice or "") in VOICE_WHITELIST else DEFAULT_VOICE
    try:
        t0 = time.time()
        mp3 = await _synthesize_tts(text, voice)
        logger.info("[试听] TTS 合成完成: %d 字节, 音色=%s, 耗时 %.1fs", len(mp3), voice, time.time() - t0)
    except Exception as e:
        logger.exception("[试听] TTS 合成失败")
        return {"code": -1, "message": f"云端语音合成失败: {e}"}
    return Response(content=mp3, media_type="audio/mpeg")
