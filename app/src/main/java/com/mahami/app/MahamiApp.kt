package com.mahami.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Locale

private enum class AppTab(val label: String) {
    TASKS("مهامي"),
    SCHEDULED("بموعد"),
    CALENDAR("التقويم"),
}

private enum class CalendarMode {
    GREGORIAN,
    HIJRI,
}

private fun tabIcon(tab: AppTab): ImageVector = when (tab) {
    AppTab.TASKS -> Icons.Outlined.CheckCircle
    AppTab.SCHEDULED -> Icons.Outlined.EventAvailable
    AppTab.CALENDAR -> Icons.Outlined.CalendarMonth
}

@Composable
fun MahamiApp(
    initialData: MahamiData,
    onDataChanged: (MahamiData) -> Unit,
) {
    var data by remember { mutableStateOf(initialData) }
    var activeTabName by rememberSaveable { mutableStateOf(AppTab.TASKS.name) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    var showAddCategory by rememberSaveable { mutableStateOf(false) }
    val activeTab = AppTab.valueOf(activeTabName)

    fun updateData(updated: MahamiData) {
        data = updated
        onDataChanged(updated)
    }

    fun addTask(title: String, categoryId: Long?, dueAt: Long?) {
        val nextId = maxOf(
            System.currentTimeMillis(),
            (data.tasks.maxOfOrNull { it.id } ?: 0L) + 1,
        )
        updateData(
            data.copy(
                tasks = listOf(Task(nextId, title, categoryId, dueAt)) + data.tasks,
            ),
        )
    }

    fun updateTask(task: Task) {
        updateData(data.copy(tasks = data.tasks.map { if (it.id == task.id) task else it }))
    }

    fun deleteTask(taskId: Long) {
        updateData(data.copy(tasks = data.tasks.filterNot { it.id == taskId }))
    }

    fun addCategory(name: String) {
        val nextId = maxOf(
            System.currentTimeMillis(),
            (data.categories.maxOfOrNull { it.id } ?: 0L) + 1,
        )
        val color = DEFAULT_CATEGORY_COLORS[data.categories.size % DEFAULT_CATEGORY_COLORS.size]
        updateData(data.copy(categories = data.categories + Category(nextId, name, color)))
        selectedCategoryId = nextId
    }

    MaterialTheme(
        colorScheme = mahamiColorScheme(),
        typography = mahamiTypography,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                        ),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTabName = tab.name },
                                icon = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = tabIcon(tab),
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Surface(
                                            modifier = Modifier.size(4.dp),
                                            shape = CircleShape,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            },
                                        ) {}
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                },
            ) { contentPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (activeTab) {
                        AppTab.TASKS -> TasksScreen(
                            tasks = data.tasks,
                            categories = data.categories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = { selectedCategoryId = it },
                            onAddCategory = { showAddCategory = true },
                            onAddTask = { showAddTask = true },
                            onTaskUpdated = ::updateTask,
                            onTaskDeleted = ::deleteTask,
                        )

                        AppTab.SCHEDULED -> ScheduledTasksScreen(
                            tasks = data.tasks,
                            categories = data.categories,
                            onAddTask = { showAddTask = true },
                            onTaskUpdated = ::updateTask,
                            onTaskDeleted = ::deleteTask,
                        )

                        AppTab.CALENDAR -> CalendarScreen(
                            tasks = data.tasks,
                            categories = data.categories,
                            onAddTask = { showAddTask = true },
                            onTaskUpdated = ::updateTask,
                            onTaskDeleted = ::deleteTask,
                        )
                    }
                }
            }

            if (showAddTask) {
                AddTaskDialog(
                    categories = data.categories,
                    initialCategoryId = selectedCategoryId,
                    onDismiss = { showAddTask = false },
                    onSave = { title, categoryId, dueAt ->
                        addTask(title, categoryId, dueAt)
                        showAddTask = false
                    },
                )
            }

            if (showAddCategory) {
                AddCategoryDialog(
                    onDismiss = { showAddCategory = false },
                    onSave = { name ->
                        addCategory(name)
                        showAddCategory = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<Task>,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onAddCategory: () -> Unit,
    onAddTask: () -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    val filteredTasks = tasks.filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
    val activeTasks = filteredTasks.filterNot { it.isDone }
    val completedTasks = filteredTasks.filter { it.isDone }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeading(
            title = "مهامي",
            subtitle = "رتّب مهامك بحسب التصنيف، وأضف موعداً عند الحاجة.",
            actionLabel = "مهمة جديدة",
            onAction = onAddTask,
        )
        CategoryFilters(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
            onAddCategory = onAddCategory,
        )
        TaskList(
            modifier = Modifier.weight(1f),
            activeTasks = activeTasks,
            completedTasks = completedTasks,
            categories = categories,
            emptyTitle = if (selectedCategoryId == null) "لا مهام بعد" else "لا مهام في هذا التصنيف",
            emptyMessage = if (selectedCategoryId == null) {
                "أضف أول مهمة لتبدأ يومك."
            } else {
                "جرّب تصنيفاً آخر أو أضف مهمة جديدة."
            },
            emptyActionLabel = "أضف مهمة",
            emptyIcon = Icons.Outlined.CheckCircle,
            onEmptyAction = onAddTask,
            onTaskUpdated = onTaskUpdated,
            onTaskDeleted = onTaskDeleted,
        )
    }
}

@Composable
private fun ScheduledTasksScreen(
    tasks: List<Task>,
    categories: List<Category>,
    onAddTask: () -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    val datedTasks = tasks.filter { it.dueAt != null }
    val activeTasks = datedTasks.filterNot { it.isDone }.sortedBy { it.dueAt }
    val completedTasks = datedTasks.filter { it.isDone }.sortedBy { it.dueAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeading(
            title = "مهام بموعد",
            subtitle = "هذه المهام التي اخترت لها تاريخاً ووقتاً.",
            actionLabel = "إضافة موعد",
            onAction = onAddTask,
        )
        TaskList(
            modifier = Modifier.weight(1f),
            activeTasks = activeTasks,
            completedTasks = completedTasks,
            categories = categories,
            emptyTitle = "لا شيء هنا الآن",
            emptyMessage = "لم تضف أي مهمة بموعد حتى الآن.",
            emptyActionLabel = "إضافة موعد",
            emptyIcon = Icons.Outlined.Event,
            onEmptyAction = onAddTask,
            onTaskUpdated = onTaskUpdated,
            onTaskDeleted = onTaskDeleted,
        )
    }
}

@Composable
private fun ScreenHeading(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onAction,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        ) {
            Text(actionLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryFilters(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onAddCategory: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            CategoryFilterChip(
                label = "الكل",
                selected = selectedCategoryId == null,
                dotColor = null,
                onClick = { onCategorySelected(null) },
            )
        }
        items(categories, key = { it.id }) { category ->
            CategoryFilterChip(
                label = category.name,
                selected = selectedCategoryId == category.id,
                dotColor = category.color,
                onClick = { onCategorySelected(category.id) },
            )
        }
        item {
            CategoryFilterChip(
                label = "+ تصنيف",
                selected = false,
                dotColor = null,
                onClick = onAddCategory,
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    dotColor: Long?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dotColor != null) CategoryDot(dotColor, size = 8.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun TaskList(
    modifier: Modifier = Modifier,
    activeTasks: List<Task>,
    completedTasks: List<Task>,
    categories: List<Category>,
    emptyTitle: String,
    emptyMessage: String,
    emptyActionLabel: String,
    emptyIcon: ImageVector,
    onEmptyAction: () -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
        PremiumEmptyState(
            modifier = modifier,
            icon = emptyIcon,
            title = emptyTitle,
            message = emptyMessage,
            actionLabel = emptyActionLabel,
            onAction = onEmptyAction,
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "active-heading") {
            SectionTitle("المهام الحالية", activeTasks.size)
        }
        if (activeTasks.isEmpty()) {
            item(key = "active-empty") {
                Text(
                    text = "لا توجد مهام حالية.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(activeTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    category = categories.find { it.id == task.categoryId },
                    onTaskUpdated = onTaskUpdated,
                    onTaskDeleted = onTaskDeleted,
                )
            }
        }

        if (completedTasks.isNotEmpty()) {
            item(key = "completed-heading") {
                SectionTitle("المكتملة", completedTasks.size)
            }
            items(completedTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    category = categories.find { it.id == task.categoryId },
                    onTaskUpdated = onTaskUpdated,
                    onTaskDeleted = onTaskDeleted,
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    category: Category?,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onTaskUpdated(task.copy(isDone = it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (category != null || task.dueAt != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (category != null) {
                            CategoryDot(category.color, size = 7.dp)
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (task.dueAt != null) {
                            Text(
                                text = formatDateTime(task.dueAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = { onTaskDeleted(task.id) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Text("حذف")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun PremiumEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    categories: List<Category>,
    initialCategoryId: Long?,
    onDismiss: () -> Unit,
    onSave: (String, Long?, Long?) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(initialCategoryId) }
    var dueAt by rememberSaveable { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مهمة جديدة") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ما المهمة؟") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (title.trim().isNotEmpty()) onSave(title.trim(), categoryId, dueAt)
                        },
                    ),
                )
                Text(
                    text = "التصنيف",
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = categoryId == null,
                            onClick = { categoryId = null },
                            label = { Text("بلا تصنيف") },
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name) },
                            leadingIcon = { CategoryDot(category.color) },
                        )
                    }
                }
                HorizontalDivider()
                Text(
                    text = "الموعد اختياري",
                    style = MaterialTheme.typography.labelLarge,
                )
                if (dueAt == null) {
                    OutlinedButton(
                        onClick = { showDateTimePicker(context, null) { dueAt = it } },
                    ) {
                        Text("اختيار تاريخ ووقت")
                    }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatDateTime(dueAt!!),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            TextButton(onClick = { dueAt = null }) {
                                Text("إزالة")
                            }
                        }
                    }
                    TextButton(onClick = { showDateTimePicker(context, dueAt) { dueAt = it } }) {
                        Text("تغيير الموعد")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), categoryId, dueAt) },
                enabled = title.trim().isNotEmpty(),
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تصنيف جديد") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم التصنيف أو الفلتر") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim()) },
                enabled = name.trim().isNotEmpty(),
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
private fun CalendarScreen(
    tasks: List<Task>,
    categories: List<Category>,
    onAddTask: () -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    val todayHijri = remember { HijrahChronology.INSTANCE.dateNow() }
    var calendarModeName by rememberSaveable { mutableStateOf(CalendarMode.GREGORIAN.name) }
    var displayedGregorianMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var displayedHijriYear by rememberSaveable {
        mutableStateOf(todayHijri.get(ChronoField.YEAR_OF_ERA))
    }
    var displayedHijriMonth by rememberSaveable {
        mutableStateOf(todayHijri.get(ChronoField.MONTH_OF_YEAR))
    }
    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }

    val calendarMode = CalendarMode.valueOf(calendarModeName)
    val displayedGregorianMonth = YearMonth.parse(displayedGregorianMonthText)
    val selectedDate = LocalDate.parse(selectedDateText)
    val datedTasks = tasks.filter { it.dueAt != null }
    val tasksByDate = remember(datedTasks) {
        datedTasks.groupBy { task -> task.dueAt!!.toLocalDate() }
    }
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    fun changeGregorianMonth(monthDelta: Long) {
        val nextMonth = displayedGregorianMonth.plusMonths(monthDelta)
        displayedGregorianMonthText = nextMonth.toString()
        selectedDateText = nextMonth.atDay(1).toString()
    }

    fun changeHijriMonth(monthDelta: Int) {
        val monthIndex = displayedHijriYear * 12 + displayedHijriMonth - 1 + monthDelta
        displayedHijriYear = monthIndex / 12
        displayedHijriMonth = monthIndex % 12 + 1
        selectedDateText = hijriDate(displayedHijriYear, displayedHijriMonth, 1)
            .toGregorianDate()
            .toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeading(
            title = "التقويم",
            subtitle = "اختر نوع التقويم؛ تظهر النقاط الملوّنة للمهام ذات الموعد.",
            actionLabel = "إضافة موعد",
            onAction = onAddTask,
        )
        CalendarModeFilter(
            mode = calendarMode,
            onModeChanged = { calendarModeName = it.name },
        )

        when (calendarMode) {
            CalendarMode.GREGORIAN -> {
                MonthHeader(
                    title = displayedGregorianMonth.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", arabicLocale),
                    ),
                    onPrevious = { changeGregorianMonth(-1) },
                    onNext = { changeGregorianMonth(1) },
                )
                GregorianCalendarGrid(
                    month = displayedGregorianMonth,
                    selectedDate = selectedDate,
                    tasksByDate = tasksByDate,
                    categoryById = categoryById,
                    onDateSelected = { selectedDateText = it.toString() },
                )
            }

            CalendarMode.HIJRI -> {
                MonthHeader(
                    title = hijriMonthLabel(displayedHijriYear, displayedHijriMonth),
                    onPrevious = { changeHijriMonth(-1) },
                    onNext = { changeHijriMonth(1) },
                )
                HijriCalendarGrid(
                    year = displayedHijriYear,
                    month = displayedHijriMonth,
                    selectedDate = selectedDate,
                    tasksByDate = tasksByDate,
                    categoryById = categoryById,
                    onDateSelected = { selectedDateText = it.toString() },
                )
            }
        }

        HorizontalDivider()
        SelectedDateTasks(
            heading = if (calendarMode == CalendarMode.HIJRI) {
                formatHijriDate(selectedDate)
            } else {
                formatGregorianDate(selectedDate)
            },
            tasks = tasksByDate[selectedDate].orEmpty(),
            categoryById = categoryById,
            onTaskUpdated = onTaskUpdated,
            onTaskDeleted = onTaskDeleted,
        )
    }
}

