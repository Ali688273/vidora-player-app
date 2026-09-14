package com.vidora.player

import android.app.Activity
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

    fun initialize(context: Context) {

        val applicationContext =
            context.applicationContext

        initializeAdivery(
            applicationContext
        )

        if (!tapsellInitialized) {

            TapsellPlus.initialize(
                applicationContext,
                BuildConfig.TAPSELL_KEY,
                object : TapsellPlusInitListener {

                    override fun onInitializeSuccess(
                        adNetworks: AdNetworks
                    ) {

                        tapsellInitialized = true

                        Log.d(
                            TAG,
                            "Tapsell initialized: ${adNetworks.name()}"
                        )

                        requestTapsellInterstitial(
                            applicationContext
                        )

                        requestTapsellRewarded(
                            applicationContext
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
    }

    private fun initializeAdivery(
        context: Context
    ) {

        try {

            Adivery.configure(
                context.applicationContext,
                BuildConfig.ADIVERY_APP_ID
            )

            Adivery.setLoggingEnabled(false)

            Adivery.prepareInterstitialAd(
                context.applicationContext,
                BuildConfig.ADIVERY_INTERSTITIAL
            )

            Adivery.prepareRewardedAd(
                context.applicationContext,
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

    private fun requestTapsellInterstitial(
        context: Context
    ) {

        TapsellPlus.requestInterstitialAd(
            context,
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

    private fun requestTapsellRewarded(
        context: Context
    ) {

        TapsellPlus.requestRewardedVideoAd(
            context,
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
                object : AdShowListener {

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

                        requestTapsellInterstitial(
                            activity.applicationContext
                        )
                    }

                    override fun onError(
                        error: TapsellPlusErrorModel
                    ) {

                        Log.e(
                            TAG,
                            "Tapsell interstitial show error: $error"
                        )

                        tapsellInterstitialResponseId =
                            null

                        requestTapsellInterstitial(
                            activity.applicationContext
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
                object : AdShowListener {

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

                        requestTapsellRewarded(
                            activity.applicationContext
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
                            "Tapsell rewarded show error: $error"
                        )

                        tapsellRewardedResponseId =
                            null

                        requestTapsellRewarded(
                            activity.applicationContext
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

                return
            }

            Log.d(
                TAG,
                "Adivery interstitial not ready"
            )

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

                onRewarded()

                return
            }

            Log.d(
                TAG,
                "Adivery rewarded not ready"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery rewarded error",
                e
            )
        }
    }
}
