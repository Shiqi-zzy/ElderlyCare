package com.elderlycare.app.data.binding

import com.elderlycare.app.data.local.UserStore
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.UserRole

/**
 * 预置演示数据（幂等）。
 *
 * 机构：
 * - 幸福社区养老驿站（社区）
 * - 新华社区医院（医院）
 *
 * 演示工作人员账号（密码统一 [DEMO_PASSWORD]，演示用明文）：
 * - 张三 13800000001（社区）
 * - 李四 13800000002（社区）
 * - 王医生 13900000001（医院）
 * - 赵护士 13900000002（医院）
 *
 * 重复启动不会创建重复机构/账号。本阶段不 seed 告警/老人档案/绑定关系。
 */
class SeedData(
    private val userStore: UserStore,
    private val bindingDao: BindingDao
) {

    companion object {
        const val COMMUNITY_ORG_ID = "org_community_01"
        const val HOSPITAL_ORG_ID = "org_hospital_01"

        /** 演示账号统一密码（演示用，生产需后端加盐哈希） */
        const val DEMO_PASSWORD = "123456"
    }

    suspend fun ensureSeeded() {
        // 机构幂等：整表为空才写入
        if (bindingDao.countOrganizations() == 0) {
            val now = System.currentTimeMillis()
            bindingDao.insertOrganizations(
                listOf(
                    OrganizationEntity(
                        id = COMMUNITY_ORG_ID,
                        name = "幸福社区养老驿站",
                        type = OrganizationType.COMMUNITY.name,
                        createdAt = now
                    ),
                    OrganizationEntity(
                        id = HOSPITAL_ORG_ID,
                        name = "新华社区医院",
                        type = OrganizationType.HOSPITAL.name,
                        createdAt = now
                    )
                )
            )
        }

        // 演示账号幂等：按手机号去重
        val now = System.currentTimeMillis()
        registerIfAbsent(
            AppUser(
                phone = "13800000001",
                name = "张三",
                password = DEMO_PASSWORD,
                role = UserRole.COMMUNITY,
                organizationId = COMMUNITY_ORG_ID,
                createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13800000002",
                name = "李四",
                password = DEMO_PASSWORD,
                role = UserRole.COMMUNITY,
                organizationId = COMMUNITY_ORG_ID,
                createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13900000001",
                name = "王医生",
                password = DEMO_PASSWORD,
                role = UserRole.HOSPITAL,
                organizationId = HOSPITAL_ORG_ID,
                createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13900000002",
                name = "赵护士",
                password = DEMO_PASSWORD,
                role = UserRole.HOSPITAL,
                organizationId = HOSPITAL_ORG_ID,
                createdAt = now
            )
        )
    }

    private suspend fun registerIfAbsent(user: AppUser) {
        if (userStore.getStaffByPhone(user.phone) == null) {
            userStore.register(user)
        }
    }
}
