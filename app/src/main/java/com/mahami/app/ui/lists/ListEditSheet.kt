package com.mahami.app.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahami.app.data.local.entity.TaskList
import com.mahami.app.ui.components.ListColorPalette
import com.mahami.app.ui.components.parseListColor
import kotlinx.coroutines.delay

/**
 * Create / edit a list (name + color). Used from the Lists section.
 * Enter or «حفظ» submits; blank names are not saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListEditSheet(
    existing: TaskList?,
    onDismiss: () -> Unit,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var selectedColor by rememberSaveable {
        mutableStateOf(existing?.colorHex ?: ListColorPalette.first())
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    fun submit() {
        if (name.isBlank()) return
        if (existing == null) {
            viewModel.createList(name, selectedColor)
        } else {
            viewModel.updateList(existing, name, selectedColor)
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = if (existing == null) "قائمة جديدة" else "تعديل القائمة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("اسم القائمة") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListColorPalette.forEach { hex ->
                    ColorChoice(
                        colorHex = hex,
                        selected = hex == selectedColor,
                        onClick = { selectedColor = hex },
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { submit() }, enabled = name.isNotBlank()) {
                    Text("حفظ", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ColorChoice(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = parseListColor(colorHex)
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(
                width = 2.dp,
                color = if (selected) color else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(color, CircleShape),
        )
    }
}
