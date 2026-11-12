package com.mahami.app.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import com.mahami.app.ui.components.RtlIcons
import com.mahami.app.ui.components.TaskItem
import com.mahami.app.ui.taskedit.TaskEditSheet
import kotlinx.coroutines.launch

/**
 * "المهام المكتملة": finished tasks (newest first). Tapping the filled circle
 * restores a task to active; tapping the row opens the edit sheet (delete lives
 * there too).
 */
@Composable
fun CompletedScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompletedViewModel = hiltViewModel(),
) {
    BackHandler { onBack() }

    val completed by viewModel.completedTasks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingTaskId by rememberSaveable { mutableLongStateOf(-1L) }

    Column(modifier = modifier.fillMaxSize()) {
        // ---------- top bar ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(RtlIcons.Back, contentDescription = "رجوع")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "المهام المكتملة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        ) {
            items(items = completed, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    showDate = false,
                    completedLook = true,
                    onCompleteClick = {
                        scope.launch {
                            viewModel.restore(task.id)
                            snackbarHostState.showSnackbar(
                                message = "تمت استعادة المهمة",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                    onClick = { editingTaskId = task.id },
                    modifier = Modifier.animateItem(),
                )
            }
            if (completed.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "لا توجد مهام مكتملة بعد",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (editingTaskId != -1L) {
        TaskEditSheet(taskId = editingTaskId, onDismiss = { editingTaskId = -1L })
    }
}
