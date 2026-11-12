package com.mahami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.mahami.app.ui.MahamiApp
import com.mahami.app.ui.theme.MahamiTheme
import com.mahami.app.widget.MahamiWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MahamiTheme {
                MahamiApp()
            }
        }
        // Keep the home-screen widget (Stage 6) in sync every time the app is opened.
        lifecycleScope.launch { MahamiWidget().updateAll(applicationContext) }
    }
}
