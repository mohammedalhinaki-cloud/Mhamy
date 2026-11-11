package com.mahami.app.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahami.app.data.local.entity.Task
import com.mahami.app.ui.components.TaskItem
import com.mahami.app.ui.theme.extendedColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val headerDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE، d MMMM", Locale("ar"))

/**
 * "Today" section: Overdue group (distinct color) on top, today's tasks below,
 * plus a small remaining-count summary in the header.
 */
@Composable
fun TodayScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    /** One-tap completion with a Snackbar offering "Undo". */
    fun completeWithUndo(task: Task) {
        scope.launch {
            viewModel.setCompleted(task.id, true)
            val result = snackbarHostState.showSnackbar(
                message = "تم إنجاز المهمة",
                actionLabel = "تراجع",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.setCompleted(task.id, false)
            }
        }
    }

    val remainingCount = uiState.overdue.size + uiState.today.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Extra bottom padding keeps the last item clear of the FAB.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
    ) {

        // ---------- header ----------
        item(key = "header") {
            Text(
                text = "اليوم",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = LocalDate.now().format(headerDateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "المهام المتبقية: $remainingCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ---------- Overdue section (distinct color, only when any exist) ----------
        if (uiState.overdue.isNotEmpty()) {
            item(key = "overdue_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "متأخرة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.extendedColors.overdue,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${uiState.overdue.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extendedColors.overdue,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(items = uiState.overdue, key = { "overdue_${it.id}" }) { task ->
                TaskItem(
                    task = task,
                    showDate = true,
                    onCompleteClick = { completeWithUndo(task) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "overdue_gap") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ---------- empty state ----------
        if (!uiState.isLoading && uiState.overdue.isEmpty() && uiState.today.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.extendedColors.priorityNormal.copy(alpha = 0.35f),
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد مهام اليوم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اضغط زر + لإضافة مهمة جديدة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---------- today's tasks ----------
        items(items = uiState.today, key = { "today_${it.id}" }) { task ->
            TaskItem(
                task = task,
                showDate = false,
                onCompleteClick = { completeWithUndo(task) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}
