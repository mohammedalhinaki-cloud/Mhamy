package com.mahami.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import kotlin.math.roundToInt

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
    val context = LocalContext.current
    var data by remember { mutableStateOf(initialData) }
    var activeTabName by rememberSaveable { mutableStateOf(AppTab.TASKS.name) }
    var selectedTasksCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedScheduledCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorSectionName by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addCategorySectionName by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteConfirmationTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    val activeTab = AppTab.valueOf(activeTabName)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) TaskNotificationScheduler.requestExactAlarmAccess(context)
    }

    fun requestScheduledNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TaskNotificationScheduler.requestExactAlarmAccess(context)
        }
    }

    fun updateData(updated: MahamiData) {
        val previous = data
        data = updated
        onDataChanged(updated)
        if (previous.tasks != updated.tasks) {
            TaskNotificationScheduler.sync(context, previous.tasks, updated.tasks)
        }
    }

    fun addTask(section: TaskSection, title: String, categoryId: Long?, dueAt: Long?) {
        val finalDueAt = if (section == TaskSection.SCHEDULED) dueAt ?: return else null
        val nextId = maxOf(
            System.currentTimeMillis(),
            (data.tasks.maxOfOrNull { it.id } ?: 0L) + 1L,
        )
        updateData(data.copy(tasks = listOf(Task(nextId, title, categoryId, finalDueAt)) + data.tasks))
    }

    fun updateTask(task: Task) {
        updateData(data.copy(tasks = data.tasks.map { if (it.id == task.id) task else it }))
    }

    fun deleteTask(taskId: Long) {
        updateData(data.copy(tasks = data.tasks.filterNot { it.id == taskId }))
    }

    fun saveDraft(draft: TaskDraft) {
        val otherDrafts = data.drafts.filterNot { it.section == draft.section }
        val updatedDrafts = if (draft.title.isBlank()) otherDrafts else otherDrafts + draft
        updateData(data.copy(drafts = updatedDrafts))
    }

    fun clearDraft(section: TaskSection) {
        updateData(data.copy(drafts = data.drafts.filterNot { it.section == section }))
    }

    fun addCategory(section: TaskSection, name: String) {
        val nextId = maxOf(
            System.currentTimeMillis(),
            (data.categories.maxOfOrNull { it.id } ?: 0L) + 1L,
        )
        val color = DEFAULT_CATEGORY_COLORS[data.categories.size % DEFAULT_CATEGORY_COLORS.size]
        updateData(data.copy(categories = data.categories + Category(nextId, name, color, section)))
        if (section == TaskSection.TASKS) selectedTasksCategoryId = nextId else selectedScheduledCategoryId = nextId
    }

    fun renameCategory(categoryId: Long, name: String) {
        updateData(data.copy(categories = data.categories.map { category ->
            if (category.id == categoryId) category.copy(name = name) else category
        }))
    }

    fun deleteCategory(categoryId: Long) {
        updateData(
            data.copy(
                categories = data.categories.filterNot { it.id == categoryId },
                tasks = data.tasks.map { task ->
                    if (task.categoryId == categoryId) task.copy(categoryId = null) else task
                },
                drafts = data.drafts.map { draft ->
                    if (draft.categoryId == categoryId) draft.copy(categoryId = null) else draft
                },
            ),
        )
        if (selectedTasksCategoryId == categoryId) selectedTasksCategoryId = null
        if (selectedScheduledCategoryId == categoryId) selectedScheduledCategoryId = null
    }

    MaterialTheme(colorScheme = mahamiColorScheme(), typography = mahamiTypography) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .height(68.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
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
                                        Icon(tabIcon(tab), tab.label, Modifier.size(20.dp))
                                        Surface(
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(3.dp),
                                            shape = CircleShape,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        ) {}
                                    }
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                alwaysShowLabel = true,
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
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (activeTab) {
                        AppTab.TASKS -> TasksScreen(
                            tasks = data.tasks.filter { it.dueAt == null },
                            categories = data.categories.filter { it.section == TaskSection.TASKS },
                            selectedCategoryId = selectedTasksCategoryId,
                            draft = data.drafts.find { it.section == TaskSection.TASKS },
                            onCategorySelected = { categoryId ->
                                selectedTasksCategoryId = if (selectedTasksCategoryId == categoryId) null else categoryId
                            },
                            onAddCategory = { addCategorySectionName = TaskSection.TASKS.name },
                            onEditCategory = { editingCategoryId = it },
                            onAddTask = { editorSectionName = TaskSection.TASKS.name },
                            onEditTask = { editingTaskId = it },
                            onTaskUpdated = ::updateTask,
                            onTaskDeleted = { deleteConfirmationTaskId = it },
                        )

                        AppTab.SCHEDULED -> ScheduledTasksScreen(
                            tasks = data.tasks.filter { it.dueAt != null },
                            categories = data.categories.filter { it.section == TaskSection.SCHEDULED },
                            selectedCategoryId = selectedScheduledCategoryId,
                            draft = data.drafts.find { it.section == TaskSection.SCHEDULED },
                            onCategorySelected = { categoryId ->
                                selectedScheduledCategoryId = if (selectedScheduledCategoryId == categoryId) null else categoryId
                            },
                            onAddCategory = { addCategorySectionName = TaskSection.SCHEDULED.name },
                            onEditCategory = { editingCategoryId = it },
                            onAddTask = { editorSectionName = TaskSection.SCHEDULED.name },
                            onEditTask = { editingTaskId = it },
                            onTaskUpdated = ::updateTask,
                            onTaskDeleted = { deleteConfirmationTaskId = it },
                        )

                        AppTab.CALENDAR -> CalendarScreen(
                            tasks = data.tasks,
                            categories = data.categories.filter { it.section == TaskSection.SCHEDULED },
                            onAddTask = { editorSectionName = TaskSection.SCHEDULED.name },
                            onEditTask = { editingTaskId = it },
                            onTaskUpdated = ::updateTask,
                            onTaskDeleted = { deleteConfirmationTaskId = it },
                        )
                    }
                }
            }

            val editorSection = editorSectionName?.let(TaskSection::valueOf)
            val editingTask = editingTaskId?.let { id -> data.tasks.find { it.id == id } }
            if (editorSection != null) {
                val existingTask = editingTask?.takeIf {
                    (editorSection == TaskSection.SCHEDULED) == (it.dueAt != null)
                }
                val draft = data.drafts.find { it.section == editorSection }
                TaskEditorDialog(
                    section = editorSection,
                    categories = data.categories.filter { it.section == editorSection },
                    initialTitle = existingTask?.title ?: draft?.title.orEmpty(),
                    initialCategoryId = existingTask?.categoryId ?: draft?.categoryId,
                    initialDueAt = existingTask?.dueAt ?: draft?.dueAt,
                    isEditing = existingTask != null,
                    onDraftChanged = if (existingTask == null) ::saveDraft else null,
                    onDismiss = {
                        editorSectionName = null
                        editingTaskId = null
                    },
                    onSave = { title, categoryId, dueAt ->
                        if (existingTask == null) {
                            addTask(editorSection, title, categoryId, dueAt)
                            clearDraft(editorSection)
                        } else {
                            updateTask(
                                existingTask.copy(
                                    title = title,
                                    categoryId = categoryId,
                                    dueAt = if (editorSection == TaskSection.SCHEDULED) dueAt else null,
                                ),
                            )
                        }
                        if (editorSection == TaskSection.SCHEDULED) requestScheduledNotifications()
                        editorSectionName = null
                        editingTaskId = null
                    },
                )
            }

            val addCategorySection = addCategorySectionName?.let(TaskSection::valueOf)
            if (addCategorySection != null) {
                AddCategoryDialog(
                    onDismiss = { addCategorySectionName = null },
                    onSave = { name ->
                        addCategory(addCategorySection, name)
                        addCategorySectionName = null
                    },
                )
            }

            data.categories.find { it.id == editingCategoryId }?.let { category ->
                EditCategoryDialog(
                    category = category,
                    onDismiss = { editingCategoryId = null },
                    onRename = { name ->
                        renameCategory(category.id, name)
                        editingCategoryId = null
                    },
                    onDelete = {
                        deleteCategory(category.id)
                        editingCategoryId = null
                    },
                )
            }

            data.tasks.find { it.id == deleteConfirmationTaskId }?.let { task ->
                AlertDialog(
                    onDismissRequest = { deleteConfirmationTaskId = null },
                    title = { Text("حذف المهمة؟") },
                    text = { Text("سيتم حذف «${task.title}» وإلغاء تنبيهها إن وُجد.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                deleteTask(task.id)
                                deleteConfirmationTaskId = null
                            },
                            modifier = Modifier.testTag("delete-confirm"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("حذف") }
                    },
                    dismissButton = { TextButton(onClick = { deleteConfirmationTaskId = null }) { Text("إلغاء") } },
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
    draft: TaskDraft?,
    onCategorySelected: (Long) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Long) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    val filteredTasks = tasks.filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeading(
            title = "مهامي",
            subtitle = "مهام مفتوحة بلا موعد. اضغط مطولاً على تصنيف لإدارته.",
            actionLabel = "مهمة جديدة",
            onAction = onAddTask,
        )
        CategoryFilters(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
            onCategoryLongClick = onEditCategory,
            onAddCategory = onAddCategory,
        )
        TaskList(
            modifier = Modifier.weight(1f),
            activeTasks = filteredTasks.filterNot { it.isDone },
            completedTasks = filteredTasks.filter { it.isDone },
            categories = categories,
            draft = draft,
            emptyTitle = if (selectedCategoryId == null) "لا مهام بعد" else "لا مهام في هذا التصنيف",
            emptyMessage = if (selectedCategoryId == null) "أضف أول مهمة لتبدأ يومك." else "جرّب تصنيفاً آخر أو أضف مهمة جديدة.",
            emptyActionLabel = "أضف مهمة",
            emptyIcon = Icons.Outlined.CheckCircle,
            onEmptyAction = onAddTask,
            onEditTask = onEditTask,
            onTaskUpdated = onTaskUpdated,
            onTaskDeleted = onTaskDeleted,
        )
    }
}

