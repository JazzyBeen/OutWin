package com.example.outwin.data.repository

import com.example.outwin.domain.repository.AnalyticsRepository
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AnalyticsRepositoryImpl : AnalyticsRepository {
    private val databaseRef = FirebaseDatabase.getInstance().getReference("globalCounter")

    override fun getRecommendationCounter(): Flow<Long> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Long::class.java) ?: 0L)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        databaseRef.addValueEventListener(listener)
        awaitClose { databaseRef.removeEventListener(listener) }
    }

    override suspend fun incrementCounter() {
        updateCounter(1)
    }

    override suspend fun decrementCounter() {
        updateCounter(-1)
    }

    private suspend fun updateCounter(delta: Long) {
        databaseRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val current = mutableData.getValue(Long::class.java) ?: 0L
                mutableData.value = current + delta
                return Transaction.success(mutableData)
            }
            override fun onComplete(err: DatabaseError?, b: Boolean, snapshot: DataSnapshot?) {}
        })
    }
}