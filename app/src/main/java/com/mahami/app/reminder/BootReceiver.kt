package com.mahami.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BOOT_COMPLETED: alarms do not survive a reboot, so the whole reminder set is
 * rebuilt from the database via [ReminderScheduler.syncAll].
 * Uses [ReminderEntryPoint] (declared in CompleteReceiver.kt), like the widget.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val scheduler = EntryPointAccessors
            .fromApplication(context.applicationContext, ReminderEntryPoint::class.java)
            .reminderScheduler()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                scheduler.syncAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
