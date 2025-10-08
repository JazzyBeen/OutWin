package com.example.outwin

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity2 : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var saveButton: Button


    private val notificationTimes = mutableMapOf<String, String?>()
    private val gson = Gson()


    private lateinit var dayViews: Map<String, DayLayoutViews>


    data class DayLayoutViews(
        val rootLayout: LinearLayout,
        val dayTextView: TextView,
        val timeTextView: TextView,
        val actionButton: ImageButton
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                scheduleNotifications()
            } else {
                Toast.makeText(this, "Разрешение на уведомления отклонено.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        backButton = findViewById(R.id.BackBotom)

        val dayLayoutIds = mapOf(
            "mon" to R.id.monLayout,
            "tue" to R.id.tueLayout,
            "wed" to R.id.wedLayout,
            "thu" to R.id.thuLayout,
            "fri" to R.id.friLayout,
            "sat" to R.id.satLayout,
            "sun" to R.id.sunLayout
        )

        val dayNames = mapOf(
            "mon" to "Понедельник",
            "tue" to "Вторник",
            "wed" to "Среда",
            "thu" to "Четверг",
            "fri" to "Пятница",
            "sat" to "Суббота",
            "sun" to "Воскресенье"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        dayViews = dayLayoutIds.mapValues { (dayKey, layoutId) ->
            val layout = findViewById<LinearLayout>(layoutId)
            DayLayoutViews(
                rootLayout = layout,
                dayTextView = layout.findViewById(R.id.dayTextView),
                timeTextView = layout.findViewById(R.id.daytime),
                actionButton = layout.findViewById(R.id.addTimeButton)
            ).also { views ->
                views.dayTextView.text = dayNames[dayKey]
            }
        }

        dayViews.forEach { (dayKey, views) ->
            views.actionButton.setOnClickListener {
                if (notificationTimes[dayKey] != null) {
                    notificationTimes[dayKey] = null
                    updateDayView(dayKey)
                } else {
                    showTimePickerDialog(dayKey)
                }
            }
        }

        loadSettings()

        backButton.setOnClickListener {
            finish()
        }

    }
    @Override
    override fun onStop() {
        super.onStop()
        saveSettings()
        scheduleNotifications()

    }
    private fun showTimePickerDialog(dayKey: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            notificationTimes[dayKey] = time
            updateDayView(dayKey)
        }, hour, minute, true).show()
    }

    private fun updateDayView(dayKey: String) {
        val views = dayViews[dayKey] ?: return
        val time = notificationTimes[dayKey]

        if (time != null) {

            views.rootLayout.setBackgroundResource(R.drawable.background_rounded_fornew)
            views.dayTextView.setTextColor(ContextCompat.getColor(this, R.color.white_blue))
            views.timeTextView.setTextColor(ContextCompat.getColor(this, R.color.white_blue))
            views.timeTextView.text = time
            views.actionButton.setImageResource(R.drawable.outline_delete_24)
            views.actionButton.background = null
        } else {

            views.rootLayout.setBackgroundResource(R.drawable.background_rounded)
            views.dayTextView.setTextColor(ContextCompat.getColor(this, R.color.dark_blue))
            views.timeTextView.setTextColor(ContextCompat.getColor(this, R.color.dark_blue))
            views.timeTextView.text = ""
            views.actionButton.setImageResource(R.drawable.rounded_add_24)
            views.actionButton.setBackgroundResource(R.drawable.background_rounded_fornew)
        }
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("NotificationSettings", Context.MODE_PRIVATE)
        val type = object : TypeToken<MutableMap<String, String?>>() {}.type
        val savedTimesJson = sharedPreferences.getString("notification_times_single", null)

        val loadedTimes: MutableMap<String, String?> = if (savedTimesJson != null) {
            gson.fromJson(savedTimesJson, type)
        } else {
            mutableMapOf("mon" to null, "tue" to null, "wed" to null, "thu" to null, "fri" to null, "sat" to null, "sun" to null)
        }

        notificationTimes.clear()
        notificationTimes.putAll(loadedTimes)

        notificationTimes.keys.forEach { dayKey ->
            updateDayView(dayKey)
        }
    }

    private fun saveSettings() {
        val sharedPreferences = getSharedPreferences("NotificationSettings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = gson.toJson(notificationTimes)
        editor.putString("notification_times_single", json)
        editor.apply()
    }

    private fun scheduleNotifications() {
        val workManager = WorkManager.getInstance(this)
        workManager.cancelAllWorkByTag("WeatherNotificationTag")

        notificationTimes.forEach { (day, time) ->
            if (time != null) {
                val hour = time.split(":")[0].toInt()
                val minute = time.split(":")[1].toInt()
                val dayOfWeek = getCalendarDayOfWeek(day)

                val now = Calendar.getInstance()
                val nextNotificationTime = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis <= now.timeInMillis) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                val initialDelay = nextNotificationTime.timeInMillis - now.timeInMillis

                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag("WeatherNotificationTag")
                    .setInputData(Data.Builder()
                        .putString("day", day)
                        .putString("time", time)
                        .build())
                    .build()

                workManager.enqueue(workRequest)
            }
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
}
