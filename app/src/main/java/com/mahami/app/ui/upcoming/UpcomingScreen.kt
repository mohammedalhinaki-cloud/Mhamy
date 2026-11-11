package com.mahami.app.ui.upcoming

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahami.app.data.local.entity.Task
import com.mahami.app.ui.components.TaskItem
import com.mahami.app.ui.components.completeTaskWithUndo
import com.mahami.app.ui.taskedit.TaskEditSheet
import com.mahami.app.ui.theme.extendedColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fullDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE، d MMMM", Locale("ar"))
private val dayMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ar"))

/**
 * "Upcoming" section: future tasks grouped by day (ascending),
 * the last group is always "بدون تاريخ" (no date).
 */
@Composable
fun UpcomingScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingTaskId by rememberSaveable { mutableLongStateOf(-1L) }

    fun complete(task: Task) =
        completeTaskWithUndo(scope, snackbarHostState, task.id, viewModel::setCompleted)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
    ) {
        // ---------- header ----------
        item(key = "header") {
            Text(
                text = "القادمة",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ---------- empty state ----------
        if (!uiState.isLoading && uiState.dayGroups.isEmpty() && uiState.noDate.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.extendedColors.priorityNormal.copy(alpha = 0.35f),
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد مهام قادمة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تظهر هنا المهام المؤرخة مستقبلاً والمهام بدون تاريخ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---------- day groups ----------
        uiState.dayGroups.forEach { group ->
            item(key = "day_${group.date}") {
                Column {
                    Text(
                        text = dayLabel(group.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(items = group.tasks, key = { "task_${it.id}" }) { task ->
                TaskItem(
                    task = task,
                    showDate = false,
                    onCompleteClick = { complete(task) },
                    onClick = { editingTaskId = task.id },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "gap_${group.date}") {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ---------- last group: no date ----------
        if (uiState.noDate.isNotEmpty()) {
            item(key = "nodate_header") {
                Column {
                    Text(
                        text = "بدون تاريخ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(items = uiState.noDate, key = { "nodate_${it.id}" }) { task ->
                TaskItem(
                    task = task,
                    showDate = false,
                    onCompleteClick = { complete(task) },
                    onClick = { editingTaskId = task.id },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (editingTaskId != -1L) {
        TaskEditSheet(taskId = editingTaskId, onDismiss = { editingTaskId = -1L })
    }
}

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return if (date == today.plusDays(1)) {
        "غداً · ${date.format(dayMonthFormatter)}"
    } else {
        date.format(fullDayFormatter)
    }
}
