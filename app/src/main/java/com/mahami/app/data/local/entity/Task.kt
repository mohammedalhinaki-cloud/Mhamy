package com.mahami.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mahami.app.domain.model.RepeatType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            // Deleting a list must NOT delete its tasks: they become list-less (listId = null).
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("listId"), Index("dueDate"), Index("isCompleted"), Index("repeatedFromId")],
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Required. */
    val title: String,
    /** Optional; empty string when unused. */
    val notes: String = "",
    /** true = "Important", false = "Normal". */
    val isImportant: Boolean = false,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val listId: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val repeatType: RepeatType = RepeatType.NONE,
    /** Minutes before the task's date-time to fire the reminder. null = reminder off (default). */
    val reminderOffsetMinutes: Int? = null,
    val createdAt: Instant = Instant.now(),
    /**
     * For an auto-created repeat occurrence: id of the task whose completion
     * spawned this row. null for normal tasks.
     * (Indexed, but deliberately NOT a foreign key: occurrences stay valid standalone
     * rows even if the original task is deleted, and restoring the original deletes
     * its still-active occurrence via DAO query.)
     */
    val repeatedFromId: Long? = null,
)
