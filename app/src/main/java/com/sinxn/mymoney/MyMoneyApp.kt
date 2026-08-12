package com.sinxn.mymoney

import android.app.Application
import com.sinxn.mymoney.core.worker.DailyReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyMoneyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create notification channels (required for API 26+)
        DailyReminderWorker.createNotificationChannel(this)
    }
}

