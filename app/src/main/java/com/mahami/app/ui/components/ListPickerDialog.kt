package com.mahami.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahami.app.data.local.entity.TaskList

/** Choose a list for a task — "بدون قائمة" (null) is always the first option. */
@Composable
fun ListPickerDialog(
    lists: List<TaskList>,
    selectedListId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("القائمة") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ListOptionRow(
                    label = "بدون قائمة",
                    colorHex = null,
                    selected = selectedListId == null,
                ) {
                    onSelect(null)
                    onDismiss()
                }
                lists.forEach { list ->
                    ListOptionRow(
                        label = list.name,
                        colorHex = list.colorHex,
                        selected = selectedListId == list.id,
                    ) {
                        onSelect(list.id)
                        onDismiss()
                    }
                }
            }
        },
    )
}

@Composable
private fun ListOptionRow(
    label: String,
    colorHex: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        if (colorHex != null) {
            ListColorDot(colorHex = colorHex)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
