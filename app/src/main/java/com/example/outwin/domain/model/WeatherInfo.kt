package com.example.outwin.domain.model

data class WeatherInfo(
    val cityName: String,
    val currentTemp: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val recommendationText: String,
    val carWashAdvice: String
)