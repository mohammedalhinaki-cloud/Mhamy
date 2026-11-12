package com.mahami.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val overdue: List<Task> = emptyList(),
    val today: List<Task> = emptyList(),
    val subtaskCounts: Map<Long, SubtaskCount> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    /**
     * Captured once per ViewModel instance. The ViewModel (and Activity) is
     * recreated on the next app start, so "today" refreshes automatically;
     * a process surviving past midnight keeps showing the old day until restart
     * (acceptable for a personal app — noted, not handled).
     */
    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<TodayUiState> =
        combine(
            repository.observeOverdue(today),
            repository.observeTasksForDate(today),
            repository.observeSubtaskCounts(),
        ) { overdue, todayTasks, counts ->
            TodayUiState(
                overdue = overdue,
                today = todayTasks,
                subtaskCounts = counts.associateBy { it.taskId },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )

    fun setCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch { repository.setTaskCompleted(taskId, completed) }
    }
}
