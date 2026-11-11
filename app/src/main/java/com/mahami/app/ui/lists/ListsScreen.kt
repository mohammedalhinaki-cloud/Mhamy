package com.mahami.app.ui.lists

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahami.app.data.local.dao.TaskListWithCount
import com.mahami.app.ui.components.ListColorDot

/**
 * "Lists" section overview: «+ قائمة جديدة», one row per list (color dot,
 * name, incomplete-task count), and the «المهام المكتملة» entry.
 * Tapping a list opens its detail view; Completed opens the restore screen.
 */
@Composable
fun ListsScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val lists by viewModel.listsWithCount.collectAsStateWithLifecycle()
    val completed by viewModel.completedTasks.collectAsStateWithLifecycle()

    var detailListId by rememberSaveable { mutableLongStateOf(-1L) }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }

    when {
        showCompleted -> CompletedScreen(
            snackbarHostState = snackbarHostState,
            onBack = { showCompleted = false },
            modifier = modifier,
        )
        detailListId > 0L -> ListDetailScreen(
            listId = detailListId,
            snackbarHostState = snackbarHostState,
            onBack = { detailListId = -1L },
            modifier = modifier,
        )
        else -> ListsOverview(
            lists = lists,
            completedCount = completed.size,
            onCreateClick = { showCreateSheet = true },
            onListClick = { detailListId = it },
            onCompletedClick = { showCompleted = true },
            modifier = modifier,
        )
    }

    if (showCreateSheet) {
        ListEditSheet(existing = null, onDismiss = { showCreateSheet = false })
    }
}

@Composable
private fun ListsOverview(
    lists: List<TaskListWithCount>,
    completedCount: Int,
    onCreateClick: () -> Unit,
    onListClick: (Long) -> Unit,
    onCompletedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
    ) {
        item(key = "header") {
            Text(
                text = "القوائم",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ---------- create ----------
        item(key = "create") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "قائمة جديدة",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // ---------- user lists ----------
        items(items = lists, key = { "list_${it.list.id}" }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onListClick(item.list.id) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListColorDot(colorHex = item.list.colorHex, size = 14.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.list.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${item.taskCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
        }

        // ---------- completed entry ----------
        item(key = "completed") {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCompletedClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "المهام المكتملة",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$completedCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