@Composable
private fun CalendarModeFilter(
    mode: CalendarMode,
    onModeChanged: (CalendarMode) -> Unit,
) {
    val outerShape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.48f),
                shape = outerShape,
            ),
        shape = outerShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CalendarModeOption(
                label = "ميلادي",
                selected = mode == CalendarMode.GREGORIAN,
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(CalendarMode.GREGORIAN) },
            )
            CalendarModeOption(
                label = "هجري أم القرى",
                selected = mode == CalendarMode.HIJRI,
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(CalendarMode.HIJRI) },
            )
        }
    }
}

@Composable
private fun CalendarModeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun MonthHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("السابق") }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onNext) { Text("التالي") }
    }
}

private data class CalendarDay(
    val date: LocalDate,
    val label: String,
)

@Composable
private fun GregorianCalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    categoryById: Map<Long, Category>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val days = (1..month.lengthOfMonth()).map { day ->
        val date = month.atDay(day)
        CalendarDay(date = date, label = day.toString())
    }
    MonthCalendarGrid(
        leadingEmptyCells = month.atDay(1).dayOfWeek.value % 7,
        days = days,
        selectedDate = selectedDate,
        tasksByDate = tasksByDate,
        categoryById = categoryById,
        onDateSelected = onDateSelected,
    )
}

