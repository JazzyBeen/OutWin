package com.example.outwin

import com.google.gson.annotations.SerializedName

data class WeatherData(
    @SerializedName("list") val list: List<ForecastItem>,
    @SerializedName("city") val city: City
)

data class ForecastItem(
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("dt_txt") val dtTxt: String
)

data class Main(
    @SerializedName("temp") val temp: Double,
    @SerializedName("humidity") val humidity: Int
)

data class Weather(
    @SerializedName("description") val description: String,
    @SerializedName("main") val main: String
)

data class Wind(
    @SerializedName("speed") val speed: Double
)

data class City(
    @SerializedName("name") val name: String
)