"""统计服务（Phase 4）—— 区域统计、老人趋势、平台概览"""
from datetime import datetime, timedelta
from ..core.database import get_db
from ..core.security import now_iso


class StatisticsService:
    """大数据统计与分析"""

    @staticmethod
    def platform_overview() -> dict:
        """全平台增强统计概览"""
        db = get_db()

        # 基础统计
        total_users = db.execute("SELECT COUNT(*) FROM users WHERE is_active=1").fetchone()[0]
        total_elderly = db.execute("SELECT COUNT(*) FROM elderly WHERE is_active=1").fetchone()[0]
        total_devices = db.execute("SELECT COUNT(*) FROM devices").fetchone()[0]
        online_devices = db.execute("SELECT COUNT(*) FROM devices WHERE status='online'").fetchone()[0]
        active_alarms = db.execute("SELECT COUNT(*) FROM alarms WHERE status IN ('active','acknowledged','processing')").fetchone()[0]
        pending_orders = db.execute("SELECT COUNT(*) FROM work_orders WHERE status IN ('pending','accepted','in_progress')").fetchone()[0]
        total_health_records = db.execute("SELECT COUNT(*) FROM health_records").fetchone()[0]

        # 角色分布
        role_dist = {}
        for row in db.execute("SELECT role, COUNT(*) as cnt FROM users WHERE is_active=1 GROUP BY role").fetchall():
            role_dist[row["role"]] = row["cnt"]

        # 设备类型分布
        device_type_dist = {}
        for row in db.execute("SELECT device_type, COUNT(*) as cnt FROM devices GROUP BY device_type").fetchall():
            device_type_dist[row["device_type"]] = row["cnt"]

        # 近30天告警趋势（按天）
        thirty_days_ago = (datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d")
        alarm_trend = {}
        for row in db.execute(
            "SELECT DATE(created_at) as day, COUNT(*) as cnt FROM alarms WHERE created_at >= ? GROUP BY day ORDER BY day",
            (thirty_days_ago,)
        ).fetchall():
            alarm_trend[row["day"]] = row["cnt"]

        # 护理等级分布
        care_dist = {}
        for row in db.execute("SELECT care_level, COUNT(*) as cnt FROM elderly WHERE is_active=1 GROUP BY care_level").fetchall():
            care_dist[row["care_level"]] = row["cnt"]

        # 活跃授权数
        active_auths = db.execute("SELECT COUNT(*) FROM authorizations WHERE status='active'").fetchone()[0]

        return {
            "total_users": total_users,
            "total_elderly": total_elderly,
            "total_devices": total_devices,
            "online_devices": online_devices,
            "device_online_rate": round(online_devices / total_devices * 100, 1) if total_devices > 0 else 0,
            "active_alarms": active_alarms,
            "pending_work_orders": pending_orders,
            "total_health_records": total_health_records,
            "active_authorizations": active_auths,
            "role_distribution": role_dist,
            "device_type_distribution": device_type_dist,
            "care_level_distribution": care_dist,
            "alarm_trend_30d": alarm_trend,
            "generated_at": now_iso()
        }

    @staticmethod
    def regional_stats(institution_id: str = None) -> dict:
        """机构管辖老人健康统计汇总"""
        db = get_db()

        if institution_id:
            # 特定机构
            elderly_ids = [
                row["id"] for row in db.execute(
                    """SELECT DISTINCT e.id FROM elderly e
                       JOIN authorizations a ON e.id = a.elderly_id
                       WHERE a.grantee_institution_id = ? AND a.status = 'active'""",
                    (institution_id,)
                ).fetchall()
            ]
            institution = db.execute("SELECT * FROM institutions WHERE id=?", (institution_id,)).fetchone()
            inst_name = dict(institution).get("name", "") if institution else ""
        else:
            # 全平台
            elderly_ids = [row["id"] for row in db.execute("SELECT id FROM elderly WHERE is_active=1").fetchall()]
            inst_name = "全平台"

        if not elderly_ids:
            return {
                "institution_name": inst_name,
                "elderly_count": 0,
                "message": "该机构暂无绑定老人"
            }

        total = len(elderly_ids)

        # 护理等级分布
        care_dist = {"自理": 0, "半自理": 0, "全护理": 0}
        for eid in elderly_ids:
            row = db.execute("SELECT care_level FROM elderly WHERE id=?", (eid,)).fetchone()
            if row and row["care_level"] in care_dist:
                care_dist[row["care_level"]] += 1

        # 告警统计（近30天）
        thirty_days_ago = (datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d")
        alarm_total = 0
        emergency_count = 0
        for eid in elderly_ids:
            cnt = db.execute(
                "SELECT COUNT(*) FROM alarms WHERE elderly_id=? AND created_at >= ?",
                (eid, thirty_days_ago)
            ).fetchone()[0]
            alarm_total += cnt
            em_cnt = db.execute(
                "SELECT COUNT(*) FROM alarms WHERE elderly_id=? AND created_at >= ? AND alarm_level='EMERGENCY'",
                (eid, thirty_days_ago)
            ).fetchone()[0]
            emergency_count += em_cnt

        # 设备统计
        device_total = 0
        device_online = 0
        for eid in elderly_ids:
            devs = db.execute("SELECT status FROM devices WHERE elderly_id=?", (eid,)).fetchall()
            device_total += len(devs)
            device_online += sum(1 for d in devs if d["status"] == "online")

        # 健康档案统计
        hr_total = 0
        for eid in elderly_ids:
            cnt = db.execute("SELECT COUNT(*) FROM health_records WHERE elderly_id=?", (eid,)).fetchone()[0]
            hr_total += cnt

        return {
            "institution_name": inst_name,
            "institution_id": institution_id,
            "elderly_count": total,
            "care_level_distribution": care_dist,
            "alarms_30d": alarm_total,
            "emergency_alarms_30d": emergency_count,
            "avg_alarms_per_elderly": round(alarm_total / total, 1) if total > 0 else 0,
            "total_devices": device_total,
            "online_devices": device_online,
            "device_online_rate": round(device_online / device_total * 100, 1) if device_total > 0 else 0,
            "avg_devices_per_elderly": round(device_total / total, 1) if total > 0 else 0,
            "total_health_records": hr_total,
            "generated_at": now_iso()
        }

    @staticmethod
    def elderly_trends(elderly_id: str, metric: str = "alarms", days: int = 30) -> dict:
        """单一老人指标趋势"""
        db = get_db()

        elderly = db.execute("SELECT name FROM elderly WHERE id=?", (elderly_id,)).fetchone()
        if not elderly:
            return {"error": "老人不存在"}

        start_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        data_points = []

        if metric == "alarms":
            rows = db.execute(
                "SELECT DATE(created_at) as day, COUNT(*) as cnt FROM alarms WHERE elderly_id=? AND created_at >= ? GROUP BY day ORDER BY day",
                (elderly_id, start_date)
            ).fetchall()
            for r in rows:
                data_points.append({"date": r["day"], "value": r["cnt"]})

        elif metric == "health_records":
            rows = db.execute(
                "SELECT DATE(record_date) as day, COUNT(*) as cnt FROM health_records WHERE elderly_id=? AND record_date >= ? GROUP BY day ORDER BY day",
                (elderly_id, start_date)
            ).fetchall()
            for r in rows:
                data_points.append({"date": r["day"], "value": r["cnt"]})

        elif metric == "device_status":
            rows = db.execute(
                "SELECT DATE(created_at) as day, status FROM devices WHERE elderly_id=?",
                (elderly_id,)
            ).fetchall()
            for r in rows:
                data_points.append({"date": r["day"] if r["day"] else "", "status": r["status"]})

        return {
            "elderly_id": elderly_id,
            "elderly_name": elderly["name"],
            "metric": metric,
            "days": days,
            "data_points": data_points,
            "total_value": sum(p.get("value", 0) for p in data_points),
            "generated_at": now_iso()
        }

    @staticmethod
    def alarm_trends(days: int = 30) -> dict:
        """告警趋势分析（全平台）"""
        db = get_db()
        start_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")

        # 按天趋势
        daily_trend = {}
        rows = db.execute(
            "SELECT DATE(created_at) as day, alarm_level, COUNT(*) as cnt FROM alarms WHERE created_at >= ? GROUP BY day, alarm_level ORDER BY day",
            (start_date,)
        ).fetchall()
        for r in rows:
            day = r["day"]
            if day not in daily_trend:
                daily_trend[day] = {"LOW": 0, "MEDIUM": 0, "HIGH": 0, "EMERGENCY": 0}
            daily_trend[day][r["alarm_level"]] = r["cnt"]

        # 告警类型分布
        type_dist = {}
        for row in db.execute(
            "SELECT alarm_type, COUNT(*) as cnt FROM alarms WHERE created_at >= ? GROUP BY alarm_type",
            (start_date,)
        ).fetchall():
            type_dist[row["alarm_type"]] = row["cnt"]

        # Top 告警老人
        top_elderly = []
        for row in db.execute(
            """SELECT e.name, a.elderly_id, COUNT(*) as cnt
               FROM alarms a JOIN elderly e ON a.elderly_id = e.id
               WHERE a.created_at >= ?
               GROUP BY a.elderly_id ORDER BY cnt DESC LIMIT 10""",
            (start_date,)
        ).fetchall():
            top_elderly.append({"name": row["name"], "elderly_id": row["elderly_id"], "count": row["cnt"]})

        return {
            "days": days,
            "total_alarms": sum(sum(v.values()) for v in daily_trend.values()),
            "daily_trend": dict(sorted(daily_trend.items())),
            "type_distribution": type_dist,
            "top_elderly_by_alarms": top_elderly,
            "generated_at": now_iso()
        }
