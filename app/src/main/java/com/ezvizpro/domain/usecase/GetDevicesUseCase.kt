package com.ezvizpro.domain.usecase

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.Device
import com.ezvizpro.domain.repository.DeviceRepository
import javax.inject.Inject

class GetDevicesUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(): NetworkResult<List<Device>> {
        return deviceRepository.getDeviceList()
    }
}
