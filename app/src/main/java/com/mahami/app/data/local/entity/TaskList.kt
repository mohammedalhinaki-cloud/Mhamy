package com.mahami.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** A user-created list (name + color). */
@Entity(tableName = "task_lists")
data class TaskList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Hex color string, e.g. "#C0392B". */
    val colorHex: String,
    val createdAt: Instant = Instant.now(),
)
