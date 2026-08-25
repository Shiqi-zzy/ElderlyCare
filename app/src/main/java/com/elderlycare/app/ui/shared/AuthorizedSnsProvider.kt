package com.elderlycare.app.ui.shared

import com.elderlycare.app.data.ezviz.ServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 角色感知的「当前用户可访问设备 SN 集合」统一数据源。
 *
 * 云端 getAlarmList 按 AppKey 账号全量返回、无 deviceSn 参数，各消费方（告警列表、
 * 消息中心报警落库、告警 WS 订阅上报）需要按设备 SN 过滤——本对象从 AlarmListViewModel 原样抽出：
 * 家属 = 本人档案 deviceSn；社区/医院 = 本人 ACTIVE 绑定照护对象的 deviceSn
 * （REVOKED 解绑 → 授权集合实时缩小）；无人登录（异常态）发射空集。
 *
 * 响应式：以家属/员工登录态（DataStore Flow）驱动分支切换，
 * 家属↔员工切换账号时自动重发新角色对应的 SN 集合。
 */
object AuthorizedSnsProvider {

    fun flow(): Flow<Set<String>> =
        combine(
            ServiceLocator.userStore.currentUserId,
            ServiceLocator.staffUserStore.currentStaffId
        ) { familyUid, staffId -> familyUid to staffId }
            .flatMapLatest { (familyUid, staffId) ->
                when {
                    familyUid != null -> {
                        // 家属：本人档案的 deviceSn
                        ServiceLocator.profileStore.observeProfiles()
                            .map { profiles ->
                                profiles.filter { it.userId == familyUid }
                                    .mapNotNull { it.deviceSn.takeIf { sn -> sn.isNotBlank() } }
                                    .toSet()
                            }
                    }
                    staffId != null -> {
                        // 社区/医院：本人 ACTIVE 绑定照护对象的 deviceSn（REVOKED 实时消失）
                        val staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
                        if (staff != null) {
                            ServiceLocator.bindingRepository.observeAccessibleElderly(staff)
                                .map { list ->
                                    list.mapNotNull { it.profile.deviceSn.takeIf { sn -> sn.isNotBlank() } }.toSet()
                                }
                        } else {
                            flowOf(emptySet())
                        }
                    }
                    else -> flowOf(emptySet())
                }
            }
}
