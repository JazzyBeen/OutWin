package com.example.outwin.presentation.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.outwin.R
import com.example.outwin.worker.WeatherNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
@AndroidEntryPoint
class NotificationActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                scheduleDailyWeatherNotification()
            } else {
                Toast.makeText(this, "Нужно разрешение для отправки уведомлений", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val backButton: ImageButton = findViewById(R.id.BackBotom)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun checkPermissionsAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                scheduleDailyWeatherNotification()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleDailyWeatherNotification()
        }
    }

    private fun scheduleDailyWeatherNotification() {
        val workRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyWeatherWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Toast.makeText(this, "Ежедневные советы по одежде включены!", Toast.LENGTH_SHORT).show()
    }
}