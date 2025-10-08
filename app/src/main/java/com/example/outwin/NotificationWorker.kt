package com.example.outwin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val mainViewModel = MainViewModel()
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "weather_notification_channel"
    }

    override suspend fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        val lat = sharedPreferences.getFloat("lat", -1f).toDouble()
        val lon = sharedPreferences.getFloat("lon", -1f).toDouble()

        if (lat == -1.0 || lon == -1.0) {
            Log.e("NotificationWorker", "Сохраненные координаты не найдены. Уведомление не будет отправлено.")
            return Result.failure()
        }

        Log.d("NotificationWorker", "Using saved location: Lat $lat, Lon $lon")

        return try {
            val weatherData = mainViewModel.fetchWeatherBlocking(lat, lon)
            if (weatherData != null) {
                val recommendation = getRecommendationForNotification(weatherData)
                val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                showNotification(recommendation, notificationId)
                rescheduleNextNotification()
                Result.success()
            } else {
                Log.e("NotificationWorker", "Не удалось получить данные о погоде для сохраненных координат.")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Ошибка при получении погоды или рекомендации: ${e.message}")
            Result.failure()
        }
    }

    private fun getRecommendationForNotification(weatherData: WeatherData?): String {
        return if (weatherData != null && weatherData.list.isNotEmpty()) {
            val todayForecasts = weatherData.list.filter {
                it.dtTxt.startsWith(LocalDate.now().toString())
            }

            if (todayForecasts.isEmpty()) {
                return "Не удалось получить прогноз на сегодня."
            }

            val avgTemp = todayForecasts.map { it.main.temp }.average()
            val avgHumidity = todayForecasts.map { it.main.humidity }.average().toInt()
            val avgWindSpeed = todayForecasts.map { it.wind.speed }.average()
            val dayWeatherCondition = WeatherRecommendationHelper.getDayWeatherCondition(todayForecasts)

            val rawRecommendation = WeatherRecommendationHelper.getClothingRecommendation(avgTemp, avgHumidity, avgWindSpeed, dayWeatherCondition)

            WeatherRecommendationHelper.getPlainTextRecommendation(rawRecommendation)

        } else {
            "Не удалось получить рекомендацию по погоде."
        }
    }

    private fun rescheduleNextNotification() {
        val day = inputData.getString("day")
        val time = inputData.getString("time")
        if (day == null || time == null) return

        val hour = time.split(":")[0].toInt()
        val minute = time.split(":")[1].toInt()
        val dayOfWeek = getCalendarDayOfWeek(day)

        val nextNotificationTime = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, 1)
        }

        val initialDelay = nextNotificationTime.timeInMillis - System.currentTimeMillis()

        if (initialDelay > 0) {
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("WeatherNotificationTag")
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(workRequest)
        }
    }

    private fun getCalendarDayOfWeek(day: String): Int {
        return when (day) {
            "sun" -> Calendar.SUNDAY
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            else -> 0
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Notifications"
            val descriptionText = "Notifications for daily weather recommendations"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(recommendation: String, notificationId: Int) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Рекомендация по одежде")
            .setStyle(NotificationCompat.BigTextStyle().bigText(recommendation))
            .setContentText(recommendation)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
