package com.mahami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = TaskStorage(applicationContext)
        val initialData = storage.read()
        TaskNotificationScheduler.createChannel(applicationContext)
        TaskNotificationScheduler.rescheduleAll(applicationContext, initialData.tasks)
        setContent {
            // The product UI is Arabic, so keep directional layout stable even if the device
            // system language is temporarily different.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MahamiApp(
                    initialData = initialData,
                    onDataChanged = storage::write,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TaskNotificationScheduler.rescheduleAll(applicationContext, TaskStorage(applicationContext).read().tasks)
    }
}
