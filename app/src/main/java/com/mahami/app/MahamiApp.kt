package com.mahami.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
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
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private enum class AppTab(val label: String, val symbol: String) {
    TASKS("مهامي", "✓"),
    SCHEDULED("بموعد", "◷"),
    CALENDAR("التقويم", "▦"),
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

    MaterialTheme(colorScheme = mahamiColorScheme()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { activeTabName = tab.name },
                                icon = { Text(tab.symbol, fontSize = 21.sp) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                },
            ) { contentPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            emptyMessage = if (selectedCategoryId == null) {
                "لا توجد مهام بعد. أضف أول مهمة لتبدأ."
            } else {
                "لا توجد مهام ضمن هذا التصنيف."
            },
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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            emptyMessage = "لم تضف أي مهمة بموعد حتى الآن.",
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
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
        Button(onClick = onAction) {
            Text(actionLabel)
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
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text("الكل") },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) },
                leadingIcon = { CategoryDot(category.color) },
            )
        }
        item {
            AssistChip(
                onClick = onAddCategory,
                label = { Text("+ تصنيف") },
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
    emptyMessage: String,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
        EmptyCard(message = emptyMessage, modifier = modifier)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onTaskUpdated(task.copy(isDone = it)) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
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
                            CategoryDot(category.color)
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
            TextButton(onClick = { onTaskDeleted(task.id) }) {
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
private fun EmptyCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "لا شيء هنا الآن",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    var displayedMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val displayedMonth = YearMonth.parse(displayedMonthText)
    val selectedDate = LocalDate.parse(selectedDateText)
    val datedTasks = tasks.filter { it.dueAt != null }
    val tasksByDate = remember(datedTasks) {
        datedTasks.groupBy { task -> task.dueAt!!.toLocalDate() }
    }
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    fun changeMonth(monthDelta: Long) {
        val nextMonth = displayedMonth.plusMonths(monthDelta)
        displayedMonthText = nextMonth.toString()
        selectedDateText = nextMonth.atDay(1).toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeading(
            title = "التقويم",
            subtitle = "النقاط الملوّنة تمثل المهام التي لها موعد.",
            actionLabel = "إضافة موعد",
            onAction = onAddTask,
        )
        MonthHeader(
            month = displayedMonth,
            onPrevious = { changeMonth(-1) },
            onNext = { changeMonth(1) },
        )
        CalendarGrid(
            month = displayedMonth,
            selectedDate = selectedDate,
            tasksByDate = tasksByDate,
            categoryById = categoryById,
            onDateSelected = { selectedDateText = it.toString() },
        )
        HorizontalDivider()
        SelectedDateTasks(
            date = selectedDate,
            tasks = tasksByDate[selectedDate].orEmpty(),
            categoryById = categoryById,
            onTaskUpdated = onTaskUpdated,
            onTaskDeleted = onTaskDeleted,
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("السابق") }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", arabicLocale)),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onNext) { Text("التالي") }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    categoryById: Map<Long, Category>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val leadingEmptyCells = month.atDay(1).dayOfWeek.value % 7 // Sunday is the first column.
    val paddedCells = buildList<LocalDate?> {
        repeat(leadingEmptyCells) { add(null) }
        (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
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
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        CalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            tasks = tasksByDate[date].orEmpty(),
                            categoryById = categoryById,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateSelected(date) },
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
    isSelected: Boolean,
    isToday: Boolean,
    tasks: List<Task>,
    categoryById: Map<Long, Category>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                tasks.take(3).forEach { task ->
                    val color = categoryById[task.categoryId]?.color ?: 0xFF075E54L
                    CategoryDot(color, size = 5.dp)
                }
                if (tasks.size > 3) {
                    Text(
                        text = "+",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDateTasks(
    date: LocalDate,
    tasks: List<Task>,
    categoryById: Map<Long, Category>,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE، d MMMM", arabicLocale)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (tasks.isEmpty()) {
            Text(
                text = "لا توجد مهام بموعد في هذا اليوم.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private fun formatDateTime(time: Long): String =
    Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMMM، HH:mm", arabicLocale))

private val arabicLocale = Locale("ar", "SA")
private val arabicWeekdays = listOf("أحد", "اثن", "ثلا", "أرب", "خمي", "جمع", "سبت")

@Composable
private fun mahamiColorScheme() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    darkColorScheme(
        primary = Color(0xFF9CCBFF),
        secondary = Color(0xFFB9C9DD),
    )
} else {
    lightColorScheme(
        primary = Color(0xFF075E54),
        secondary = Color(0xFF45645F),
    )
}
