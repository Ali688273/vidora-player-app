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
import ir.tapsell.plus.model.TapsellPlusErrorModel

object AdsManager {

    private const val TAG = "VidoraAds"

    private var tapsellInitialized = false

    private var tapsellInterstitialResponseId: String? = null
    private var tapsellRewardedResponseId: String? = null

    fun initialize(
        context: Context
    ) {

        val application =
            context.applicationContext as Application

        initializeAdivery(application)
        initializeTapsell(application)
    }

    private fun initializeAdivery(
        application: Application
    ) {

        try {

            Adivery.configure(
                application,
                BuildConfig.ADIVERY_APP_ID
            )

            Adivery.setLoggingEnabled(false)

            Adivery.prepareInterstitialAd(
                application,
                BuildConfig.ADIVERY_INTERSTITIAL
            )

            Adivery.prepareRewardedAd(
                application,
                BuildConfig.ADIVERY_REWARDED
            )

            Log.d(
                TAG,
                "Adivery initialized"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery initialization error",
                e
            )
        }
    }

    private fun initializeTapsell(
        application: Application
    ) {

        if (tapsellInitialized) {
            return
        }

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
    }

    fun prepareInterstitial(
        activity: Activity
    ) {

        if (!tapsellInitialized) {
            return
        }

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
    }

    fun prepareRewarded(
        activity: Activity
    ) {

        if (!tapsellInitialized) {
            return
        }

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
    }

    fun showInterstitial(
        activity: Activity
    ) {

        val responseId =
            tapsellInterstitialResponseId

        if (!responseId.isNullOrBlank()) {

            TapsellPlus.showInterstitialAd(
                activity,
                responseId,
                object : AdShowListener() {

                    override fun onOpened(
                        adModel: TapsellPlusAdModel
                    ) {
                        Log.d(
                            TAG,
                            "Tapsell interstitial opened"
                        )
                    }

                    override fun onClosed(
                        adModel: TapsellPlusAdModel
                    ) {

                        tapsellInterstitialResponseId =
                            null

                        prepareInterstitial(
                            activity
                        )
                    }

                    override fun onError(
                        error: TapsellPlusErrorModel
                    ) {

                        Log.e(
                            TAG,
                            "Tapsell interstitial error: $error"
                        )

                        tapsellInterstitialResponseId =
                            null

                        prepareInterstitial(
                            activity
                        )
                    }
                }
            )

            return
        }

        showAdiveryInterstitial()
    }

    fun showRewarded(
        activity: Activity,
        onRewarded: () -> Unit
    ) {

        val responseId =
            tapsellRewardedResponseId

        if (!responseId.isNullOrBlank()) {

            TapsellPlus.showRewardedVideoAd(
                activity,
                responseId,
                object : AdShowListener() {

                    override fun onOpened(
                        adModel: TapsellPlusAdModel
                    ) {
                        Log.d(
                            TAG,
                            "Tapsell rewarded opened"
                        )
                    }

                    override fun onClosed(
                        adModel: TapsellPlusAdModel
                    ) {

                        tapsellRewardedResponseId =
                            null

                        prepareRewarded(
                            activity
                        )
                    }

                    override fun onRewarded(
                        adModel: TapsellPlusAdModel
                    ) {

                        onRewarded()
                    }

                    override fun onError(
                        error: TapsellPlusErrorModel
                    ) {

                        Log.e(
                            TAG,
                            "Tapsell rewarded error: $error"
                        )

                        tapsellRewardedResponseId =
                            null

                        prepareRewarded(
                            activity
                        )
                    }
                }
            )

            return
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
