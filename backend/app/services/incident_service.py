"""四端协同应急事件服务（云端对齐轨）

与 Android App 的 IncidentRepository 状态机保持一致：
  RAISED → COMMUNITY_RECEIVED(自动) → [联系家属] → DISPATCH_REQUESTED
    → (每 15s 加急, 3 次后 URGENT→ESCALATED 升级下一排班医生并处罚)
    → HOSPITAL_ACCEPTED(先接先得 CAS) → HOSPITAL_DONE(措施必填) → CLOSED
  社区也可在 COMMUNITY_RECEIVED 阶段 SELF_CLOSED 自行闭环。
时间戳统一使用毫秒 INTEGER。
"""
import time
import uuid
from typing import Optional

from ..core.database import get_db

# ── 状态常量（与 App IncidentStatus 对齐） ──
RAISED = "RAISED"
COMMUNITY_RECEIVED = "COMMUNITY_RECEIVED"
DISPATCH_REQUESTED = "DISPATCH_REQUESTED"
URGENT = "URGENT"
ESCALATED = "ESCALATED"
ESCALATED_UNANSWERED = "ESCALATED_UNANSWERED"
HOSPITAL_ACCEPTED = "HOSPITAL_ACCEPTED"
HOSPITAL_DONE = "HOSPITAL_DONE"
CLOSED = "CLOSED"
SELF_CLOSED = "SELF_CLOSED"

TERMINAL = {CLOSED, SELF_CLOSED}
DISPATCH_LIKE = {DISPATCH_REQUESTED, URGENT, ESCALATED}

# ── 业务参数（与 App IncidentConfig 对齐） ──
URGENT_INTERVAL_MS = 15_000
URGENT_MAX_TIMES = 3
SCORE_INIT = 100
SCORE_MISSED = -5
SCORE_ESCALATION_FAULT = -10
NOTICE_THRESHOLD = 3
SUSPEND_THRESHOLD = 5

SCHEDULE_WEEKLY = 0
SCHEDULE_ONCE = 1

STATUS_LABEL = {
    RAISED: "已触发", COMMUNITY_RECEIVED: "社区已接收", DISPATCH_REQUESTED: "已呼叫医院",
    URGENT: "加急中", ESCALATED: "已升级", ESCALATED_UNANSWERED: "升级无人接",
    HOSPITAL_ACCEPTED: "医院已处警", HOSPITAL_DONE: "医院已完成", CLOSED: "已闭环", SELF_CLOSED: "社区自行闭环",
}


def _now_ms() -> int:
    return int(time.time() * 1000)


def _gen_incident_no() -> str:
    return "INC" + time.strftime("%Y%m%d%H%M%S") + uuid.uuid4().hex[:4].upper()


def _row_to_inc(row) -> dict:
    d = dict(row)
    d["status_label"] = STATUS_LABEL.get(d.get("status"), d.get("status"))
    return d


# ════════════════════════════════════════════════════════
# 事件生命周期
# ════════════════════════════════════════════════════════
def raise_incident(elderly_id: str, elderly_name: str = "", alarm_id: Optional[str] = None,
                   alarm_type: str = "FALL", alarm_level: str = "HIGH",
                   family_user_id: Optional[str] = None, family_phone: str = "",
                   community_org_id: Optional[str] = None, community_staff_id: Optional[str] = None,
                   building_no: str = "", unit_no: str = "", room_no: str = "") -> dict:
    """RK3 跌倒触发：建事件并自动置为社区已接收（同时生成网格员待办由 App 侧承载）"""
    db = get_db()
    now = _now_ms()
    inc_id = uuid.uuid4().hex
    no = _gen_incident_no()

    staff_name = ""
    # 未指定网格员时，取该社区任一 community 用户兜底
    if not community_staff_id and community_org_id:
        u = db.execute(
            "SELECT id, real_name FROM users WHERE role='community' AND institution_id=? AND is_active=1 ORDER BY created_at LIMIT 1",
            (community_org_id,)).fetchone()
        if u:
            community_staff_id, staff_name = u["id"], u["real_name"] or ""
    elif community_staff_id:
        u = db.execute("SELECT real_name FROM users WHERE id=?", (community_staff_id,)).fetchone()
        if u:
            staff_name = u["real_name"] or ""

    db.execute("""
        INSERT INTO incidents(id, incident_no, alarm_id, elderly_id, elderly_name, family_user_id, family_phone,
            community_org_id, community_staff_id, community_staff_name, building_no, unit_no, room_no,
            alarm_type, alarm_level, status, raised_at, community_received_at, updated_at, created_at)
        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    """, (inc_id, no, alarm_id, elderly_id, elderly_name, family_user_id, family_phone,
          community_org_id, community_staff_id, staff_name, building_no, unit_no, room_no,
          alarm_type, alarm_level, COMMUNITY_RECEIVED, now, now, now, now))
    db.commit()
    return get_incident(inc_id)


