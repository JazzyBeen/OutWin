package com.example.outwin.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import androidx.activity.viewModels

import com.example.outwin.presentation.notifications.NotificationActivity
import com.example.outwin.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var currentTempTextMIN: TextView
    private lateinit var currentTempTextMAX: TextView
    private lateinit var currentTempText: TextView
    private lateinit var myImageButton: ImageButton
    private lateinit var counterTextView: TextView
    private lateinit var notificationArrowButton: Button
    private lateinit var carText: TextView
    private lateinit var welcomeText: TextView
    private lateinit var clothingAdviceText: TextView
    private lateinit var mainContentGroup: ConstraintLayout
    private lateinit var loadingAnimationView: ImageView

    private var forwardAnimationDrawable: AnimationDrawable? = null
    private var reverseAnimationDrawable: AnimationDrawable? = null
    private var loadingAnimation: AnimationDrawable? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.fetchWeather(useSavedLocationFallback = false)
            } else {
                Toast.makeText(this, "Нужен доступ к GPS для прогноза", Toast.LENGTH_LONG).show()
                showErrorState("Пожалуйста, разрешите доступ к геолокации в настройках.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        initViews()
        setupAnimations()
        setupListeners()
        observeViewModel()

        checkLocationPermissionAndFetchWeather()
    }

    private fun initViews() {
        myImageButton = findViewById(R.id.myImageButton)
        mainContentGroup = findViewById(R.id.mainContentGroup)
        currentTempTextMIN = findViewById(R.id.currentTempTextMIN)
        currentTempTextMAX = findViewById(R.id.currentTempTextMAX)
        currentTempText = findViewById(R.id.currentTempText)
        carText = findViewById(R.id.carText)
        welcomeText = findViewById(R.id.WelcomeText)
        loadingAnimationView = findViewById(R.id.loadingAnimationView)
        notificationArrowButton = findViewById(R.id.notification_arrow_button)
        clothingAdviceText = findViewById(R.id.clothingAdviceText)
        counterTextView = findViewById(R.id.counterTextView)
    }

    private fun setupAnimations() {
        loadingAnimationView.setBackgroundResource(R.drawable.my_animation_loud)
        loadingAnimation = loadingAnimationView.background as? AnimationDrawable

        forwardAnimationDrawable = ContextCompat.getDrawable(this, R.drawable.my_animation) as? AnimationDrawable
        reverseAnimationDrawable = ContextCompat.getDrawable(this, R.drawable.my_animation2) as? AnimationDrawable

        myImageButton.setBackgroundResource(R.drawable.img0001)
    }

    private fun setupListeners() {
        myImageButton.setOnClickListener {
            viewModel.toggleCounter()
        }

        notificationArrowButton.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MainUiState.Loading -> showLoading()
                        is MainUiState.Success -> showContent(state)
                        is MainUiState.Error -> showErrorState(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        mainContentGroup.visibility = View.GONE
        loadingAnimationView.visibility = View.VISIBLE
        loadingAnimation?.start()
    }

    private fun showErrorState(message: String) {
        loadingAnimation?.stop()
        loadingAnimationView.visibility = View.GONE
        mainContentGroup.visibility = View.VISIBLE
        clothingAdviceText.text = message
        welcomeText.text = "Ошибка"
    }

    private fun showContent(state: MainUiState.Success) {
        loadingAnimation?.stop()
        loadingAnimationView.visibility = View.GONE

        if (mainContentGroup.visibility == View.GONE) {
            mainContentGroup.visibility = View.VISIBLE
            mainContentGroup.alpha = 0f
            mainContentGroup.animate().alpha(1f).setDuration(500).start()
        }

        val greeting = getGreetingBasedOnTime()
        welcomeText.text = "$greeting,\n${state.weatherInfo.cityName}!"

        currentTempText.text = "${state.weatherInfo.currentTemp}°"
        currentTempTextMIN.text = "${state.weatherInfo.minTemp}°"
        currentTempTextMAX.text = "${state.weatherInfo.maxTemp}°"
        carText.text = state.weatherInfo.carWashAdvice

        // Поддержка HTML разметки из хелпера
        clothingAdviceText.text = HtmlCompat.fromHtml(
            state.weatherInfo.recommendationText,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        handleCounterState(state.isCounterActive, state.counter)
    }

    private fun handleCounterState(isActive: Boolean, count: Long) {
        if (isActive) {
            // Запуск анимации "включения"
            if (myImageButton.background != forwardAnimationDrawable) {
                myImageButton.background = forwardAnimationDrawable
            }
            forwardAnimationDrawable?.stop() // Сброс кадра
            forwardAnimationDrawable?.start()

            counterTextView.text = "Рекомендации помогли $count раз пользователям"
            if (counterTextView.visibility != View.VISIBLE) {
                counterTextView.alpha = 0f
                counterTextView.visibility = View.VISIBLE
                counterTextView.animate().alpha(1f).setDuration(500).start()
            }
        } else {
            // Запуск анимации "выключения"
            if (myImageButton.background != reverseAnimationDrawable) {
                myImageButton.background = reverseAnimationDrawable
            }
            reverseAnimationDrawable?.stop()
            reverseAnimationDrawable?.start()

            if (counterTextView.visibility == View.VISIBLE) {
                counterTextView.animate().alpha(0f).setDuration(500).withEndAction {
                    counterTextView.visibility = View.INVISIBLE
                }.start()
            }
        }
    }

    private fun checkLocationPermissionAndFetchWeather() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            viewModel.fetchWeather(useSavedLocationFallback = true)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getGreetingBasedOnTime(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }

    override fun onDestroy() {
        loadingAnimation?.stop()
        super.onDestroy()
    }
}