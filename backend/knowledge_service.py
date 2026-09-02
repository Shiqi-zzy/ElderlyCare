"""萤石智能体-知识库对接服务（后端全权负责，App 不直连萤石知识库接口）。

职责：
  1. 智能体绑定知识库（agentId ↔ knowledgeId）：启动自动执行一次 + /debug/bindKnowledge 手动触发；
  2. 老人档案同步知识库：档案业务库保存成功后，先删除该 device_serial 全部旧文档，
     再上传携带 metadata.device_serial 的 Markdown 档案（删旧传新，不追加）。

⚠️ 端点说明（2026-08-25）：
  公开渠道未能检索到萤石智能体知识库绑定/上传/删除的官方 API 文档
  （控制台目前仅支持 UI 手动绑定；与此前「萤石无外部 memory API」结论一致）。
  已实证的智能体网关接口只有「智能体分析」：
  https://aidialoggw.ys7.com/api/service/open/intelligent/agent/engine/agent/anaylsis
  （open.ys7.com/help/5006）。知识库接口按同一网关命名惯例做最佳猜测，
  端点集中在 config.KNOWLEDGE_API_PATHS，拿到官方文档后只改那一处。

降级铁律（风险约束清单）：
  - 本模块所有函数绝不抛异常，任何失败仅记日志并返回结果 dict；
  - 知识库 API 失败不阻断档案业务保存，不上抛给前端；
  - metadata.device_serial 严格必填：设备序列号为空时直接跳过同步（绝不上传无元数据文档）。
"""
import json
import logging
import urllib.error
import urllib.parse
import urllib.request

from config import (
    KNOWLEDGE_AGENT_ID,
    KNOWLEDGE_API_BASE,
    KNOWLEDGE_API_PATHS,
    KNOWLEDGE_ID,
)

logger = logging.getLogger("knowledge")

# 复用 ertc_service 的 accessToken 缓存（同一开放平台账号）
from ertc_service import _get_access_token  # noqa: E402

# 档案枚举值 → 中文标签（App 端 Gson 序列化枚举为 name 字符串，如 "EVERYDAY"）
_GENDER_LABELS = {"MALE": "男", "FEMALE": "女"}
_EXERCISE_LABELS = {
    "EVERYDAY": "每天锻炼", "WEEKLY_PLUS": "每周1次以上", "OCCASIONALLY": "偶尔锻炼", "NONE": "不锻炼",
}
_DIET_LABELS = {"BALANCED": "荤素均衡", "MEAT_HEAVY": "荤食为主", "VEGGIE_HEAVY": "素食为主"}
_SMOKING_LABELS = {"NEVER": "从不吸烟", "QUIT": "已戒烟", "STILL": "仍吸烟"}
_DRINKING_LABELS = {"NEVER": "从不饮酒", "OCCASIONALLY": "偶尔饮酒", "OFTEN": "经常饮酒", "EVERYDAY": "每天饮酒"}


def _post_knowledge(path_key: str, payload: dict) -> dict:
    """POST JSON 到知识库网关（Header accessToken）。返回萤石完整原始 JSON。

    兼容两种返回形态：service/* 的 meta.code、lapp/* 的顶层 code。
    任何异常（含 token 获取失败）都不抛出，返回带 code 的 dict。
    """
    path = KNOWLEDGE_API_PATHS.get(path_key)
    if not path:
        return {"meta": {"code": -1, "message": f"未配置端点: {path_key}"}, "data": None}
    token = _get_access_token()
    if not token:
        return {"meta": {"code": -1, "message": "获取 accessToken 失败"}, "data": None}

    url = f"{KNOWLEDGE_API_BASE}{path}"
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", "accessToken": token},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        logger.error("[知识库] %s HTTP %s: %s", path_key, e.code, raw)
        try:
            return json.loads(raw)
        except Exception:
            return {"meta": {"code": e.code, "message": raw}, "data": None}
    except Exception as e:
        logger.error("[知识库] %s 请求异常: %s", path_key, e)
        return {"meta": {"code": -1, "message": str(e)}, "data": None}


def _resp_ok(result: dict) -> bool:
    """兼容 meta.code / 顶层 code 两种成功判断（与 ertc_service._resp_ok 一致）。"""
    code = (result.get("meta") or {}).get("code")
    if code is None:
        code = result.get("code")
    return code == 200 or str(code) == "200"


def bind_agent_knowledge() -> dict:
    """任务1：智能体绑定知识库（启动自动执行一次 + 调试接口手动触发）。

    返回萤石完整原始响应（调用方打印）；绝不抛异常。
    """
    payload = {"agentId": KNOWLEDGE_AGENT_ID, "knowledgeId": KNOWLEDGE_ID}
    logger.info("[知识库] 开始绑定 agentId=%s knowledgeId=%s", KNOWLEDGE_AGENT_ID, KNOWLEDGE_ID)
    result = _post_knowledge("bind", payload)
    if _resp_ok(result):
        logger.info("[知识库] 绑定成功: %s", json.dumps(result, ensure_ascii=False))
    else:
        logger.error("[知识库] 绑定失败（已降级，不阻断服务）: %s", json.dumps(result, ensure_ascii=False))
    return result


