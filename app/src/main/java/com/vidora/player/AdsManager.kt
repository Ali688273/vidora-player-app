package com.vidora.player

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

import com.adivery.sdk.Adivery

import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks
import ir.tapsell.plus.model.TapsellPlusAdModel

object AdsManager {

    private const val TAG = "VidoraAds"

    private const val PREFS_NAME = "vidora_ads"
    private const val OPEN_COUNT_KEY = "player_open_count"

    private const val SHOW_AFTER_OPENS = 4

    private var initialized = false
    private var tapsellInitialized = false

    private var tapsellInterstitialResponseId: String? = null
    private var tapsellRewardedResponseId: String? = null

    private var lifecycleRegistered = false
    private var showingAd = false

    private val mainHandler =
        Handler(Looper.getMainLooper())

    fun initialize(context: Context) {

        if (initialized) {
            return
        }

        initialized = true

        val application =
            context.applicationContext as Application

        initializeAdivery(application)
        initializeTapsell(application)

        registerActivityLifecycle(application)
    }

    private fun initializeAdivery(
        application: Application
    ) {

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

        try {

            TapsellPlus.initialize(
                application,
                BuildConfig.TAPSELL_KEY,
                object : ir.tapsell.plus.TapsellPlusInitListener {

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

    private fun registerActivityLifecycle(
        application: Application
    ) {

        if (lifecycleRegistered) {
            return
        }

        lifecycleRegistered = true

        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: android.os.Bundle?
                ) {
                }

                override fun onActivityStarted(
                    activity: Activity
                ) {

                    if (
                        activity !is PlayerActivity
                    ) {
                        return
                    }

                    prepareInterstitial(
                        activity
                    )

                    registerPlayerOpen(
                        activity
                    )
                }

                override fun onActivityResumed(
                    activity: Activity
                ) {
                }

                override fun onActivityPaused(
                    activity: Activity
                ) {
                }

                override fun onActivityStopped(
                    activity: Activity
                ) {
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: android.os.Bundle
                ) {
                }

                override fun onActivityDestroyed(
                    activity: Activity
                ) {
                }
            }
        )
    }

    private fun registerPlayerOpen(
        activity: Activity
    ) {

        val preferences =
            activity.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val oldCount =
            preferences.getInt(
                OPEN_COUNT_KEY,
                0
            )

        val newCount =
            oldCount + 1

        if (
            newCount >= SHOW_AFTER_OPENS
        ) {

            preferences.edit()
                .putInt(
                    OPEN_COUNT_KEY,
                    0
                )
                .apply()

            mainHandler.postDelayed(
                {
                    if (
                        !activity.isFinishing &&
                        !activity.isDestroyed &&
                        !showingAd
                    ) {
                        showInterstitial(
                            activity
                        )
                    }
                },
                900L
            )

        } else {

            preferences.edit()
                .putInt(
                    OPEN_COUNT_KEY,
                    newCount
                )
                .apply()
        }
    }

    fun prepareInterstitial(
        activity: Activity
    ) {

        if (!tapsellInitialized) {
            return
        }

        if (
            !tapsellInterstitialResponseId.isNullOrBlank()
        ) {
            return
        }

        try {

            TapsellPlus.requestInterstitialAd(
                activity,
                BuildConfig.TAPSELL_INTERSTITIAL,
                object : AdRequestCallback() {

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

    fun prepareRewarded(
        activity: Activity
    ) {

        if (!tapsellInitialized) {
            return
        }

        if (
            !tapsellRewardedResponseId.isNullOrBlank()
        ) {
            return
        }

        try {

            TapsellPlus.requestRewardedVideoAd(
                activity,
                BuildConfig.TAPSELL_REWARDED,
                object : AdRequestCallback() {

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

    fun showInterstitial(
        activity: Activity
    ) {

        if (showingAd) {
            return
        }

        if (
            activity.isFinishing ||
            activity.isDestroyed
        ) {
            return
        }

        val responseId =
            tapsellInterstitialResponseId

        if (
            !responseId.isNullOrBlank()
        ) {

            pausePlayerForAd(activity)

            showingAd = true

            try {

                TapsellAdShowListener.showInterstitial(
                    activity,
                    responseId,

                    {
                        Log.d(
                            TAG,
                            "Tapsell interstitial opened"
                        )
                    },

                    {
                        showingAd = false

                        tapsellInterstitialResponseId =
                            null

                        prepareInterstitial(
                            activity
                        )

                        resumePlayerAfterAd(
                            activity
                        )

                        Log.d(
                            TAG,
                            "Tapsell interstitial closed"
                        )
                    },

                    {
                        tapsellInterstitialResponseId =
                            null

                        Log.e(
                            TAG,
                            "Tapsell interstitial show error"
                        )

                        showAdiveryInterstitial(
                            activity
                        )
                    }
                )

                return

            } catch (e: Exception) {

                showingAd = false

                tapsellInterstitialResponseId =
                    null

                Log.e(
                    TAG,
                    "Tapsell interstitial exception",
                    e
                )
            }
        }

        pausePlayerForAd(activity)

        showAdiveryInterstitial(
            activity
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewarded: () -> Unit
    ) {

        if (showingAd) {
            return
        }

        if (
            activity.isFinishing ||
            activity.isDestroyed
        ) {
            return
        }

        val responseId =
            tapsellRewardedResponseId

        if (
            !responseId.isNullOrBlank()
        ) {

            pausePlayerForAd(activity)

            showingAd = true

            try {

                TapsellAdShowListener.showRewarded(
                    activity,
                    responseId,

                    {
                        Log.d(
                            TAG,
                            "Tapsell rewarded opened"
                        )
                    },

                    {
                        showingAd = false

                        tapsellRewardedResponseId =
                            null

                        prepareRewarded(
                            activity
                        )

                        resumePlayerAfterAd(
                            activity
                        )

                        Log.d(
                            TAG,
                            "Tapsell rewarded closed"
                        )
                    },

                    {
                        Log.d(
                            TAG,
                            "Tapsell rewarded completed"
                        )

                        onRewarded()
                    },

                    {
                        tapsellRewardedResponseId =
                            null

                        Log.e(
                            TAG,
                            "Tapsell rewarded show error"
                        )

                        showAdiveryRewarded(
                            activity,
                            onRewarded
                        )
                    }
                )

                return

            } catch (e: Exception) {

                showingAd = false

                tapsellRewardedResponseId =
                    null

                Log.e(
                    TAG,
                    "Tapsell rewarded exception",
                    e
                )
            }
        }

        pausePlayerForAd(activity)

        showAdiveryRewarded(
            activity,
            onRewarded
        )
    }

    private fun pausePlayerForAd(
        activity: Activity
    ) {

        if (activity !is PlayerActivity) {
            return
        }

        val currentPlayer =
            activity.player

        if (
            currentPlayer != null &&
            currentPlayer.isPlaying
        ) {

            activity.wasPlayingBeforeWindowFocusLoss =
                true

            currentPlayer.pause()

            activity.updatePauseButton()
            activity.updateCenterPlayButton()
        }
    }

    private fun resumePlayerAfterAd(
        activity: Activity
    ) {

        if (activity !is PlayerActivity) {
            return
        }

        if (
            activity.isExitingPlayer ||
            activity.isInPictureInPictureMode
        ) {
            return
        }

        if (
            activity.wasPlayingBeforeWindowFocusLoss
        ) {

            activity.player?.play()

            activity.wasPlayingBeforeWindowFocusLoss =
                false

            activity.updatePauseButton()
            activity.updateCenterPlayButton()
        }
    }

    internal fun isShowingAd(): Boolean {
        return showingAd
    }

    internal fun onPlayerWindowFocusGained(
        activity: PlayerActivity
    ) {
        if (!showingAd) {
            return
        }

        showingAd = false
        resumePlayerAfterAd(activity)
    }

    private fun showAdiveryInterstitial(
        activity: Activity
    ) {

        showingAd = true

        try {

            Adivery.showAd(
                BuildConfig.ADIVERY_INTERSTITIAL
            )

            Log.d(
                TAG,
                "Adivery interstitial requested"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery interstitial error",
                e
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery interstitial error",
                e
            )

            showingAd = false
            resumePlayerAfterAd(activity)

        } finally {

            try {

                Adivery.prepareInterstitialAd(
                    activity.application,
                    BuildConfig.ADIVERY_INTERSTITIAL
                )

            } catch (_: Exception) {
            }
        }
    }

    private fun showAdiveryRewarded(
        activity: Activity,
        onRewarded: () -> Unit
    ) {

        showingAd = true

        try {

            Adivery.showAd(
                BuildConfig.ADIVERY_REWARDED
            )

            Log.d(
                TAG,
                "Adivery rewarded requested"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery rewarded error",
                e
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Adivery rewarded error",
                e
            )

            showingAd = false
            resumePlayerAfterAd(activity)
        }
    }
}
