package com.example.outwin.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.outwin.R
import com.example.outwin.domain.model.WeatherInfo
import com.example.outwin.presentation.main.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "weather_notifications"
    private const val NOTIFICATION_ID = 1001

    fun showWeatherNotification(context: Context, weatherInfo: WeatherInfo) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Прогноз погоды",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ежедневные советы по одежде"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cleanRecommendation = weatherInfo.recommendationText
            .replace(Regex("<.*?>"), "")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Погода в ${weatherInfo.cityName}: ${weatherInfo.currentTemp}°")
            .setContentText(cleanRecommendation)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanRecommendation))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}