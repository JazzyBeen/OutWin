package com.example.outwin.domain.repository
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getRecommendationCounter(): Flow<Long>
    suspend fun incrementCounter()
    suspend fun decrementCounter()
}