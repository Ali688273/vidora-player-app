package com.vidora.player

import android.app.Application

class VidoraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AdsManager.initialize(
            applicationContext
        )
    }
}
