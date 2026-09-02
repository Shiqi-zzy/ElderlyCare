"""应用配置"""
import os

# 数据库路径
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(os.path.dirname(BASE_DIR), "data")
DB_PATH = os.path.join(DATA_DIR, "elderly_care.db")

# 萤石开放平台（设备 API + 云信令短信）
EZVIZ_APP_KEY = os.getenv("EZVIZ_APP_KEY", "3ced69472ba24336b5a2d595bf80e5c7")
EZVIZ_APP_SECRET = os.getenv("EZVIZ_APP_SECRET", "b5dac274bc29f7546af3933b81561b48")

# JWT 配置
SECRET_KEY = os.getenv("JWT_SECRET_KEY", "elderly-care-platform-secret-key-change-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24  # 24小时

# 告警等级常量
ALARM_LEVEL_LOW = "LOW"
ALARM_LEVEL_MEDIUM = "MEDIUM"
ALARM_LEVEL_HIGH = "HIGH"
ALARM_LEVEL_EMERGENCY = "EMERGENCY"

# 告警类型
ALARM_TYPE_FALL = "fall"
ALARM_TYPE_STILLNESS = "stillness"
ALARM_TYPE_SMOKE = "smoke"
ALARM_TYPE_GAS = "gas"
ALARM_TYPE_DOOR_OPEN = "door_open"
ALARM_TYPE_BED_ABSENCE = "bed_absence"
ALARM_TYPE_VITAL_SIGN = "vital_sign"

# 用户角色
ROLE_FAMILY = "family"
ROLE_COMMUNITY = "community"
ROLE_HOSPITAL = "hospital"
ROLE_ADMIN = "admin"

# 授权状态
AUTH_STATUS_ACTIVE = "active"
AUTH_STATUS_REVOKED = "revoked"
AUTH_STATUS_EXPIRED = "expired"
AUTH_STATUS_REJECTED = "rejected"

# 工单状态
ORDER_STATUS_PENDING = "pending"
ORDER_STATUS_ACCEPTED = "accepted"
ORDER_STATUS_IN_PROGRESS = "in_progress"
ORDER_STATUS_COMPLETED = "completed"
ORDER_STATUS_CANCELLED = "cancelled"

# 告警状态
ALARM_STATUS_ACTIVE = "active"
ALARM_STATUS_ACKNOWLEDGED = "acknowledged"
ALARM_STATUS_PROCESSING = "processing"
ALARM_STATUS_RESOLVED = "resolved"
ALARM_STATUS_ARCHIVED = "archived"

# 资质审核状态
REVIEW_STATUS_PENDING = "pending"
REVIEW_STATUS_AUTO_PASSED = "auto_passed"
REVIEW_STATUS_AUTO_FAILED = "auto_failed"
REVIEW_STATUS_APPROVED = "approved"
REVIEW_STATUS_REJECTED = "rejected"

# 设备类型
DEVICE_TYPE_CAMERA = "camera"
DEVICE_TYPE_SMOKE_SENSOR = "smoke_sensor"
DEVICE_TYPE_GAS_SENSOR = "gas_sensor"
DEVICE_TYPE_WEARABLE = "wearable"
DEVICE_TYPE_DOOR_SENSOR = "door_sensor"
DEVICE_TYPE_BED_SENSOR = "bed_sensor"

# 设备状态
DEVICE_STATUS_ONLINE = "online"
DEVICE_STATUS_OFFLINE = "offline"
DEVICE_STATUS_FAULT = "fault"
DEVICE_STATUS_MAINTENANCE = "maintenance"

# AI 防误报等级
AI_VERIFY_UNVERIFIED = 0
AI_VERIFY_LOW = 1
AI_VERIFY_MEDIUM = 2
AI_VERIFY_HIGH = 3

# 防误报阈值
FALL_DETECTION_MIN_CONFIDENCE = 0.65       # 跌倒最低置信度
STILLNESS_TIMEOUT_SECONDS = 300             # 静止超时(5分钟)
ANTI_FALSE_POSITIVE_MIN_FRAMES = 3          # 最少连续帧数
