package com.bihstudio.uvindex

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bihstudio.uvindex.ads.AppOpenAdManager
import com.bihstudio.uvindex.ads.StartupAdGate
import com.bihstudio.uvindex.presentation.MainActivity
import com.bihstudio.uvindex.service.cleanupPendingLauncherAliases
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UVIndexApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private var startedActivities = 0
    private var currentActivity: Activity? = null
    private var hasFinishedInitialAdGate = false
    private var hasRequestedInitialAppOpenAd = false
    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        appOpenAdManager = AppOpenAdManager(this)
        MobileAds.initialize(this) {
            appOpenAdManager.loadAd()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                (currentActivity as? MainActivity)?.let { activity ->
                    appOpenAdManager.showAdIfAvailable(activity) {
                        finishInitialAdGate()
                    }
                } ?: finishInitialAdGate()
            }
        })
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities += 1
                currentActivity = activity
                if (!hasRequestedInitialAppOpenAd && activity is MainActivity) {
                    hasRequestedInitialAppOpenAd = true
                    appOpenAdManager.showAdIfAvailable(activity) {
                        finishInitialAdGate()
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (currentActivity === activity) {
                    currentActivity = null
                }
                if (startedActivities == 0) {
                    cleanupPendingLauncherAliases(applicationContext)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun finishInitialAdGate() {
        if (!hasFinishedInitialAdGate) {
            hasFinishedInitialAdGate = true
            StartupAdGate.finish()
        }
    }
}
