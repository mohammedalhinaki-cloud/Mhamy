package com.mahami.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mahami.app.data.local.entity.TaskList
import kotlinx.coroutines.flow.Flow

/** A list plus the number of incomplete tasks it contains. */
data class TaskListWithCount(
    @Embedded val list: TaskList,
    val taskCount: Int,
)

@Dao
interface TaskListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: TaskList): Long

    @Update
    suspend fun update(list: TaskList)

    @Delete
    suspend fun delete(list: TaskList)

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getById(id: Long): TaskList?

    @Query("SELECT * FROM task_lists ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<TaskList>>

    @Query(
        """
        SELECT task_lists.*,
               (SELECT COUNT(*) FROM tasks
                 WHERE tasks.listId = task_lists.id AND tasks.isCompleted = 0) AS taskCount
        FROM task_lists
        ORDER BY task_lists.createdAt ASC
        """
    )
    fun observeWithTaskCount(): Flow<List<TaskListWithCount>>
}
