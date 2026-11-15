package com.mahami.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Schedules real device alarms for scheduled, unfinished tasks. */
object TaskNotificationScheduler {
    const val CHANNEL_ID = "scheduled_tasks"
    private const val CHANNEL_NAME = "تذكيرات مهامي"
    private const val EXTRA_TASK_ID = "task_id"
    private const val EXTRA_TASK_TITLE = "task_title"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "تنبيهات المهام ذات الموعد"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun sync(context: Context, previousTasks: List<Task>, currentTasks: List<Task>) {
        previousTasks.filter { it.dueAt != null }.forEach { cancel(context, it.id) }
        currentTasks
            .filter { it.dueAt != null && !it.isDone }
            .forEach { schedule(context, it) }
    }

    fun rescheduleAll(context: Context, tasks: List<Task>) {
        createChannel(context)
        tasks
            .filter { it.dueAt != null && !it.isDone }
            .forEach { schedule(context, it) }
    }

    fun schedule(context: Context, task: Task) {
        val dueAt = task.dueAt ?: return
        if (dueAt <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operation = reminderPendingIntent(context, task.id, task.title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, operation)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, operation)
        } else {
            // A reliable fallback when the user has not granted exact-alarm access yet.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, operation)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context, taskId, ""))
    }

    fun requestExactAlarmAccess(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun reminderPendingIntent(context: Context, taskId: Long, title: String): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "${context.packageName}.TASK_REMINDER.$taskId"
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal fun taskId(intent: Intent): Long = intent.getLongExtra(EXTRA_TASK_ID, 0L)
    internal fun taskTitle(intent: Intent): String = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TaskNotificationScheduler.createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val launchIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            TaskNotificationScheduler.taskId(intent).hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, TaskNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_task_reminder)
            .setContentTitle("حان موعد المهمة")
            .setContentText(TaskNotificationScheduler.taskTitle(intent))
            .setStyle(NotificationCompat.BigTextStyle().bigText(TaskNotificationScheduler.taskTitle(intent)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        NotificationManagerCompat.from(context).notify(TaskNotificationScheduler.taskId(intent).hashCode(), notification)
    }
}

class TaskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            TaskNotificationScheduler.rescheduleAll(context, TaskStorage(context).read().tasks)
        }
    }
}
