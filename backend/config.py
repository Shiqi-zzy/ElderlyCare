"""ElderlyCare 云通话(ERTC) 后端配置"""
import os

# 萤石开放平台地址
EZVIZ_OPEN_BASE_URL = "https://open.ys7.com"

# 设备所属账号（与 ElderlyCare App 一致，设备 BK9267115 绑在此账号下）
EZVIZ_RTC_APP_KEY = os.getenv("EZVIZ_RTC_APP_KEY", "226d2b4143894068bea091bcab01b6b3")
EZVIZ_RTC_APP_SECRET = os.getenv("EZVIZ_RTC_APP_SECRET", "36a2e4d9b0a5e66ffd4188642aaed120")

# 云通话 RTC AppId（控制台「云通话-实时音视频」创建）
EZVIZ_RTC_APP_ID = os.getenv("EZVIZ_RTC_APP_ID", "1aafc61ecfba4b48b4eccdbe7849e4e8")

# 关联设备（后续可扩展多台）
EZVIZ_RTC_DEVICE_SERIAL = os.getenv("EZVIZ_RTC_DEVICE_SERIAL", "BK9267115")
