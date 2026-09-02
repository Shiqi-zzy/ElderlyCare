"""行为分析引擎（Phase 4）—— 睡眠规律、活动量、异常检测"""
from datetime import datetime, timedelta
from ..core.database import get_db
from ..core.security import now_iso


class BehaviorAnalyzer:
    """基于设备数据的老年人行为模式分析"""

    @staticmethod
    def analyze_sleep_pattern(elderly_id: str, days: int = 7) -> dict:
        """
        从 bed_sensor 设备相关告警推断睡眠规律
        基于 stillness/absence 告警推断夜间离床和睡眠质量
        """
        db = get_db()
        start_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")

        # 查询夜间告警（20:00-06:00）
        night_alarms = db.execute(
            """SELECT alarm_type, created_at, title FROM alarms
               WHERE elderly_id=? AND created_at >= ?
               AND alarm_type IN ('stillness', 'absence', 'fall')
               ORDER BY created_at DESC""",
            (elderly_id, start_date)
        ).fetchall()

        alarms_list = [dict(r) for r in night_alarms]

        # 按天统计夜间告警
        daily_counts = {}
        hour_distribution = {}
        for alarm in alarms_list:
            try:
                created = alarm["created_at"]
                day = created[:10]
                hour = int(created[11:13]) if len(created) >= 13 else 0
                # 只统计夜间 (20:00-06:00)
                if hour >= 20 or hour < 6:
                    daily_counts[day] = daily_counts.get(day, 0) + 1
                    hour_distribution[hour] = hour_distribution.get(hour, 0) + 1
            except (ValueError, IndexError):
                continue

        if not daily_counts:
            return {
                "elderly_id": elderly_id,
                "pattern": "数据不足",
                "pattern_code": "insufficient",
                "avg_nightly_alarms": 0,
                "peak_hour": None,
                "sleep_quality": "unknown",
                "days_analyzed": days,
                "detail": "近7天无夜间异常告警（可能是睡眠良好或设备数据缺失）"
            }

        avg_nightly = sum(daily_counts.values()) / len(daily_counts)
        peak_hour = max(hour_distribution, key=hour_distribution.get) if hour_distribution else None

        # 睡眠质量评估
        if avg_nightly <= 0.5:
            sleep_quality = "良好"
            sleep_code = "good"
        elif avg_nightly <= 1.5:
            sleep_quality = "一般"
            sleep_code = "fair"
        elif avg_nightly <= 3:
            sleep_quality = "较差"
            sleep_code = "poor"
        else:
            sleep_quality = "很差"
            sleep_code = "very_poor"

        # 规律性判断
        if len(daily_counts) >= 5:
            values = list(daily_counts.values())
            if max(values) - min(values) <= 1:
                pattern = "规律"
                pattern_code = "regular"
            else:
                pattern = "不规律"
                pattern_code = "irregular"
        else:
            pattern = "样本不足"
            pattern_code = "insufficient"

        return {
            "elderly_id": elderly_id,
            "pattern": pattern,
            "pattern_code": pattern_code,
            "avg_nightly_alarms": round(avg_nightly, 1),
            "peak_hour": peak_hour,
            "peak_hour_label": f"{peak_hour}:00" if peak_hour is not None else None,
            "sleep_quality": sleep_quality,
            "sleep_quality_code": sleep_code,
            "daily_counts": daily_counts,
            "days_analyzed": days,
            "total_night_alarms": len(alarms_list),
            "assessed_at": now_iso()
        }

    @staticmethod
    def analyze_activity_level(elderly_id: str, days: int = 7) -> dict:
        """
        从设备告警频率推断活动量
        告警越多 → 设备触发越频繁 → 活动量越高（或异常越多）
        """
        db = get_db()
        start_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")

        # 所有设备告警按天统计
        alarms = db.execute(
            """SELECT created_at, alarm_type, alarm_level FROM alarms
               WHERE elderly_id=? AND created_at >= ?
               ORDER BY created_at""",
            (elderly_id, start_date)
        ).fetchall()

        alarms_list = [dict(r) for r in alarms]

        daily_total = {}
        daily_by_type = {}
        for a in alarms_list:
            try:
                day = a["created_at"][:10]
                daily_total[day] = daily_total.get(day, 0) + 1
                at = a["alarm_type"]
                if day not in daily_by_type:
                    daily_by_type[day] = {}
                daily_by_type[day][at] = daily_by_type[day].get(at, 0) + 1
            except (ValueError, IndexError):
                continue

        if not daily_total:
            return {
                "elderly_id": elderly_id,
                "activity_level": "极低",
                "level_code": "very_low",
                "avg_daily_alarms": 0,
                "description": "近7天无任何告警记录，设备可能离线或老人活动极低",
                "days_analyzed": days
            }

        avg_daily = sum(daily_total.values()) / len(daily_total)

        if avg_daily >= 5:
            level = "高"
            level_code = "high"
        elif avg_daily >= 2:
            level = "中等"
            level_code = "medium"
        elif avg_daily >= 0.5:
            level = "低"
            level_code = "low"
        else:
            level = "极低"
            level_code = "very_low"

        # 趋势（后3天 vs 前4天）
        sorted_days = sorted(daily_total.keys())
        recent = {k: v for k, v in daily_total.items() if k >= sorted_days[-3]} if len(sorted_days) >= 3 else daily_total
        earlier = {k: v for k, v in daily_total.items() if k not in recent}
        recent_avg = sum(recent.values()) / len(recent) if recent else 0
        earlier_avg = sum(earlier.values()) / len(earlier) if earlier else recent_avg

        if recent_avg > earlier_avg * 1.5:
            trend = "上升"
            trend_code = "up"
        elif recent_avg < earlier_avg * 0.5:
            trend = "下降"
            trend_code = "down"
        else:
            trend = "平稳"
            trend_code = "stable"

        return {
            "elderly_id": elderly_id,
            "activity_level": level,
            "level_code": level_code,
            "avg_daily_alarms": round(avg_daily, 1),
            "recent_avg": round(recent_avg, 1),
            "earlier_avg": round(earlier_avg, 1),
            "trend": trend,
            "trend_code": trend_code,
            "total_alarms": len(alarms_list),
            "daily_counts": daily_total,
            "days_analyzed": days,
            "assessed_at": now_iso()
        }

    @staticmethod
    def detect_anomaly_pattern(elderly_id: str) -> dict:
        """
        与近7天基线对比，检测异常模式
        如果今天的告警数量超过基线均值 + 2标准差，标记为异常日
        """
        db = get_db()

        # 近7天基线
        seven_days_ago = (datetime.now() - timedelta(days=7)).strftime("%Y-%m-%d")
        baseline = db.execute(
            """SELECT DATE(created_at) as day, COUNT(*) as cnt
               FROM alarms
               WHERE elderly_id=? AND created_at >= ?
               GROUP BY day""",
            (elderly_id, seven_days_ago)
        ).fetchall()

        if not baseline:
            return {
                "elderly_id": elderly_id,
                "anomaly_detected": False,
                "anomaly_days": [],
                "baseline_mean": 0,
                "message": "基线数据不足（近7天无告警）"
            }

        counts = [row["cnt"] for row in baseline]
        mean = sum(counts) / len(counts)
        variance = sum((c - mean) ** 2 for c in counts) / len(counts) if len(counts) > 1 else 0
        std_dev = variance ** 0.5
        threshold = mean + 2 * std_dev if std_dev > 0 else mean + 2

        anomaly_days = []
        for row in baseline:
            if row["cnt"] > threshold:
                anomaly_days.append({
                    "date": row["day"],
                    "count": row["cnt"],
                    "baseline_mean": round(mean, 1),
                    "threshold": round(threshold, 1),
                    "deviation": round(row["cnt"] - mean, 1)
                })

        # 同时检查今天是否有异常
        today = datetime.now().strftime("%Y-%m-%d")
        today_count = db.execute(
            "SELECT COUNT(*) FROM alarms WHERE elderly_id=? AND DATE(created_at)=?",
            (elderly_id, today)
        ).fetchone()[0]

        today_anomaly = today_count > threshold if threshold > 0 else False

        return {
            "elderly_id": elderly_id,
            "anomaly_detected": len(anomaly_days) > 0,
            "today_anomaly": today_anomaly,
            "today_count": today_count,
            "anomaly_days": anomaly_days,
            "baseline_mean": round(mean, 1),
            "baseline_std_dev": round(std_dev, 1),
            "threshold": round(threshold, 1),
            "assessed_at": now_iso()
        }
