package com.example.outwin.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.outwin.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WeatherNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getWeatherUseCase: GetWeatherUseCase

    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = getWeatherUseCase(useSavedFallback = true)
            result.onSuccess { weatherInfo ->
                NotificationHelper.showWeatherNotification(context, weatherInfo)
            }
        }
    }
}