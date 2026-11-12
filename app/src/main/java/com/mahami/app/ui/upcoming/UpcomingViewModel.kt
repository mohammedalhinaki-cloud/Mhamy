package com.mahami.app.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.repository.TaskRepository
import com.mahami.app.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One date group on the Upcoming screen. */
data class UpcomingDayGroup(
    val date: LocalDate,
    val tasks: List<Task>,
)

data class UpcomingUiState(
    val dayGroups: List<UpcomingDayGroup> = emptyList(),
    val noDate: List<Task> = emptyList(),
    val subtaskCounts: Map<Long, SubtaskCount> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    // Same day-boundary note as TodayViewModel: refreshes on next app start.
    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<UpcomingUiState> =
        combine(
            repository.observeUpcoming(today),
            repository.observeNoDate(),
            repository.observeSubtaskCounts(),
        ) { upcoming, noDate, counts ->
            UpcomingUiState(
                // The query is already ordered by date, so the LinkedHashMap
                // grouping below preserves ascending day order automatically.
                dayGroups = upcoming
                    .groupBy { it.dueDate }
                    .mapNotNull { (date, tasks) -> date?.let { UpcomingDayGroup(it, tasks) } },
                noDate = noDate,
                subtaskCounts = counts.associateBy { it.taskId },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UpcomingUiState(),
        )

    fun setCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, completed)
            // Re-sync reminders (Stage 7): cancel/re-schedule, incl. repeat occurrences.
            reminderScheduler.syncAll()
        }
    }
}
