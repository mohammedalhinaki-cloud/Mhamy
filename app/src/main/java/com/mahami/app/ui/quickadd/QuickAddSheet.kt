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
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import com.mahami.app.ui.components.RtlIcons
import com.mahami.app.ui.theme.extendedColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
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
 * Option icons: date & time / priority / note. (List selection is added in
 * Stage 3, together with the Lists screens.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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

    // ---- Date picker dialog ----
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (state.date ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.setDate(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ---- Time picker dialog ----
    if (showTimePicker && state.date != null) {
        val now = LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = state.time?.hour ?: now.hour,
            initialMinute = state.time?.minute ?: now.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("إلغاء") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                    if (state.time != null) {
                        TextButton(
                            onClick = {
                                viewModel.setTime(null)
                                showTimePicker = false
                            },
                        ) { Text("بدون وقت") }
                    }
                }
            },
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
