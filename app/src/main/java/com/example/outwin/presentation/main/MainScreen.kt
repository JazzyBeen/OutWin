package com.example.outwin.presentation.main

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.outwin.R
import com.example.outwin.domain.model.WeatherInfo
import com.example.outwin.worker.WeatherNotificationScheduler
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.util.Calendar
import androidx.compose.foundation.Image
import com.example.outwin.domain.model.DailyForecast


val RobotoFont = FontFamily.Default

val TgBackground = Color(0xFF000000)
val TgRecBackground = Color(0xFF232323)
val TgCard = Color(0xFF181818)
val TgReactionBackground = Color(0xFF6D91FF)
val TgTextLightBlue = Color(0xFF759AF6)
val TgTextWhite = Color(0xFFFFFFFF)

val ReactionAnimations = listOf(
    R.raw.heart,
    R.raw.fire,
    R.raw.smile,
    R.raw.sad,
    R.raw.govno,
    R.raw.zloi,
    R.raw.thinking
)

data class ReactionSnackbarData(val count: Long, val lottieRes: Int)

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("outwin_prefs", Context.MODE_PRIVATE)

    var showPrivacyDialog by remember { mutableStateOf(!prefs.getBoolean("privacy_accepted", false)) }
    var showAnalyticsDialog by remember { mutableStateOf(!prefs.getBoolean("analytics_answered", false) && prefs.getBoolean("privacy_accepted", false)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.setOnlineStatus(true)
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.setOnlineStatus(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (locationGranted) {
            viewModel.fetchWeather(useSavedLocationFallback = false)
        } else {
            viewModel.showError("Доступ к геолокации запрещен. Приложение не может узнать погоду.")
        }
    }

    LaunchedEffect(Unit) {
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val isGranted = ContextCompat.checkSelfPermission(context, locationPermission) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            viewModel.fetchWeather(useSavedLocationFallback = true)
        } else {
            permissionLauncher.launch(arrayOf(locationPermission))
        }
    }

    val scrollState = rememberScrollState()
    var activeMenu by remember { mutableStateOf<String?>(null) }

    val scrollOffset = scrollState.value.toFloat()
    val collapseFraction = (scrollOffset / 250f).coerceIn(0f, 1f)
    val titleFontSize = (36f - (14f * collapseFraction)).sp

    val globalAlpha by animateFloatAsState(targetValue = if (activeMenu != null || showPrivacyDialog || showAnalyticsDialog) 0.3f else 1f, label = "globalAlpha")

    Box(modifier = Modifier
        .fillMaxSize()
        .background(TgBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(globalAlpha)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text("ЗаОкном", fontFamily = RobotoFont, fontWeight = FontWeight.ExtraBold, color = TgTextWhite, fontSize = titleFontSize)
                if (collapseFraction > 0.1f && uiState is MainUiState.Success) {
                    Text(
                        text = " • ${getGreetingBasedOnTime()}, ${(uiState as MainUiState.Success).weatherInfo.cityName}",
                        fontFamily = RobotoFont, fontWeight = FontWeight.Medium, color = TgTextLightBlue, fontSize = 16.sp,
                        modifier = Modifier
                            .alpha(collapseFraction)
                            .padding(start = 8.dp, bottom = 0.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is MainUiState.Loading -> SkeletonLoadingState()
                    is MainUiState.Error -> ErrorStateView(message = state.message)
                    is MainUiState.Success -> {
                        SuccessStateView(
                            state = state,
                            scrollState = scrollState,
                            activeMenu = activeMenu,
                            onMenuStateChanged = { activeMenu = it },
                            onAddReaction = { resId ->
                                val key = context.resources.getResourceEntryName(resId)
                                viewModel.addReaction(key)
                            },
                            onRemoveReaction = { resId ->
                                val key = context.resources.getResourceEntryName(resId)
                                viewModel.removeReaction(key)
                            },
                            onDaysChanged = { newDays ->
                                viewModel.updateNotificationDays(newDays)
                            },
                            onNavigateToSettings = onNavigateToSettings
                        )
                    }
                }
            }
        }

        if (activeMenu != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { activeMenu = null })
                    }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { /* Не даем закрыть по клику вне окна */ },
                containerColor = TgCard,
                title = { Text("Политика конфиденциальности", color = TgTextWhite, fontFamily = RobotoFont, fontWeight = FontWeight.Bold) },
                text = { Text("Мы ценим вашу конфиденциальность. Данное приложение собирает анонимные данные (ваша примерная геолокация на уровне города) исключительно для предоставления точного прогноза погоды и улучшения работы сервиса. Мы не собираем и не передаем ваши точные координаты третьим лицам.\n\nПродолжая использовать приложение, вы соглашаетесь с нашей Политикой конфиденциальности.", color = TgTextLightBlue, fontFamily = RobotoFont) },
                confirmButton = {
                    TextButton(onClick = {
                        prefs.edit().putBoolean("privacy_accepted", true).apply()
                        showPrivacyDialog = false
                        if (!prefs.getBoolean("analytics_answered", false)) {
                            showAnalyticsDialog = true
                        }
                    }) { Text("Принять", color = TgReactionBackground, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        Toast.makeText(context, "Без принятия политики приложение не сможет работать корректно", Toast.LENGTH_LONG).show()
                    }) { Text("Отказаться", color = Color.Gray) }
                }
            )
        }

        if (showAnalyticsDialog) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = TgCard,
                title = { Text("Улучшение приложения", color = TgTextWhite, fontFamily = RobotoFont, fontWeight = FontWeight.Bold) },
                text = { Text("Разрешите нам собирать полностью обезличенную статистику использования (например, время запуска или то, какие эмодзи-реакции вы ставите). Это поможет нам понять, какие функции нравятся вам больше всего, и сделать «ЗаОкном» еще лучше!\n\nВы всегда можете удалить свои данные в Настройках.", color = TgTextLightBlue, fontFamily = RobotoFont) },
                confirmButton = {
                    TextButton(onClick = {
                        prefs.edit().putBoolean("analytics_answered", true).apply()
                        prefs.edit().putBoolean("analytics_granted", true).apply()
                        showAnalyticsDialog = false
                    }) { Text("Разрешить", color = TgReactionBackground, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        prefs.edit().putBoolean("analytics_answered", true).apply()
                        prefs.edit().putBoolean("analytics_granted", false).apply()
                        showAnalyticsDialog = false
                    }) { Text("Нет, спасибо", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
fun SuccessStateView(
    state: MainUiState.Success,
    scrollState: ScrollState,
    activeMenu: String?,
    onMenuStateChanged: (String?) -> Unit,
    onAddReaction: (Int) -> Unit,
    onRemoveReaction: (Int) -> Unit,
    onDaysChanged: (Set<Int>) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var promoClosed by remember { mutableStateOf(false) }
    var clothesReaction by remember { mutableStateOf<Int?>(null) }
    var carReaction by remember { mutableStateOf<Int?>(null) }
    var snackbarData by remember { mutableStateOf<ReactionSnackbarData?>(null) }

    val globalAlpha by animateFloatAsState(if (activeMenu != null) 0.3f else 1f, label = "globalAlpha")

    LaunchedEffect(snackbarData) {
        if (snackbarData != null) {
            delay(5000)
            snackbarData = null
        }
    }

    val handleReactionClick: (Int?, Int?, (Int?) -> Unit) -> Unit = { newReaction, oldReaction, updateState ->
        if (newReaction != null) {
            if (oldReaction != null && oldReaction != newReaction) onRemoveReaction(oldReaction)

            val key = context.resources.getResourceEntryName(newReaction)
            val currentGlobalCount = state.reactionCounters[key] ?: 0L

            onAddReaction(newReaction)
            updateState(newReaction)

            snackbarData = ReactionSnackbarData(count = currentGlobalCount + 1L, lottieRes = newReaction)
        } else {
            if (oldReaction != null) onRemoveReaction(oldReaction)
            updateState(null)
            snackbarData = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(globalAlpha)
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeatherChip("Ощущается", "${state.weatherInfo.feelsLike}°")
                WeatherChip("Влажность", "${state.weatherInfo.humidity}%")
                WeatherChip("Осадки", "${state.weatherInfo.pop}%")
                WeatherChip("Давление", "${state.weatherInfo.pressure} мм")
                WeatherChip("Ветер", "${state.weatherInfo.windSpeed} м/с")
                WeatherChip("Видимость", "${state.weatherInfo.visibility} км")
                WeatherChip("Восход", state.weatherInfo.sunrise)
                WeatherChip("Закат", state.weatherInfo.sunset)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(globalAlpha)
                    .background(TgCard, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.weatherInfo.cityName}", fontFamily = RobotoFont, color = TgTextLightBlue, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text("${state.weatherInfo.currentTemp}", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextWhite, fontSize = 100.sp, modifier = Modifier.offset(y = (-10).dp))
                        Text("°", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextLightBlue, fontSize = 70.sp)
                    }
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Text("Мин: ${state.weatherInfo.minTemp}°", fontFamily = RobotoFont, color = TgTextLightBlue, fontSize = 18.sp)
                        Spacer(Modifier.width(16.dp))
                        Text("Макс: ${state.weatherInfo.maxTemp}°", fontFamily = RobotoFont, color = TgTextLightBlue, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TelegramReactionBlock(
                modifier = Modifier.fillMaxWidth(),
                contentAlpha = globalAlpha,
                menuId = "clothes",
                activeMenu = activeMenu,
                onMenuStateChanged = { onMenuStateChanged(it) },
                selectedReactionRes = clothesReaction,
                globalCounters = state.reactionCounters,
                onReactionSelected = { handleReactionClick(it, clothesReaction) { newVal -> clothesReaction = newVal } }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(id = R.drawable.baseline_auto_awesome_24), contentDescription = null, tint = TgTextLightBlue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    HtmlText(html = state.weatherInfo.recommendationText, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TelegramReactionBlock(
                modifier = Modifier.fillMaxWidth(),
                contentAlpha = globalAlpha,
                menuId = "car",
                activeMenu = activeMenu,
                onMenuStateChanged = { onMenuStateChanged(it) },
                selectedReactionRes = carReaction,
                globalCounters = state.reactionCounters,
                onReactionSelected = { handleReactionClick(it, carReaction) { newVal -> carReaction = newVal } }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(id = R.drawable.outline_directions_car_24), contentDescription = null, tint = TgTextLightBlue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(state.weatherInfo.carWashAdvice, fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextWhite, fontSize = 20.sp, modifier = Modifier.weight(1f))
                }
            }
            if (!promoClosed) {
                PromoBannerPager(
                    modifier = Modifier.alpha(globalAlpha),
                    onAllClosed = { promoClosed = it }
                )
            }

            if (promoClosed) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            NotificationDaysBlock(
                modifier = Modifier.alpha(globalAlpha),
                onDaysChanged = onDaysChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.weatherInfo.futureForecasts?.isNotEmpty() == true) {
                FutureForecastPager(
                    forecasts = state.weatherInfo.futureForecasts,
                    modifier = Modifier.alpha(globalAlpha)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(globalAlpha)
                    .background(TgCard, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onNavigateToSettings() }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text("Настройки", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextWhite, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text(">", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextLightBlue, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            AnimatedVisibility(
                visible = snackbarData != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                snackbarData?.let { data ->
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2B3A4C), RoundedCornerShape(50))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${data.count} пользователей отреагировали ", color = TgTextWhite, fontSize = 14.sp, fontFamily = RobotoFont, fontWeight = FontWeight.Medium)
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(data.lottieRes))
                        LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FutureForecastPager(forecasts: List<DailyForecast>, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { forecasts.size })

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 16.dp,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) { index ->
                val forecast = forecasts[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(TgCard)
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (index == 0) "Завтра ${forecast.date}" else forecast.date,
                            fontFamily = RobotoFont, color = TgTextLightBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${forecast.minTemp}° ... ${forecast.maxTemp}°",
                            fontFamily = RobotoFont, color = TgTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_auto_awesome_24),
                            contentDescription = null,
                            tint = TgTextLightBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        HtmlText(html = forecast.recommendationText, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (forecasts.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(forecasts.size) { i ->
                        val isSelected = pagerState.currentPage == i
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (isSelected) 16.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) TgTextLightBlue else TgTextLightBlue.copy(alpha = 0.3f))
                        )
                        if (i != forecasts.size - 1) Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromoBannerPager(modifier: Modifier = Modifier, onAllClosed: (Boolean) -> Unit) {
    val context = LocalContext.current
    var visiblePages by remember { mutableStateOf(setOf(0, 1)) }

    LaunchedEffect(visiblePages) {
        onAllClosed(visiblePages.isEmpty())
    }


    val pagesList = visiblePages.toList().sorted()
    val pagerState = rememberPagerState(pageCount = { pagesList.size })

    Column(modifier = modifier
        .fillMaxWidth()
        .animateContentSize()) {

        Box(modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-15).dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                pageSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) { index ->
                val pageId = pagesList[index]
                val backgroundBrush = if (pageId == 0) {
                    Brush.horizontalGradient(listOf(Color(0xFF461E6B), Color(0xFF7924C8)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF01224D), Color(0xFF085BB2)))
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(100.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(backgroundBrush)
                            .clickable {
                                if (pageId == 0) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.rustore.ru/catalog/app/com.outwin.android"))
                                    context.startActivity(intent)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        if (pageId == 0) {
                            Column(modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.65f)) {
                                Text(
                                    text = "Оставь отзыв в RuStore!",
                                    fontFamily = RobotoFont, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Написать отзыв", fontFamily = RobotoFont, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(" >", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Column(modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.7f)) {
                                Text("Ставьте реакции!", fontFamily = RobotoFont, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("Нажми на блок одежды или авто", fontFamily = RobotoFont, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 17.sp)
                            }
                        }
                    }

                    if (pageId == 0) {
                        Image(
                            painter = painterResource(id = R.drawable.duck),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = -20.dp, y = 5.dp)
                                .requiredSize(140.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    if (pageId == 1) {
                        Image(
                            painter = painterResource(id = R.drawable.duck_think),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = -20.dp, y = 5.dp)
                                .requiredSize(120.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 65.dp, end = 12.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable {
                        val currentPageId = pagesList[pagerState.currentPage]
                        visiblePages = visiblePages - currentPageId
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (pagesList.size > 1) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(pagesList.size) { i ->
                        val isSelected = pagerState.currentPage == i
                        Box(modifier = Modifier.height(5.dp).width(if (isSelected) 16.dp else 5.dp).clip(CircleShape).background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Composable
fun WeatherChip(title: String, value: String) {
    Column(
        modifier = Modifier
            .background(TgCard, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontFamily = RobotoFont, color = TgTextLightBlue, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontFamily = RobotoFont, color = TgTextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NotificationDaysBlock(modifier: Modifier = Modifier, onDaysChanged: (Set<Int>) -> Unit = {}) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("outwin_prefs", Context.MODE_PRIVATE)
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    var selectedDays by remember {
        mutableStateOf(prefs.getStringSet("selected_days", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet())
    }

    var pendingDay by remember { mutableStateOf<Int?>(null) }

    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(context, { _, hour, minute ->
        prefs.edit().putInt("notif_hour", hour).putInt("notif_minute", minute).apply()
        WeatherNotificationScheduler.schedule(context, hour, minute)

        onDaysChanged(selectedDays)
        Toast.makeText(context, "Уведомление установлено на ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            timePickerDialog.show()
        } else {
            Toast.makeText(context, "Без разрешения уведомления не придут", Toast.LENGTH_LONG).show()
            pendingDay?.let { day -> selectedDays = selectedDays - day }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TgCard, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text("Ежедневные уведомления", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextWhite, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEachIndexed { index, day ->
                val isSelected = selectedDays.contains(index)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) TgReactionBackground else Color(0xFF3A4A5A))
                        .clickable {
                            val wasSelected = isSelected
                            val newDays = if (wasSelected) selectedDays - index else selectedDays + index
                            selectedDays = newDays

                            if (!wasSelected) {
                                pendingDay = index

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                    if (status == PackageManager.PERMISSION_GRANTED) {
                                        timePickerDialog.show()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    timePickerDialog.show()
                                }
                            } else {
                                if (newDays.isEmpty()) {
                                    WorkManager.getInstance(context).cancelAllWorkByTag("weather_notif")
                                }
                            }

                            prefs.edit().putStringSet("selected_days", newDays.map { it.toString() }.toSet()).apply()
                            onDaysChanged(newDays)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, fontFamily = RobotoFont, color = TgTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@androidx.compose.runtime.Composable
fun TelegramReactionBlock(
    modifier: Modifier = Modifier,
    contentAlpha: Float,
    menuId: String,
    activeMenu: String?,
    onMenuStateChanged: (String?) -> Unit,
    selectedReactionRes: Int?,
    globalCounters: Map<String, Long>,
    onReactionSelected: (Int?) -> Unit,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val showMenu = activeMenu == menuId

    val menuCompositions = ReactionAnimations.map { resId ->
        rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    }

    val selectedResult = if (selectedReactionRes != null) {
        rememberLottieComposition(LottieCompositionSpec.RawRes(selectedReactionRes))
    } else {
        androidx.compose.runtime.remember {
            object : androidx.compose.runtime.State<com.airbnb.lottie.LottieComposition?> {
                override val value: com.airbnb.lottie.LottieComposition? = null
            }
        }
    }

    var popupVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(showMenu) {
        if (showMenu) {
            popupVisible = true
        } else {
            kotlinx.coroutines.delay(200)
            popupVisible = false
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha }
                .clip(RoundedCornerShape(20.dp))
                .background(TgCard)
                .combinedClickable(
                    onClick = { onMenuStateChanged(if (showMenu) null else menuId) },
                    onLongClick = { onMenuStateChanged(if (showMenu) null else menuId) }
                )
                .animateContentSize(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
                .padding(20.dp)
        ) {
            content()

            if (selectedReactionRes != null && selectedResult.value != null) {
                val key = androidx.compose.runtime.remember(selectedReactionRes) {
                    context.resources.getResourceEntryName(selectedReactionRes)
                }
                val count = globalCounters[key] ?: 1L

                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF233246), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable { onReactionSelected(null) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LottieAnimation(
                            composition = selectedResult.value,
                            iterations = LottieConstants.IterateForever,
                            renderMode = RenderMode.HARDWARE,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        androidx.compose.material3.Text(
                            text = count.toString(),
                            color = TgTextWhite,
                            fontSize = 15.sp,
                            fontFamily = RobotoFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (popupVisible) {
            val menuWidth = config.screenWidthDp.dp - 32.dp
            val yOffset = with(density) { -70.dp.roundToPx() }

            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, yOffset),
                properties = PopupProperties(focusable = true),
                onDismissRequest = { onMenuStateChanged(null) }
            ) {
                AnimatedVisibility(
                    visible = showMenu,
                    enter = scaleIn(initialScale = 0.8f, animationSpec = tween(200), transformOrigin = TransformOrigin(0.5f, 1f)) + fadeIn(tween(200)),
                    exit = scaleOut(targetScale = 0.8f, animationSpec = tween(200), transformOrigin = TransformOrigin(0.5f, 1f)) + fadeOut(tween(200))
                ) {
                    Row(
                        modifier = Modifier
                            .width(menuWidth)
                            .background(TgCard, RoundedCornerShape(32.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ReactionAnimations.forEachIndexed { index, animRes ->
                            val composition = menuCompositions[index].value

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = composition != null) {
                                        onReactionSelected(if (selectedReactionRes == animRes) null else animRes)
                                        onMenuStateChanged(null)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (composition != null) {
                                    LottieAnimation(
                                        composition = composition,
                                        iterations = 1,
                                        renderMode = RenderMode.HARDWARE,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(TgBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "< Назад",
                color = TgTextLightBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
            )
            Text("Настройки", fontFamily = RobotoFont, fontWeight = FontWeight.ExtraBold, color = TgTextWhite, fontSize = 28.sp)
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Конфиденциальность", color = TgTextLightBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TgCard, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗑️", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Удалить мои данные", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252), fontSize = 18.sp)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = TgCard,
            title = { Text("Удаление данных", color = TgTextWhite, fontFamily = RobotoFont, fontWeight = FontWeight.Bold) },
            text = { Text("Вы уверены, что хотите удалить все свои данные (историю запусков, реакции, настройки)? Это действие необратимо.", color = TgTextLightBlue, fontFamily = RobotoFont) },
            confirmButton = {
                TextButton(onClick = {
                    val prefs = context.getSharedPreferences("outwin_prefs", Context.MODE_PRIVATE)
                    val analyticsPrefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)

                    prefs.edit().clear().apply()
                    analyticsPrefs.edit().clear().apply()

                    showDeleteConfirm = false
                    Toast.makeText(context, "Ваши данные успешно удалены", Toast.LENGTH_SHORT).show()
                    onBack()
                }) { Text("Удалить", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена", color = Color.Gray) }
            }
        )
    }
}


@Composable
fun ErrorStateView(message: String) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.duck_cry))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 0.dp, bottom = 150.dp)
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(220.dp)
                .offset(x = (-10).dp)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Упс...",
            fontSize = 34.sp,
            fontFamily = RobotoFont,
            fontWeight = FontWeight.ExtraBold,
            color = TgTextWhite
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth(0.8f),
            fontSize = 18.sp,
            fontFamily = RobotoFont,
            color = TgTextLightBlue,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val adjustedHtml = html.replace("#2A345C", "#FFFFFF").replace("#455A94", "#759AF6")
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 19f
                includeFontPadding = false
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(android.graphics.Color.parseColor("#759AF6"))
            }
        },
        update = { it.text = HtmlCompat.fromHtml(adjustedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY) }
    )
}

@Composable
fun SkeletonLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(48.dp)
                        .shimmerEffect()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(initialValue = -2 * size.width.toFloat(), targetValue = 2 * size.width.toFloat(), animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)), label = "shimmer_anim")
    background(brush = Brush.linearGradient(colors = listOf(TgCard, Color(0xFF353538), TgCard), start = Offset(startOffsetX, 0f), end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())), shape = RoundedCornerShape(24.dp)).onGloballyPositioned { size = it.size }
}

fun getGreetingBasedOnTime(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, backgroundColor = 0xFF181818, name = "1. Успешная загрузка")
@Composable
fun PreviewSuccessState() {
    val mockWeather = WeatherInfo(
        cityName = "Москва",
        currentTemp = 22,
        minTemp = 15,
        maxTemp = 25,
        recommendationText = "На улице тепло. Надевайте <font color='#FFFFFF'>футболку</font> и <font color='#FFFFFF'>шорты</font>.",
        carWashAdvice = "Можно ехать на мойку",
        feelsLike = 24,
        humidity = 50,
        pressure = 745,
        windSpeed = 3.5,
        visibility = 10,
        pop = 10,
        sunrise = "05:30",
        sunset = "21:00",
        futureForecasts = listOf(
            DailyForecast("06.04", 10, 18, "Дождь. Возьмите <font color='#FFFFFF'>зонт</font> и <font color='#FFFFFF'>дождевик</font>."),
            DailyForecast("07.04", 12, 20, "Ясно. Легкая <font color='#FFFFFF'>ветровка</font>."),
            DailyForecast("08.04", 15, 22, "Солнечно. Подойдет <font color='#FFFFFF'>футболка</font>."),
            DailyForecast("09.04", 16, 24, "Отличная погода. <font color='#FFFFFF'>Шорты</font> и <font color='#FFFFFF'>футболка</font>.")
        )
    )
    MaterialTheme {
        Box(modifier = Modifier.background(TgBackground)) {
            SuccessStateView(
                state = MainUiState.Success(weatherInfo = mockWeather, reactionCounters = mapOf("fire" to 1250L)),
                scrollState = rememberScrollState(),
                activeMenu = null,
                onMenuStateChanged = {},
                onAddReaction = {},
                onRemoveReaction = {},
                onDaysChanged = {},
                onNavigateToSettings = {}
            )
        }
    }
}