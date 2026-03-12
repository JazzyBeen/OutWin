package com.example.outwin.domain.repository
import com.example.outwin.domain.model.WeatherInfo

interface WeatherRepository {
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherInfo>
}