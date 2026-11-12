package com.mahami.app.di

import android.content.Context
import androidx.room.Room
import com.mahami.app.data.local.MIGRATION_1_2
import com.mahami.app.data.local.MahamiDatabase
import com.mahami.app.data.local.dao.SubTaskDao
import com.mahami.app.data.local.dao.TaskDao
import com.mahami.app.data.local.dao.TaskListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MahamiDatabase =
        Room.databaseBuilder(context, MahamiDatabase::class.java, "mahami.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideTaskDao(database: MahamiDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideSubTaskDao(database: MahamiDatabase): SubTaskDao = database.subTaskDao()

    @Provides
    fun provideTaskListDao(database: MahamiDatabase): TaskListDao = database.taskListDao()
}
