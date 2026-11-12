package com.mahami.app.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahami.app.ui.components.ListColorDot
import com.mahami.app.ui.components.RtlIcons
import com.mahami.app.ui.components.TaskItem
import com.mahami.app.ui.components.completeTaskWithUndo
import com.mahami.app.ui.taskedit.TaskEditSheet

/**
 * Tasks of one list, with a top bar: back, color dot + name, edit list, delete list.
 * Deleting a list keeps its tasks (they become list-less).
 */
@Composable
fun ListDetailScreen(
    listId: Long,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListDetailViewModel = hiltViewModel(),
    // Same activity-scoped ListsViewModel instance as the overview — used for
    // list edit/delete actions.
    listsViewModel: ListsViewModel = hiltViewModel(),
) {
    LaunchedEffect(listId) { viewModel.load(listId) }
    DisposableEffect(Unit) { onDispose { viewModel.onLeft() } }
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var editingTaskId by rememberSaveable { mutableLongStateOf(-1L) }
    var showEditListSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteListConfirm by rememberSaveable { mutableStateOf(false) }

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
            uiState.list?.let { list ->
                ListColorDot(colorHex = list.colorHex, size = 14.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showEditListSheet = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "تعديل القائمة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showDeleteListConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "حذف القائمة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        // ---------- tasks ----------
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        ) {
            items(items = uiState.tasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    showDate = false,
                    onCompleteClick = {
                        completeTaskWithUndo(scope, snackbarHostState, task.id, viewModel::setCompleted)
                    },
                    onClick = { editingTaskId = task.id },
                    modifier = Modifier.animateItem(),
                    subtaskProgress = uiState.subtaskCounts[task.id],
                )
            }
            if (!uiState.isLoading && uiState.tasks.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "لا مهام في هذه القائمة",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // ---------- task edit ----------
    if (editingTaskId != -1L) {
        TaskEditSheet(taskId = editingTaskId, onDismiss = { editingTaskId = -1L })
    }

    // ---------- list edit ----------
    val list = uiState.list
    if (showEditListSheet && list != null) {
        ListEditSheet(existing = list, onDismiss = { showEditListSheet = false })
    }

    // ---------- list delete confirmation ----------
    if (showDeleteListConfirm && list != null) {
        AlertDialog(
            onDismissRequest = { showDeleteListConfirm = false },
            title = { Text("حذف القائمة") },
            text = { Text("ستُحذف القائمة «${list.name}»، وستبقى مهامها بدون قائمة.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        listsViewModel.deleteList(list)
                        showDeleteListConfirm = false
                        onBack()
                    },
                ) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteListConfirm = false }) { Text("إلغاء") }
            },
        )
    }
}