def delete_device_docs(device_serial: str) -> dict:
    """删除该设备序列号对应的全部旧文档（按 metadata 过滤，防历史档案残留）。"""
    payload = {
        "knowledgeId": KNOWLEDGE_ID,
        "metadata": {"device_serial": device_serial},
    }
    logger.info("[知识库] 删除旧文档 device_serial=%s", device_serial)
    result = _post_knowledge("doc_delete", payload)
    if _resp_ok(result):
        logger.info("[知识库] 删除旧文档成功 device_serial=%s", device_serial)
    else:
        logger.error("[知识库] 删除旧文档失败（继续上传，已降级）: %s",
                     json.dumps(result, ensure_ascii=False))
    return result


def upload_profile_doc(device_serial: str, markdown: str) -> dict:
    """上传老人档案 Markdown 文档；metadata.device_serial 严格必填（调用方已保证非空）。"""
    payload = {
        "knowledgeId": KNOWLEDGE_ID,
        "docName": f"老人档案-{device_serial}",
        "content": markdown,
        "metadata": {"device_serial": device_serial},
    }
    logger.info("[知识库] 上传档案 device_serial=%s 长度=%d", device_serial, len(markdown))
    result = _post_knowledge("doc_upload", payload)
    if _resp_ok(result):
        logger.info("[知识库] 上传档案成功 device_serial=%s", device_serial)
    else:
        logger.error("[知识库] 上传档案失败（已降级）: %s",
                     json.dumps(result, ensure_ascii=False))
    return result


def build_profile_markdown(profile: dict) -> str:
    """任务2：档案 dict（App Gson 序列化的 ElderlyProfile）→ 简洁 Markdown。

    字段：姓名、年龄、既往病史、兴趣爱好、日常作息习惯；空字段跳过该行。
    枚举（Gson 序列化为 name）在此转中文标签，未知值原样展示。
    """
    def _s(key, default=""):
        v = profile.get(key)
        return str(v).strip() if v is not None else default

    def _list(key):
        v = profile.get(key)
        if isinstance(v, list):
            return [str(x).strip() for x in v if str(x).strip()]
        return []

    def _age_text(age: str) -> str:
        if not age:
            return ""
        return age if ("岁" in age) else f"{age}岁"

    lines = ["# 老人档案"]

    name = _s("name")
    gender = _GENDER_LABELS.get(_s("gender"), _s("gender"))
    lines.append(f"- 姓名：{name or '未填写'}{f'（{gender}）' if gender else ''}")

    age = _age_text(_s("age"))
    if age:
        lines.append(f"- 年龄：{age}")

    # 既往病史：慢性病为主，附带过敏史/精神情绪病史
    diseases = _list("chronicDiseases")
    allergy = _s("allergyHistory")
    mental = _s("mentalHealthHistory")
    history_parts = []
    if diseases:
        history_parts.append("、".join(diseases))
    if allergy:
        history_parts.append(f"过敏史：{allergy}")
    if mental:
        history_parts.append(f"精神情绪病史：{mental}")
    lines.append(f"- 既往病史：{'；'.join(history_parts) if history_parts else '无记录'}")

    # 兴趣爱好
    hobbies = _list("hobbies")
    lines.append(f"- 兴趣爱好：{'、'.join(hobbies) if hobbies else '未填写'}")

    # 日常作息习惯：锻炼 / 饮食 / 烟酒
    routine_parts = []
    exercise = _EXERCISE_LABELS.get(_s("exerciseFrequency"), _s("exerciseFrequency"))
    if exercise:
        routine_parts.append(exercise)
    exercise_types = _list("exerciseTypes")
    if exercise_types:
        routine_parts.append(f"运动项目：{'、'.join(exercise_types)}")
    diet = _DIET_LABELS.get(_s("dietType"), _s("dietType"))
    if diet:
        routine_parts.append(f"饮食：{diet}")
    diet_prefs = _list("dietPreferences")
    if diet_prefs:
        routine_parts.append(f"饮食偏好：{'、'.join(diet_prefs)}")
    smoking = _SMOKING_LABELS.get(_s("smokingStatus"), _s("smokingStatus"))
    if smoking:
        routine_parts.append(smoking)
    drinking = _DRINKING_LABELS.get(_s("drinkingFrequency"), _s("drinkingFrequency"))
    if drinking:
        routine_parts.append(drinking)
    lines.append(f"- 日常作息习惯：{'；'.join(routine_parts) if routine_parts else '未填写'}")

    return "\n".join(lines)


def sync_profile_to_knowledge(device_serial: str, profile: dict) -> dict:
    """任务2 完整业务：删旧 → 传新（先删后传，不追加）。

    device_serial 为空直接跳过并返回说明（metadata 绝对不能缺失，宁可不同步）；
    两步任一失败仅记日志，返回汇总 dict，绝不抛异常。
    """
    device_serial = (device_serial or "").strip()
    if not device_serial:
        logger.error("[知识库] device_serial 为空，跳过知识库同步（metadata 禁止缺失）")
        return {"code": -1, "message": "device_serial 为空，跳过同步"}

    delete_result = delete_device_docs(device_serial)
    markdown = build_profile_markdown(profile)
    upload_result = upload_profile_doc(device_serial, markdown)
    return {
        "code": 200 if _resp_ok(upload_result) else -1,
        "message": "知识库同步完成（delete+upload，失败不阻断档案保存）",
        "delete": delete_result,
        "upload": upload_result,
    }
