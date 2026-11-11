package com.mahami.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahami.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Stage 1 placeholder ViewModel. It only exposes the total number of tasks,
 * which proves the whole MVVM chain (Room → Repository → ViewModel → Compose
 * → Hilt injection) works end-to-end before any real screen is built.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    taskRepository: TaskRepository,
) : ViewModel() {

    val taskCount: StateFlow<Int> =
        taskRepository.observeTaskCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0,
            )
}
