package com.mahami.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.local.dao.TaskListWithCount
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.local.entity.TaskList
import com.mahami.app.data.repository.TaskListRepository
import com.mahami.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val taskListRepository: TaskListRepository,
    taskRepository: TaskRepository,
) : ViewModel() {

    val listsWithCount: StateFlow<List<TaskListWithCount>> =
        taskListRepository.observeListsWithTaskCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    /** Used for the count badge on the "المهام المكتملة" row. */
    val completedTasks: StateFlow<List<Task>> =
        taskRepository.observeCompleted()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun createList(name: String, colorHex: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            taskListRepository.addList(TaskList(name = trimmed, colorHex = colorHex))
        }
    }

    fun updateList(list: TaskList, name: String, colorHex: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            taskListRepository.updateList(list.copy(name = trimmed, colorHex = colorHex))
        }
    }

    /** Tasks of a deleted list are kept and become list-less (DB FK: SET_NULL). */
    fun deleteList(list: TaskList) {
        viewModelScope.launch { taskListRepository.deleteList(list) }
    }
}
