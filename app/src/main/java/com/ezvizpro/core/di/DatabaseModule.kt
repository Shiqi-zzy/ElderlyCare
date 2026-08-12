package com.ezvizpro.core.di

import android.content.Context
import androidx.room.Room
import com.ezvizpro.data.local.room.AppDatabase
import com.ezvizpro.data.local.room.dao.AlarmDao
import com.ezvizpro.data.local.room.dao.ElderlyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "elderly_care_cache.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideElderlyDao(db: AppDatabase): ElderlyDao = db.elderlyDao()

    @Provides
    @Singleton
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()
}
