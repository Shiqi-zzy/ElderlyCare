package com.ezvizpro.domain.repository

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.AlarmMessage

/**
 * 报警消息仓库接口
 */
interface AlarmRepository {

    /**
     * 获取报警消息列表（分页）
     */
    suspend fun getAlarmList(
        pageStart: Int = 0,
        pageSize: Int = 20,
        alarmType: Int? = null
    ): NetworkResult<List<AlarmMessage>>

    /**
     * 标记消息为已读
     */
    suspend fun markAsRead(alarmId: String): NetworkResult<Unit>
}
