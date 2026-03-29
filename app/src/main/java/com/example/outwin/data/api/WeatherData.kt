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
    @SerializedName("pop") val pop: Double?, // Вероятность осадков (от 0 до 1)
    @SerializedName("visibility") val visibility: Int? // Видимость в метрах
)

data class Main(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double, // Ощущается как
    @SerializedName("pressure") val pressure: Int, // Давление в гПа
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
    @SerializedName("sunrise") val sunrise: Long?, // Восход (Unix)
    @SerializedName("sunset") val sunset: Long?,   // Закат (Unix)
    @SerializedName("timezone") val timezone: Int? // Сдвиг часового пояса (секунды)
)