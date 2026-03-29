package com.example.outwin.data.repository

import com.example.outwin.domain.repository.AnalyticsRepository
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AnalyticsRepositoryImpl : AnalyticsRepository {
    private val databaseRef = FirebaseDatabase.getInstance().getReference("reactions")
    // Ссылка на новую папку со статистикой
    private val statsRef = FirebaseDatabase.getInstance().getReference("stats")

    override fun getReactionCounters(): Flow<Map<String, Long>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Long>()
                snapshot.children.forEach { child ->
                    map[child.key ?: ""] = child.getValue(Long::class.java) ?: 0L
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        databaseRef.addValueEventListener(listener)
        awaitClose { databaseRef.removeEventListener(listener) }
    }

    override suspend fun incrementReaction(reactionId: String) = updateCounter(databaseRef, reactionId, 1L)
    override suspend fun decrementReaction(reactionId: String) = updateCounter(databaseRef, reactionId, -1L)

    // --- ЛОГИРУЕМ ЗАПУСК ПРИЛОЖЕНИЯ ---
    override suspend fun logAppOpen() = updateCounter(statsRef, "app_opens", 1L)

    // Сделали универсальную функцию обновления, чтобы она принимала ссылку на папку (reactions или stats)
    private suspend fun updateCounter(ref: DatabaseReference, childId: String, delta: Long) {
        ref.child(childId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val current = mutableData.getValue(Long::class.java) ?: 0L
                val newValue = current + delta
                mutableData.value = if (newValue < 0) 0L else newValue
                return Transaction.success(mutableData)
            }
            override fun onComplete(err: DatabaseError?, b: Boolean, snapshot: DataSnapshot?) {}
        })
    }
}