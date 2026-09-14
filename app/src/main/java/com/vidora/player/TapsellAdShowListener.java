package com.vidora.player;

import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

public final class TapsellAdShowListener extends AdShowListener {

    private final Runnable onOpened;
    private final Runnable onClosed;
    private final Runnable onRewarded;
    private final Runnable onError;

    public TapsellAdShowListener(
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
