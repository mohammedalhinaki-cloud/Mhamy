package com.mahami.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahami.app.data.local.entity.Task
import com.mahami.app.ui.theme.extendedColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ar"))
private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("H:mm")

/**
 * A single task row (Todoist style): priority-colored completion circle,
 * title, and optional date/time meta-line. Ends with a hairline divider.
 *
 * @param showDate when true (Overdue section / other-day contexts) the task's
 * date is shown in the overdue accent color; otherwise only the time is shown.
 */
@Composable
fun TaskItem(
    task: Task,
    showDate: Boolean,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PriorityCircle(
                isImportant = task.isImportant,
                onClick = onCompleteClick,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val metaText = buildMetaText(task, showDate)
                if (metaText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showDate) {
                            MaterialTheme.extendedColors.overdue
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}

/** Todoist-style outlined completion circle; red for Important, gray for Normal. */
@Composable
fun PriorityCircle(
    isImportant: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (isImportant) {
        MaterialTheme.extendedColors.priorityImportant
    } else {
        MaterialTheme.extendedColors.priorityNormal
    }
    // 40.dp touch target around a 22.dp visual circle.
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(onClickLabel = "إنجاز المهمة", role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = if (isImportant) color.copy(alpha = 0.10f) else Color.Transparent,
                    shape = CircleShape,
                )
                .border(width = 2.dp, color = color, shape = CircleShape),
        )
    }
}

private fun buildMetaText(task: Task, showDate: Boolean): String? {
    val parts = mutableListOf<String>()
    if (showDate && task.dueDate != null) parts += task.dueDate.format(shortDateFormatter)
    task.dueTime?.let { parts += it.format(timeFormatter) }
    return if (parts.isEmpty()) null else parts.joinToString(" · ")
}
