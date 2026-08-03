import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.bihstudio.uvindex"
    compileSdk = 36

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use(::load)
        }
    }

    defaultConfig {
        applicationId = "com.bihstudio.uvindex"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Replace with your actual AdMob App ID
        manifestPlaceholders["admobAppId"] = "ca-app-pub-8342448049337544~3445701850"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            buildConfigField(
                "String",
                "APP_OPEN_AD_UNIT_ID",
                "\"ca-app-pub-8342448049337544/6638952778\""
            )
            buildConfigField(
                "String",
                "BANNER_AD_UNIT_ID",
                "\"ca-app-pub-8342448049337544/5326737077\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_UNIT_ID",
                "\"ca-app-pub-8342448049337544/1960691762\""
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField(
                "String",
                "APP_OPEN_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9257395921\""
            )
            buildConfigField(
                "String",
                "BANNER_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/6300978111\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/1033173712\""
            )
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.google.material)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Location
    implementation(libs.play.services.location)

    // WorkManager
    implementation(libs.work.runtime)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Ads
    implementation(libs.play.services.ads)

    // Image loading
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation("junit:junit:4.13.2")

    debugImplementation(libs.androidx.ui.tooling)
}
