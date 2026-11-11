package com.mahami.app.data.repository

import com.mahami.app.data.local.dao.TaskListDao
import com.mahami.app.data.local.dao.TaskListWithCount
import com.mahami.app.data.local.entity.TaskList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class TaskListRepository @Inject constructor(
    private val taskListDao: TaskListDao,
) {

    fun observeLists(): Flow<List<TaskList>> = taskListDao.observeAll()

    fun observeListsWithTaskCount(): Flow<List<TaskListWithCount>> = taskListDao.observeWithTaskCount()

    suspend fun getList(id: Long): TaskList? = taskListDao.getById(id)

    suspend fun addList(list: TaskList): Long = taskListDao.insert(list)

    suspend fun updateList(list: TaskList) = taskListDao.update(list)

    /** Deleting a list keeps its tasks — the FK (onDelete = SET_NULL) clears their listId. */
    suspend fun deleteList(list: TaskList) = taskListDao.delete(list)
}
