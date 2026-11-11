package com.mahami.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mahami.app.data.local.entity.SubTask
import kotlinx.coroutines.flow.Flow

/** Progress of one task's subtasks (shown as "2/5" on the parent). */
data class SubtaskCount(
    val taskId: Long,
    val total: Int,
    val completed: Int,
)

@Dao
interface SubTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subTask: SubTask): Long

    @Update
    suspend fun update(subTask: SubTask)

    @Delete
    suspend fun delete(subTask: SubTask)

    @Query("SELECT * FROM subtasks WHERE id = :id")
    suspend fun getById(id: Long): SubTask?

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeForTask(taskId: Long): Flow<List<SubTask>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getForTask(taskId: Long): List<SubTask>

    /** One row per task that has subtasks — used for the parent's progress indicator. */
    @Query(
        """
        SELECT taskId,
               COUNT(*) AS total,
               SUM(CASE WHEN isCompleted THEN 1 ELSE 0 END) AS completed
        FROM subtasks
        GROUP BY taskId
        """
    )
    fun observeCounts(): Flow<List<SubtaskCount>>
}
