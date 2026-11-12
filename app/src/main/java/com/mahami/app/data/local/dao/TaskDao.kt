package com.mahami.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mahami.app.data.local.entity.Task
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ---------- writes ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ---------- single-task reads ----------

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    // ---------- screen reads (Flow: the UI updates automatically on any change) ----------

    /** Incomplete tasks due exactly on [date] (Today screen, calendar day details, widget). */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND dueDate = :date
        ORDER BY isImportant DESC, (dueTime IS NULL), dueTime ASC, createdAt ASC
        """
    )
    fun observeTasksForDate(date: LocalDate): Flow<List<Task>>

    /** Incomplete tasks dated before [today] — the "Overdue" section of the Today screen. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :today
        ORDER BY dueDate ASC, (dueTime IS NULL), dueTime ASC, createdAt ASC
        """
    )
    fun observeOverdue(today: LocalDate): Flow<List<Task>>

    /** Incomplete tasks dated after [today] (Upcoming screen, grouped by day). */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate > :today
        ORDER BY dueDate ASC, (dueTime IS NULL), dueTime ASC, createdAt ASC
        """
    )
    fun observeUpcoming(today: LocalDate): Flow<List<Task>>

    /** Incomplete tasks with no date at all — the "No date" group. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND dueDate IS NULL
        ORDER BY isImportant DESC, createdAt ASC
        """
    )
    fun observeNoDate(): Flow<List<Task>>

    /** Finished tasks, most recently completed first ("Completed" screen). */
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<Task>>

    /** Incomplete tasks of one list, auto-sorted: priority → date → time → creation. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND listId = :listId
        ORDER BY isImportant DESC, (dueDate IS NULL), dueDate ASC, (dueTime IS NULL), dueTime ASC, createdAt ASC
        """
    )
    fun observeTasksForList(listId: Long): Flow<List<Task>>

    /**
     * Incomplete tasks due anywhere inside [fromDate]..[toDate] inclusive —
     * one query per displayed calendar month (day dots + selected-day list).
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate BETWEEN :fromDate AND :toDate
        ORDER BY dueDate ASC, isImportant DESC, (dueTime IS NULL), dueTime ASC, createdAt ASC
        """
    )
    fun observeTasksForPeriod(fromDate: LocalDate, toDate: LocalDate): Flow<List<Task>>

    /**
     * Reminder candidates (Stage 7): incomplete tasks that have a reminder offset
     * AND a date AND a time. One-shot read used by ReminderScheduler.syncAll().
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0
          AND reminderOffsetMinutes IS NOT NULL
          AND dueDate IS NOT NULL
          AND dueTime IS NOT NULL
        """
    )
    suspend fun getReminderCandidates(): List<Task>

    /** Total number of tasks (any state) — small liveness probe for the MVVM chain. */
    @Query("SELECT COUNT(*) FROM tasks")
    fun observeTaskCount(): Flow<Int>

    /**
     * Deletes the repeat occurrences that were auto-created when [originId]
     * was completed — used when that completion is undone (restore).
     */
    @Query("DELETE FROM tasks WHERE repeatedFromId = :originId")
    suspend fun deleteOccurrencesSpawnedFrom(originId: Long)
}
