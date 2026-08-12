package com.ezvizpro.domain.repository

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.Device

/**
 * 设备仓库接口
 */
interface DeviceRepository {

    /**
     * 获取设备列表
     */
    suspend fun getDeviceList(): NetworkResult<List<Device>>

    /**
     * 获取单个设备详情（含能力集）
     */
    suspend fun getDeviceDetail(deviceSerial: String): NetworkResult<Device>

    /**
     * 修改设备名称
     */
    suspend fun updateDeviceName(deviceSerial: String, name: String): NetworkResult<Unit>
}
