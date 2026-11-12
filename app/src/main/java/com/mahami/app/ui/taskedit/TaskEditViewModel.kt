package com.mahami.app.ui.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.entity.SubTask
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.local.entity.TaskList
import com.mahami.app.data.repository.TaskListRepository
import com.mahami.app.data.repository.TaskRepository
import com.mahami.app.domain.model.RepeatType
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
    val repeatType: RepeatType = RepeatType.NONE,
    val subtasks: List<SubTask> = emptyList(),
    val subtaskInput: String = "",
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

    /** Original DB row — fields not editable here (reminder, createdAt…) are preserved on save. */
    private var original: Task? = null

    private var loadJob: Job? = null
    private var subtasksJob: Job? = null
    private var loadedTaskId = -1L

    /** Loads the task once per sheet instance; [onSheetClosed] re-arms it for the next open. */
    fun load(taskId: Long) {
        if (loadedTaskId == taskId) return
        loadedTaskId = taskId
        loadJob?.cancel()
        subtasksJob?.cancel()
        loadJob = viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            original = task
            _uiState.update {
                if (task == null) {
                    TaskEditUiState(isLoaded = true)
                } else {
                    it.copy(
                        isLoaded = true,
                        title = task.title,
                        notes = task.notes,
                        isImportant = task.isImportant,
                        date = task.dueDate,
                        time = task.dueTime,
                        listId = task.listId,
                        repeatType = task.repeatType,
                    )
                }
            }
        }
        subtasksJob = viewModelScope.launch {
            taskRepository.observeSubtasks(taskId).collect { subtasks ->
                _uiState.update { it.copy(subtasks = subtasks) }
            }
        }
    }

    fun onSheetClosed() {
        loadedTaskId = -1L
        loadJob?.cancel()
        subtasksJob?.cancel()
        _uiState.value = TaskEditUiState()
        original = null
    }

    // ---------- task fields ----------

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun toggleImportant() = _uiState.update { it.copy(isImportant = !it.isImportant) }

    fun setDate(date: LocalDate?) =
        _uiState.update {
            it.copy(
                date = date,
                // no date → no time
                time = if (date == null) null else it.time,
                // no date → no repeat (a repeat always has a date)
                repeatType = if (date == null) RepeatType.NONE else it.repeatType,
            )
        }

    fun setTime(time: LocalTime?) =
        _uiState.update { if (it.date != null) it.copy(time = time) else it }

    fun setList(listId: Long?) = _uiState.update { it.copy(listId = listId) }

    fun setRepeat(repeat: RepeatType) =
        _uiState.update {
            if (repeat != RepeatType.NONE && it.date == null) {
                // Repeat requires a date → fall back to today (stated choice).
                it.copy(repeatType = repeat, date = LocalDate.now())
            } else {
                it.copy(repeatType = repeat)
            }
        }

    // ---------- subtasks (one level only) ----------

    fun onSubtaskInputChange(value: String) = _uiState.update { it.copy(subtaskInput = value) }

    /** Adds the subtask instantly (Enter) and keeps the input open for the next one. */
    fun addSubtask() {
        val title = _uiState.value.subtaskInput.trim()
        val taskId = original?.id ?: return
        if (title.isEmpty()) return
        _uiState.update { it.copy(subtaskInput = "") }
        viewModelScope.launch {
            taskRepository.addSubtask(SubTask(taskId = taskId, title = title))
        }
    }

    /**
     * Toggles subtask completion. Completing ALL subtasks deliberately does
     * NOT auto-complete the parent task.
     */
    fun toggleSubtask(subTask: SubTask) {
        viewModelScope.launch {
            taskRepository.updateSubtask(subTask.copy(isCompleted = !subTask.isCompleted))
        }
    }

    fun deleteSubtask(subTask: SubTask) {
        viewModelScope.launch { taskRepository.deleteSubtask(subTask) }
    }

    // ---------- save / delete ----------

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
                    repeatType = state.repeatType,
                ),
            )
            onDone()
        }
    }

    /** Deletes the task permanently — its subtasks go with it (DB FK: CASCADE). */
    fun delete(onDone: () -> Unit) {
        val base = original ?: run { onDone(); return }
        viewModelScope.launch {
            taskRepository.deleteTask(base)
            onDone()
        }
    }
}
