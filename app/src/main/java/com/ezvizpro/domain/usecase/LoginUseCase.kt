package com.ezvizpro.domain.usecase

import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        appKey: String,
        appSecret: String
    ): NetworkResult<String> {
        return authRepository.login(appKey, appSecret)
    }
}
