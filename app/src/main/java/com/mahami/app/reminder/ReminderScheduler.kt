package com.mahami.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mahami.app.data.local.dao.TaskDao
import com.mahami.app.data.local.entity.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central alarm manager wrapper (Stage 7).
 *
 * Invariant (self-healing): the set of scheduled alarms must equal the set of
 * DB tasks that are incomplete AND have reminderOffsetMinutes + dueDate + dueTime.
 * [syncAll] re-establishes that invariant and is called after every task
 * mutation (complete / restore / edit / snooze / delete), on boot, and after a
 * notification's «إنجاز» action. Because completing a repeating task copies its
 * reminderOffsetMinutes into the spawned occurrence, syncAll automatically
 * schedules the next occurrence's reminder too.
 *
 * Scheduling: exact alarm when allowed; otherwise an inexact 10-minute window
 * (the UI tells the user when only inexact is available). A scheduled-ids set is
 * kept in SharedPreferences so alarms of removed/changed tasks can be cancelled
 * (AlarmManager itself cannot be enumerated).
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    private val prefs
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True when exact alarms are usable (always below Android 12; user-revocable since). */
    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        val previouslyScheduled: Set<Long> = prefs
            .getStringSet(KEY_SCHEDULED_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

        val now = System.currentTimeMillis()
        val candidates = taskDao.getReminderCandidates()
        val scheduledNow = mutableSetOf<Long>()

        for (task in candidates) {
            val triggerAt = triggerAtMillis(task) ?: continue
            scheduledNow += task.id
            if (triggerAt > now) {
                schedule(task.id, task.title, triggerAt)
            } else {
                // Stale past trigger (e.g. device was off): drop it, don't fire late.
                alarmManager.cancel(alarmPendingIntent(task.id))
            }
        }
        // Cancel alarms whose tasks no longer need them (completed / edited / deleted).
        for (removedId in previouslyScheduled - scheduledNow) {
            alarmManager.cancel(alarmPendingIntent(removedId))
        }
        prefs.edit()
            .putStringSet(KEY_SCHEDULED_IDS, scheduledNow.map(Long::toString).toSet())
            .apply()
    }

    private fun schedule(taskId: Long, title: String, triggerAt: Long) {
        val pendingIntent = alarmPendingIntent(taskId, title)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // Inexact fallback: ~10-minute delivery window starting 5 minutes early.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt - INEXACT_WINDOW_LEAD_MS,
                INEXACT_WINDOW_MS,
                pendingIntent,
            )
        }
    }

    /**
     * PendingIntent identity = component + action + requestCode (taskId); extras don't
     * affect matching, so the same call cancels regardless of the [title] passed.
     */
    private fun alarmPendingIntent(taskId: Long, title: String = ""): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val ACTION_TASK_REMINDER = "com.mahami.app.action.TASK_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.mahami.app.action.COMPLETE_TASK"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"

        private const val PREFS_NAME = "mahami_reminders"
        private const val KEY_SCHEDULED_IDS = "scheduled_task_ids"
        private const val INEXACT_WINDOW_LEAD_MS = 5 * 60_000L
        private const val INEXACT_WINDOW_MS = 10 * 60_000L

        /** Fire time = task date-time minus the chosen offset; null when not remindable. */
        fun triggerAtMillis(task: Task): Long? {
            val date = task.dueDate ?: return null
            val time = task.dueTime ?: return null
            val offset = task.reminderOffsetMinutes ?: return null
            val base = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return base - offset * 60_000L
        }
    }
}