def _get(db, inc_id: str):
    row = db.execute("SELECT * FROM incidents WHERE id=?", (inc_id,)).fetchone()
    if not row:
        raise ValueError("事件不存在")
    return row


def contact_family(inc_id: str, note: str = "") -> dict:
    db = get_db(); now = _now_ms(); row = _get(db, inc_id)
    if row["status"] != COMMUNITY_RECEIVED:
        raise ValueError("仅社区已接收阶段可登记联系家属")
    db.execute("UPDATE incidents SET family_contacted_at=?, community_note=?, updated_at=? WHERE id=?",
               (now, note, now, inc_id))
    db.commit()
    return get_incident(inc_id)


def _active_hospital(db, community_org_id: str):
    rows = db.execute(
        "SELECT hospital_org_id FROM hospital_community_bindings WHERE community_org_id=? AND status='active'",
        (community_org_id,)).fetchall()
    ids = [r["hospital_org_id"] for r in rows]
    if not ids:
        raise ValueError("该社区尚未绑定医院，无法出警")
    if len(ids) > 1:
        raise ValueError("该社区绑定了多家医院，请指定医院")
    return ids[0]


def request_dispatch(inc_id: str, hospital_org_id: Optional[str] = None) -> dict:
    """社区紧急出警：仅 COMMUNITY_RECEIVED 可触发，生成对医院的正式急救事件"""
    db = get_db(); now = _now_ms(); row = _get(db, inc_id)
    if row["status"] != COMMUNITY_RECEIVED:
        raise ValueError("当前状态不可呼叫医院")
    hosp = hospital_org_id or _active_hospital(db, row["community_org_id"])
    db.execute("""UPDATE incidents SET status=?, hospital_org_id=?, dispatch_requested_at=?,
                  last_urgent_at=?, updated_at=? WHERE id=?""",
               (DISPATCH_REQUESTED, hosp, now, now, now, inc_id))
    _write_record(db, side="community", staff_id=row["community_staff_id"], staff_name=row["community_staff_name"],
                  elderly_id=row["elderly_id"], elderly_name=row["elderly_name"],
                  service_type="应急处置", content="判断事态后呼叫医院紧急出警", incident_id=inc_id,
                  started_at=row["community_received_at"], finished_at=None)
    db.commit()
    return get_incident(inc_id)


def accept_by_doctor(inc_id: str, doctor_id: str, doctor_name: str = "") -> bool:
    """医生一键处警：先接先得 CAS"""
    db = get_db(); now = _now_ms()
    placeholders = ",".join("?" for _ in DISPATCH_LIKE)
    cur = db.execute(
        f"""UPDATE incidents SET status=?, hospital_doctor_id=?, hospital_doctor_name=?,
            hospital_accepted_at=?, updated_at=?
            WHERE id=? AND hospital_doctor_id IS NULL AND status IN ({placeholders})""",
        (HOSPITAL_ACCEPTED, doctor_id, doctor_name, now, now, inc_id, *DISPATCH_LIKE))
    db.commit()
    if cur.rowcount == 0:
        return False
    row = _get(db, inc_id)
    _write_record(db, side="hospital", staff_id=doctor_id, staff_name=doctor_name,
                  elderly_id=row["elderly_id"], elderly_name=row["elderly_name"],
                  service_type="急救处警", content="值班医生接单出警", incident_id=inc_id,
                  started_at=now, finished_at=None)
    db.commit()
    return True


