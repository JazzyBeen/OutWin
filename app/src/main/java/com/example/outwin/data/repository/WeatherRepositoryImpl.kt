package com.example.outwin.data.repository

import com.example.outwin.data.api.WeatherApiService
import com.example.outwin.domain.model.WeatherInfo
import com.example.outwin.domain.repository.WeatherRepository
import com.example.outwin.domain.usecase.WeatherRecommendationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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


            val pressureMmHg = (currentForecast.main.pressure * 0.750062).roundToInt()
            val visibilityKm = (currentForecast.visibility ?: 10000) / 1000
            val popPercent = ((currentForecast.pop ?: 0.0) * 100).roundToInt()

            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            val zoneOffset = ZoneOffset.ofTotalSeconds(response.city.timezone ?: 0)

            val sunriseStr = if (response.city.sunrise != null && response.city.sunrise > 0) {
                Instant.ofEpochSecond(response.city.sunrise).atOffset(zoneOffset).format(timeFormatter)
            } else "--:--"

            val sunsetStr = if (response.city.sunset != null && response.city.sunset > 0) {
                Instant.ofEpochSecond(response.city.sunset).atOffset(zoneOffset).format(timeFormatter)
            } else "--:--"

            val weatherInfo = WeatherInfo(
                cityName = response.city.name,
                currentTemp = currentForecast.main.temp.toInt(),
                minTemp = minTemp,
                maxTemp = maxTemp,
                recommendationText = recommendation,
                carWashAdvice = if (condition == "Осадки") "Не стоит мыть машину" else "Можно ехать на мойку",
                feelsLike = currentForecast.main.feelsLike.toInt(),
                humidity = currentForecast.main.humidity,
                pressure = pressureMmHg,
                windSpeed = currentForecast.wind.speed,
                visibility = visibilityKm,
                pop = popPercent,
                sunrise = sunriseStr,
                sunset = sunsetStr
            )
            Result.success(weatherInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}