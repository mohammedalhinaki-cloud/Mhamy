package com.mahami.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.mahami.app.data.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hilt bridge for the reminder receivers — receivers are created by the system,
 * so (like the widget) they fetch dependencies from the app's SingletonComponent
 * via an EntryPoint instead of field injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun taskRepository(): TaskRepository
    fun reminderScheduler(): ReminderScheduler
}

/**
 * Handles the «إنجاز» action on a reminder notification: completes the task
 * (repeating rules still apply — the next occurrence is auto-created), dismisses
 * the notification, then re-syncs alarms (cancels this task's alarm and schedules
 * the spawned occurrence's reminder).
 */
class CompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_COMPLETE_TASK) return
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderEntryPoint::class.java,
        )
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                entryPoint.taskRepository().setTaskCompleted(taskId, true)
                NotificationManagerCompat.from(context).cancel(taskId.toInt())
                entryPoint.reminderScheduler().syncAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
