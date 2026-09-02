"""数据库初始化：12张核心表建表 + 种子数据"""
import os
import sqlite3
import threading
from .config import DB_PATH

# 线程本地存储，确保每个线程有自己的连接
_local = threading.local()


def get_db() -> sqlite3.Connection:
    """获取当前线程的数据库连接"""
    if not hasattr(_local, "conn") or _local.conn is None:
        os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
        _local.conn = sqlite3.connect(DB_PATH, check_same_thread=False)
        _local.conn.row_factory = sqlite3.Row
        _local.conn.execute("PRAGMA journal_mode=WAL")
        _local.conn.execute("PRAGMA foreign_keys=ON")
    return _local.conn


def init_db():
    """初始化数据库：建表 + 种子数据"""
    conn = get_db()
    cursor = conn.cursor()

    # ============================================================
    # 1. users - 统一用户表（四角色）
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id              TEXT PRIMARY KEY,
            client_id       TEXT NOT NULL UNIQUE,
            ezviz_access_token TEXT,
            real_name       TEXT NOT NULL DEFAULT '',
            phone           TEXT NOT NULL DEFAULT '',
            id_card         TEXT,
            role            TEXT CHECK(role IN ('family','community','hospital','admin')),
            institution_id  TEXT,
            avatar_url      TEXT,
            is_active       INTEGER DEFAULT 1,
            last_login_at   TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            updated_at      TEXT DEFAULT (datetime('now','localtime'))
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_client_id ON users(client_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_role ON users(role)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_institution ON users(institution_id)")

    # ============================================================
    # 2. elderly - 老人信息档案
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS elderly (
            id              TEXT PRIMARY KEY,
            name            TEXT NOT NULL,
            gender          TEXT CHECK(gender IN ('M','F','')),
            birth_date      TEXT,
            id_card         TEXT,
            phone           TEXT,
            address         TEXT,
            emergency_contact TEXT,
            medical_history TEXT,
            care_level      TEXT DEFAULT '自理',
            binding_family_user_id TEXT NOT NULL,
            is_active       INTEGER DEFAULT 1,
            privacy_paused  INTEGER DEFAULT 0,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            updated_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (binding_family_user_id) REFERENCES users(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_elderly_family ON elderly(binding_family_user_id)")

    # ============================================================
    # 3. devices - 硬件设备
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS devices (
            id              TEXT PRIMARY KEY,
            device_name     TEXT NOT NULL,
            device_type     TEXT NOT NULL CHECK(device_type IN ('camera','smoke_sensor','gas_sensor','wearable','door_sensor','bed_sensor')),
            manufacturer    TEXT,
            model           TEXT,
            elderly_id      TEXT NOT NULL,
            location        TEXT,
            stream_url      TEXT,
            status          TEXT DEFAULT 'online' CHECK(status IN ('online','offline','fault','maintenance')),
            last_heartbeat  INTEGER,
            capabilities    TEXT,
            install_date    TEXT,
            warranty_expiry TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (elderly_id) REFERENCES elderly(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_devices_elderly ON devices(elderly_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status)")

    # ============================================================
    # 4. institutions - 机构信息
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS institutions (
            id              TEXT PRIMARY KEY,
            name            TEXT NOT NULL,
            institution_type TEXT NOT NULL CHECK(institution_type IN ('community','hospital')),
            address         TEXT,
            contact_phone   TEXT,
            license_number  TEXT,
            license_expiry  TEXT,
            is_verified     INTEGER DEFAULT 0,
            created_at      TEXT DEFAULT (datetime('now','localtime'))
        )
    """)

    # ============================================================
    # 5. authorizations - 核心权限表：多端数据访问授权
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS authorizations (
            id              TEXT PRIMARY KEY,
            elderly_id      TEXT NOT NULL,
            grantor_user_id TEXT NOT NULL,
            grantee_user_id TEXT NOT NULL,
            grantee_institution_id TEXT,
            permission_type TEXT NOT NULL CHECK(permission_type IN ('monitoring','health_records','alarm_video','all')),
            data_scope      TEXT NOT NULL,
            effective_from  TEXT NOT NULL,
            effective_until TEXT NOT NULL,
            status          TEXT DEFAULT 'pending' CHECK(status IN ('pending','active','revoked','expired','rejected')),
            revoked_by      TEXT,
            revoked_at      TEXT,
            revoke_reason   TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (elderly_id) REFERENCES elderly(id),
            FOREIGN KEY (grantor_user_id) REFERENCES users(id),
            FOREIGN KEY (grantee_user_id) REFERENCES users(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_auth_elderly ON authorizations(elderly_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_auth_grantee ON authorizations(grantee_user_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_auth_status ON authorizations(status)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_auth_expiry ON authorizations(effective_until)")

    # ============================================================
    # 6. qualification_reviews - 资质审核流程
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS qualification_reviews (
            id              TEXT PRIMARY KEY,
            applicant_user_id TEXT NOT NULL,
            elderly_id      TEXT,
            institution_id  TEXT NOT NULL,
            review_type     TEXT NOT NULL CHECK(review_type IN ('initial','renewal')),
            documents_json  TEXT,
            ocr_name_match  REAL,
            ocr_id_match    REAL,
            ocr_license_valid INTEGER DEFAULT 1,
            auto_review_result TEXT,
            manual_reviewer_id TEXT,
            manual_review_result TEXT,
            manual_review_note TEXT,
            validity_months INTEGER DEFAULT 6,
            status          TEXT DEFAULT 'pending',
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            reviewed_at     TEXT,
            FOREIGN KEY (applicant_user_id) REFERENCES users(id),
            FOREIGN KEY (elderly_id) REFERENCES elderly(id),
            FOREIGN KEY (institution_id) REFERENCES institutions(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_qr_applicant ON qualification_reviews(applicant_user_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_qr_status ON qualification_reviews(status)")

    # ============================================================
    # 7. alarms - 老人异常告警
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS alarms (
            id              TEXT PRIMARY KEY,
            elderly_id      TEXT NOT NULL,
            device_id       TEXT,
            alarm_type      TEXT NOT NULL,
            alarm_level     TEXT NOT NULL CHECK(alarm_level IN ('LOW','MEDIUM','HIGH','EMERGENCY')),
            ai_score        REAL,
            ai_verified     INTEGER DEFAULT 0,
            raw_data_json   TEXT,
            snapshot_url    TEXT,
            video_clip_url  TEXT,
            title           TEXT NOT NULL,
            description     TEXT,
            status          TEXT DEFAULT 'active' CHECK(status IN ('active','acknowledged','processing','resolved','archived')),
            push_family     INTEGER DEFAULT 1,
            push_community  INTEGER DEFAULT 0,
            push_hospital   INTEGER DEFAULT 0,
            related_work_order_id TEXT,
            acknowledged_by TEXT,
            acknowledged_at TEXT,
            resolved_by     TEXT,
            resolved_at     TEXT,
            resolution_note TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (elderly_id) REFERENCES elderly(id),
            FOREIGN KEY (device_id) REFERENCES devices(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_alarms_elderly ON alarms(elderly_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_alarms_level ON alarms(alarm_level)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_alarms_status ON alarms(status)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_alarms_created ON alarms(created_at)")

    # ============================================================
    # 8. work_orders - 处置工单
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS work_orders (
            id              TEXT PRIMARY KEY,
            alarm_id        TEXT,
            elderly_id      TEXT NOT NULL,
            order_type      TEXT NOT NULL CHECK(order_type IN ('alarm_handling','device_maintenance','inspection','emergency','binding_review')),
            title           TEXT NOT NULL,
            description     TEXT,
            priority        TEXT DEFAULT 'normal' CHECK(priority IN ('low','normal','high','urgent')),
            assigned_to     TEXT,
            assigned_institution_id TEXT,
            status          TEXT DEFAULT 'pending' CHECK(status IN ('pending','accepted','in_progress','completed','cancelled')),
            result_json     TEXT,
            result_photos   TEXT,
            deadline        TEXT,
            completed_at    TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (alarm_id) REFERENCES alarms(id),
            FOREIGN KEY (elderly_id) REFERENCES elderly(id),
            FOREIGN KEY (assigned_to) REFERENCES users(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_wo_alarm ON work_orders(alarm_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_wo_elderly ON work_orders(elderly_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_wo_assignee ON work_orders(assigned_to)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_wo_status ON work_orders(status)")

    # ============================================================
    # 9. health_records - 医疗健康档案
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS health_records (
            id              TEXT PRIMARY KEY,
            elderly_id      TEXT NOT NULL,
            record_type     TEXT NOT NULL,
            record_date     TEXT NOT NULL,
            doctor_name     TEXT,
            hospital_name   TEXT,
            content_json    TEXT NOT NULL,
            attachment_urls TEXT,
            visibility      TEXT DEFAULT 'family' CHECK(visibility IN ('family','hospital','both')),
            created_by      TEXT NOT NULL,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (elderly_id) REFERENCES elderly(id),
            FOREIGN KEY (created_by) REFERENCES users(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_hr_elderly ON health_records(elderly_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_hr_date ON health_records(record_date)")

    # ============================================================
    # 10. access_records - 不可修改数据访问记录
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS access_records (
            id              TEXT PRIMARY KEY,
            user_id         TEXT NOT NULL,
            accessed_elderly_id TEXT,
            access_type     TEXT NOT NULL,
            data_scope      TEXT,
            authorized_by   TEXT,
            access_result   TEXT DEFAULT 'granted' CHECK(access_result IN ('granted','denied')),
            deny_reason     TEXT,
            ip_address      TEXT,
            user_agent      TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_ar_user ON access_records(user_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_ar_elderly ON access_records(accessed_elderly_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_ar_created ON access_records(created_at)")

    # ============================================================
    # 11. audit_logs - 全操作审计日志
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS audit_logs (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            event_type      TEXT NOT NULL,
            event_source    TEXT DEFAULT 'system',
            operator        TEXT DEFAULT 'system',
            target_type     TEXT,
            target_id       TEXT,
            detail_json     TEXT,
            ip_address      TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime'))
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_audit_type ON audit_logs(event_type)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_audit_operator ON audit_logs(operator)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at)")

    # ============================================================
    # 12. device_maintenance - 设备运维巡检台账
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS device_maintenance (
            id              TEXT PRIMARY KEY,
            device_id       TEXT NOT NULL,
            maintenance_type TEXT NOT NULL,
            inspector_id    TEXT,
            inspection_date TEXT NOT NULL,
            status          TEXT CHECK(status IN ('normal','needs_repair','replaced','fault')),
            findings        TEXT,
            photos          TEXT,
            next_inspection_date TEXT,
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (device_id) REFERENCES devices(id),
            FOREIGN KEY (inspector_id) REFERENCES users(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_dm_device ON device_maintenance(device_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_dm_date ON device_maintenance(inspection_date)")

    # ============================================================
    # 13. device_verification_codes - 设备验证码（三端联动）
    # ============================================================
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS device_verification_codes (
            id              TEXT PRIMARY KEY,
            device_id       TEXT NOT NULL,
            elderly_id      TEXT NOT NULL,
            family_user_id  TEXT NOT NULL,
            code            TEXT NOT NULL,
            expires_at      TEXT NOT NULL,
            max_uses        INTEGER DEFAULT 1,
            use_count       INTEGER DEFAULT 0,
            status          TEXT DEFAULT 'active' CHECK(status IN ('active','used','expired','revoked')),
            created_at      TEXT DEFAULT (datetime('now','localtime')),
            FOREIGN KEY (device_id) REFERENCES devices(id),
            FOREIGN KEY (elderly_id) REFERENCES elderly(id),
            FOREIGN KEY (family_user_id) REFERENCES users(id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_dvc_device ON device_verification_codes(device_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_dvc_code ON device_verification_codes(code)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_dvc_family ON device_verification_codes(family_user_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_dvc_status ON device_verification_codes(status)")

    _create_collab_tables(cursor)
    _extend_institutions(cursor)
    conn.commit()
    _seed_data(conn)


def _seed_data(conn: sqlite3.Connection):
    """插入种子数据（测试用）"""
    from .security import generate_uuid, now_iso

    cursor = conn.cursor()

    # 检查是否已有数据
    cursor.execute("SELECT COUNT(*) FROM users")
    if cursor.fetchone()[0] > 0:
        return  # 已有数据，跳过种子

    ts = now_iso()

    # 1. 创建机构
    inst_community_id = generate_uuid()
    inst_hospital_id = generate_uuid()
    cursor.execute(
        "INSERT INTO institutions(id, name, institution_type, address, contact_phone, license_number, is_verified) VALUES(?,?,?,?,?,?,?)",
        (inst_community_id, "阳光社区养老驿站", "community", "XX市XX区XX路100号", "010-12345678", "CL20240001", 1)
    )
    cursor.execute(
        "INSERT INTO institutions(id, name, institution_type, address, contact_phone, license_number, is_verified) VALUES(?,?,?,?,?,?,?)",
        (inst_hospital_id, "XX市老年病专科医院", "hospital", "XX市XX区XX路200号", "010-87654321", "HL20240001", 1)
    )

    # 2. 创建用户（四个角色测试用户，使用 client_id 标识）
    # client_id = "test_<role>" 作为模拟标识，实际运行时由 App 生成 UUID

    # 家属 - client_id: test_family_001
    family_id = generate_uuid()
    cursor.execute(
        "INSERT INTO users(id, client_id, real_name, phone, role, created_at) VALUES(?,?,?,?,?,?)",
        (family_id, "test_family_001", "张小明", "13800001111", "family", ts)
    )

    # 社区人员 - client_id: test_community_001
    community_id = generate_uuid()
    cursor.execute(
        "INSERT INTO users(id, client_id, real_name, phone, role, institution_id, created_at) VALUES(?,?,?,?,?,?,?)",
        (community_id, "test_community_001", "李网格", "13800002222", "community", inst_community_id, ts)
    )

    # 医院人员 - client_id: test_hospital_001
    hospital_id = generate_uuid()
    cursor.execute(
        "INSERT INTO users(id, client_id, real_name, phone, role, institution_id, created_at) VALUES(?,?,?,?,?,?,?)",
        (hospital_id, "test_hospital_001", "王医生", "13800003333", "hospital", inst_hospital_id, ts)
    )

    # 管理员 - client_id: test_admin_001
    admin_id = generate_uuid()
    cursor.execute(
        "INSERT INTO users(id, client_id, real_name, phone, role, created_at) VALUES(?,?,?,?,?,?)",
        (admin_id, "test_admin_001", "系统管理员", "13800000000", "admin", ts)
    )

    # 3. 创建老人档案
    elderly_id = generate_uuid()
    cursor.execute(
        "INSERT INTO elderly(id, name, gender, birth_date, id_card, phone, address, emergency_contact, care_level, binding_family_user_id, created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
        (elderly_id, "张德福", "M", "1950-03-15", "3201**********1234", "139****5678", "XX市XX区XX小区1栋101", "13800001111", "半自理", family_id, ts)
    )

    # 4. 创建设备
    camera_id = generate_uuid()
    cursor.execute(
        "INSERT INTO devices(id, device_name, device_type, elderly_id, location, status, capabilities, stream_url, created_at) VALUES(?,?,?,?,?,?,?,?,?)",
        (camera_id, "客厅摄像头", "camera", elderly_id, "客厅", "online", '{"ptz":true,"audio":true,"ai_detect":true}', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', ts)
    )

    wearable_id = generate_uuid()
    cursor.execute(
        "INSERT INTO devices(id, device_name, device_type, elderly_id, location, status, capabilities, created_at) VALUES(?,?,?,?,?,?,?,?)",
        (wearable_id, "智能手环", "wearable", elderly_id, "随身", "online", '{"heart_rate":true,"fall_detect":true}', ts)
    )

    bed_sensor_id = generate_uuid()
    cursor.execute(
        "INSERT INTO devices(id, device_name, device_type, elderly_id, location, status, capabilities, created_at) VALUES(?,?,?,?,?,?,?,?)",
        (bed_sensor_id, "床垫传感器", "bed_sensor", elderly_id, "卧室", "online", '{"pressure":true,"absence_detect":true}', ts)
    )

    # 5. 创建家属对社区人员的授权（示例：授权查看告警视频）
    auth_id = generate_uuid()
    from datetime import datetime, timedelta
    effective_from = ts
    effective_until = (datetime.now() + timedelta(days=365)).strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute(
        "INSERT INTO authorizations(id, elderly_id, grantor_user_id, grantee_user_id, grantee_institution_id, permission_type, data_scope, effective_from, effective_until, status, created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
        (auth_id, elderly_id, family_id, community_id, inst_community_id, "alarm_video", '{"video":true,"medical":false,"alarm":true}', effective_from, effective_until, "active", ts)
    )

    # 6. 添加审计日志
    cursor.execute(
        "INSERT INTO audit_logs(event_type, operator, target_type, target_id, detail_json, created_at) VALUES(?,?,?,?,?,?)",
        ("system_init", "system", "system", "system", '{"message":"系统初始化完成，种子数据已导入"}', ts)
    )

    conn.commit()
    print("[DB] 数据库初始化完成，种子数据已导入")
    print(f"  测试 client_id: test_family_001 (家属)")
    print(f"  测试 client_id: test_community_001 (社区)")
    print(f"  测试 client_id: test_hospital_001 (医院)")
    print(f"  测试 client_id: test_admin_001 (管理员)")
    print(f"  提示: 正式使用时 App 会自动生成 UUID 作为 client_id")

def _extend_institutions(cursor):
    """institutions 幂等补列（网格化/前台化所需），已存在则跳过"""
    exists = {r["name"] for r in cursor.execute("PRAGMA table_info(institutions)").fetchall()}
    add = {
        "area_buildings": "TEXT DEFAULT ''",
        "contact_person": "TEXT DEFAULT ''",
        "service_area": "TEXT DEFAULT ''",
        "intro": "TEXT DEFAULT ''",
    }
    for col, decl in add.items():
        if col not in exists:
            cursor.execute(f"ALTER TABLE institutions ADD COLUMN {col} {decl}")


def _create_collab_tables(cursor):
    """四端协同：事件主表 / 排班 / 服务记录 / 医生处罚 / 医院-社区绑定（幂等）"""
    # 1. 跌倒等应急事件全生命周期主表（毫秒时间戳，与 App 状态机对齐）
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS incidents (
            id                     TEXT PRIMARY KEY,
            incident_no            TEXT NOT NULL UNIQUE,
            alarm_id               TEXT,
            elderly_id             TEXT NOT NULL,
            elderly_name           TEXT NOT NULL DEFAULT '',
            family_user_id         TEXT,
            family_phone           TEXT DEFAULT '',
            community_org_id       TEXT,
            community_staff_id     TEXT,
            community_staff_name   TEXT DEFAULT '',
            building_no            TEXT DEFAULT '',
            unit_no                TEXT DEFAULT '',
            room_no                TEXT DEFAULT '',
            hospital_org_id        TEXT,
            hospital_doctor_id     TEXT,
            hospital_doctor_name   TEXT DEFAULT '',
            alarm_type             TEXT DEFAULT 'FALL',
            alarm_level            TEXT DEFAULT 'HIGH',
            status                 TEXT NOT NULL DEFAULT 'RAISED',
            urgent_count           INTEGER DEFAULT 0,
            raised_at              INTEGER,
            community_received_at  INTEGER,
            family_contacted_at    INTEGER,
            dispatch_requested_at  INTEGER,
            hospital_accepted_at   INTEGER,
            hospital_done_at       INTEGER,
            closed_at              INTEGER,
            self_closed_at         INTEGER,
            last_urgent_at         INTEGER,
            escalated_at           INTEGER,
            community_note         TEXT DEFAULT '',
            hospital_treatment     TEXT DEFAULT '',
            updated_at             INTEGER,
            created_at             INTEGER
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_inc_status ON incidents(status)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_inc_community ON incidents(community_org_id, community_staff_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_inc_hospital ON incidents(hospital_org_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_inc_family ON incidents(family_user_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_inc_elderly ON incidents(elderly_id)")

    # 2. 工作人员排班（周循环 / 指定日期）
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS staff_schedules (
            id            TEXT PRIMARY KEY,
            staff_id      TEXT NOT NULL,
            staff_name    TEXT DEFAULT '',
            role          TEXT NOT NULL DEFAULT 'hospital',
            title         TEXT DEFAULT '',
            schedule_date INTEGER,
            start_time    TEXT NOT NULL,
            end_time      TEXT NOT NULL,
            location      TEXT DEFAULT '',
            schedule_mode INTEGER DEFAULT 0,
            weekday       INTEGER DEFAULT 0,
            status        TEXT DEFAULT 'active',
            created_at    INTEGER
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_ss_staff ON staff_schedules(staff_id, role)")

    # 3. 服务记录（社区/医院处置双写，完整时间链）
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS service_records (
            id               TEXT PRIMARY KEY,
            side             TEXT DEFAULT '',
            staff_id         TEXT,
            staff_name       TEXT DEFAULT '',
            elderly_id       TEXT NOT NULL,
            elderly_name     TEXT DEFAULT '',
            service_type     TEXT NOT NULL,
            content          TEXT DEFAULT '',
            treatment        TEXT DEFAULT '',
            incident_id      TEXT,
            started_at       INTEGER,
            finished_at      INTEGER,
            duration_minutes INTEGER DEFAULT 0,
            created_at       INTEGER
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_sr_elderly ON service_records(elderly_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_sr_incident ON service_records(incident_id)")

    # 4. 医生值班处罚/绩效
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS doctor_penalties (
            id              TEXT PRIMARY KEY,
            doctor_id       TEXT NOT NULL,
            doctor_name     TEXT DEFAULT '',
            hospital_org_id TEXT,
            incident_id     TEXT,
            penalty_type    TEXT NOT NULL,
            level           TEXT DEFAULT 'notice',
            score_delta     INTEGER DEFAULT 0,
            reason          TEXT DEFAULT '',
            status          TEXT DEFAULT 'active',
            revoked_by      TEXT,
            revoked_at      INTEGER,
            created_at      INTEGER
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_dp_doctor ON doctor_penalties(doctor_id, status)")

    # 5. 医院-社区绑定（多对多，管理端审批）
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS hospital_community_bindings (
            id               TEXT PRIMARY KEY,
            hospital_org_id  TEXT NOT NULL,
            community_org_id TEXT NOT NULL,
            apply_note       TEXT DEFAULT '',
            status           TEXT DEFAULT 'pending',
            reviewed_by      TEXT,
            reviewed_at      INTEGER,
            review_note      TEXT DEFAULT '',
            created_at       INTEGER,
            UNIQUE(hospital_org_id, community_org_id)
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_hcb_hospital ON hospital_community_bindings(hospital_org_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_hcb_community ON hospital_community_bindings(community_org_id)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_hcb_status ON hospital_community_bindings(status)")
