"""萤石云信令短信服务（验证码发送）

对接 EZVIZ open.xy.ys7.com 云信令平台，发送手机验证码。

API 格式参考: https://open.ys7.com/help/5129
"""

import hashlib
import time
import json
import urllib.request
import urllib.error
from ..core.config import EZVIZ_APP_KEY, EZVIZ_APP_SECRET

# 云信令平台地址
CLOUD_SIGNAL_URL = "https://open.xy.ys7.com/api/msg/send"

# 短信签名和模板（需在萤石控制台预先配置）
SMS_SIGN_NAME = "智慧养老平台"
SMS_TEMPLATE_ID = "SMS_0001"  # 验证码模板ID


def _make_sign(key: str, secret: str, timestamp: int) -> str:
    """生成萤石云信令请求签名: MD5(secret + key + str(time) + ver)"""
    raw = f"{secret}{key}{timestamp}1.0"
    return hashlib.md5(raw.encode()).hexdigest()


def send_sms_code(phone: str, code: str) -> bool:
    """通过萤石云信令发送短信验证码。

    Args:
        phone: 手机号
        code: 验证码

    Returns:
        True 发送成功, False 发送失败
    """
    if not EZVIZ_APP_KEY or not EZVIZ_APP_SECRET:
        print(f"[SMS] 萤石 AppKey/Secret 未配置，无法发送短信。验证码: {code}")
        return False

    ts = int(time.time())
    body = {
        "system": {
            "sign": _make_sign(EZVIZ_APP_KEY, EZVIZ_APP_SECRET, ts),
            "time": ts,
            "ver": "1.0",
            "key": EZVIZ_APP_KEY
        },
        "method": "msg/send",
        "params": {
            "phone": phone,
            "signName": SMS_SIGN_NAME,
            "templateId": SMS_TEMPLATE_ID,
            "templateParam": json.dumps({"code": code})
        }
    }

    try:
        data = json.dumps(body).encode("utf-8")
        req = urllib.request.Request(
            CLOUD_SIGNAL_URL,
            data=data,
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            # 萤石返回格式: {"result": {"code": "200", "msg": "..."}}
            r = result.get("result", result)
            code_val = r.get("code", "")
            if code_val == "200" or code_val == 200:
                print(f"[SMS] 验证码已发送至 {phone}")
                return True
            else:
                print(f"[SMS] 发送失败: {r.get('msg', result)}")
                return False
    except urllib.error.HTTPError as e:
        body_text = e.read().decode("utf-8", errors="replace")
        print(f"[SMS] HTTP {e.code}: {body_text}")
        return False
    except Exception as e:
        print(f"[SMS] 发送异常: {e}")
        return False
