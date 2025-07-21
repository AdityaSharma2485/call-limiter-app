package com.example.calllimiter.di

import com.example.calllimiter.data.AppDatabase
import com.example.calllimiter.data.SettingsRepository
import com.example.calllimiter.domain.CallLimiterUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CallLimiterEntryPoint {
    fun getAppDatabase(): AppDatabase
    fun getSettingsRepository(): SettingsRepository
    fun getCallLimiterUseCase(): CallLimiterUseCase // Add this line
}