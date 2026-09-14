package com.vidora.player;

import android.app.Activity;

import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

public final class TapsellAdShowListener extends AdShowListener {

    private final Runnable onOpened;
    private final Runnable onClosed;
    private final Runnable onRewarded;
    private final Runnable onError;

    private TapsellAdShowListener(
            Runnable onOpened,
            Runnable onClosed,
            Runnable onRewarded,
            Runnable onError
    ) {
        super();

        this.onOpened = onOpened;
        this.onClosed = onClosed;
        this.onRewarded = onRewarded;
        this.onError = onError;
    }

    public static void showInterstitial(
            Activity activity,
            String responseId,
            Runnable onOpened,
            Runnable onClosed,
            Runnable onError
    ) {

        TapsellPlus.showInterstitialAd(
                activity,
                responseId,
                new TapsellAdShowListener(
                        onOpened,
                        onClosed,
                        null,
                        onError
                )
        );
    }

    public static void showRewarded(
            Activity activity,
            String responseId,
            Runnable onOpened,
            Runnable onClosed,
            Runnable onRewarded,
            Runnable onError
    ) {

        TapsellPlus.showRewardedVideoAd(
                activity,
                responseId,
                new TapsellAdShowListener(
                        onOpened,
                        onClosed,
                        onRewarded,
                        onError
                )
        );
    }

    @Override
    public void onOpened(TapsellPlusAdModel adModel) {
        if (onOpened != null) {
            onOpened.run();
        }
    }

    @Override
    public void onClosed(TapsellPlusAdModel adModel) {
        if (onClosed != null) {
            onClosed.run();
        }
    }

    @Override
    public void onRewarded(TapsellPlusAdModel adModel) {
        if (onRewarded != null) {
            onRewarded.run();
        }
    }

    @Override
    public void onError(TapsellPlusErrorModel error) {
        if (onError != null) {
            onError.run();
        }
    }
}
