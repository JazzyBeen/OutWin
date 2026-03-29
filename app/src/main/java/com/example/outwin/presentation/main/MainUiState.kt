package com.example.outwin.presentation.main

import com.example.outwin.domain.model.WeatherInfo

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(
        val weatherInfo: WeatherInfo,
        val reactionCounters: Map<String, Long> = emptyMap()
    ) : MainUiState()
    data class Error(val message: String) : MainUiState()
}