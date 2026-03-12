package com.example.outwin.domain.usecase

import com.example.outwin.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageCounterUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    fun observeCounter(): Flow<Long> = analyticsRepository.getRecommendationCounter()

    suspend fun increment() = analyticsRepository.incrementCounter()

    suspend fun decrement() = analyticsRepository.decrementCounter()
}