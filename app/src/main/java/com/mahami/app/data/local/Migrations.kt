package com.mahami.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * DB v1 → v2: adds Task.repeatedFromId (used by repeating tasks so that
 * undoing a completion can delete the auto-created next occurrence).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN repeatedFromId INTEGER DEFAULT NULL")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_tasks_repeatedFromId` ON `tasks` (`repeatedFromId`)",
        )
    }
}
