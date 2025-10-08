package com.example.outwin


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val apiKey = "2afb7facce33051d64f487918d28bcae"
    private val weatherApiService: WeatherApiService = RetrofitClient.instance
    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getForecast(
                    lat = lat,
                    lon = lon,
                    apiKey = apiKey
                )
                _weatherData.postValue(response)
            } catch (e: Exception) {
                _error.postValue("Ошибка загрузки данных: ${e.message}")
            }
        }
    }
    suspend fun fetchWeatherBlocking(latitude: Double, longitude: Double): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                weatherApiService.getForecast(latitude, longitude, apiKey)
            } catch (e: Exception) {
                null
            }
        }
    }
}