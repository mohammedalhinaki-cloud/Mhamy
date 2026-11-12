package com.mahami.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One-tap completion with a Snackbar offering "Undo".
 * Shared by every section that lists tasks (Today / Upcoming / Lists).
 */
fun completeTaskWithUndo(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    taskId: Long,
    setCompleted: (taskId: Long, completed: Boolean) -> Unit,
) {
    scope.launch {
        setCompleted(taskId, true)
        val result = snackbarHostState.showSnackbar(
            message = "تم إنجاز المهمة",
            actionLabel = "تراجع",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            setCompleted(taskId, false)
        }
    }
}
