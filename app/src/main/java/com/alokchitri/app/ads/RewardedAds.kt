package com.alokchitri.app.ads

import android.app.Activity
import android.content.Context
import com.alokchitri.app.AppConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * One rewarded ad at a time for "watch an ad for one more feedback". Loads ahead, so the ad shows instantly
 * on tap, and loads the next one after it closes. Main thread only.
 */
class RewardedAds(context: Context) {
    private val appContext = context.applicationContext
    private var ad: RewardedAd? = null
    private var loading = false

    init {
        MobileAds.initialize(appContext) { load() }
    }

    fun load() {
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(appContext, AppConfig.REWARDED_AD_UNIT, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(loaded: RewardedAd) {
                ad = loaded
                loading = false
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                loading = false
            }
        })
    }

    /** Shows the ad; [onReward] runs only if the user watched it to the end. False if no ad was ready. */
    fun show(activity: Activity, onReward: () -> Unit): Boolean {
        val current = ad ?: run { load(); return false }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = load()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = load()
        }
        current.show(activity) { onReward() }
        return true
    }
}
