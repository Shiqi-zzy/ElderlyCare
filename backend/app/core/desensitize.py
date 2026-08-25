"""数据脱敏工具：按角色分层脱敏展示"""
import re
from .config import ROLE_FAMILY, ROLE_COMMUNITY, ROLE_HOSPITAL, ROLE_ADMIN


def desensitize_phone(phone: str, role: str) -> str:
    """手机号脱敏：家属全量，其他角色 138****5678"""
    if role == ROLE_FAMILY:
        return phone
    if not phone or len(phone) < 7:
        return phone or ""
    return phone[:3] + "****" + phone[-4:]


def desensitize_id_card(id_card: str, role: str) -> str:
    """身份证脱敏：家属全量，其他角色 3201**********1234"""
    if role == ROLE_FAMILY:
        return id_card
    if not id_card or len(id_card) < 8:
        return id_card or ""
    return id_card[:4] + "**********" + id_card[-4:]


def desensitize_address(address: str, role: str) -> str:
    """地址脱敏：家属全量，其他角色仅显示到区级"""
    if role == ROLE_FAMILY:
        return address
    if not address:
        return address or ""
    # 对于社区/医院：XX市XX区****
    parts = re.split(r"[市区]", address, maxsplit=2)
    if len(parts) >= 2:
        return parts[0] + "市" + parts[1] + "区****"
    return address[:6] + "****" if len(address) > 6 else address


def desensitize_name(name: str, role: str) -> str:
    """姓名脱敏：家属全量，其他角色 张*三"""
    if role == ROLE_FAMILY:
        return name
    if not name:
        return name or ""
    if len(name) == 2:
        return name[0] + "*"
    elif len(name) >= 3:
        return name[0] + "*" + name[-1]
    return name


def desensitize_elderly_record(row: dict, role: str) -> dict:
    """
    对老人信息记录按角色进行完整脱敏。

    Args:
        row: 数据库查询结果行（dict 或 sqlite3.Row）
        role: 查看者的角色

    Returns:
        脱敏后的 dict
    """
    data = dict(row)
    # SQLite int → Python bool for boolean fields
    if "is_active" in data and not isinstance(data["is_active"], bool):
        data["is_active"] = bool(data["is_active"])
    if "privacy_paused" in data and not isinstance(data["privacy_paused"], bool):
        data["privacy_paused"] = bool(data["privacy_paused"])
    # SQLite NULL → empty string for optional text fields
    for k in list(data.keys()):
        if data[k] is None:
            data[k] = ""
    if role == ROLE_FAMILY:
        return data  # 家属全量

    # 社区/医院/管理员：脱敏处理
    data["id_card"] = desensitize_id_card(data.get("id_card", ""), role)
    data["phone"] = desensitize_phone(data.get("phone", ""), role)
    data["address"] = desensitize_address(data.get("address", ""), role)
    data["name"] = desensitize_name(data.get("name", ""), role)
    data["emergency_contact"] = desensitize_phone(data.get("emergency_contact", ""), role)

    # 医院额外处理：不展示安防相关字段
    if role == ROLE_HOSPITAL:
        data.pop("privacy_paused", None)

    return data


def desensitize_alarm_record(alarm: dict, role: str) -> dict:
    """对告警记录按角色脱敏"""
    data = dict(alarm)
    if role == ROLE_FAMILY:
        return data
    # 社区/医院：隐藏详细原始数据引用
    if role in (ROLE_COMMUNITY, ROLE_HOSPITAL):
        data.pop("raw_data_json", None)
        if role == ROLE_HOSPITAL:
            # 医院不展示安防视频截图
            data["snapshot_url"] = None
            data["video_clip_url"] = None
    return data


def desensitize_health_record(record: dict, role: str) -> dict:
    """对健康档案按角色脱敏"""
    data = dict(record)
    if role == ROLE_FAMILY:
        return data
    # 社区：不应看到健康档案（应该在前置权限层拦截）
    if role == ROLE_COMMUNITY:
        return {}
    # 医院：完整医疗数据
    return data
