package com.example.outwin.data.repository

import android.content.Context
import android.location.Location
import com.example.outwin.data.api.WeatherApiService
import com.example.outwin.domain.model.DailyForecast
import com.example.outwin.domain.model.WeatherInfo
import com.example.outwin.domain.repository.WeatherRepository
import com.example.outwin.domain.usecase.WeatherRecommendationHelper
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
    context: Context
) : WeatherRepository {

    private val apiKey = "ваш_ключ"

    private val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherInfo> {
        val lastLat = prefs.getFloat("last_lat", Float.MAX_VALUE).toDouble()
        val lastLon = prefs.getFloat("last_lon", Float.MAX_VALUE).toDouble()
        val lastTime = prefs.getLong("last_time", 0L)
        val cachedData = prefs.getString("weather_data", null)

        val results = FloatArray(1)
        Location.distanceBetween(lat, lon, lastLat, lastLon, results)
        val distanceMeters = results[0]

        val isCacheValid = distanceMeters < 3000 && (System.currentTimeMillis() - lastTime) < 3600000

        if (cachedData != null && isCacheValid) {
            try {
                val weatherInfo = gson.fromJson(cachedData, WeatherInfo::class.java)
                if (weatherInfo.futureForecasts != null) {
                    return Result.success(weatherInfo)
                }
            } catch (e: Exception) {
            }
        }

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

            val allDates = response.list.map { it.dtTxt.substring(0, 10) }.distinct()
            val futureDates = allDates.filter { it != todayDate }.take(4)
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())

            val futureForecasts = futureDates.map { dateStr ->
                val dayData = response.list.filter { it.dtTxt.startsWith(dateStr) }
                val dAvgTemp = dayData.map { it.main.temp }.average()
                val dAvgHumidity = dayData.map { it.main.humidity }.average().toInt()
                val dAvgWindSpeed = dayData.map { it.wind.speed }.average()
                val dCondition = WeatherRecommendationHelper.getDayWeatherCondition(dayData)

                val dMinT = dayData.minOfOrNull { it.main.temp }?.toInt() ?: 0
                val dMaxT = dayData.maxOfOrNull { it.main.temp }?.toInt() ?: 0
                val dRecommendation = WeatherRecommendationHelper.getClothingRecommendation(
                    dAvgTemp, dAvgHumidity, dAvgWindSpeed, dCondition
                )

                val parsedDate = LocalDate.parse(dateStr)
                val displayDate = parsedDate.format(dateFormatter)

                DailyForecast(
                    date = displayDate,
                    minTemp = dMinT,
                    maxTemp = dMaxT,
                    recommendationText = dRecommendation
                )
            }

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
                sunset = sunsetStr,
                futureForecasts = futureForecasts
            )

            prefs.edit()
                .putFloat("last_lat", lat.toFloat())
                .putFloat("last_lon", lon.toFloat())
                .putLong("last_time", System.currentTimeMillis())
                .putString("weather_data", gson.toJson(weatherInfo))
                .apply()

            Result.success(weatherInfo)

        } catch (e: Exception) {
            if (cachedData != null) {
                try {
                    val weatherInfo = gson.fromJson(cachedData, WeatherInfo::class.java)
                    return Result.success(weatherInfo)
                } catch (ex: Exception) {}
            }
            Result.failure(e)
        }
    }
}