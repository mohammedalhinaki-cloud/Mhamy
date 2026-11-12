package com.mahami.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mahami.app.data.local.dao.SubTaskDao
import com.mahami.app.data.local.dao.TaskDao
import com.mahami.app.data.local.dao.TaskListDao
import com.mahami.app.data.local.entity.SubTask
import com.mahami.app.data.local.entity.Task
import com.mahami.app.data.local.entity.TaskList

@Database(
    entities = [Task::class, SubTask::class, TaskList::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MahamiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun taskListDao(): TaskListDao
}
