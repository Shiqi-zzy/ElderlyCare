"""健康预测引擎（Phase 4）—— 基于规则的风险评分"""
import json
from datetime import datetime, timedelta
from ..core.database import get_db
from ..core.security import now_iso


class HealthPredictor:
    """基于规则的老年人健康风险评估引擎"""

    @staticmethod
    def assess_fall_risk(elderly_id: str) -> dict:
        """
        综合评估跌倒风险 (0-100分)
        因子：年龄、护理等级、跌倒告警历史、活动量、设备在线率
        """
        db = get_db()

        # 基础信息
        elderly = db.execute(
            "SELECT * FROM elderly WHERE id=?", (elderly_id,)
        ).fetchone()
        if not elderly:
            return {"error": "老人不存在"}

        elderly = dict(elderly)
        score = 0
        factors = []

        # 1. 年龄因子 (0-30分)
        birth = elderly.get("birth_date", "")
        if birth:
            try:
                birth_year = int(birth[:4])
                age = datetime.now().year - birth_year
                if age >= 85:
                    score += 30
                    factors.append({"factor": "高龄(≥85岁)", "score": 30, "detail": f"当前{age}岁"})
                elif age >= 75:
                    score += 20
                    factors.append({"factor": "高龄(75-84岁)", "score": 20, "detail": f"当前{age}岁"})
                elif age >= 65:
                    score += 10
                    factors.append({"factor": "初老(65-74岁)", "score": 10, "detail": f"当前{age}岁"})
            except (ValueError, IndexError):
                score += 10
                factors.append({"factor": "年龄未知", "score": 10, "detail": "出生日期格式异常"})

        # 2. 护理等级因子 (0-25分)
        care_level = elderly.get("care_level", "自理")
        if care_level == "全护理":
            score += 25
            factors.append({"factor": "全护理", "score": 25, "detail": "重度失能，跌倒风险极高"})
        elif care_level == "半自理":
            score += 15
            factors.append({"factor": "半自理", "score": 15, "detail": "行动能力受限"})
        else:
            factors.append({"factor": "自理", "score": 0, "detail": "具备基本行动能力"})

        # 3. 跌倒告警历史 (0-25分，近90天)
        ninety_days_ago = (datetime.now() - timedelta(days=90)).strftime("%Y-%m-%d %H:%M:%S")
        fall_alarms = db.execute(
            """SELECT COUNT(*) as cnt FROM alarms
               WHERE elderly_id=? AND alarm_type='fall'
               AND created_at >= ?""",
            (elderly_id, ninety_days_ago)
        ).fetchone()
        fall_count = fall_alarms["cnt"] if fall_alarms else 0
        if fall_count >= 3:
            score += 25
            factors.append({"factor": "频繁跌倒", "score": 25, "detail": f"近90天跌倒{fall_count}次"})
        elif fall_count >= 1:
            score += 15
            factors.append({"factor": "跌倒史", "score": 15, "detail": f"近90天跌倒{fall_count}次"})
        else:
            factors.append({"factor": "无跌倒史", "score": 0, "detail": "近90天无跌倒记录"})

        # 4. 设备在线率 (0-10分)
        devices = db.execute(
            "SELECT status FROM devices WHERE elderly_id=?", (elderly_id,)
        ).fetchall()
        if devices:
            online_count = sum(1 for d in devices if d["status"] == "online")
            online_rate = online_count / len(devices)
            if online_rate < 0.5:
                score += 10
                factors.append({"factor": "设备离线率高", "score": 10, "detail": f"在线率{online_rate:.0%}"})
            elif online_rate < 0.8:
                score += 5
                factors.append({"factor": "部分设备离线", "score": 5, "detail": f"在线率{online_rate:.0%}"})
            else:
                factors.append({"factor": "设备正常在线", "score": 0, "detail": f"在线率{online_rate:.0%}"})
        else:
            factors.append({"factor": "无监控设备", "score": 5, "detail": "该老人未绑定任何监控设备"})
            score += 5

        # 5. 告警响应时效 (0-10分，平均响应时间越长分越高)
        avg_ack = db.execute(
            """SELECT AVG(
                   (julianday(acknowledged_at) - julianday(created_at)) * 24 * 60
               ) as avg_minutes
               FROM alarms
               WHERE elderly_id=? AND acknowledged_at IS NOT NULL
               AND created_at >= ?""",
            (elderly_id, ninety_days_ago)
        ).fetchone()
        if avg_ack and avg_ack["avg_minutes"]:
            avg_min = avg_ack["avg_minutes"]
            if avg_min > 30:
                score += 10
                factors.append({"factor": "告警响应慢", "score": 10, "detail": f"平均响应{avg_min:.0f}分钟"})
            elif avg_min > 10:
                score += 5
                factors.append({"factor": "告警响应偏慢", "score": 5, "detail": f"平均响应{avg_min:.0f}分钟"})
            else:
                factors.append({"factor": "告警响应及时", "score": 0, "detail": f"平均响应{avg_min:.0f}分钟"})

        score = min(score, 100)

        # 风险等级
        if score >= 70:
            level = "高风险"
            level_code = "high"
        elif score >= 40:
            level = "中风险"
            level_code = "medium"
        else:
            level = "低风险"
            level_code = "low"

        return {
            "elderly_id": elderly_id,
            "elderly_name": elderly.get("name", ""),
            "risk_score": score,
            "risk_level": level,
            "risk_level_code": level_code,
            "factors": factors,
            "assessed_at": now_iso()
        }

    @staticmethod
    def assess_health_trend(elderly_id: str, days: int = 30) -> dict:
        """基于健康档案记录频率和类型分析健康趋势"""
        db = get_db()

        start_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        records = db.execute(
            """SELECT record_type, record_date, content_json
               FROM health_records
               WHERE elderly_id=? AND record_date >= ?
               ORDER BY record_date DESC""",
            (elderly_id, start_date)
        ).fetchall()

        if not records:
            return {
                "elderly_id": elderly_id,
                "trend": "数据不足",
                "record_count": 0,
                "type_distribution": {},
                "recent_records": [],
                "days_analyzed": days
            }

        # 类型分布
        type_dist = {}
        for r in records:
            rt = r["record_type"]
            type_dist[rt] = type_dist.get(rt, 0) + 1

        # 趋势判断（基于记录频率变化）
        records_list = [dict(r) for r in records]
        mid = days // 2
        mid_date = (datetime.now() - timedelta(days=mid)).strftime("%Y-%m-%d")
        recent_half = [r for r in records_list if r["record_date"] >= mid_date]
        older_half = [r for r in records_list if r["record_date"] < mid_date]

        if len(recent_half) > len(older_half) * 1.5:
            trend = "就医频率上升 ⚠"
            trend_code = "worsening"
        elif len(recent_half) > len(older_half):
            trend = "就医频率略增"
            trend_code = "slight_worsening"
        elif len(recent_half) == 0:
            trend = "近期无记录"
            trend_code = "no_data"
        else:
            trend = "稳定"
            trend_code = "stable"

        return {
            "elderly_id": elderly_id,
            "trend": trend,
            "trend_code": trend_code,
            "record_count": len(records_list),
            "recent_count": len(recent_half),
            "older_count": len(older_half),
            "type_distribution": type_dist,
            "recent_records": records_list[:5],
            "days_analyzed": days,
            "assessed_at": now_iso()
        }

    @staticmethod
    def generate_health_report(elderly_id: str) -> dict:
        """生成结构化健康摘要报告"""
        db = get_db()
        elderly = db.execute("SELECT * FROM elderly WHERE id=?", (elderly_id,)).fetchone()
        if not elderly:
            return {"error": "老人不存在"}

        elderly = dict(elderly)

        # 跌倒风险
        fall_risk = HealthPredictor.assess_fall_risk(elderly_id)
        # 健康趋势
        health_trend = HealthPredictor.assess_health_trend(elderly_id, days=30)

        # 最近告警统计
        thirty_days_ago = (datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d")
        alarm_stats = db.execute(
            """SELECT alarm_level, COUNT(*) as cnt FROM alarms
               WHERE elderly_id=? AND created_at >= ?
               GROUP BY alarm_level""",
            (elderly_id, thirty_days_ago)
        ).fetchall()
        alarm_summary = {row["alarm_level"]: row["cnt"] for row in alarm_stats}

        # 设备状态
        devices = db.execute(
            "SELECT device_type, status FROM devices WHERE elderly_id=?", (elderly_id,)
        ).fetchall()
        device_summary = {"total": len(devices), "online": 0, "offline": 0, "by_type": {}}
        for d in devices:
            if d["status"] == "online":
                device_summary["online"] += 1
            else:
                device_summary["offline"] += 1
            dt = d["device_type"]
            if dt not in device_summary["by_type"]:
                device_summary["by_type"][dt] = 0
            device_summary["by_type"][dt] += 1

        return {
            "elderly_id": elderly_id,
            "elderly_name": elderly.get("name", ""),
            "care_level": elderly.get("care_level", ""),
            "fall_risk": fall_risk,
            "health_trend": health_trend,
            "alarm_summary_30d": alarm_summary,
            "device_summary": device_summary,
            "generated_at": now_iso()
        }
