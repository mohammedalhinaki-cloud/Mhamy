package com.mahami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = TaskStorage(applicationContext)
        setContent {
            MahamiApp(
                initialData = storage.read(),
                onDataChanged = storage::write,
            )
        }
    }
}
