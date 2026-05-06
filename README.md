# ☀️ UV Index App — BIH Studio

A production-ready Android application built with **Kotlin + Jetpack Compose + MVVM + Clean Architecture**.

---

## 🏗️ Architecture

```
com.bihstudio.uvindex/
├── data/
│   ├── local/          → Room DB, DataStore preferences
│   ├── remote/         → Retrofit API (Open-Meteo, free, no key)
│   └── repository/     → UVRepository, LocationRepository
├── di/                 → Hilt modules (Network, Database)
├── domain/
│   └── model/          → UVData, UVHourly, UVIndexLevel, NearbyLocation, AppLanguage
├── presentation/
│   ├── MainActivity
│   ├── navigation/     → NavGraph, Screen sealed class
│   ├── screens/
│   │   ├── splash/     → SplashScreen + ViewModel
│   │   ├── language/   → LanguageScreen + ViewModel
│   │   ├── permission/ → PermissionScreen + ViewModel
│   │   ├── uvindex/    → UVIndexScreen + ViewModel (main screen)
│   │   └── location/   → LocationSearchScreen + ViewModel
│   ├── components/     → SunAnimation, AdBanner, InterstitialAdManager
│   └── theme/          → Material3 dark theme with UV-inspired palette
├── service/            → UVCheckWorker, BootReceiver, notification helpers
└── analytics/          → Firebase Analytics wrapper
```

---

## 📱 App Flow

```
Splash (BIH Studio) → Language Picker → Permission Request → [Ad on 1st launch] → UV Index Screen
                                                                                         ↕
                                                                               Location Search Screen
```

---

## ✅ Features Implemented

| Feature | Status |
|---------|--------|
| Splash screen with BIH Studio branding | ✅ |
| Language selection (EN/HE/FR/ES/AR) | ✅ |
| Location permission request | ✅ |
| Notification permission request | ✅ |
| Current UV index with animated sun | ✅ |
| UV level color coding (Low→Extreme) | ✅ |
| 4-hour forecast row | ✅ |
| UV scale bar | ✅ |
| Google Interstitial Ad (first launch) | ✅ |
| Google Banner Ad on UV screen | ✅ |
| Firebase Analytics on all screens | ✅ |
| Background UV check (WorkManager) | ✅ |
| Push notifications for UV alerts | ✅ |
| Location search (lat/lon) | ✅ |
| Nearby UV spots (25 km radius) | ✅ |
| RTL support (Hebrew, Arabic) | ✅ |
| Room caching (30-min TTL) | ✅ |
| MVVM + Hilt DI | ✅ |
| Clean Architecture layers | ✅ |

---

## 🔧 Setup Instructions

### 1. Firebase Setup
1. Create a project at [Firebase Console](https://console.firebase.google.com)
2. Add Android app with package `com.bihstudio.uvindex`
3. Download `google-services.json` and replace the placeholder in `app/`
4. Enable **Analytics** and **Crashlytics** in the Firebase console

### 2. AdMob Setup
1. Create an [AdMob account](https://admob.google.com)
2. Create an app and two ad units: **Banner** and **Interstitial**
3. Replace test IDs in:
   - `app/build.gradle.kts` → `admobAppId`
   - `Components.kt` → `BANNER_AD_UNIT_ID`
   - `Components.kt` → `INTERSTITIAL_AD_UNIT_ID`

### 3. UV Data API
- Uses **Open-Meteo** (https://open-meteo.com) — **completely free, no API key needed**

### 4. Build
```bash
./gradlew assembleDebug
```

---

## 🎨 Design Highlights
- **Dark sky theme** with night-blue gradient background
- **Animated sun** with rotating rays and glow pulse
- **UV-level colors**: Green → Yellow → Orange → Red → Purple
- **Glass morphism** cards
- **RTL-ready** layouts for Hebrew and Arabic

---

## 📋 Dependencies Overview

| Library | Purpose |
|---------|---------|
| Jetpack Compose + Material3 | UI |
| Hilt | Dependency Injection |
| Retrofit + OkHttp | Network |
| Room | Local cache |
| DataStore | Preferences |
| WorkManager | Background checks |
| Firebase Analytics | Analytics |
| AdMob | Monetisation |
| Accompanist Permissions | Runtime permissions |
| Coil | Image loading |
| Open-Meteo | Free UV API |

---

## 🔔 Notifications Logic
- Scheduled hourly via **WorkManager** after notification permission granted
- Fires only when UV index ≥ 3 (Moderate+)
- Stops if user revokes permission
- Restarts on device boot via `BootReceiver`

---

*Made with ☀️ by BIH Studio*
