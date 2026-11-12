package com.mahami.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mahami.app.reminder.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MahamiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Notification channel for task reminders (Stage 7).
        // Channels are required since API 26 — our minSdk — so this is unconditional.
        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            "تنبيهات المهام",
            NotificationManager.IMPORTANCE_HIGH,
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
