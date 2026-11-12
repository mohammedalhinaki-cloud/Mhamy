package com.mahami.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.repository.TaskRepository
import com.mahami.app.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarUiState(
    /** The Gregorian month currently shown in the grid. */
    val displayedMonth: YearMonth = YearMonth.now(),
    /** The tapped day — its tasks are listed below the grid. */
    val selectedDate: LocalDate = LocalDate.now(),
    /** Incomplete tasks of the displayed month, grouped by their due date (day dots). */
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    /** Tasks of the selected day (= tasksByDate[selectedDate], or empty). */
    val selectedTasks: List<Task> = emptyList(),
    val subtaskCounts: Map<Long, SubtaskCount> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val displayedMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    /**
     * Re-queries Room whenever the displayed month changes; Room then keeps
     * the month fresh on any task insert/update/delete automatically.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthTasks: Flow<Map<LocalDate, List<Task>>> =
        displayedMonth.flatMapLatest { month ->
            repository.observeTasksForPeriod(month.atDay(1), month.atEndOfMonth())
                .map { tasks -> tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! } }
        }

    val uiState: StateFlow<CalendarUiState> =
        combine(
            displayedMonth,
            selectedDate,
            monthTasks,
            repository.observeSubtaskCounts(),
        ) { month, selected, byDate, counts ->
            CalendarUiState(
                displayedMonth = month,
                selectedDate = selected,
                tasksByDate = byDate,
                selectedTasks = byDate[selected] ?: emptyList(),
                subtaskCounts = counts.associateBy { it.taskId },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(),
        )

    fun nextMonth() = displayedMonth.update { it.plusMonths(1) }

    fun previousMonth() = displayedMonth.update { it.minusMonths(1) }

    /** Selecting a day also switches the grid to its month (tapping a day cell only). */
    fun selectDay(date: LocalDate) {
        selectedDate.value = date
        if (YearMonth.from(date) != displayedMonth.value) {
            displayedMonth.value = YearMonth.from(date)
        }
    }

    /** «اليوم» shortcut: back to the current month with today selected. */
    fun goToToday() {
        val today = LocalDate.now()
        selectedDate.value = today
        displayedMonth.value = YearMonth.from(today)
    }

    fun setCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, completed)
            // Re-sync reminders (Stage 7): cancel/re-schedule, incl. repeat occurrences.
            reminderScheduler.syncAll()
        }
    }
}
