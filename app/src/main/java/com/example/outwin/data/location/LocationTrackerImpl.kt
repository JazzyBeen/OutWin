package com.example.outwin.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.outwin.domain.repository.LocationTracker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationTrackerImpl(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationTracker {

    private val sharedPreferences = context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return null
        }

        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()

            location ?: fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    override fun getSavedLocation(): Pair<Double, Double>? {
        val lat = sharedPreferences.getFloat("lat", -1f).toDouble()
        val lon = sharedPreferences.getFloat("lon", -1f).toDouble()
        return if (lat != -1.0 && lon != -1.0) Pair(lat, lon) else null
    }

    override fun saveLocation(lat: Double, lon: Double) {
        sharedPreferences.edit()
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .apply()
    }
}