package com.mahami.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.repository.TaskRepository
import com.mahami.app.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CompletedViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val completedTasks: StateFlow<List<Task>> =
        taskRepository.observeCompleted()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    /** Restore = mark incomplete; the task returns to its date (or "No date"). */
    fun restore(taskId: Long) {
        viewModelScope.launch {
            taskRepository.setTaskCompleted(taskId, false)
            // Re-sync reminders (Stage 7): a restored task re-arms its reminder if future.
            reminderScheduler.syncAll()
        }
    }
}