@Composable
private fun ScheduledTasksScreen(
    tasks: List<Task>,
    categories: List<Category>,
    selectedCategoryId: Long?,
    draft: TaskDraft?,
    onCategorySelected: (Long) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Long) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    val filteredTasks = tasks.filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeading(
            title = "مهام بموعد",
            subtitle = "التاريخ والوقت مطلوبان، وسيصلك تنبيه عند حلول الموعد.",
            actionLabel = "إضافة موعد",
            onAction = onAddTask,
        )
        CategoryFilters(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
            onCategoryLongClick = onEditCategory,
            onAddCategory = onAddCategory,
        )
        TaskList(
            modifier = Modifier.weight(1f),
            activeTasks = filteredTasks.filterNot { it.isDone }.sortedBy { it.dueAt },
            completedTasks = filteredTasks.filter { it.isDone }.sortedBy { it.dueAt },
            categories = categories,
            draft = draft,
            emptyTitle = "لا شيء هنا الآن",
            emptyMessage = "أضف مهمة بموعد ليظهر تنبيهها في الوقت المحدد.",
            emptyActionLabel = "إضافة موعد",
            emptyIcon = Icons.Outlined.Event,
            onEmptyAction = onAddTask,
            onEditTask = onEditTask,
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        ) { Text(actionLabel, fontWeight = FontWeight.SemiBold) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryFilters(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    onCategoryLongClick: (Long) -> Unit,
    onAddCategory: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.id }) { category ->
            CategoryFilterChip(
                label = category.name,
                selected = selectedCategoryId == category.id,
                dotColor = category.color,
                onClick = { onCategorySelected(category.id) },
                onLongClick = { onCategoryLongClick(category.id) },
            )
        }
        item {
            CategoryFilterChip(
                label = "+ تصنيف",
                selected = false,
                dotColor = null,
                onClick = onAddCategory,
                onLongClick = null,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    dotColor: Long?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clip(shape)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
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
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
    draft: TaskDraft?,
    emptyTitle: String,
    emptyMessage: String,
    emptyActionLabel: String,
    emptyIcon: ImageVector,
    onEmptyAction: () -> Unit,
    onEditTask: (Long) -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    var openSwipeTaskId by remember { mutableStateOf<Long?>(null) }
    val categoryById = categories.associateBy { it.id }
    val orderedActive = activeTasks.sortedWith(compareByDescending<Task> { it.isPinned }.thenByDescending { it.id })
    val orderedCompleted = completedTasks.sortedWith(compareByDescending<Task> { it.isPinned }.thenByDescending { it.id })
    if (orderedActive.isEmpty() && orderedCompleted.isEmpty() && draft?.title.isNullOrBlank()) {
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

    @Composable
    fun taskCard(task: Task) {
        TaskCard(
            task = task,
            category = categoryById[task.categoryId],
            isSwipeOpen = openSwipeTaskId == task.id,
            onSwipeOpened = { openSwipeTaskId = task.id },
            onSwipeClosed = { if (openSwipeTaskId == task.id) openSwipeTaskId = null },
            onEditTask = onEditTask,
            onTaskUpdated = onTaskUpdated,
            onTaskDeleted = onTaskDeleted,
        )
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle("المهام الحالية", orderedActive.size)
        if (!draft?.title.isNullOrBlank()) DraftTaskCard(draft!!, onEmptyAction)
        if (orderedActive.isEmpty()) {
            Text("لا توجد مهام حالية.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            orderedActive.forEach { task -> taskCard(task) }
        }
        if (orderedCompleted.isNotEmpty()) {
            SectionTitle("المكتملة", orderedCompleted.size)
            orderedCompleted.forEach { task -> taskCard(task) }
        }
    }
}

@Composable
private fun DraftTaskCard(draft: TaskDraft, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("مسودة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                SelectionContainer { Text(draft.title, style = MaterialTheme.typography.titleSmall) }
            }
            Text("متابعة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    category: Category?,
    isSwipeOpen: Boolean,
    onSwipeOpened: () -> Unit,
    onSwipeClosed: () -> Unit,
    onEditTask: (Long) -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    SwipeTaskRow(
        taskId = task.id,
        isOpen = isSwipeOpen,
        onOpen = onSwipeOpened,
        onClose = onSwipeClosed,
        onEdit = {
            onSwipeClosed()
            onEditTask(task.id)
        },
        onPin = {
            onSwipeClosed()
            onTaskUpdated(task.copy(isPinned = !task.isPinned))
        },
        onDelete = {
            onSwipeClosed()
            onTaskDeleted(task.id)
        },
    ) {
        val shape = RoundedCornerShape(18.dp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task-content-${task.id}")
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), shape),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (task.isDone) MaterialTheme.colorScheme.surface.copy(alpha = 0.64f) else MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
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
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SelectionContainer {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        if (task.isPinned) Icon(Icons.Outlined.PushPin, "مثبتة", Modifier.size(16.dp), MaterialTheme.colorScheme.primary)
                    }
                    if (category != null || task.dueAt != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (category != null) {
                                CategoryDot(category.color, size = 7.dp)
                                Text(category.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (task.dueAt != null) {
                                Text(formatDateTime(task.dueAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class SwipeAnchor { Closed, Open }

/**
 * The outer layout is deliberately LTR only for geometry: a physical right swipe always
 * translates the card toward the physical right and exposes the panel at physical left.
 * The card content is composed again with the app's original (Arabic RTL) direction.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeTaskRow(
    taskId: Long,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val contentLayoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val actionPanelWidth = 228.dp
    val actionPanelWidthPx = with(density) { actionPanelWidth.toPx() }
    val anchors = remember(actionPanelWidthPx) {
        DraggableAnchors {
            SwipeAnchor.Closed at 0f
            SwipeAnchor.Open at actionPanelWidthPx
        }
    }
    val settleAnimation = remember { tween<Float>(durationMillis = 180) }
    val swipeState = remember(taskId, actionPanelWidthPx) {
        AnchoredDraggableState(
            initialValue = SwipeAnchor.Closed,
            anchors = anchors,
            positionalThreshold = { distance -> distance * 0.40f },
            velocityThreshold = { with(density) { 96.dp.toPx() } },
            snapAnimationSpec = settleAnimation,
            decayAnimationSpec = exponentialDecay(),
        )
    }
    val latestIsOpen by rememberUpdatedState(isOpen)
    val latestOnOpen by rememberUpdatedState(onOpen)
    val latestOnClose by rememberUpdatedState(onClose)
    var cardHeightPx by remember(taskId) { mutableIntStateOf(0) }

    // The parent owns which row is open. This state has fixed anchors, so it does not need
    // to rebuild or update anchors while a user is dragging.
    LaunchedEffect(isOpen) {
        val requestedAnchor = if (isOpen) SwipeAnchor.Open else SwipeAnchor.Closed
        if (swipeState.currentValue != requestedAnchor) swipeState.animateTo(requestedAnchor)
    }

    // This legacy Compose version exposes the settled value as currentValue. The callback is
    // consequently emitted once per finished gesture, never for individual drag deltas.
    LaunchedEffect(swipeState.currentValue) {
        when (swipeState.currentValue) {
            SwipeAnchor.Open -> if (!latestIsOpen) latestOnOpen()
            SwipeAnchor.Closed -> if (latestIsOpen) latestOnClose()
        }
    }

    val offsetPx = swipeState.offset.takeUnless(Float::isNaN) ?: 0f
    val actionPanelHeight = with(density) { cardHeightPx.toDp() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxWidth().clipToBounds()) {
            if (offsetPx > 0.5f && cardHeightPx > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(actionPanelWidth)
                        .height(actionPanelHeight),
                ) {
                    SwipeAction(
                        label = "تعديل",
                        icon = Icons.Outlined.Edit,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onEdit,
                        testTag = "swipe-edit-$taskId",
                        modifier = Modifier.weight(1f),
                    )
                    SwipeAction(
                        label = "أعلى",
                        icon = Icons.Outlined.PushPin,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = onPin,
                        testTag = "swipe-pin-$taskId",
                        modifier = Modifier.weight(1f),
                    )
                    SwipeAction(
                        label = "حذف",
                        icon = Icons.Outlined.Delete,
                        color = MaterialTheme.colorScheme.error,
                        onClick = onDelete,
                        testTag = "swipe-delete-$taskId",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task-card-$taskId")
                    .onSizeChanged { cardHeightPx = it.height }
                    .offset { IntOffset(offsetPx.roundToInt(), 0) }
                    .anchoredDraggable(
                        state = swipeState,
                        orientation = Orientation.Horizontal,
                        // Explicitly use physical screen coordinates: right is always open,
                        // including when Arabic RTL content is inside the card.
                        reverseDirection = false,
                        startDragImmediately = false,
                    )
                    .zIndex(1f),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SwipeAction(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = color,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, label, Modifier.size(19.dp), MaterialTheme.colorScheme.onPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Text("$title ($count)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
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
private fun TaskEditorDialog(
    section: TaskSection,
    categories: List<Category>,
    initialTitle: String,
    initialCategoryId: Long?,
    initialDueAt: Long?,
    isEditing: Boolean,
    onDraftChanged: ((TaskDraft) -> Unit)?,
    onDismiss: () -> Unit,
    onSave: (String, Long?, Long?) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable(initialTitle, isEditing) { mutableStateOf(initialTitle) }
    var categoryId by rememberSaveable(initialCategoryId, isEditing) {
        mutableStateOf(initialCategoryId ?: categories.firstOrNull()?.id)
    }
    var dueAt by rememberSaveable(initialDueAt, isEditing) { mutableStateOf(initialDueAt) }

    fun saveDraft(updatedTitle: String = title, updatedCategoryId: Long? = categoryId, updatedDueAt: Long? = dueAt) {
        onDraftChanged?.invoke(
            TaskDraft(
                section = section,
                title = updatedTitle,
                categoryId = updatedCategoryId,
                dueAt = if (section == TaskSection.SCHEDULED) updatedDueAt else null,
            ),
        )
    }

    val canSave = title.trim().isNotEmpty() && (section != TaskSection.SCHEDULED || dueAt != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "تعديل المهمة" else if (title.isNotBlank()) "مسودة مهمة" else "مهمة جديدة") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { value ->
                        title = value
                        saveDraft(updatedTitle = value)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ما المهمة؟") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canSave) onSave(title.trim(), categoryId, dueAt) },
                    ),
                )
                Text("التصنيف", style = MaterialTheme.typography.labelLarge)
                if (categories.isEmpty()) {
                    Text("أضف تصنيفاً من أعلى الصفحة أولاً.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = categoryId == category.id,
                                onClick = {
                                    categoryId = category.id
                                    saveDraft(updatedCategoryId = category.id)
                                },
                                label = { Text(category.name) },
                                leadingIcon = { CategoryDot(category.color) },
                            )
                        }
                    }
                }
                if (section == TaskSection.SCHEDULED) {
                    HorizontalDivider()
                    Text("الموعد مطلوب", style = MaterialTheme.typography.labelLarge)
                    if (dueAt == null) {
                        OutlinedButton(
                            onClick = {
                                showDateTimePicker(context, null) { selected ->
                                    dueAt = selected
                                    saveDraft(updatedDueAt = selected)
                                }
                            },
                        ) { Text("اختيار تاريخ ووقت") }
                    } else {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    formatDateTime(dueAt!!),
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                TextButton(
                                    onClick = {
                                        showDateTimePicker(context, dueAt) { selected ->
                                            dueAt = selected
                                            saveDraft(updatedDueAt = selected)
                                        }
                                    },
                                ) { Text("تغيير") }
                            }
                        }
                    }
                    Text(
                        "سيتم طلب إذن الإشعارات عند حفظ هذه المهمة.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title.trim(), categoryId, dueAt) }, enabled = canSave) {
                Text(if (isEditing) "حفظ" else "إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
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
                label = { Text("اسم التصنيف") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim()) }, enabled = name.trim().isNotEmpty()) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(category.id) { mutableStateOf(category.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة التصنيف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم التصنيف") },
                    singleLine = true,
                )
                Text(
                    "حذف التصنيف لا يحذف مهامه؛ تبقى المهام محفوظة دون تصنيف.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRename(name.trim()) }, enabled = name.trim().isNotEmpty()) { Text("حفظ الاسم") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("حذف") }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        },
    )
}

@Composable
private fun CalendarScreen(
    tasks: List<Task>,
    categories: List<Category>,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
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
            onEditTask = onEditTask,
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
    onEditTask: (Long) -> Unit,
    onTaskUpdated: (Task) -> Unit,
    onTaskDeleted: (Long) -> Unit,
) {
    var openSwipeTaskId by remember { mutableStateOf<Long?>(null) }
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
                    isSwipeOpen = openSwipeTaskId == task.id,
                    onSwipeOpened = { openSwipeTaskId = task.id },
                    onSwipeClosed = { if (openSwipeTaskId == task.id) openSwipeTaskId = null },
                    onEditTask = onEditTask,
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
