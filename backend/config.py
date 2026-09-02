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

# ═══════════════════════════════════════════════════════
# 智能体-知识库（家属档案知识库，按设备序列号隔离）
# ═══════════════════════════════════════════════════════
# 银龄-老人居家管理智能体
KNOWLEDGE_AGENT_ID = os.getenv("KNOWLEDGE_AGENT_ID", "ba9c5e697fbb4c0b9529")
# 家属档案知识库
KNOWLEDGE_ID = os.getenv("KNOWLEDGE_ID", "ad36b885c66e4c5289db7c9fb018f6da")

# 智能体网关域名（实证接口 open.ys7.com/help/5006 智能体分析即在该网关）
KNOWLEDGE_API_BASE = os.getenv("KNOWLEDGE_API_BASE", "https://aidialoggw.ys7.com")

# ⚠️ 知识库接口端点：公开渠道未检索到官方文档（控制台目前仅 UI 手动绑定），
# 以下按智能体网关命名惯例做最佳猜测；拿到官方文档后只改这里。
KNOWLEDGE_API_PATHS = {
    # 智能体绑定知识库（agentId + knowledgeId）
    "bind": "/api/service/open/intelligent/knowledge/bind",
    # 删除知识库文档（按 metadata 过滤，删除该设备全部旧档案）
    "doc_delete": "/api/service/open/intelligent/knowledge/document/delete",
    # 上传文档（Markdown 文本 + metadata 携带 device_serial）
    "doc_upload": "/api/service/open/intelligent/knowledge/document/upload",
}
