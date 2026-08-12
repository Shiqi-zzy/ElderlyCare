package com.ezvizpro.domain.usecase

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.PtzDirection
import com.ezvizpro.domain.model.PtzSpeed
import com.ezvizpro.domain.repository.LiveRepository
import javax.inject.Inject

class ControlPTZUseCase @Inject constructor(
    private val liveRepository: LiveRepository
) {
    suspend fun start(
        deviceSerial: String,
        channelNo: Int,
        direction: PtzDirection,
        speed: PtzSpeed = PtzSpeed.NORMAL
    ): NetworkResult<Unit> {
        return liveRepository.startPtz(deviceSerial, channelNo, direction, speed)
    }

    suspend fun stop(
        deviceSerial: String,
        channelNo: Int,
        direction: PtzDirection? = null
    ): NetworkResult<Unit> {
        return liveRepository.stopPtz(deviceSerial, channelNo, direction)
    }
}
