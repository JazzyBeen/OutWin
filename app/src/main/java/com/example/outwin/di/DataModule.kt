package com.example.outwin.di

import android.content.Context
import com.example.outwin.data.location.LocationTrackerImpl
import com.example.outwin.data.repository.AnalyticsRepositoryImpl
import com.example.outwin.data.repository.WeatherRepositoryImpl
import com.example.outwin.domain.repository.AnalyticsRepository
import com.example.outwin.domain.repository.LocationTracker
import com.example.outwin.domain.repository.WeatherRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(
        apiService: com.example.outwin.data.api.WeatherApiService,
        @ApplicationContext context: Context
    ): WeatherRepository {
        return WeatherRepositoryImpl(apiService, context)
    }

    @Provides
    @Singleton
    fun provideAnalyticsRepository(@ApplicationContext context: Context): AnalyticsRepository {
        return AnalyticsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(
        @ApplicationContext context: Context,
        fusedLocationClient: FusedLocationProviderClient
    ): LocationTracker {
        return LocationTrackerImpl(context, fusedLocationClient)
    }
}