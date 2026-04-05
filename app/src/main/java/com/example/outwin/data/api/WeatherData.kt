package com.example.outwin.data.api

import com.google.gson.annotations.SerializedName

data class WeatherData(
    @SerializedName("list") val list: List<ForecastItem>,
    @SerializedName("city") val city: City
)

data class ForecastItem(
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("dt_txt") val dtTxt: String,
    @SerializedName("pop") val pop: Double?,
    @SerializedName("visibility") val visibility: Int?
)

data class Main(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("pressure") val pressure: Int,
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
    @SerializedName("name") val name: String,
    @SerializedName("sunrise") val sunrise: Long?,
    @SerializedName("sunset") val sunset: Long?,
    @SerializedName("timezone") val timezone: Int?
)