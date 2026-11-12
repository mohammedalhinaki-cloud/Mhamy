package com.mahami.app.data.local

import androidx.room.TypeConverter
import com.mahami.app.domain.model.RepeatType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * java.time converters for Room.
 * LocalDate / LocalTime are stored as ISO-8601 strings ("2026-11-11", "14:30"),
 * for which lexicographic order equals chronological order — safe for < , = , ORDER BY.
 */
class Converters {

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    // Room converts enums automatically, but we declare the mapping explicitly.
    @TypeConverter
    fun repeatTypeToString(value: RepeatType): String = value.name

    @TypeConverter
    fun stringToRepeatType(value: String): RepeatType = RepeatType.valueOf(value)
}