def hospital_complete(inc_id: str, treatment: str, doctor_id: str, doctor_name: str = "") -> dict:
    if not treatment or not treatment.strip():
        raise ValueError("处置措施必填")
    db = get_db(); now = _now_ms(); row = _get(db, inc_id)
    if row["status"] != HOSPITAL_ACCEPTED:
        raise ValueError("仅已处警事件可登记处置完成")
    db.execute("UPDATE incidents SET status=?, hospital_treatment=?, hospital_done_at=?, updated_at=? WHERE id=?",
               (HOSPITAL_DONE, treatment.strip(), now, now, inc_id))
    _write_record(db, side="hospital", staff_id=doctor_id, staff_name=doctor_name,
                  elderly_id=row["elderly_id"], elderly_name=row["elderly_name"],
                  service_type="急救处警", content=treatment.strip(), treatment=treatment.strip(),
                  incident_id=inc_id, started_at=row["hospital_accepted_at"], finished_at=now)
    db.commit()
    return get_incident(inc_id)


def community_complete(inc_id: str, note: str, staff_id: str, staff_name: str = "") -> dict:
    """社区闭环：强校验医院已完成（自行闭环走 self_close）"""
    db = get_db(); now = _now_ms(); row = _get(db, inc_id)
    if row["status"] != HOSPITAL_DONE:
        raise ValueError("需医院处置完成后，社区才能闭环")
    db.execute("UPDATE incidents SET status=?, community_note=?, closed_at=?, updated_at=? WHERE id=?",
               (CLOSED, note, now, now, inc_id))
    _write_record(db, side="community", staff_id=staff_id, staff_name=staff_name,
                  elderly_id=row["elderly_id"], elderly_name=row["elderly_name"],
                  service_type="应急处置", content="社区确认闭环：" + (note or "事件处置完成"),
                  incident_id=inc_id, started_at=row["dispatch_requested_at"], finished_at=now)
    db.commit()
    return get_incident(inc_id)


def self_close(inc_id: str, note: str, staff_id: str, staff_name: str = "") -> dict:
    db = get_db(); now = _now_ms(); row = _get(db, inc_id)
    if row["status"] != COMMUNITY_RECEIVED:
        raise ValueError("仅社区已接收阶段可自行闭环")
    db.execute("UPDATE incidents SET status=?, community_note=?, self_closed_at=?, updated_at=? WHERE id=?",
               (SELF_CLOSED, note, now, now, inc_id))
    _write_record(db, side="community", staff_id=staff_id, staff_name=staff_name,
                  elderly_id=row["elderly_id"], elderly_name=row["elderly_name"],
                  service_type="应急处置", content="社区自行闭环，未呼叫医院：" + (note or ""),
                  incident_id=inc_id, started_at=row["community_received_at"], finished_at=now)
    db.commit()
    return get_incident(inc_id)


