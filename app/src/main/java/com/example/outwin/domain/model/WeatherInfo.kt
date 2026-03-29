package com.example.outwin.domain.model

data class WeatherInfo(
    val cityName: String,
    val currentTemp: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val recommendationText: String,
    val carWashAdvice: String,

    val feelsLike: Int,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val visibility: Int,
    val pop: Int,
    val sunrise: String,
    val sunset: String
)