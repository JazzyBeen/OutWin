
# Outwin (Умный погодный помощник)

**Outwin** — это Android-приложение, которое не только показывает точный прогноз погоды, но и выступает в роли умного стилиста. Приложение анализирует температуру, влажность и скорость ветра, чтобы дать детальные рекомендации по выбору одежды на день, а также подсказывает, стоит ли сегодня ехать на автомойку.

>  **Проект построен на современных стандартах:** Использование **Clean Architecture + MVVM**, Kotlin Coroutines и Dagger Hilt.

---

##  Скриншоты
<p align="center">
  <img src="https://github.com/user-attachments/assets/0b39b981-2f86-49a4-9899-4372d6a6828c" width="200" />
  <img src="https://github.com/user-attachments/assets/9bf960a5-c26d-4489-b9f9-b4f184747e20" width="200" />
  <img src="https://github.com/user-attachments/assets/deb50b22-d4ed-4b80-9d2d-967400b7a528" width="200" />
  <img src="https://github.com/user-attachments/assets/9a6d7097-3b15-4e04-9490-f88b7e9c32c8" width="200" />
</p>

---

##  Технологический стек

Проект демонстрирует использование современных инструментов разработки под Android (Kotlin) и следование принципам "Чистой Архитектуры".

*   **Language:** Kotlin
*   **Architecture:** Clean Architecture (Domain / Data / Presentation layers)
*   **Design Pattern:** MVVM (Model-View-ViewModel)
*   **Dependency Injection:** Hilt (Dagger)
*   **Network:** Retrofit 2 + Gson (взаимодействие с OpenWeatherMap API)
*   **Cloud / Backend:** Firebase Realtime Database (глобальный счетчик пользы)
*   **Background Tasks:** WorkManager (для ежедневных push-уведомлений)
*   **Location:** Google Play Services (FusedLocationProviderClient)
*   **Async/Reactive:** Coroutines, StateFlow
*   **UI:** ConstraintLayout, Custom Animations (AnimationDrawable), HTML-форматирование текста

---

##  Архитектура

Приложение разделено на три независимых слоя согласно принципам Clean Architecture:

1.  **Domain Layer** (Kotlin Module):
    *   Содержит бизнес-логику и сущности (`WeatherInfo`).
    *   Определяет интерфейсы репозиториев (`WeatherRepository`, `AnalyticsRepository`, `LocationTracker`).
    *   Содержит `UseCases` и сложный алгоритм подбора одежды `WeatherRecommendationHelper`.
    *   Не имеет зависимостей от тяжелых компонентов Android SDK.

2.  **Data Layer** (Android Module):
    *   Реализует интерфейсы из Domain слоя.
    *   **Remote:** Клиент Retrofit для работы с погодным API.
    *   **Cloud:** Транзакции и подписки на Firebase Realtime DB.
    *   **Location:** Получение точных GPS-координат и кэширование их в SharedPreferences.
    *   **Repository:** `WeatherRepositoryImpl`, `AnalyticsRepositoryImpl` — управляют получением и обработкой данных.

3.  **Presentation Layer** (Android Module):
    *   **ViewModel:** Управляет состоянием UI через `StateFlow` (`MainUiState`), переживает повороты экрана, общается с UseCases.
    *   **UI:** Activity — отвечают только за отрисовку, запуск покадровых анимаций и запрос системных разрешений.

---

##  Функционал

*   [x] **Умный подбор одежды:** Детальный алгоритм, учитывающий температуру, силу ветра и влажность, чтобы посоветовать идеальный наряд (от шорт до арктической парки).
*   [x] **Точная геолокация:** Автоматическое определение местоположения с fallback-механизмом на последние сохраненные координаты, если GPS недоступен.
*   [x] **Советы для водителей:** Анализ осадков на день, чтобы подсказать, благоприятная ли сегодня погода для мойки машины.
*   [x] **Ежедневные уведомления:** Фоновая рассылка утренних уведомлений с прогнозом и советом по одежде с помощью WorkManager.
*   [x] **Глобальная статистика:** Подсчет того, скольким пользователям помогли советы (Realtime-синхронизация с Firebase).

---

##  Структура проекта

```text
com.example.outwin
├── data                # Реализация работы с данными
│   ├── api             # Retrofit интерфейсы, DTO модели
│   ├── location        # Получение координат (FusedLocationProvider)
│   └── repository      # Имплементация репозиториев (Weather, Firebase)
├── di                  # Hilt модули (NetworkModule, DataModule)
├── domain              # Бизнес-логика (Чистый Kotlin)
│   ├── model           # Доменные модели (WeatherInfo)
│   ├── repository      # Интерфейсы репозиториев
│   └── usecase         # GetWeatherUseCase, Алгоритмы рекомендаций
├── presentation        # UI слой
│   ├── main            # MainActivity, MainViewModel, UiState
│   └── notifications   # Экран включения ежедневных пушей
└── worker              # Фоновые задачи (WeatherNotificationWorker)
```

---
