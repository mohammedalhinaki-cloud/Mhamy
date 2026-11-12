package com.mahami.app.ui.taskedit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahami.app.data.local.entity.SubTask
import com.mahami.app.domain.model.RepeatType
import com.mahami.app.ui.components.MahamiDatePickerDialog
import com.mahami.app.ui.components.MahamiTimePickerDialog
import com.mahami.app.ui.components.ListPickerDialog
import com.mahami.app.ui.theme.extendedColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ar"))
private val timeLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("H:mm")

/**
 * Task edit sheet (opened by tapping a task anywhere).
 * Edits title / notes / priority / date / time / list / repeat; manages subtasks
 * (one level); offers snooze shortcuts and delete.
 * Auto-saves on dismiss and on «حفظ»; a blank title discards edits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditSheet(
    taskId: Long,
    onDismiss: () -> Unit,
    viewModel: TaskEditViewModel = hiltViewModel(),
) {
    LaunchedEffect(taskId) { viewModel.load(taskId) }
    DisposableEffect(Unit) { onDispose { viewModel.onSheetClosed() } }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var showRepeatPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun closeWithSave() = viewModel.save(onDone = onDismiss)

    ModalBottomSheet(
        onDismissRequest = { closeWithSave() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            if (!state.isLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else {
                // ---- top actions: delete / save ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "حذف المهمة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { closeWithSave() }) {
                        Text("حفظ", color = MaterialTheme.colorScheme.primary)
                    }
                }

                // ---- title ----
                TextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("اسم المهمة") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { closeWithSave() }),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                // ---- subtasks (single level) ----
                if (state.subtasks.isNotEmpty()) {
                    val doneCount = state.subtasks.count { it.isCompleted }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "المهام الفرعية",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$doneCount/${state.subtasks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    state.subtasks.forEach { subTask ->
                        SubTaskRow(
                            subTask = subTask,
                            onToggle = { viewModel.toggleSubtask(subTask) },
                            onDelete = { viewModel.deleteSubtask(subTask) },
                        )
                    }
                }
                // ---- add subtask (Enter adds and keeps the field open) ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    TextField(
                        value = state.subtaskInput,
                        onValueChange = viewModel::onSubtaskInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("إضافة مهمة فرعية") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.addSubtask() }),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }

                // ---- notes ----
                TextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ملاحظة") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    minLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                // ---- options row ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(dateLabel(state.date)) },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    )
                    if (state.date != null) {
                        AssistChip(
                            onClick = { showTimePicker = true },
                            label = { Text(state.time?.format(timeLabelFormatter) ?: "بدون وقت") },
                            leadingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = null) },
                        )
                        IconButton(onClick = { viewModel.setDate(null) }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "إزالة التاريخ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    AssistChip(
                        onClick = { showListPicker = true },
                        label = { Text(lists.firstOrNull { it.id == state.listId }?.name ?: "بدون قائمة") },
                        leadingIcon = { Icon(Icons.Filled.List, contentDescription = null) },
                    )
                    AssistChip(
                        onClick = { showRepeatPicker = true },
                        label = { Text(repeatLabel(state.repeatType)) },
                        leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                    )
                    IconButton(onClick = viewModel::toggleImportant) {
                        Icon(
                            Icons.Filled.Flag,
                            contentDescription = "أولوية: مهمة",
                            tint = if (state.isImportant) {
                                MaterialTheme.extendedColors.priorityImportant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                        )
                    }
                }

                // ---- snooze ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "تأجيل:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val today = LocalDate.now()
                    AssistChip(
                        onClick = { viewModel.setDate(today.plusDays(1)) },
                        label = { Text("غداً") },
                    )
                    AssistChip(
                        onClick = { viewModel.setDate(today.plusDays(2)) },
                        label = { Text("بعد يومين") },
                    )
                    AssistChip(
                        onClick = { viewModel.setDate(today.plusWeeks(1)) },
                        label = { Text("الأسبوع القادم") },
                    )
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text("تاريخ مخصص") },
                    )
                }
            }
        }
    }

    // ---- dialogs ----
    if (showDatePicker) {
        MahamiDatePickerDialog(
            initialDate = state.date,
            onConfirm = viewModel::setDate,
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker && state.date != null) {
        MahamiTimePickerDialog(
            initialTime = state.time,
            onConfirm = viewModel::setTime,
            onDismiss = { showTimePicker = false },
        )
    }
    if (showListPicker) {
        ListPickerDialog(
            lists = lists,
            selectedListId = state.listId,
            onSelect = viewModel::setList,
            onDismiss = { showListPicker = false },
        )
    }
    if (showRepeatPicker) {
        RepeatPickerDialog(
            selected = state.repeatType,
            onSelect = viewModel::setRepeat,
            onDismiss = { showRepeatPicker = false },
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف المهمة") },
            text = { Text("سيتم حذف هذه المهمة ومهامها الفرعية نهائياً.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete(onDone = onDismiss)
                    },
                ) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            },
        )
    }
}

/** One subtask row: small circle toggle + title + delete (no nesting, no editing). */
@Composable
private fun SubTaskRow(
    subTask: SubTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val circleColor = MaterialTheme.extendedColors.priorityNormal
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = if (subTask.isCompleted) circleColor else Color.Transparent,
                        shape = CircleShape,
                    )
                    .border(width = 1.5.dp, color = circleColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (subTask.isCompleted) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subTask.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subTask.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Clear,
                contentDescription = "حذف المهمة الفرعية",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Repeat picker: بدون تكرار / يومي / أسبوعي / شهري (Gregorian). */
@Composable
private fun RepeatPickerDialog(
    selected: RepeatType,
    onSelect: (RepeatType) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(RepeatType.NONE, RepeatType.DAILY, RepeatType.WEEKLY, RepeatType.MONTHLY)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("التكرار") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = {
                                onSelect(option)
                                onDismiss()
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = repeatLabel(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
    )
}

private fun dateLabel(date: LocalDate?): String {
    val today = LocalDate.now()
    return when (date) {
        null -> "بدون تاريخ"
        today -> "اليوم"
        today.plusDays(1) -> "غداً"
        else -> date.format(dateLabelFormatter)
    }
}

private fun repeatLabel(repeat: RepeatType): String = when (repeat) {
    RepeatType.NONE -> "بدون تكرار"
    RepeatType.DAILY -> "يومي"
    RepeatType.WEEKLY -> "أسبوعي"
    RepeatType.MONTHLY -> "شهري"
}
