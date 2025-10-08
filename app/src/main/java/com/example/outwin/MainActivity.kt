package com.example.outwin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.LocalTime
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.location.Priority
import com.google.android.gms.common.api.ResolvableApiException
import android.content.IntentSender
import androidx.activity.result.launch
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job


import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentTempTextMIN: TextView
    private lateinit var currentTempTextMAX: TextView
    private lateinit var myImageButton: ImageButton
    private lateinit var counterTextView: TextView
    private lateinit var databaseReference: DatabaseReference
    private var forwardAnimationDrawable: AnimationDrawable? = null
    private var reverseAnimationDrawable: AnimationDrawable? = null
    private lateinit var notificationArrowButton: Button
    private lateinit var carText: TextView
    private lateinit var Welcome: TextView
    private var k = 0
    private var p = 0
    private lateinit var mainContentGroup: ConstraintLayout
    private var loadingAnimation: AnimationDrawable? = null
    private lateinit var loadingAnimationView: ImageView
    private lateinit var currentTempText: TextView
    private lateinit var clothingAdviceText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myImageButton = findViewById(R.id.myImageButton)

        mainContentGroup = findViewById(R.id.mainContentGroup)
        currentTempTextMIN = findViewById(R.id.currentTempTextMIN)
        currentTempTextMAX = findViewById(R.id.currentTempTextMAX)
        carText = findViewById(R.id.carText)
        Welcome = findViewById(R.id.WelcomeText)
        loadingAnimationView = findViewById(R.id.loadingAnimationView)
        notificationArrowButton = findViewById(R.id.notification_arrow_button)
        currentTempText = findViewById(R.id.currentTempText)
        clothingAdviceText = findViewById(R.id.clothingAdviceText)
        counterTextView = findViewById(R.id.counterTextView)
        databaseReference = FirebaseDatabase.getInstance().getReference("globalCounter")
        loadingAnimationView.setBackgroundResource(R.drawable.my_animation_loud)
        loadingAnimation = loadingAnimationView.background as? AnimationDrawable


        if (loadingAnimationView.background is AnimationDrawable) {
            loadingAnimation = loadingAnimationView.background as AnimationDrawable
        }



        showLoading()



        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val currentCount = snapshot.getValue(Long::class.java)
                if (p == 1) {
                    counterTextView.text = "Рекомендации помогли "  + (currentCount ?: 0).toString() + " раз пользователям"
                    animateTextAppear()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                println("Ошибка чтения данных базы данных: ${error.message}")
            }
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val initialBackground = myImageButton.background
        if (initialBackground is AnimationDrawable) {
            forwardAnimationDrawable = initialBackground
        } else {

            try {
                forwardAnimationDrawable = ContextCompat.getDrawable(this, R.drawable.my_animation) as? AnimationDrawable

                if (myImageButton.background != forwardAnimationDrawable && forwardAnimationDrawable != null) {
                    myImageButton.background = forwardAnimationDrawable
                }
            } catch (e: Exception) {

            }
        }

        try {
            reverseAnimationDrawable = ContextCompat.getDrawable(this, R.drawable.my_animation2) as? AnimationDrawable

        } catch (e: Exception) {

        }


        myImageButton.setBackgroundResource(R.drawable.img0001)
        myImageButton.setOnClickListener {
            p = 1
            if (k == 0) {
                if (myImageButton.background != forwardAnimationDrawable) {
                    myImageButton.background = forwardAnimationDrawable
                }
                forwardAnimationDrawable?.start()
                databaseReference.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        var currentCount = mutableData.getValue(Long::class.java)

                        if (currentCount == null) {
                            currentCount = 0L
                        }

                        mutableData.value = currentCount + 1
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(
                        databaseError: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (databaseError != null) {
                            println("Ошибка транзакции: ${databaseError.message}")
                        }
                    }
                })
                k = 1
            } else {
                forwardAnimationDrawable?.stop()
                if (myImageButton.background != reverseAnimationDrawable) {
                    myImageButton.background = reverseAnimationDrawable
                }
                reverseAnimationDrawable?.start()
                databaseReference.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        var currentCount =   mutableData.getValue(Long::class.java)

                        if (currentCount == null) {
                            currentCount = 0L
                        }

                        mutableData.value = currentCount!! - 1
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(
                        databaseError: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (databaseError != null) {
                            println("Ошибка транзакции: ${databaseError.message}")
                        }
                    }
                })
                k = 0
            }

        }

        notificationArrowButton.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }

        setupObservers()
        checkLocationPermissionAndFetchWeather()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                fetchLastLocation()
            } else {
                Toast.makeText(this, "Разрешение на геолокацию отклонено.", Toast.LENGTH_LONG).show()

                Welcome.text = "Привет!"
                clothingAdviceText.text = "Не удалось получить данные о погоде. Пожалуйста, предоставьте доступ к геолокации."
            }
        }

    private fun showLoading() {
        mainContentGroup.visibility = View.GONE
        loadingAnimationView.visibility = View.VISIBLE
        loadingAnimation?.start()
    }

    private fun showContent() {
        loadingAnimation?.stop()
        loadingAnimationView.visibility = View.GONE
        mainContentGroup.visibility = View.VISIBLE


        mainContentGroup.alpha = 0f
        mainContentGroup.animate().alpha(1f).setDuration(500).start()
    }
    override fun onDestroy() {
        super.onDestroy()
        loadingAnimation?.stop()
    }

    private fun checkLocationPermissionAndFetchWeather() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchLastLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun fetchLastLocation() {
        showLoading()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val sharedPreferences = getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putFloat("lat", location.latitude.toFloat())
                    putFloat("lon", location.longitude.toFloat())
                    apply()
                }
                Log.d("MainActivity", "Location saved: Lat ${location.latitude}, Lon ${location.longitude}")

                mainViewModel.fetchWeather(location.latitude, location.longitude)
            } else {
                val locationRequest = LocationRequest.create().apply {
                    priority = Priority.PRIORITY_HIGH_ACCURACY
                }

                val builder = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)



                val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)

                settingsClient.checkLocationSettings(builder.build())
                    .addOnSuccessListener {
                        val cancellationTokenSource =
                            CancellationTokenSource()

                        fusedLocationClient.getCurrentLocation(locationRequest.priority, cancellationTokenSource.token)
                            .addOnSuccessListener { currentLocation ->

                                    mainViewModel.fetchWeather(
                                        currentLocation.latitude,
                                        currentLocation.longitude
                                    )
                                }

                    }
                    .addOnFailureListener { exception ->
                        if (exception is ResolvableApiException) {



                                try {

                                    exception.startResolutionForResult(this@MainActivity, 1001)


                                } catch (sendEx: IntentSender.SendIntentException) {
                                    sendEx.printStackTrace()
                                }

                        } else {
                            val sharedPreferences = applicationContext.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
                            val lat = sharedPreferences.getFloat("lat", -1f).toDouble()
                            val lon = sharedPreferences.getFloat("lon", -1f).toDouble()
                            mainViewModel.fetchWeather(lat, lon)
                            Toast.makeText(this, "Используем сохранённое местоположение1.", Toast.LENGTH_LONG).show()
                        }
                    }

            }

        }.addOnFailureListener {
            Toast.makeText(this, "Ошибка при получении местоположения.", Toast.LENGTH_LONG).show()
        }
    }
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {

                fetchLastLocation()
            } else {
                val sharedPreferences = applicationContext.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
                val lat = sharedPreferences.getFloat("lat", -1f).toDouble()
                val lon = sharedPreferences.getFloat("lon", -1f).toDouble()
                mainViewModel.fetchWeather(lat, lon)
                Toast.makeText(this, "Используем сохранённое местоположение.", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun setupObservers() {
        mainViewModel.weatherData.observe(this) { data ->
            updateUI(data)
        }
        mainViewModel.error.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            clothingAdviceText.text = errorMessage
        }
    }

    private fun getGreetingBasedOnTime(): String {
        val currentTime = LocalTime.now()
        val currentHour = currentTime.hour

        return when (currentHour) {
            in 0..3 -> "Доброй ночи"
            in 4..11 -> "Доброе утро"
            in 12..15 -> "Добрый день"
            in 16..23 -> "Добрый вечер"
            else -> "Привет"
        }
    }

    fun animateTextAppear() {
        counterTextView.apply {
            if (alpha == 1f && visibility == View.VISIBLE  && (k == 0)) {
                animate()
                    .alpha(0f)
                    .setDuration(500)
                    .start()
            } else {
                alpha = 0f
                if (k == 1) {visibility = View.VISIBLE}

                animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setListener(null)
                    .start()
            }
        }
    }

    private fun updateUI(data: WeatherData) {
        if (data.list.isEmpty()) {
            Toast.makeText(this, "Нет данных о прогнозе.", Toast.LENGTH_SHORT).show()
            return
        }
        showContent()
        val greeting = getGreetingBasedOnTime()
        Welcome.text = "$greeting,\n${data.city.name}!"

        val currentForecast = data.list.first()
        val currentTemp = currentForecast.main.temp.toInt()
        currentTempText.text = "${currentTemp}°"

        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val todayDate = LocalDate.parse(currentForecast.dtTxt, dateTimeFormatter).toString()

        val todayForecasts = data.list.filter {
            it.dtTxt.startsWith(todayDate)
        }

        if (todayForecasts.isEmpty()) {
            Toast.makeText(this, "Нет данных о прогнозе на сегодня.", Toast.LENGTH_SHORT).show()
            clothingAdviceText.text = "Не удалось загрузить прогноз на сегодня."
            return
        }

        val avgTemp = todayForecasts.map { it.main.temp }.average()
        val avgHumidity = todayForecasts.map { it.main.humidity }.average().toInt()
        val avgWindSpeed = todayForecasts.map { it.wind.speed }.average()
        val dayWeatherCondition = WeatherRecommendationHelper.getDayWeatherCondition(todayForecasts)

        val minTemp = todayForecasts.minOfOrNull { it.main.temp }?.toInt()
        val maxTemp = todayForecasts.maxOfOrNull { it.main.temp }?.toInt()

        currentTempTextMIN.text = "${minTemp}°"
        currentTempTextMAX.text = "${maxTemp}°"

        val recommendation = WeatherRecommendationHelper.getClothingRecommendation(avgTemp, avgHumidity, avgWindSpeed, dayWeatherCondition)
        clothingAdviceText.text = HtmlCompat.fromHtml(recommendation, HtmlCompat.FROM_HTML_MODE_LEGACY)


        val willHaveRainOrSnow = dayWeatherCondition == "Осадки"
        if (willHaveRainOrSnow) {
            carText.text = "Не стоит мыть машину"
        } else {
            carText.text = "Можно ехать на мойку"
        }
    }
}
