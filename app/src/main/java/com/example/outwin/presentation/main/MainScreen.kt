package com.example.outwin.presentation.main

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.outwin.R
import com.example.outwin.domain.model.WeatherInfo
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.util.Calendar

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
    onNavigateToNotifications: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var activeMenu by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.fetchWeather(useSavedLocationFallback = false)
        else Toast.makeText(context, "Нужен доступ к GPS", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.fetchWeather(useSavedLocationFallback = true)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val scrollOffset = scrollState.value.toFloat()
    val collapseFraction = (scrollOffset / 250f).coerceIn(0f, 1f)
    val titleFontSize = (36f - (14f * collapseFraction)).sp

    val globalAlpha by animateFloatAsState(targetValue = if (activeMenu != null) 0.95f else 1f, label = "globalAlpha")

    Box(modifier = Modifier.fillMaxSize().background(TgBackground)) {
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
                        modifier = Modifier.alpha(collapseFraction).padding(start = 8.dp, bottom = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
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
                            }
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
    }
}

@Composable
fun SuccessStateView(
    state: MainUiState.Success,
    scrollState: androidx.compose.foundation.ScrollState,
    activeMenu: String?,
    onMenuStateChanged: (String?) -> Unit,
    onAddReaction: (Int) -> Unit,
    onRemoveReaction: (Int) -> Unit
) {
    val context = LocalContext.current
    var clothesReaction by remember { mutableStateOf<Int?>(null) }
    var carReaction by remember { mutableStateOf<Int?>(null) }
    var snackbarData by remember { mutableStateOf<ReactionSnackbarData?>(null) }

    val globalAlpha by animateFloatAsState(if (activeMenu != null) 0.3f else 1f, label = "globalAlpha")

    val prefs = context.getSharedPreferences("outwin_prefs", Context.MODE_PRIVATE)
    var showHint by remember { mutableStateOf(!prefs.getBoolean("hint_closed", false)) }

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
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp),
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
                modifier = Modifier.fillMaxWidth().alpha(globalAlpha).background(TgCard, RoundedCornerShape(24.dp)).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.weatherInfo.cityName}", fontFamily = RobotoFont, color = TgTextLightBlue, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text("${state.weatherInfo.currentTemp}", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextWhite, fontSize = 100.sp, modifier = Modifier.offset(y = (-10).dp))
                        Text("°", fontFamily = RobotoFont, fontWeight = FontWeight.Bold, color = TgTextLightBlue, fontSize = 60.sp)
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
                onMenuStateChanged = onMenuStateChanged,
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
                onMenuStateChanged = onMenuStateChanged,
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

            Spacer(modifier = Modifier.height(16.dp))

            NotificationDaysBlock(modifier = Modifier.alpha(globalAlpha))

            AnimatedVisibility(visible = showHint) {
                Box(
                    modifier = Modifier.fillMaxWidth().alpha(globalAlpha).padding(top = 16.dp).background(TgTextLightBlue, RoundedCornerShape(20.dp)).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "Подсказка: нажмите на блок рекомендаций, чтобы поставить реакцию!",
                            fontFamily = RobotoFont, color = TgBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "✕", fontFamily = RobotoFont, color = TgBackground, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                            modifier = Modifier.clickable {
                                showHint = false
                                prefs.edit().putBoolean("hint_closed", true).apply()
                            }.padding(4.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = showHint) {
                Box(
                    modifier = Modifier.fillMaxWidth().alpha(globalAlpha).padding(top = 16.dp).background(TgTextLightBlue, RoundedCornerShape(20.dp)).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "Понравилось приложение? Пожалуйста, поставьте нам оценку в маркете! Каждая ваша звезда и пара добрых слов вдохновят нас на новые свершения",
                            fontFamily = RobotoFont, color = TgBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "✕", fontFamily = RobotoFont, color = TgBackground, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                            modifier = Modifier.clickable {
                                showHint = false
                                prefs.edit().putBoolean("hint_closed", true).apply()
                            }.padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            AnimatedVisibility(
                visible = snackbarData != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                snackbarData?.let { data ->
                    Row(
                        modifier = Modifier.background(Color(0xFF2B3A4C), RoundedCornerShape(50)).padding(horizontal = 20.dp, vertical = 12.dp),
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

@Composable
fun WeatherChip(title: String, value: String) {
    Column(
        modifier = Modifier.background(TgCard, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontFamily = RobotoFont, color = TgTextLightBlue, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontFamily = RobotoFont, color = TgTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NotificationDaysBlock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("outwin_prefs", Context.MODE_PRIVATE)
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    val savedDaysStr = prefs.getStringSet("selected_days", emptySet()) ?: emptySet()
    val savedDays = savedDaysStr.mapNotNull { it.toIntOrNull() }.toSet()

    var selectedDays by remember { mutableStateOf(savedDays) }
    var pendingDay by remember { mutableStateOf<Int?>(null) }

    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(context, { _, hour, minute ->
        pendingDay?.let { day ->
            val newDays = selectedDays + day
            selectedDays = newDays

            prefs.edit().putStringSet("selected_days", newDays.map { it.toString() }.toSet()).apply()

            // TODO: Вызов AlarmManager для точного системного будильника
            Toast.makeText(context, "Уведомление установлено на ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
        }
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)

    Column(
        modifier = modifier.fillMaxWidth().background(TgCard, RoundedCornerShape(24.dp)).padding(20.dp)
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
                            if (isSelected) {
                                val newDays = selectedDays - index
                                selectedDays = newDays
                                prefs.edit().putStringSet("selected_days", newDays.map { it.toString() }.toSet()).apply()
                                // TODO: Отменить AlarmManager будильник для этого дня
                            } else {
                                pendingDay = index
                                timePickerDialog.show()
                            }
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
@Composable
fun TelegramReactionBlock(
    modifier: Modifier = Modifier,
    contentAlpha: Float,
    menuId: String,
    activeMenu: String?,
    onMenuStateChanged: (String?) -> Unit,
    selectedReactionRes: Int?,
    globalCounters: Map<String, Long>,
    onReactionSelected: (Int?) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val showMenu = activeMenu == menuId

    var popupVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showMenu) {
        if (showMenu) popupVisible = true
        else {
            delay(200)
            popupVisible = false
        }
    }

    val menuCompositions = ReactionAnimations.associateWith { animRes ->
        rememberLottieComposition(LottieCompositionSpec.RawRes(animRes))
    }

    val selectedComposition by rememberLottieComposition(
        spec = selectedReactionRes?.let { LottieCompositionSpec.RawRes(it) }
            ?: LottieCompositionSpec.RawRes(0)
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                .clip(RoundedCornerShape(20.dp))
                .background(TgCard)
                .combinedClickable(
                    onClick = { onMenuStateChanged(if (showMenu) null else menuId) },
                    onLongClick = { onMenuStateChanged(if (showMenu) null else menuId) }
                )
                .animateContentSize(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
                .padding(20.dp)
        ) {
            content()

            if (selectedReactionRes != null) {
                val key = context.resources.getResourceEntryName(selectedReactionRes)
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
                            composition = selectedComposition,
                            iterations = LottieConstants.IterateForever,
                            renderMode = RenderMode.HARDWARE,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = count.toString(), color = TgTextWhite, fontSize = 15.sp, fontFamily = RobotoFont, fontWeight = FontWeight.Bold)
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
                        ReactionAnimations.forEach { animRes ->
                            val composition by menuCompositions.getValue(animRes)

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onReactionSelected(if (selectedReactionRes == animRes) null else animRes)
                                        onMenuStateChanged(null)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
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

@Composable
fun ErrorStateView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Text("Упс...", fontSize = 34.sp, fontFamily = RobotoFont, fontWeight = FontWeight.ExtraBold, color = TgTextWhite)
        Spacer(Modifier.height(16.dp))
        Text(message, fontSize = 18.sp, fontFamily = RobotoFont, color = TgTextLightBlue)
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
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp).shimmerEffect())
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).shimmerEffect())
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(70.dp).shimmerEffect())
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
        sunset = "21:00"
    )
    MaterialTheme {
        Box(modifier = Modifier.background(TgBackground)) {
            SuccessStateView(
                state = MainUiState.Success(weatherInfo = mockWeather, reactionCounters = mapOf("fire" to 1250L)),
                scrollState = rememberScrollState(),
                activeMenu = null,
                onMenuStateChanged = {},
                onAddReaction = {},
                onRemoveReaction = {}
            )
        }
    }
}