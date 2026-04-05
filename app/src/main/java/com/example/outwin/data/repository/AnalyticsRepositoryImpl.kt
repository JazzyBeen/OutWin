package com.example.outwin.data.repository

import android.content.Context
import com.example.outwin.domain.repository.AnalyticsRepository
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class AnalyticsRepositoryImpl(private val context: Context) : AnalyticsRepository {
    private val database = FirebaseDatabase.getInstance()
    private val reactionsRef = database.getReference("reactions")

    private val eventsRef = database.getReference("analytics/events")
    private val presenceRef = database.getReference("analytics/presence")
    private val notifRef = database.getReference("analytics/notifications")

    private val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
    private val userId: String
        get() {
            var id = prefs.getString("user_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("user_id", id).apply()
            }
            return id
        }

    override fun getReactionCounters(): Flow<Map<String, Long>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Long>()
                snapshot.children.forEach { child -> map[child.key ?: ""] = child.getValue(Long::class.java) ?: 0L }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        reactionsRef.addValueEventListener(listener)
        awaitClose { reactionsRef.removeEventListener(listener) }
    }

    override suspend fun incrementReaction(reactionId: String) = updateCounter(reactionsRef, reactionId, 1L)
    override suspend fun decrementReaction(reactionId: String) = updateCounter(reactionsRef, reactionId, -1L)

    override suspend fun logAppOpen(city: String) {
        val event = mapOf(
            "userId" to userId,
            "timestamp" to ServerValue.TIMESTAMP,
            "city" to city,
            "eventType" to "app_open"
        )
        eventsRef.push().setValue(event)
    }

    override suspend fun setOnlineStatus(isOnline: Boolean) {
        val userPresenceRef = presenceRef.child(userId)
        if (isOnline) {
            userPresenceRef.setValue(mapOf("online" to true, "last_active" to ServerValue.TIMESTAMP))
            userPresenceRef.onDisconnect().setValue(mapOf("online" to false, "last_active" to ServerValue.TIMESTAMP))
        } else {
            userPresenceRef.setValue(mapOf("online" to false, "last_active" to ServerValue.TIMESTAMP))
            userPresenceRef.onDisconnect().cancel()
        }
    }

    override suspend fun saveNotificationPrefs(days: Set<Int>) {
        notifRef.child(userId).setValue(days.toList())
    }

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