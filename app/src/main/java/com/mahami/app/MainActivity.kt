package com.mahami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = TaskStorage(applicationContext)
        val initialData = storage.read()
        TaskNotificationScheduler.createChannel(applicationContext)
        TaskNotificationScheduler.rescheduleAll(applicationContext, initialData.tasks)
        setContent {
            MahamiApp(
                initialData = initialData,
                onDataChanged = storage::write,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        TaskNotificationScheduler.rescheduleAll(applicationContext, TaskStorage(applicationContext).read().tasks)
    }
}
