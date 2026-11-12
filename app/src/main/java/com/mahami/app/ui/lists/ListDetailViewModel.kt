package com.mahami.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.local.entity.TaskList
import com.mahami.app.data.repository.TaskListRepository
import com.mahami.app.data.repository.TaskRepository
import com.mahami.app.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListDetailUiState(
    val list: TaskList? = null,
    val tasks: List<Task> = emptyList(),
    val subtaskCounts: Map<Long, SubtaskCount> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskListRepository: TaskListRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var countsJob: Job? = null
    private var currentListId = -1L

    /** (Re)loads when a different list is opened; a no-op while viewing the same list. */
    fun load(listId: Long) {
        if (currentListId == listId) return
        currentListId = listId
        loadJob?.cancel()
        countsJob?.cancel()
        loadJob = viewModelScope.launch {
            val list = taskListRepository.getList(listId)
            taskRepository.observeTasksForList(listId).collect { tasks ->
                _uiState.update { it.copy(list = list, tasks = tasks, isLoading = false) }
            }
        }
        countsJob = viewModelScope.launch {
            taskRepository.observeSubtaskCounts().collect { counts ->
                _uiState.update { it.copy(subtaskCounts = counts.associateBy { c -> c.taskId }) }
            }
        }
    }

    /** Called when the detail view leaves composition so the next list reloads. */
    fun onLeft() {
        currentListId = -1L
        loadJob?.cancel()
        countsJob?.cancel()
        _uiState.value = ListDetailUiState()
    }

    fun setCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.setTaskCompleted(taskId, completed)
            // Re-sync reminders (Stage 7): cancel/re-schedule, incl. repeat occurrences.
            reminderScheduler.syncAll()
        }
    }
}