# ════════════════════════════════════════════════════════
# 定时加急 / 升级 / 处罚
# ════════════════════════════════════════════════════════
def on_duty_doctors(hospital_org_id: str, at_ms: Optional[int] = None):
    """返回当前时刻在班医生 user 列表（周循环/指定日期 + 跨夜班）"""
    import datetime
    at = datetime.datetime.fromtimestamp((at_ms or _now_ms()) / 1000)
    hm = at.strftime("%H:%M")
    weekday = at.isoweekday()  # 周一=1..周日=7
    db = get_db()
    rows = db.execute("""SELECT s.*, u.real_name, u.institution_id FROM staff_schedules s
                         JOIN users u ON u.id=s.staff_id
                         WHERE s.role='hospital' AND s.status='active'
                         AND (u.institution_id=? OR ? IS NULL)""",
                      (hospital_org_id, hospital_org_id)).fetchall()
    duty = []
    for s in rows:
        start, end = s["start_time"], s["end_time"]
        if s["schedule_mode"] == SCHEDULE_WEEKLY and s["weekday"] != weekday:
            continue
        hit = (hm >= start) if end <= start else (start <= hm < end)
        if end <= start:  # 跨夜班
            hit = hm >= start or hm <= end
        if hit:
            duty.append({"doctor_id": s["staff_id"], "doctor_name": s["real_name"] or ""})
    return duty


def _penalize(db, doctor_id, doctor_name, hospital_org_id, inc_id, ptype, level, delta, reason):
    db.execute("""INSERT INTO doctor_penalties(id, doctor_id, doctor_name, hospital_org_id, incident_id,
                  penalty_type, level, score_delta, reason, status, created_at)
                  VALUES(?,?,?,?,?,?,?,?,?,'active',?)""",
               (uuid.uuid4().hex, doctor_id, doctor_name, hospital_org_id, inc_id, ptype, level, delta, reason, _now_ms()))


def tick() -> int:
    """调度器周期调用：对等待医院接单的事件做加急 / 升级 / 处罚。返回处理条数"""
    db = get_db(); now = _now_ms(); n = 0
    rows = db.execute(f"SELECT * FROM incidents WHERE status IN ({','.join('?' for _ in DISPATCH_LIKE)})",
                      tuple(DISPATCH_LIKE)).fetchall()
    for row in rows:
        last = row["last_urgent_at"] or now
        if now - last < URGENT_INTERVAL_MS:
            continue
        n += 1
        cnt = (row["urgent_count"] or 0) + 1
        duty = on_duty_doctors(row["hospital_org_id"], now)
        duty_ids = [d["doctor_id"] for d in duty]
        current = row["hospital_doctor_id"]

        if cnt >= URGENT_MAX_TIMES:
            # 三次加急：升级下一排班人；当前在班责任人处罚；无人在班不罚医生
            if current and current in duty_ids:
                name = next((d["doctor_name"] for d in duty if d["doctor_id"] == current), "")
                _penalize(db, current, name, row["hospital_org_id"], row["id"],
                          "ESCALATION_FAULT", "escalation", SCORE_ESCALATION_FAULT, "三次加急未接，升级处理")
            nxt = next((d for d in duty if d["doctor_id"] != current), None)
            if nxt:
                db.execute("UPDATE incidents SET status=?, urgent_count=?, escalated_at=?, last_urgent_at=?, hospital_doctor_id=NULL, updated_at=? WHERE id=?",
                           (ESCALATED, cnt, now, now, now, row["id"]))
            else:
                # 无人在班 / 无下一班：不罚医生，记升级无人接
                db.execute("UPDATE incidents SET status=?, urgent_count=?, escalated_at=?, last_urgent_at=?, updated_at=? WHERE id=?",
                           (ESCALATED_UNANSWERED, cnt, now, now, now, row["id"]))
        else:
            # 普通加急：在班医生漏接记 -5
            if current and current in duty_ids:
                name = next((d["doctor_name"] for d in duty if d["doctor_id"] == current), "")
                _penalize(db, current, name, row["hospital_org_id"], row["id"],
                          "MISSED", "missed", SCORE_MISSED, f"第{cnt}次加急仍未接单")
            db.execute("UPDATE incidents SET status=?, urgent_count=?, last_urgent_at=?, updated_at=? WHERE id=?",
                       (URGENT, cnt, now, now, row["id"]))
    db.commit()
    return n


