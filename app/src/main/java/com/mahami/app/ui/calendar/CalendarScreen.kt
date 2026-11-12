package com.mahami.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.entity.Task
import com.mahami.app.ui.components.RtlIcons
import com.mahami.app.ui.components.TaskItem
import com.mahami.app.ui.components.completeTaskWithUndo
import com.mahami.app.ui.taskedit.TaskEditSheet
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

private val gregorianMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM y", Locale("ar"))
private val selectedDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE، d MMMM", Locale("ar"))

/** Fixed Arabic names of the Hijri (Umm al-Qura) months — avoids locale data quirks. */
private val hijriMonthNames = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
)

/** Weekday initials, Saturday-first (common Gulf convention — the simplest choice). */
private val weekdayInitials = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")

/**
 * «التقويم» — Gregorian monthly calendar.
 * Every day cell shows the Gregorian day number with the Hijri (Umm al-Qura)
 * day number under it, and a dot when the day has open tasks.
 * Tapping a day lists its tasks below the grid (complete with Undo / tap to edit).
 * Navigation is plain month arrows — no pager library (simplest option).
 */
@Composable
fun CalendarScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingTaskId by rememberSaveable { mutableLongStateOf(-1L) }
    val today = LocalDate.now()

    fun complete(task: Task) =
        completeTaskWithUndo(scope, snackbarHostState, task.id, viewModel::setCompleted)

    val weeks = remember(uiState.displayedMonth) { buildWeeks(uiState.displayedMonth) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
    ) {

        // ---------- header ----------
        item(key = "header") {
            Text(
                text = "التقويم",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- month navigation ----------
        item(key = "month_nav") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(RtlIcons.NavigateBefore, contentDescription = "الشهر السابق")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.displayedMonth.format(gregorianMonthFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = hijriMonthRangeLabel(uiState.displayedMonth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = viewModel::goToToday) { Text("اليوم") }
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(RtlIcons.NavigateNext, contentDescription = "الشهر التالي")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ---------- weekday initials row ----------
        item(key = "weekdays") {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayInitials.forEach { initial ->
                    Text(
                        text = initial,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---------- the month grid (4–6 rows of 7 day cells) ----------
        items(items = weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        DayCell(
                            date = date,
                            hasTasks = uiState.tasksByDate.containsKey(date),
                            isToday = date == today,
                            isSelected = date == uiState.selectedDate,
                            onClick = { viewModel.selectDay(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ---------- selected-day task list ----------
        item(key = "day_header") {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مهام ${uiState.selectedDate.format(selectedDayFormatter)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hijriLabel(uiState.selectedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "(${uiState.selectedTasks.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (uiState.selectedTasks.isEmpty()) {
            item(key = "day_empty") {
                Text(
                    text = "لا توجد مهام في هذا اليوم",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        } else {
            items(items = uiState.selectedTasks, key = { "day_${it.id}" }) { task ->
                TaskItem(
                    task = task,
                    showDate = false,
                    onCompleteClick = { complete(task) },
                    onClick = { editingTaskId = task.id },
                    modifier = Modifier.animateItem(),
                    subtaskProgress = uiState.subtaskCounts[task.id],
                )
            }
        }
    }

    if (editingTaskId != -1L) {
        TaskEditSheet(taskId = editingTaskId, onDismiss = { editingTaskId = -1L })
    }
}

/** One month-grid cell: Gregorian number on top, Hijri day number under it, task dot below. */
@Composable
private fun DayCell(
    date: LocalDate,
    hasTasks: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isToday -> MaterialTheme.colorScheme.primary
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = hijriDayOf(date).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected || isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        // Fixed-height slot so cells align whether or not the dot is shown.
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    color = if (hasTasks) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    },
                    shape = CircleShape,
                ),
        )
    }
}

/**
 * Weeks of [month] as 7-cell rows; null = leading cell before the 1st of the month
 * (trailing cells are null too). The week starts on Saturday (see [weekdayInitials]).
 */
private fun buildWeeks(month: YearMonth): List<List<LocalDate?>> {
    // Saturday-first offset: DayOfWeek SATURDAY(6) → 0, SUNDAY(7) → 1, …, FRIDAY(5) → 6.
    val leading = (month.atDay(1).dayOfWeek.value + 1) % 7
    val length = month.lengthOfMonth()
    val lastUsed = leading + length - 1
    val weeks = mutableListOf<List<LocalDate?>>()
    var day = 1
    var index = 0
    while (index <= lastUsed) {
        val week = MutableList<LocalDate?>(7) { null }
        repeat(7) {
            if (index >= leading && day <= length) {
                week[it] = month.atDay(day)
                day++
            }
            index++
        }
        weeks += week
    }
    return weeks
}

/** Hijri (Umm al-Qura) day-of-month via the JDK's built-in HijrahChronology. */
private fun hijriDayOf(date: LocalDate): Int =
    HijrahDate.from(date).get(ChronoField.DAY_OF_MONTH)

/** Full Hijri date: «14 جمادى الأولى 1447هـ». */
private fun hijriLabel(date: LocalDate): String {
    val hijri = HijrahDate.from(date)
    val month = hijriMonthNames[hijri.get(ChronoField.MONTH_OF_YEAR) - 1]
    return "${hijri.get(ChronoField.DAY_OF_MONTH)} $month ${hijri.get(ChronoField.YEAR_OF_ERA)}هـ"
}

/** Hijri month span covered by a displayed Gregorian month: «جمادى الأولى – جمادى الآخرة 1447هـ». */
private fun hijriMonthRangeLabel(month: YearMonth): String {
    val first = HijrahDate.from(month.atDay(1))
    val last = HijrahDate.from(month.atEndOfMonth())
    val firstName = hijriMonthNames[first.get(ChronoField.MONTH_OF_YEAR) - 1]
    val lastName = hijriMonthNames[last.get(ChronoField.MONTH_OF_YEAR) - 1]
    return if (firstName == lastName) {
        "$firstName ${first.get(ChronoField.YEAR_OF_ERA)}هـ"
    } else {
        "$firstName – $lastName ${last.get(ChronoField.YEAR_OF_ERA)}هـ"
    }
}
