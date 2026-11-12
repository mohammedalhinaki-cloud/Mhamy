package com.mahami.app.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mahami.app.MainActivity
import com.mahami.app.R

/**
 * Fired by AlarmManager at the reminder time. Shows a notification with the
 * task title; tapping it opens the app, tapping «إنجاز» completes the task
 * (handled by [CompleteReceiver]).
 *
 * A plain (non-Hilt) receiver: showing a notification needs no injected state.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_TASK_REMINDER) return
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_TITLE).orEmpty()

        // Android 13+: the runtime permission is requested lazily inside the app;
        // if it was never granted there is simply no notification to show.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openApp = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val completeTask = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            Intent(context, CompleteReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_COMPLETE_TASK
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(title)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(NotificationCompat.Action(null, "إنجاز", completeTask))
            .build()

        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }
}