# ════════════════════════════════════════════════════════
# 排班
# ════════════════════════════════════════════════════════
def create_shift(staff_id, staff_name, role, title, start_time, end_time, location="",
                 schedule_mode=SCHEDULE_WEEKLY, weekday=0, schedule_date=None) -> dict:
    db = get_db(); sid = uuid.uuid4().hex; now = _now_ms()
    db.execute("""INSERT INTO staff_schedules(id, staff_id, staff_name, role, title, schedule_date,
                  start_time, end_time, location, schedule_mode, weekday, status, created_at)
                  VALUES(?,?,?,?,?,?,?,?,?,?,?,'active',?)""",
               (sid, staff_id, staff_name, role, title, schedule_date or now,
                start_time, end_time, location, schedule_mode, weekday, now))
    db.commit()
    return {"id": sid}


def list_shifts(staff_id: Optional[str] = None, role: Optional[str] = None):
    sql, args = "SELECT * FROM staff_schedules WHERE status='active'", []
    if staff_id:
        sql += " AND staff_id=?"; args.append(staff_id)
    if role:
        sql += " AND role=?"; args.append(role)
    sql += " ORDER BY weekday, start_time"
    return [dict(r) for r in get_db().execute(sql, args).fetchall()]


# ════════════════════════════════════════════════════════
# 医院-社区绑定（管理端审批）
# ════════════════════════════════════════════════════════
def apply_hc_binding(hospital_org_id, community_org_id, note=""):
    db = get_db()
    existing = db.execute(
        "SELECT * FROM hospital_community_bindings WHERE hospital_org_id=? AND community_org_id=?",
        (hospital_org_id, community_org_id)).fetchone()
    if existing and existing["status"] in ("pending", "active"):
        raise ValueError("已存在待审批或生效的绑定")
    if existing:
        db.execute("UPDATE hospital_community_bindings SET status='pending', apply_note=?, created_at=? WHERE id=?",
                   (note, _now_ms(), existing["id"])); bid = existing["id"]
    else:
        bid = uuid.uuid4().hex
        db.execute("""INSERT INTO hospital_community_bindings(id, hospital_org_id, community_org_id, apply_note,
                      status, created_at) VALUES(?,?,?,?,'pending',?)""",
                   (bid, hospital_org_id, community_org_id, note, _now_ms()))
    db.commit()
    return {"id": bid, "status": "pending"}


def review_hc_binding(binding_id, approved: bool, reviewer: str, note=""):
    db = get_db()
    if not db.execute("SELECT id FROM hospital_community_bindings WHERE id=?", (binding_id,)).fetchone():
        raise ValueError("绑定申请不存在")
    db.execute("UPDATE hospital_community_bindings SET status=?, reviewed_by=?, reviewed_at=?, review_note=? WHERE id=?",
               ("active" if approved else "rejected", reviewer, _now_ms(), note, binding_id))
    db.commit()
    return {"id": binding_id, "status": "active" if approved else "rejected"}


def list_hc_bindings(hospital_org_id=None, community_org_id=None, status=None):
    sql, args = "SELECT * FROM hospital_community_bindings WHERE 1=1", []
    if hospital_org_id:
        sql += " AND hospital_org_id=?"; args.append(hospital_org_id)
    if community_org_id:
        sql += " AND community_org_id=?"; args.append(community_org_id)
    if status:
        sql += " AND status=?"; args.append(status)
    sql += " ORDER BY created_at DESC"
    return [dict(r) for r in get_db().execute(sql, args).fetchall()]


# ════════════════════════════════════════════════════════
# 查询 / 大屏 / 绩效
# ════════════════════════════════════════════════════════
def get_incident(inc_id: str) -> dict:
    return _row_to_inc(_get(get_db(), inc_id))


