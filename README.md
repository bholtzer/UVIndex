# ☀️Index App — BIH Studio

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
Splash (BIH Studio) → Language Picker → Permission Request → [Ad on 1st launch] → Index Screen
                                                                                         ↕
                                                                               Location Search Screen
```

---

## ✅ Features Implemented

| Feature                               | Status |
|---------------------------------------|--------|
| Splash screen with BIH Studio branding | ✅ |
| Language selection (EN/HE/FR/ES/DE/AR/RU/JA/IT/SV/BG/EL/PT/HR/TR) | ✅ |
| Location permission request           | ✅ |
| Notification permission request       | ✅ |
| Current index with animated sun       | ✅ |
| UV level color coding (Low→Extreme)   | ✅ |
| 4-hour forecast row                   | ✅ |
| UV scale bar                          | ✅ |
| Google Interstitial Ad (first launch) | ✅ |
| Google Banner Ad on UV screen         | ✅ |
| Firebase Analytics on all screens     | ✅ |
| Background UV check (WorkManager)     | ✅ |
| Push notifications for UV alerts      | ✅ |
| Location search (lat/lon)             | ✅ |
| Nearby UV spots (25 km radius)        | ✅ |
| RTL support (Hebrew, Arabic)          | ✅ |
| Room caching (30-min TTL)             | ✅ |
| MVVM + Hilt DI                        | ✅ |
| Clean Architecture layers             | ✅ |

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

#### app-ads.txt verification

The website file [`docs/app-ads.txt`](docs/app-ads.txt) authorizes the Google publisher configured in `app/build.gradle.kts`:

```text
google.com, pub-8342448049337544, DIRECT, f08c47fec0942fa0
```

Publish this file at the root of the developer website listed in Google Play. For the current GitHub Pages hostname, the required URL is `https://bholtzer.github.io/app-ads.txt`. Publishing this repository's `docs` folder under `/UVIndex/` alone does not expose that root URL; also copy the file into the publishing source of the `bholtzer.github.io` website repository, or use a developer domain that serves it at `/app-ads.txt`.

Confirm that the public root URL returns the plain text record, then request **Check for updates** in AdMob's app-ads.txt section. Google advises allowing up to 24 hours for verification. See [AdMob's setup instructions](https://support.google.com/admob/answer/9363762?hl=en).

### 3. UV Data API
- Uses **Open-Meteo** (https://open-meteo.com) — **completely free, no API key needed**

### 4. Build
Open this directory (the one containing `settings.gradle.kts`) in Android Studio.
Use a working JDK 21 for Gradle, as configured in `gradle/gradle-daemon-jvm.properties`,
and disable Gradle offline mode when dependencies need to be downloaded.
For terminal builds, set `JAVA_HOME` to that JDK; Java 8 cannot run this Gradle wrapper.

```bash
./gradlew assembleDebug
```

On Windows, use `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest`.
Select the `app` run configuration and a device running Android 8.0 (API 26) or later.
`MainActivity` must remain exported so Android Studio and launcher shortcuts can
start it directly; the launcher aliases provide the home-screen icon.

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
- Refreshes the forecast twice daily and schedules a unique alert with **WorkManager**
- Alerts every day, 30 minutes before the forecast daily maximum
- Stops if user revokes permission
- Restarts on device boot via `BootReceiver`

---

*Made with ☀️ by BIH Studio*
