package com.mahami.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.mahami.app.ui.components.ComingSoonSection
import com.mahami.app.ui.lists.ListsScreen
import com.mahami.app.ui.quickadd.QuickAddSheet
import com.mahami.app.ui.today.TodayScreen
import com.mahami.app.ui.upcoming.UpcomingScreen

private data class BottomSection(val label: String, val icon: ImageVector)

private val bottomSections = listOf(
    BottomSection("اليوم", Icons.Filled.Today),
    BottomSection("القادمة", Icons.Filled.EventNote),
    BottomSection("التقويم", Icons.Filled.CalendarMonth),
    BottomSection("القوائم", Icons.Filled.FormatListBulleted),
)

/**
 * App root: 4-section bottom bar + a fixed (+) FAB visible on all sections,
 * hosting the quick-add bottom sheet and the shared Snackbar.
 * Four fixed sections with no deep links → plain state switching, no
 * Navigation library (simplest option).
 */
@Composable
fun MahamiApp() {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                bottomSections.forEachIndexed { index, section ->
                    NavigationBarItem(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        icon = { Icon(section.icon, contentDescription = section.label) },
                        label = { Text(section.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showQuickAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة مهمة")
            }
        },
    ) { innerPadding ->
        when (selectedSection) {
            0 -> TodayScreen(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(innerPadding),
            )
            1 -> UpcomingScreen(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(innerPadding),
            )
            2 -> ComingSoonSection(title = "التقويم", modifier = Modifier.padding(innerPadding))
            3 -> ListsScreen(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(innerPadding),
            )
        }

        if (showQuickAdd) {
            QuickAddSheet(onDismiss = { showQuickAdd = false })
        }
    }
}
