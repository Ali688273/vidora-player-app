package com.vidora.player

import android.app.Activity
import android.content.Context
import android.util.Log
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel
import com.adivery.sdk.Adivery

object AdsManager {

    private const val TAG = "VidoraAds"

    private var tapsellInitialized = false

    private var tapsellInterstitialResponseId: String? = null
    private var tapsellRewardedResponseId: String? = null

    fun initialize(context: Context) {

        if (!tapsellInitialized) {

            TapsellPlus.initialize(
                context,
                BuildConfig.TAPSELL_KEY,
                object : ir.tapsell.plus.TapsellPlusInitListener {

                    override fun onInitializeSuccess(
                        adNetworks: ir.tapsell.plus.model.AdNetworks
                    ) {
                        tapsellInitialized = true

                        Log.d(
                            TAG,
                            "Tapsell initialized: ${adNetworks.name()}"
                        )

                        requestTapsellInterstitial(context)
                        requestTapsellRewarded(context)
                    }

                    override fun onInitializeFailed(
                        adNetworks: ir.tapsell.plus.model.AdNetworks,
                        error: ir.tapsell.plus.model.AdNetworkError
                    ) {
                        Log.e(
                            TAG,
                            "Tapsell initialization failed: ${error.errorMessage}"
                        )
                    }
                }
            )
        }

        initializeAdivery(context)
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

        val tapsellResponse =
            tapsellInterstitialResponseId

        if (!tapsellResponse.isNullOrBlank()) {

            TapsellPlus.showInterstitialAd(
                activity,
                tapsellResponse,
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

                        tapsellInterstitialResponseId = null

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

                        tapsellInterstitialResponseId = null

                        requestTapsellInterstitial(
                            activity.applicationContext
                        )

                        showAdiveryInterstitial()
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

        val tapsellResponse =
            tapsellRewardedResponseId

        if (!tapsellResponse.isNullOrBlank()) {

            TapsellPlus.showRewardedVideoAd(
                activity,
                tapsellResponse,
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

                        tapsellRewardedResponseId = null

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
                            "Tapsell rewarded error: $error"
                        )

                        tapsellRewardedResponseId = null

                        requestTapsellRewarded(
                            activity.applicationContext
                        )
                    }
                }
            )

            return
        }

        showAdiveryRewarded(
            activity,
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

            Adivery.prepareInterstitialAd(
                null,
                BuildConfig.ADIVERY_INTERSTITIAL
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
        activity: Activity,
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

            Adivery.prepareRewardedAd(
                activity.applicationContext,
                BuildConfig.ADIVERY_REWARDED
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
