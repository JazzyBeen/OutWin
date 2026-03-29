package com.example.outwin.domain.repository
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getReactionCounters(): Flow<Map<String, Long>>
    suspend fun incrementReaction(reactionId: String)
    suspend fun decrementReaction(reactionId: String)

    suspend fun logAppOpen()
}