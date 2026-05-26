package com.bihstudio.uvindex.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.bihstudio.uvindex.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(private val application: Application) {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTimeMillis = 0L
    private var pendingShowActivity: Activity? = null
    private var pendingShowUntilMillis = 0L
    private var pendingShowFinished: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        AppOpenAd.load(
            application,
            BuildConfig.APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTimeMillis = SystemClock.elapsedRealtime()
                    showPendingAdIfStillOpening()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    finishPendingShow()
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity, onFinished: () -> Unit = {}) {
        if (isShowingAd) return
        if (pendingShowActivity != null) return

        val ad = appOpenAd
        if (ad == null || !isAdAvailable()) {
            pendingShowActivity = activity
            pendingShowFinished = onFinished
            pendingShowUntilMillis = SystemClock.elapsedRealtime() + OPENING_AD_WINDOW_MILLIS
            loadAd()
            mainHandler.postDelayed(
                {
                    if (!isShowingAd && pendingShowActivity === activity) {
                        finishPendingShow()
                    }
                },
                OPENING_AD_WINDOW_MILLIS
            )
            return
        }

        showAd(activity, ad, onFinished)
    }

    private fun showPendingAdIfStillOpening() {
        val activity = pendingShowActivity ?: return
        if (SystemClock.elapsedRealtime() > pendingShowUntilMillis) {
            finishPendingShow()
            return
        }

        appOpenAd?.let { showAd(activity, it, pendingShowFinished ?: {}) }
    }

    private fun showAd(activity: Activity, ad: AppOpenAd, onFinished: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onFinished()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                pendingShowActivity = null
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                pendingShowFinished = null
                onFinished()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                pendingShowFinished = null
                onFinished()
                loadAd()
            }
        }

        ad.show(activity)
    }

    private fun finishPendingShow() {
        pendingShowActivity = null
        pendingShowUntilMillis = 0L
        pendingShowFinished?.invoke()
        pendingShowFinished = null
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null &&
            SystemClock.elapsedRealtime() - loadTimeMillis < AD_EXPIRATION_MILLIS
    }

    private companion object {
        const val OPENING_AD_WINDOW_MILLIS = 8_000L
        const val AD_EXPIRATION_MILLIS = 4 * 60 * 60 * 1_000L
    }
}
