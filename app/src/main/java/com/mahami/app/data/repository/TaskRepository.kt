package com.mahami.app.data.repository

import com.mahami.app.data.local.dao.SubTaskDao
import com.mahami.app.data.local.dao.SubtaskCount
import com.mahami.app.data.local.dao.TaskDao
import com.mahami.app.data.local.entity.SubTask
import com.mahami.app.data.local.entity.Task
import com.mahami.app.domain.model.RepeatType
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

    /** Total number of tasks (any state) — small liveness probe for the MVVM chain. */
    fun observeTaskCount(): Flow<Int> = taskDao.observeTaskCount()

    // ---------------- Tasks: writes ----------------

    suspend fun addTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteById(id)

    /**
     * Completion toggle with repeating-task logic:
     * - completing a repeating task ALSO inserts its next occurrence
     *   (same properties, new id, `repeatedFromId` = the completed task),
     * - restoring (undo) removes the occurrence that completion auto-created.
     * Occurrences roll forward past today so they never land in the past.
     * Subtasks are NOT copied to the next occurrence.
     */
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) {
        val task = taskDao.getById(taskId) ?: return
        if (completed) {
            taskDao.update(task.copy(isCompleted = true, completedAt = Instant.now()))
            if (task.repeatType != RepeatType.NONE && task.dueDate != null) {
                var nextDate = nextOccurrence(task.dueDate, task.repeatType)
                val today = LocalDate.now()
                while (!nextDate.isAfter(today)) {
                    nextDate = nextOccurrence(nextDate, task.repeatType)
                }
                taskDao.insert(
                    task.copy(
                        id = 0,
                        isCompleted = false,
                        completedAt = null,
                        dueDate = nextDate,
                        repeatedFromId = task.id,
                        createdAt = Instant.now(),
                    ),
                )
            }
        } else {
            taskDao.update(task.copy(isCompleted = false, completedAt = null))
            taskDao.deleteOccurrencesSpawnedFrom(taskId)
        }
    }

    /** Gregorian next occurrence (RepeatType.NONE never reaches here). */
    private fun nextOccurrence(from: LocalDate, repeat: RepeatType): LocalDate = when (repeat) {
        RepeatType.NONE -> from
        RepeatType.DAILY -> from.plusDays(1)
        RepeatType.WEEKLY -> from.plusWeeks(1)
        RepeatType.MONTHLY -> from.plusMonths(1)
    }

    // ---------------- Subtasks ----------------

    fun observeSubtasks(taskId: Long): Flow<List<SubTask>> = subTaskDao.observeForTask(taskId)

    fun observeSubtaskCounts(): Flow<List<SubtaskCount>> = subTaskDao.observeCounts()

    suspend fun addSubtask(subTask: SubTask): Long = subTaskDao.insert(subTask)

    suspend fun updateSubtask(subTask: SubTask) = subTaskDao.update(subTask)

    suspend fun deleteSubtask(subTask: SubTask) = subTaskDao.delete(subTask)
}
