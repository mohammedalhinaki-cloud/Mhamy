package com.mahami.app.ui.quickadd

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahami.app.ui.components.ListPickerDialog
import com.mahami.app.ui.components.MahamiDatePickerDialog
import com.mahami.app.ui.components.MahamiTimePickerDialog
import com.mahami.app.ui.components.RtlIcons
import com.mahami.app.ui.theme.extendedColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val dateLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ar"))
private val timeLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("H:mm")

/**
 * Quick-add bottom sheet: opens with the keyboard ready, Enter saves the task
 * immediately and keeps the sheet open for the next one.
 * Option icons: date & time / list / priority / note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val focusRequester = remember { FocusRequester() }

    // Focus the title field right after the sheet opens → keyboard appears.
    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            TextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("اسم المهمة") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.save() }),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            if (state.showNotes) {
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Date (defaults to today)
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(dateLabel(state.date)) },
                    leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                )
                // Time (only possible when a date is set)
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
                // List
                AssistChip(
                    onClick = { showListPicker = true },
                    label = { Text(lists.firstOrNull { it.id == state.listId }?.name ?: "قائمة") },
                    leadingIcon = { Icon(Icons.Filled.List, contentDescription = null) },
                )
                // Priority
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
                // Note
                IconButton(onClick = viewModel::toggleNotes) {
                    Icon(
                        Icons.Filled.Notes,
                        contentDescription = "ملاحظة",
                        tint = if (state.showNotes) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                    )
                }
                // Save (same as pressing Enter)
                IconButton(onClick = viewModel::save, enabled = state.title.isNotBlank()) {
                    Icon(
                        RtlIcons.Send,
                        contentDescription = "حفظ المهمة",
                        tint = if (state.title.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
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
