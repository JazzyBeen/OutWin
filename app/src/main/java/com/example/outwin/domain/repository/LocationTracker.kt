package com.example.outwin.domain.repository
import android.location.Location

interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
    fun getSavedLocation(): Pair<Double, Double>?
    fun saveLocation(lat: Double, lon: Double)
}