def list_incidents(scope: str, org_or_user_id: str, active_only: bool = False):
    col = {"community": "community_org_id", "hospital": "hospital_org_id", "family": "family_user_id"}[scope]
    sql = f"SELECT * FROM incidents WHERE {col}=?"
    args = [org_or_user_id]
    if active_only:
        ph = ",".join("?" for _ in TERMINAL)
        sql += f" AND status NOT IN ({ph})"; args += list(TERMINAL)
    sql += " ORDER BY created_at DESC"
    return [_row_to_inc(r) for r in get_db().execute(sql, args).fetchall()]


def hospital_grid(hospital_org_id: str):
    """医院大屏：每个绑定社区的活跃告警数"""
    db = get_db()
    binds = db.execute(
        "SELECT * FROM hospital_community_bindings WHERE hospital_org_id=? AND status='active'",
        (hospital_org_id,)).fetchall()
    out = []
    for b in binds:
        inst = db.execute("SELECT name, service_area FROM institutions WHERE id=?", (b["community_org_id"],)).fetchone()
        cnt = db.execute(
            f"SELECT COUNT(*) c FROM incidents WHERE community_org_id=? AND status IN ({','.join('?' for _ in DISPATCH_LIKE)})",
            (b["community_org_id"], *DISPATCH_LIKE)).fetchone()["c"]
        out.append({"community_org_id": b["community_org_id"],
                    "community_name": inst["name"] if inst else b["community_org_id"],
                    "service_area": inst["service_area"] if inst else "",
                    "active_incident_count": cnt})
    return out


def service_records(elderly_id: Optional[str] = None, side: Optional[str] = None):
    sql, args = "SELECT * FROM service_records WHERE 1=1", []
    if elderly_id:
        sql += " AND elderly_id=?"; args.append(elderly_id)
    if side:
        sql += " AND side=?"; args.append(side)
    sql += " ORDER BY created_at DESC"
    return [dict(r) for r in get_db().execute(sql, args).fetchall()]


def _write_record(db, side, staff_id, staff_name, elderly_id, elderly_name, service_type,
                  content, incident_id, started_at, finished_at, treatment=""):
    duration = 0
    if started_at and finished_at:
        duration = max(0, int((finished_at - started_at) / 60000))
    db.execute("""INSERT INTO service_records(id, side, staff_id, staff_name, elderly_id, elderly_name,
                  service_type, content, treatment, incident_id, started_at, finished_at, duration_minutes, created_at)
                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
               (uuid.uuid4().hex, side, staff_id, staff_name, elderly_id, elderly_name,
                service_type, content, treatment, incident_id, started_at, finished_at, duration, _now_ms()))


def doctor_performance(doctor_id: str):
    db = get_db()
    accepted = db.execute("SELECT COUNT(*) c FROM incidents WHERE hospital_doctor_id=? AND hospital_accepted_at IS NOT NULL",
                          (doctor_id,)).fetchone()["c"]
    pens = db.execute("SELECT * FROM doctor_penalties WHERE doctor_id=?", (doctor_id,)).fetchall()
    missed = sum(1 for p in pens if p["penalty_type"] == "MISSED" and p["status"] == "active")
    score = SCORE_INIT + sum(p["score_delta"] for p in pens if p["status"] == "active")
    return {"doctor_id": doctor_id, "score": score, "accepted_count": accepted,
            "missed_count": missed, "penalty_count": len(pens)}


def list_penalties(doctor_id: Optional[str] = None):
    sql, args = "SELECT * FROM doctor_penalties WHERE 1=1", []
    if doctor_id:
        sql += " AND doctor_id=?"; args.append(doctor_id)
    sql += " ORDER BY created_at DESC"
    return [dict(r) for r in get_db().execute(sql, args).fetchall()]


def revoke_penalty(penalty_id: str, reviewer: str):
    db = get_db()
    db.execute("UPDATE doctor_penalties SET status='revoked', revoked_by=?, revoked_at=? WHERE id=?",
               (reviewer, _now_ms(), penalty_id))
    db.commit()
    return {"id": penalty_id, "status": "revoked"}
