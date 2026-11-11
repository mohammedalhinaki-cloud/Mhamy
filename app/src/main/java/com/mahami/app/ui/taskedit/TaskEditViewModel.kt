package com.mahami.app.ui.taskedit

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditUiState(
    val isLoaded: Boolean = false,
    val title: String = "",
    val notes: String = "",
    val isImportant: Boolean = false,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val listId: Long? = null,
)

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    taskListRepository: TaskListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    val lists: StateFlow<List<TaskList>> =
        taskListRepository.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    /** Original DB row — fields not editable here (repeat, reminder, createdAt…) are preserved on save. */
    private var original: Task? = null

    private var loadJob: Job? = null
    private var loadedTaskId = -1L

    /** Loads the task once per sheet instance; [onSheetClosed] re-arms it for the next open. */
    fun load(taskId: Long) {
        if (loadedTaskId == taskId) return
        loadedTaskId = taskId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            original = task
            _uiState.value = if (task == null) {
                TaskEditUiState(isLoaded = true)
            } else {
                TaskEditUiState(
                    isLoaded = true,
                    title = task.title,
                    notes = task.notes,
                    isImportant = task.isImportant,
                    date = task.dueDate,
                    time = task.dueTime,
                    listId = task.listId,
                )
            }
        }
    }

    fun onSheetClosed() {
        loadedTaskId = -1L
        loadJob?.cancel()
        _uiState.value = TaskEditUiState()
        original = null
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun toggleImportant() = _uiState.update { it.copy(isImportant = !it.isImportant) }

    fun setDate(date: LocalDate?) =
        _uiState.update { it.copy(date = date, time = if (date == null) null else it.time) }

    fun setTime(time: LocalTime?) =
        _uiState.update { if (it.date != null) it.copy(time = time) else it }

    fun setList(listId: Long?) = _uiState.update { it.copy(listId = listId) }

    /**
     * Persists the editor state, then runs [onDone]. A blank title discards all
     * changes (title is required) but still closes the sheet.
     */
    fun save(onDone: () -> Unit) {
        val base = original ?: run { onDone(); return }
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isEmpty()) {
            onDone()
            return
        }
        viewModelScope.launch {
            taskRepository.updateTask(
                base.copy(
                    title = title,
                    notes = state.notes.trim(),
                    isImportant = state.isImportant,
                    dueDate = state.date,
                    dueTime = state.time,
                    listId = state.listId,
                ),
            )
            onDone()
        }
    }

    /** Deletes the task permanently (a confirmation dialog is shown by the UI). */
    fun delete(onDone: () -> Unit) {
        val base = original ?: run { onDone(); return }
        viewModelScope.launch {
            taskRepository.deleteTask(base)
            onDone()
        }
    }
}
