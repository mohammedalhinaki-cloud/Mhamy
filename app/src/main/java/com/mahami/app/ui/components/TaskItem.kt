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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.entity.Task
import com.mahami.app.ui.theme.extendedColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ar"))
private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("H:mm")

/**
 * A single task row (Todoist style): priority-colored completion circle,
 * title, subtask progress ("2/5" + mini progress bar), optional date/time
 * meta-line. Ends with a hairline divider.
 *
 * @param showDate when true (Overdue section) the task's date is shown in the
 * overdue accent color; otherwise only the time is shown.
 * @param completedLook true on the Completed screen: filled check circle,
 * struck-through title, meta shows the completion date; onCompleteClick then
 * means "restore" (subtask progress is hidden in this mode).
 * @param subtaskProgress the parent's subtask progress, if it has subtasks.
 */
@Composable
fun TaskItem(
    task: Task,
    showDate: Boolean,
    onCompleteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    completedLook: Boolean = false,
    subtaskProgress: SubtaskCount? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PriorityCircle(
                isImportant = task.isImportant,
                filled = completedLook,
                onClick = onCompleteClick,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (completedLook) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (completedLook) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val metaText = buildMetaText(task, showDate, completedLook)
                val progress = if (completedLook) null else subtaskProgress?.takeIf { it.total > 0 }
                if (metaText != null || progress != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (progress != null) {
                            Text(
                                text = "${progress.completed}/${progress.total}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            LinearProgressIndicator(
                                progress = {
                                    progress.completed.toFloat() / progress.total.toFloat()
                                },
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                            )
                            if (metaText != null) Spacer(modifier = Modifier.width(10.dp))
                        }
                        if (metaText != null) {
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
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}

/**
 * Todoist-style completion circle; red for Important, gray for Normal.
 * [filled] shows the completed state (solid circle + white check).
 */
@Composable
fun PriorityCircle(
    isImportant: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
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
                    color = when {
                        filled -> color
                        isImportant -> color.copy(alpha = 0.10f)
                        else -> Color.Transparent
                    },
                    shape = CircleShape,
                )
                .border(width = 2.dp, color = color, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (filled) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private fun buildMetaText(task: Task, showDate: Boolean, completedLook: Boolean): String? {
    if (completedLook) {
        val completedDay = task.completedAt
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate()
            ?: return null
        return "أُنجزت ${completedDay.format(shortDateFormatter)}"
    }
    val parts = mutableListOf<String>()
    if (showDate && task.dueDate != null) parts += task.dueDate.format(shortDateFormatter)
    task.dueTime?.let { parts += it.format(timeFormatter) }
    return if (parts.isEmpty()) null else parts.joinToString(" · ")
}
