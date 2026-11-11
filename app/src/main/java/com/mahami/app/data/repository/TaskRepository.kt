package com.mahami.app.data.repository

import com.mahami.app.data.local.dao.SubTaskDao
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.dao.TaskDao
import com.mahami.app.data.local.entity.SubTask
import com.mahami.app.data.local.entity.Task
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Tasks + subtasks. A concrete class (no interface) on purpose — zero complexity
 * for a single-user app.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val subTaskDao: SubTaskDao,
) {

    // ---------------- Tasks: reads ----------------

    fun observeOverdue(today: LocalDate): Flow<List<Task>> = taskDao.observeOverdue(today)

    fun observeTasksForDate(date: LocalDate): Flow<List<Task>> = taskDao.observeTasksForDate(date)

    fun observeUpcoming(today: LocalDate): Flow<List<Task>> = taskDao.observeUpcoming(today)

    fun observeNoDate(): Flow<List<Task>> = taskDao.observeNoDate()

    fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted()

    fun observeTasksForList(listId: Long): Flow<List<Task>> = taskDao.observeTasksForList(listId)

    fun observeTask(id: Long): Flow<Task?> = taskDao.observeById(id)

    suspend fun getTask(id: Long): Task? = taskDao.getById(id)

    // ---------------- Tasks: writes ----------------

    suspend fun addTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteById(id)

    /**
     * Plain completion toggle.
     * (Repeating-task logic — spawning the next occurrence — is added on top of
     * this in Stage 4, per the staged plan.)
     */
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) {
        val task = taskDao.getById(taskId) ?: return
        taskDao.update(
            task.copy(
                isCompleted = completed,
                completedAt = if (completed) Instant.now() else null,
            ),
        )
    }

    // ---------------- Subtasks ----------------

    fun observeSubtasks(taskId: Long): Flow<List<SubTask>> = subTaskDao.observeForTask(taskId)

    fun observeSubtaskCounts(): Flow<List<SubtaskCount>> = subTaskDao.observeCounts()

    suspend fun addSubtask(subTask: SubTask): Long = subTaskDao.insert(subTask)

    suspend fun updateSubtask(subTask: SubTask) = subTaskDao.update(subTask)

    suspend fun deleteSubtask(subTask: SubTask) = subTaskDao.delete(subTask)
}
