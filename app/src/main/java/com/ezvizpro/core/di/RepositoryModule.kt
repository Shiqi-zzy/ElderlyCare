package com.ezvizpro.core.di

import com.ezvizpro.data.repository.AlarmRepositoryImpl
import com.ezvizpro.data.repository.AuthRepositoryImpl
import com.ezvizpro.data.repository.DeviceRepositoryImpl
import com.ezvizpro.data.repository.LiveRepositoryImpl
import com.ezvizpro.data.repository.PlaybackRepositoryImpl
import com.ezvizpro.domain.repository.AlarmRepository
import com.ezvizpro.domain.repository.AuthRepository
import com.ezvizpro.domain.repository.DeviceRepository
import com.ezvizpro.domain.repository.LiveRepository
import com.ezvizpro.domain.repository.PlaybackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindLiveRepository(impl: LiveRepositoryImpl): LiveRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(impl: PlaybackRepositoryImpl): PlaybackRepository

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(impl: AlarmRepositoryImpl): AlarmRepository
}
