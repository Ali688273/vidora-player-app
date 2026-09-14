package com.vidora.player

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.adivery.sdk.Adivery
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusInitListener
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks
import ir.tapsell.plus.model.TapsellPlusAdModel

object AdsManager {

    private const val TAG = "VidoraAds"

    private var tapsellInitialized = false

    private var tapsellInterstitialResponseId: String? = null
    private var tapsellRewardedResponseId: String? = null

    fun initialize(context: Context) {

        val application =
            context.applicationContext as Application

        initializeAdivery(application)
        initializeTapsell(application)
    }

    private fun initializeAdivery(application: Application) {

        try {
            Adivery.setLoggingEnabled(false)

            Adivery.configure(
                application,
                BuildConfig.ADIVERY_APP_ID
            )

            Adivery.prepareInterstitialAd(
                application,
                BuildConfig.ADIVERY_INTERSTITIAL
            )

            Adivery.prepareRewardedAd(
                application,
                BuildConfig.ADIVERY_REWARDED
            )

            Log.d(TAG, "Adivery initialized")

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Adivery initialization error",
                e
            )
        }
    }

    private fun initializeTapsell(application: Application) {

        if (tapsellInitialized) {
            return
        }

        try {

            TapsellPlus.initialize(
                application,
                BuildConfig.TAPSELL_KEY,
                object : TapsellPlusInitListener {

                    override fun onInitializeSuccess(
                        adNetworks: AdNetworks
                    ) {

                        tapsellInitialized = true

                        Log.d(
                            TAG,
                            "Tapsell initialized: ${adNetworks.name}"
                        )
                    }

                    override fun onInitializeFailed(
                        adNetworks: AdNetworks,
                        error: AdNetworkError
                    ) {

                        Log.e(
                            TAG,
                            "Tapsell initialization failed: ${error.errorMessage}"
                        )
                    }
                }
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Tapsell initialization error",
                e
            )
        }
    }

    fun prepareInterstitial(activity: Activity) {

        if (!tapsellInitialized) {
            return
        }

        try {

            TapsellPlus.requestInterstitialAd(
                activity,
                BuildConfig.TAPSELL_INTERSTITIAL,
                object : AdRequestCallback {

                    override fun response(
                        adModel: TapsellPlusAdModel
                    ) {

                        tapsellInterstitialResponseId =
                            adModel.responseId

                        Log.d(
                            TAG,
                            "Tapsell interstitial ready"
                        )
                    }

                    override fun error(
                        message: String
                    ) {

                        Log.e(
                            TAG,
                            "Tapsell interstitial error: $message"
                        )
                    }
                }
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Tapsell interstitial request error",
                e
            )
        }
    }

    fun prepareRewarded(activity: Activity) {

        if (!tapsellInitialized) {
            return
        }

        try {

            TapsellPlus.requestRewardedVideoAd(
                activity,
                BuildConfig.TAPSELL_REWARDED,
                object : AdRequestCallback {

                    override fun response(
                        adModel: TapsellPlusAdModel
                    ) {

                        tapsellRewardedResponseId =
                            adModel.responseId

                        Log.d(
                            TAG,
                            "Tapsell rewarded ready"
                        )
                    }

                    override fun error(
                        message: String
                    ) {

                        Log.e(
                            TAG,
                            "Tapsell rewarded error: $message"
                        )
                    }
                }
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Tapsell rewarded request error",
                e
            )
        }
    }

    fun showInterstitial(activity: Activity) {

        val tapsellId =
            tapsellInterstitialResponseId

        if (!tapsellId.isNullOrBlank()) {

            try {

                TapsellPlus.showInterstitialAd(
                    activity,
                    tapsellId,
                    object : AdShowListener() {

                        override fun onOpened() {
                            Log.d(
                                TAG,
                                "Tapsell interstitial opened"
                            )
                        }

                        override fun onClosed() {

                            tapsellInterstitialResponseId =
                                null

                            prepareInterstitial(activity)

                            Log.d(
                                TAG,
                                "Tapsell interstitial closed"
                            )
                        }

                        override fun onError(
                            message: String
                        ) {

                            tapsellInterstitialResponseId =
                                null

                            Log.e(
                                TAG,
                                "Tapsell interstitial show error: $message"
                            )

                            showAdiveryInterstitial()
                        }
                    }
                )

                return

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Tapsell interstitial exception",
                    e
                )

                tapsellInterstitialResponseId = null
            }
        }

        showAdiveryInterstitial()
    }

    fun showRewarded(
        activity: Activity,
        onRewarded: () -> Unit
    ) {

        val tapsellId =
            tapsellRewardedResponseId

        if (!tapsellId.isNullOrBlank()) {

            try {

                TapsellPlus.showRewardedVideoAd(
                    activity,
                    tapsellId,
                    object : AdShowListener() {

                        override fun onOpened() {
                            Log.d(
                                TAG,
                                "Tapsell rewarded opened"
                            )
                        }

                        override fun onClosed() {

                            tapsellRewardedResponseId =
                                null

                            prepareRewarded(activity)

                            Log.d(
                                TAG,
                                "Tapsell rewarded closed"
                            )
                        }

                        override fun onRewarded() {

                            Log.d(
                                TAG,
                                "Tapsell rewarded completed"
                            )

                            onRewarded()
                        }

                        override fun onError(
                            message: String
                        ) {

                            tapsellRewardedResponseId =
                                null

                            Log.e(
                                TAG,
                                "Tapsell rewarded show error: $message"
                            )

                            showAdiveryRewarded(
                                onRewarded
                            )
                        }
                    }
                )

                return

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Tapsell rewarded exception",
                    e
                )

                tapsellRewardedResponseId = null
            }
        }

        showAdiveryRewarded(
            onRewarded
        )
    }

    private fun showAdiveryInterstitial() {

        try {

            if (
                Adivery.isLoaded(
                    BuildConfig.ADIVERY_INTERSTITIAL
                )
            ) {

                Adivery.showAd(
                    BuildConfig.ADIVERY_INTERSTITIAL
                )

                Log.d(
                    TAG,
                    "Adivery interstitial shown"
                )

            } else {

                Log.d(
                    TAG,
                    "No interstitial ad available"
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery interstitial error",
                e
            )
        }
    }

    private fun showAdiveryRewarded(
        onRewarded: () -> Unit
    ) {

        try {

            if (
                Adivery.isLoaded(
                    BuildConfig.ADIVERY_REWARDED
                )
            ) {

                Adivery.showAd(
                    BuildConfig.ADIVERY_REWARDED
                )

                Log.d(
                    TAG,
                    "Adivery rewarded shown"
                )

                onRewarded()

            } else {

                Log.d(
                    TAG,
                    "No rewarded ad available"
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery rewarded error",
                e
            )
        }
    }
}
