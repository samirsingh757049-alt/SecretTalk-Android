package com.example

import android.app.Application
import org.webrtc.PeerConnectionFactory

class SecretTalkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val options = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)
        } catch (ignored: Throwable) {}
    }
}
