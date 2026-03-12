package com.example.outwin.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.outwin.domain.repository.AnalyticsRepository
import com.example.outwin.domain.repository.LocationTracker
import com.example.outwin.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeCounter()
    }

    fun fetchWeather(useSavedLocationFallback: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            var location = locationTracker.getCurrentLocation()

            if (location != null) {
                locationTracker.saveLocation(location.latitude, location.longitude)
            } else if (useSavedLocationFallback) {
                val saved = locationTracker.getSavedLocation()
                if (saved != null) {
                    location = android.location.Location("").apply {
                        latitude = saved.first
                        longitude = saved.second
                    }
                }
            }

            if (location != null) {
                weatherRepository.getWeather(location.latitude, location.longitude).fold(
                    onSuccess = { weatherInfo ->
                        val currentState = _uiState.value
                        val isActive = if (currentState is MainUiState.Success) currentState.isCounterActive else false
                        _uiState.value = MainUiState.Success(weatherInfo, 0L, isActive)
                        observeCounter()
                    },
                    onFailure = {
                        _uiState.value = MainUiState.Error(it.message ?: "Ошибка загрузки")
                    }
                )
            } else {
                _uiState.value = MainUiState.Error("Геолокация недоступна")
            }
        }
    }

    fun toggleCounter() {
        val currentState = _uiState.value as? MainUiState.Success ?: return
        val newStateActive = !currentState.isCounterActive
        _uiState.update { currentState.copy(isCounterActive = newStateActive) }

        viewModelScope.launch {
            if (newStateActive) analyticsRepository.incrementCounter()
            else analyticsRepository.decrementCounter()
        }
    }

    private fun observeCounter() {
        viewModelScope.launch {
            analyticsRepository.getRecommendationCounter().collect { count ->
                val currentState = _uiState.value
                if (currentState is MainUiState.Success) {
                    _uiState.value = currentState.copy(counter = count)
                }
            }
        }
    }
}