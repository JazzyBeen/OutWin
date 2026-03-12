package com.example.outwin.domain.usecase

import android.location.Location
import com.example.outwin.domain.model.WeatherInfo
import com.example.outwin.domain.repository.LocationTracker
import com.example.outwin.domain.repository.WeatherRepository
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationTracker: LocationTracker
) {
    suspend operator fun invoke(useSavedFallback: Boolean): Result<WeatherInfo> {
        var location: Location? = locationTracker.getCurrentLocation()

        if (location != null) {
            locationTracker.saveLocation(location.latitude, location.longitude)
        } else if (useSavedFallback) {
            val saved = locationTracker.getSavedLocation()
            if (saved != null) {
                location = Location("").apply {
                    latitude = saved.first
                    longitude = saved.second
                }
            }
        }

        return if (location != null) {
            weatherRepository.getWeather(location.latitude, location.longitude)
        } else {
            Result.failure(Exception("Не удалось получить геолокацию. Пожалуйста, включите GPS."))
        }
    }
}