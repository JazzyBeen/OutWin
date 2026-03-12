package com.example.outwin.data.repository

import com.example.outwin.data.api.WeatherApiService
import com.example.outwin.domain.model.WeatherInfo
import com.example.outwin.domain.repository.WeatherRepository
import com.example.outwin.domain.usecase.WeatherRecommendationHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WeatherRepositoryImpl(
    private val apiService: WeatherApiService
) : WeatherRepository {

    private val apiKey = "YOUR_API"

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherInfo> {
        return try {
            val response = apiService.getForecast(lat, lon, apiKey)
            if (response.list.isEmpty()) throw Exception("Нет данных о прогнозе")

            val currentForecast = response.list.first()
            val todayDate = LocalDate.parse(
                currentForecast.dtTxt,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ).toString()

            val todayForecasts = response.list.filter { it.dtTxt.startsWith(todayDate) }
            if (todayForecasts.isEmpty()) throw Exception("Нет данных на сегодня")

            val avgTemp = todayForecasts.map { it.main.temp }.average()
            val avgHumidity = todayForecasts.map { it.main.humidity }.average().toInt()
            val avgWindSpeed = todayForecasts.map { it.wind.speed }.average()
            val condition = WeatherRecommendationHelper.getDayWeatherCondition(todayForecasts)

            val minTemp = todayForecasts.minOfOrNull { it.main.temp }?.toInt() ?: 0
            val maxTemp = todayForecasts.maxOfOrNull { it.main.temp }?.toInt() ?: 0
            val recommendation = WeatherRecommendationHelper.getClothingRecommendation(
                avgTemp, avgHumidity, avgWindSpeed, condition
            )

            val weatherInfo = WeatherInfo(
                cityName = response.city.name,
                currentTemp = currentForecast.main.temp.toInt(),
                minTemp = minTemp,
                maxTemp = maxTemp,
                recommendationText = recommendation,
                carWashAdvice = if (condition == "Осадки") "Не стоит мыть машину" else "Можно ехать на мойку"
            )
            Result.success(weatherInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}