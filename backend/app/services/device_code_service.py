"""设备验证码服务：生成、验证、撤销设备分享验证码

三端联动核心 —— 家属生成6位验证码 → 社区/医院输入 → 自动创建授权 → 设备+老人数据访问权限
"""
import random
from datetime import datetime, timedelta
from ..core.database import get_db
from ..core.security import generate_uuid, now_iso


class DeviceCodeService:
    """设备验证码生命周期管理"""

    CODE_VALIDITY_HOURS = 24  # 验证码默认有效期
    CODE_LENGTH = 6           # 6位数字验证码

    @staticmethod
    def generate_code(device_id: str, family_user_id: str) -> dict:
        """
        家属为设备生成验证码。

        Returns:
            dict: { code_id, code, expires_at, device_id, elderly_id }
        Raises:
            ValueError: 设备不属于该家属
        """
        db = get_db()

        # 验证设备属于该家属的老人
        device = db.execute(
            """SELECT d.*, e.name as elderly_name FROM devices d
               JOIN elderly e ON d.elderly_id = e.id
               WHERE d.id = ? AND e.binding_family_user_id = ?""",
            (device_id, family_user_id)
        ).fetchone()

        if not device:
            raise ValueError("设备不存在或不属于您的老人")

        # 生成6位数字验证码（避免歧义：排除0和O容易混淆的，使用1-9）
        code = ''.join(str(random.randint(1, 9)) for _ in range(DeviceCodeService.CODE_LENGTH))

        code_id = generate_uuid()
        expires_at = (datetime.now() + timedelta(hours=DeviceCodeService.CODE_VALIDITY_HOURS)).strftime("%Y-%m-%d %H:%M:%S")
        ts = now_iso()

        db.execute(
            """INSERT INTO device_verification_codes(
                   id, device_id, elderly_id, family_user_id, code, expires_at, created_at)
               VALUES(?, ?, ?, ?, ?, ?, ?)""",
            (code_id, device_id, device["elderly_id"], family_user_id, code, expires_at, ts)
        )
        db.commit()

        return {
            "code_id": code_id,
            "code": code,
            "expires_at": expires_at,
            "device_id": device_id,
            "elderly_id": device["elderly_id"],
            "elderly_name": device["elderly_name"],
            "device_name": device["device_name"]
        }

    @staticmethod
    def verify_and_bind(code: str, redeem_user_id: str) -> dict:
        """
        社区/医院工作人员输入验证码绑定设备。

        验证通过后自动创建 authorization 记录，授予 monitoring 权限30天。

        Returns:
            dict: { message, device_id, elderly_id, elderly_name, authorization_id }
        Raises:
            ValueError: 验证码无效/过期/已使用
        """
        db = get_db()

        # 查找验证码记录
        row = db.execute(
            """SELECT vc.*, d.device_name, e.name as elderly_name
               FROM device_verification_codes vc
               JOIN devices d ON vc.device_id = d.id
               JOIN elderly e ON vc.elderly_id = e.id
               WHERE vc.code = ? AND vc.status = 'active'
               ORDER BY vc.created_at DESC LIMIT 1""",
            (code,)
        ).fetchone()

        if not row:
            raise ValueError("验证码无效或已使用")

        # 检查是否过期
        if datetime.now() > datetime.strptime(row["expires_at"], "%Y-%m-%d %H:%M:%S"):
            db.execute(
                "UPDATE device_verification_codes SET status='expired' WHERE id=?",
                (row["id"],)
            )
            db.commit()
            raise ValueError("验证码已过期")

        # 获取兑换者信息（机构ID）
        redeemer = db.execute(
            "SELECT * FROM users WHERE id = ?", (redeem_user_id,)
        ).fetchone()
        if not redeemer:
            raise ValueError("用户不存在")

        # 检查是否已绑定过同一设备+老人（避免重复授权）
        existing_auth = db.execute(
            """SELECT id FROM authorizations
               WHERE elderly_id = ? AND grantee_user_id = ?
               AND status = 'active'
               AND datetime(effective_until) > datetime('now','localtime')""",
            (row["elderly_id"], redeem_user_id)
        ).fetchone()
        if existing_auth:
            # 验证码仍标记为已使用（防止别人再用）
            db.execute(
                "UPDATE device_verification_codes SET use_count=use_count+1, status='used' WHERE id=?",
                (row["id"],)
            )
            db.commit()
            return {
                "message": "您已绑定过该老人的设备，无需重复绑定",
                "device_id": row["device_id"],
                "elderly_id": row["elderly_id"],
                "elderly_name": row["elderly_name"],
                "authorization_id": existing_auth["id"],
                "already_bound": True
            }

        # 自动创建授权（30天监控权限）
        auth_id = generate_uuid()
        ts = now_iso()
        from datetime import datetime as dt, timedelta as td
        effective_until = (dt.now() + td(days=30)).strftime("%Y-%m-%d %H:%M:%S")

        db.execute(
            """INSERT INTO authorizations(
                   id, elderly_id, grantor_user_id, grantee_user_id, grantee_institution_id,
                   permission_type, data_scope, effective_from, effective_until, status, created_at)
               VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?)""",
            (auth_id, row["elderly_id"], row["family_user_id"], redeem_user_id,
             redeemer["institution_id"], "monitoring",
             '{"video":true,"alarm":true,"device_status":true}',
             ts, effective_until, ts)
        )

        # 更新验证码使用次数
        db.execute(
            "UPDATE device_verification_codes SET use_count=use_count+1, status='used' WHERE id=?",
            (row["id"],)
        )
        db.commit()

        from .audit_service import AuditService
        AuditService.log(
            event_type="device_code_redeemed",
            operator=redeem_user_id,
            target_type="device_verification_code",
            target_id=row["id"],
            detail={
                "code": code,
                "device_id": row["device_id"],
                "elderly_id": row["elderly_id"],
                "authorization_id": auth_id,
                "redeemer_role": redeemer["role"]
            }
        )

        return {
            "message": "设备绑定成功，已获得30天监控权限",
            "device_id": row["device_id"],
            "elderly_id": row["elderly_id"],
            "elderly_name": row["elderly_name"],
            "authorization_id": auth_id,
            "already_bound": False
        }

    @staticmethod
    def revoke_code(code_id: str, family_user_id: str) -> bool:
        """家属撤销未使用的验证码"""
        db = get_db()
        row = db.execute(
            "SELECT * FROM device_verification_codes WHERE id=? AND family_user_id=? AND status='active'",
            (code_id, family_user_id)
        ).fetchone()
        if not row:
            return False

        db.execute(
            "UPDATE device_verification_codes SET status='revoked' WHERE id=?",
            (code_id,)
        )
        db.commit()
        return True

    @staticmethod
    def list_active_codes(family_user_id: str) -> list:
        """查看家属的活跃验证码列表"""
        db = get_db()
        rows = db.execute(
            """SELECT vc.*, d.device_name, e.name as elderly_name
               FROM device_verification_codes vc
               JOIN devices d ON vc.device_id = d.id
               JOIN elderly e ON vc.elderly_id = e.id
               WHERE vc.family_user_id = ? AND vc.status = 'active'
               ORDER BY vc.created_at DESC""",
            (family_user_id,)
        ).fetchall()
        return [dict(r) for r in rows]
