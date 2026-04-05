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
        observeCounters()
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
                        val counters = (_uiState.value as? MainUiState.Success)?.reactionCounters ?: emptyMap()
                        _uiState.value = MainUiState.Success(weatherInfo, counters)

                        analyticsRepository.logAppOpen(weatherInfo.cityName)
                    },
                    onFailure = { _uiState.value = MainUiState.Error(it.message ?: "Ошибка загрузки") }
                )
            } else {
                _uiState.value = MainUiState.Error("Геолокация недоступна")
            }
        }
    }

    fun addReaction(reactionId: String) = viewModelScope.launch { analyticsRepository.incrementReaction(reactionId) }
    fun removeReaction(reactionId: String) = viewModelScope.launch { analyticsRepository.decrementReaction(reactionId) }

    fun setOnlineStatus(isOnline: Boolean) = viewModelScope.launch { analyticsRepository.setOnlineStatus(isOnline) }
    fun updateNotificationDays(days: Set<Int>) = viewModelScope.launch { analyticsRepository.saveNotificationPrefs(days) }
    fun showError(message: String) {
        _uiState.value = MainUiState.Error(message)
    }

    private fun observeCounters() {
        viewModelScope.launch {
            analyticsRepository.getReactionCounters().collect { counters ->
                val currentState = _uiState.value
                if (currentState is MainUiState.Success) {
                    _uiState.value = currentState.copy(reactionCounters = counters)
                }
            }
        }
    }
}