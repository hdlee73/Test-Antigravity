package io.github.hdlee73.etftracker

import android.app.Application
import io.github.hdlee73.etftracker.data.log.CrashLogger

class EtfTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
