package com.vidora.player;

import android.util.Log;

import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

public final class TapsellAdShowListener extends AdShowListener {

    public interface Callback {
        void onOpened(TapsellPlusAdModel adModel);

        void onClosed(TapsellPlusAdModel adModel);

        void onRewarded(TapsellPlusAdModel adModel);

        void onError(TapsellPlusErrorModel error);
    }

    private final Callback callback;

    public TapsellAdShowListener(Callback callback) {
        super();
        this.callback = callback;
    }

    @Override
    public void onOpened(TapsellPlusAdModel adModel) {
        if (callback != null) {
            callback.onOpened(adModel);
        }
    }

    @Override
    public void onClosed(TapsellPlusAdModel adModel) {
        if (callback != null) {
            callback.onClosed(adModel);
        }
    }

    @Override
    public void onRewarded(TapsellPlusAdModel adModel) {
        if (callback != null) {
            callback.onRewarded(adModel);
        }
    }

    @Override
    public void onError(TapsellPlusErrorModel error) {
        if (callback != null) {
            callback.onError(error);
        }
    }
}
