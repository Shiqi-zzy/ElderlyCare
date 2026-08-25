"""告警事件/抓拍记录入库（sqlite3 标准库，零第三方依赖）。

萤石告警 webhook 到达后：先实时推送给已订阅客户端（内存路由），
再异步入库（审计/排查用）；以萤石 header.messageId 幂等，
萤石重推不产生重复行。

alarm_events 同时承载两类抓拍记录（专供 App「全部抓拍」页）：
  - capture_type=auto   ：设备人形侦测自动抓拍（webhook 写入）
  - capture_type=manual ：手动云端抓拍（/api/ezviz/capture 写入）
device_auth 存设备验证码（App 绑定成功后上报），供后端解密加密图片。
health_profiles 存家属健康档案 JSON 云同步副本（App 家属端上传 / 机构端拉取）。
"""
import os
import sqlite3
import threading
import time

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "alarm_events.db")

# sqlite3 连接跨线程使用不安全，写入统一串行化（量级低，够用）
_lock = threading.Lock()


def _conn():
    conn = sqlite3.connect(DB_PATH, timeout=5)
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db():
    with _lock, _conn() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS alarm_events (
                message_id   TEXT PRIMARY KEY,
                msg_type     TEXT NOT NULL,
                device_id    TEXT NOT NULL DEFAULT '',
                message_time INTEGER NOT NULL DEFAULT 0,
                alarm_id     TEXT NOT NULL DEFAULT '',
                alarm_name   TEXT NOT NULL DEFAULT '',
                body         TEXT NOT NULL DEFAULT '',
                created_at   INTEGER NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS device_auth (
                device_serial TEXT PRIMARY KEY,
                validate_code TEXT NOT NULL DEFAULT '',
                updated_at    INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS health_profiles (
                user_id      TEXT PRIMARY KEY,
                device_sn    TEXT NOT NULL DEFAULT '',
                profile_json TEXT NOT NULL,
                version      INTEGER NOT NULL DEFAULT 1,
                created_at   INTEGER NOT NULL,
                updated_at   INTEGER NOT NULL
            )
            """
        )
    migrate_db()


# alarm_events 扩展列（抓拍记录；全部带默认值满足 SQLite ALTER ADD COLUMN 约束）
_EXTRA_COLUMNS = (
    ("pic_url", "TEXT NOT NULL DEFAULT ''"),          # 萤石原始图片地址
    ("local_save_path", "TEXT NOT NULL DEFAULT ''"),  # 后端落盘相对路径（/media/captures/...）
    ("capture_type", "TEXT NOT NULL DEFAULT 'auto'"),  # manual | auto（历史行全是 webhook 告警）
    ("device_serial", "TEXT NOT NULL DEFAULT ''"),
    ("event_time", "INTEGER NOT NULL DEFAULT 0"),     # 抓拍时间，毫秒时间戳
    ("is_read", "INTEGER NOT NULL DEFAULT 0"),
)


def migrate_db():
    """老库补列（幂等，可重复执行）。"""
    with _lock, _conn() as conn:
        existing = {row[1] for row in conn.execute("PRAGMA table_info(alarm_events)")}
        for name, decl in _EXTRA_COLUMNS:
            if name not in existing:
                conn.execute(f"ALTER TABLE alarm_events ADD COLUMN {name} {decl}")


def save_alarm(message_id, msg_type, device_id, message_time, alarm_id, alarm_name, body):
    """同步写库（调用方用 asyncio.to_thread 放线程执行）；messageId 幂等。"""
    with _lock, _conn() as conn:
        conn.execute(
            "INSERT OR IGNORE INTO alarm_events"
            " (message_id, msg_type, device_id, message_time, alarm_id, alarm_name, body, created_at)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (message_id, msg_type, device_id, message_time, alarm_id, alarm_name, body, int(time.time())),
        )


def save_capture(message_id, capture_type, device_serial, event_time,
                 alarm_id, alarm_name, pic_url, local_save_path, body):
    """写入抓拍记录（manual/auto 共用）；message_id 幂等。"""
    with _lock, _conn() as conn:
        conn.execute(
            "INSERT OR IGNORE INTO alarm_events"
            " (message_id, msg_type, device_id, message_time, alarm_id, alarm_name, body, created_at,"
            "  pic_url, local_save_path, capture_type, device_serial, event_time, is_read)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            (message_id, "alarm", device_serial, event_time, alarm_id, alarm_name,
             body, int(time.time()), pic_url, local_save_path, capture_type, device_serial, event_time),
        )


def update_capture_pic(message_id, local_save_path):
    """图片下载落盘完成后回填本地路径。"""
    with _lock, _conn() as conn:
        conn.execute(
            "UPDATE alarm_events SET local_save_path = ? WHERE message_id = ?",
            (local_save_path, message_id),
        )


def list_captures(device_serial, limit=100, offset=0):
    """抓拍列表（新→旧）。"""
    with _lock, _conn() as conn:
        conn.row_factory = sqlite3.Row
        rows = conn.execute(
            "SELECT * FROM alarm_events WHERE device_serial = ?"
            " ORDER BY event_time DESC, created_at DESC LIMIT ? OFFSET ?",
            (device_serial, limit, offset),
        ).fetchall()
        return [dict(r) for r in rows]


def count_captures(device_serial):
    with _lock, _conn() as conn:
        row = conn.execute(
            "SELECT COUNT(*) FROM alarm_events WHERE device_serial = ?",
            (device_serial,),
        ).fetchone()
        return int(row[0]) if row else 0


def mark_capture_read(message_id, device_serial):
    """标记单条已读（限定设备防串读）；返回是否命中。"""
    with _lock, _conn() as conn:
        cur = conn.execute(
            "UPDATE alarm_events SET is_read = 1 WHERE message_id = ? AND device_serial = ?",
            (message_id, device_serial),
        )
        return cur.rowcount > 0


def unread_capture_count(device_serial):
    with _lock, _conn() as conn:
        row = conn.execute(
            "SELECT COUNT(*) FROM alarm_events WHERE device_serial = ? AND is_read = 0",
            (device_serial,),
        ).fetchone()
        return int(row[0]) if row else 0


def upsert_device_auth(device_serial, validate_code):
    """设备验证码 upsert（App 绑定成功后上报；更换设备重绑时更新）。"""
    with _lock, _conn() as conn:
        conn.execute(
            "INSERT INTO device_auth (device_serial, validate_code, updated_at) VALUES (?, ?, ?)"
            " ON CONFLICT(device_serial) DO UPDATE SET validate_code = excluded.validate_code,"
            " updated_at = excluded.updated_at",
            (device_serial, validate_code, int(time.time())),
        )


def get_validate_code(device_serial):
    """按设备序列号取验证码（图片解密用）；无记录返回 None。"""
    with _lock, _conn() as conn:
        row = conn.execute(
            "SELECT validate_code FROM device_auth WHERE device_serial = ?",
            (device_serial,),
        ).fetchone()
        return row[0] if row and row[0] else None


def upsert_health_profile(user_id, device_sn, profile_json):
    """健康档案云同步 upsert（version 自增）；返回新 version。"""
    with _lock, _conn() as conn:
        row = conn.execute(
            "SELECT version FROM health_profiles WHERE user_id = ?", (user_id,)
        ).fetchone()
        version = (int(row[0]) + 1) if row and row[0] else 1
        now = int(time.time())
        conn.execute(
            "INSERT INTO health_profiles (user_id, device_sn, profile_json, version, created_at, updated_at)"
            " VALUES (?, ?, ?, ?, ?, ?)"
            " ON CONFLICT(user_id) DO UPDATE SET device_sn = excluded.device_sn,"
            " profile_json = excluded.profile_json, version = excluded.version,"
            " updated_at = excluded.updated_at",
            (user_id, device_sn, profile_json, version, now, now),
        )
        return version


def get_health_profile(user_id):
    """按 userId 取云端健康档案；无记录返回 None。"""
    with _lock, _conn() as conn:
        conn.row_factory = sqlite3.Row
        row = conn.execute(
            "SELECT * FROM health_profiles WHERE user_id = ?", (user_id,)
        ).fetchone()
        return dict(row) if row else None


init_db()
