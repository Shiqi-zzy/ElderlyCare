package com.ezvizpro.domain.usecase

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.LiveStream
import com.ezvizpro.domain.repository.LiveRepository
import javax.inject.Inject

class GetLiveAddressUseCase @Inject constructor(
    private val liveRepository: LiveRepository
) {
    suspend operator fun invoke(
        deviceSerial: String,
        channelNo: Int = 1,
        code: String? = null
    ): NetworkResult<LiveStream> {
        return liveRepository.getLiveAddress(deviceSerial, channelNo, code = code)
    }
}
