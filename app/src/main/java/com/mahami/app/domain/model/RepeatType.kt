package com.mahami.app.domain.model

/**
 * Repeat interval of a task. Repeating is always computed on the Gregorian
 * calendar (java.time), never Hijri.
 */
enum class RepeatType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
}