@Composable
private fun HijriCalendarGrid(
    year: Int,
    month: Int,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    categoryById: Map<Long, Category>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val firstDay = hijriDate(year, month, 1)
    val days = (1..firstDay.lengthOfMonth()).map { day ->
        val hijriDay = hijriDate(year, month, day)
        CalendarDay(date = hijriDay.toGregorianDate(), label = day.toString())
    }
    MonthCalendarGrid(
        leadingEmptyCells = firstDay.toGregorianDate().dayOfWeek.value % 7,
        days = days,
        selectedDate = selectedDate,
        tasksByDate = tasksByDate,
        categoryById = categoryById,
        onDateSelected = onDateSelected,
    )
}

@Composable
private fun MonthCalendarGrid(
    leadingEmptyCells: Int,
    days: List<CalendarDay>,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    categoryById: Map<Long, Category>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val paddedCells = buildList<CalendarDay?> {
        repeat(leadingEmptyCells) { add(null) }
        addAll(days)
        while (size % 7 != 0) add(null)
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            arabicWeekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        paddedCells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        CalendarDayCell(
                            date = day.date,
                            label = day.label,
                            isSelected = day.date == selectedDate,
                            isToday = day.date == LocalDate.now(),
                            tasks = tasksByDate[day.date].orEmpty(),
                            categoryById = categoryById,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateSelected(day.date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    label: String,
    isSelected: Boolean,
    isToday: Boolean,
    tasks: List<Task>,
    categoryById: Map<Long, Category>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val labelColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = labelColor,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                tasks.take(3).forEach { task ->
                    val color = categoryById[task.categoryId]?.color ?: 0xFF6D7CFFL
                    CategoryDot(color, size = 4.dp)
                }
                if (tasks.size > 3) {
                    Text(
                        text = "+",
                        fontSize = 8.sp,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDateTasks(
    heading: String,
    tasks: List<Task>,
    categoryById: Map<Long, Category>,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = "لا توجد مهام بموعد في هذا اليوم.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            tasks.sortedBy { it.dueAt }.take(3).forEach { task ->
                TaskCard(
                    task = task,
                    category = categoryById[task.categoryId],
                    onTaskUpdated = onTaskUpdated,
                    onTaskDeleted = onTaskDeleted,
                )
            }
            if (tasks.size > 3) {
                Text(
                    text = "+ ${tasks.size - 3} مهام أخرى في هذا اليوم",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryDot(color: Long, size: androidx.compose.ui.unit.Dp = 9.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, Color(color).copy(alpha = 0.25f), CircleShape)
            .padding(1.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color(color),
        ) {}
    }
}

private fun showDateTimePicker(
    context: Context,
    initialTime: Long?,
    onSelected: (Long) -> Unit,
) {
    val initial = Calendar.getInstance().apply {
        if (initialTime != null) timeInMillis = initialTime
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelected(selected.timeInMillis)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(context),
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH),
    ).show()
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** Android's built-in Hijrah chronology uses the Umm al-Qura calendar data. */
private fun hijriDate(year: Int, month: Int, day: Int): HijrahDate =
    HijrahChronology.INSTANCE.date(year, month, day)

private fun HijrahDate.toGregorianDate(): LocalDate = LocalDate.ofEpochDay(toEpochDay())

private fun hijriMonthLabel(year: Int, month: Int): String =
    "${hijriMonthNames[month - 1]} $year هـ"

private fun formatGregorianDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE، d MMMM", arabicLocale))

private fun formatHijriDate(date: LocalDate): String {
    val hijriDate = HijrahChronology.INSTANCE.date(date)
    val year = hijriDate.get(ChronoField.YEAR_OF_ERA)
    val month = hijriDate.get(ChronoField.MONTH_OF_YEAR)
    val day = hijriDate.get(ChronoField.DAY_OF_MONTH)
    val weekday = date.format(DateTimeFormatter.ofPattern("EEEE", arabicLocale))
    return "$weekday، $day ${hijriMonthNames[month - 1]} $year هـ"
}

private fun formatDateTime(time: Long): String =
    Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMMM، HH:mm", arabicLocale))

private val arabicLocale = Locale("ar", "SA")
private val arabicWeekdays = listOf("أحد", "اثن", "ثلا", "أرب", "خمي", "جمع", "سبت")
private val hijriMonthNames = listOf(
    "محرم",
    "صفر",
    "ربيع الأول",
    "ربيع الآخر",
    "جمادى الأولى",
    "جمادى الآخرة",
    "رجب",
    "شعبان",
    "رمضان",
    "شوال",
    "ذو القعدة",
    "ذو الحجة",
)

private val mahamiArabicFont = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

private val mahamiTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 25.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = mahamiArabicFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

private fun mahamiColorScheme() = darkColorScheme(
    primary = Color(0xFF6D7CFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF293A80),
    onPrimaryContainer = Color(0xFFE1E7FF),
    secondary = Color(0xFF38C9B4),
    onSecondary = Color(0xFF062B27),
    secondaryContainer = Color(0xFF173F3A),
    onSecondaryContainer = Color(0xFFB8F4EA),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1A2638),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF233247),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF718198),
    error = Color(0xFFFB7185),
)
