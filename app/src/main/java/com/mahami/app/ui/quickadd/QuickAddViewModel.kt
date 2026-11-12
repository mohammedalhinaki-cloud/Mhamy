package com.mahami.app.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.local.entity.TaskList
import com.mahami.app.data.repository.TaskListRepository
import com.mahami.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickAddUiState(
    val title: String = "",
    val notes: String = "",
    val showNotes: Boolean = false,
    // Quick-add defaults: today's date, no time, normal priority, no list, no reminder.
    val date: LocalDate? = LocalDate.now(),
    val time: LocalTime? = null,
    val isImportant: Boolean = false,
    val listId: Long? = null,
)

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repository: TaskRepository,
    taskListRepository: TaskListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    /** Available lists for the list-picker chip (Stage 3). */
    val lists: StateFlow<List<TaskList>> =
        taskListRepository.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun toggleNotes() = _uiState.update { it.copy(showNotes = !it.showNotes) }

    fun toggleImportant() = _uiState.update { it.copy(isImportant = !it.isImportant) }

    fun setDate(date: LocalDate?) {
        // A task may not keep a time without a date.
        _uiState.update { it.copy(date = date, time = if (date == null) null else it.time) }
    }

    fun setTime(time: LocalTime?) {
        // Guard: a time can never exist without a date.
        _uiState.update { if (it.date != null) it.copy(time = time) else it }
    }

    fun setList(listId: Long?) = _uiState.update { it.copy(listId = listId) }

    /**
     * Saves the task immediately and keeps the sheet open for the next one:
     * text fields are cleared, the other options (date/time/priority/list) are kept.
     */
    fun save() {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isEmpty()) return

        val newTask = Task(
            title = title,
            notes = state.notes.trim(),
            isImportant = state.isImportant,
            dueDate = state.date,
            dueTime = state.time,
            listId = state.listId,
        )
        // Reset text fields synchronously, so rapid Enter presses can never
        // submit the same title twice.
        _uiState.update { it.copy(title = "", notes = "", showNotes = false) }
        viewModelScope.launch { repository.addTask(newTask) }
    }
}
