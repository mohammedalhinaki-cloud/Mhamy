package com.mahami.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Meaning colors that Material 3's colorScheme does not cover:
 * - the priority-colored completion circles (Todoist style),
 * - the "Overdue" section accent on the Today screen.
 *
 * Access them anywhere with `MaterialTheme.extendedColors`.
 */
@Immutable
data class ExtendedColors(
    /** Border color of the completion circle for "Important" tasks (Todoist red). */
    val priorityImportant: Color,
    /** Border color of the completion circle for "Normal" tasks (warm gray). */
    val priorityNormal: Color,
    /** Accent of the "Overdue" section (matches colorScheme.error on purpose). */
    val overdue: Color,
)

val LightExtendedColors = ExtendedColors(
    priorityImportant = Color(0xFFD1453B),
    priorityNormal = Color(0xFF8A837E),
    overdue = Color(0xFFBA1A1A),
)

val DarkExtendedColors = ExtendedColors(
    priorityImportant = Color(0xFFFF897D),
    priorityNormal = Color(0xFFA79C97),
    overdue = Color(0xFFFFB4AB),
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> { LightExtendedColors }

/** Convenience accessor: `MaterialTheme.extendedColors.priorityImportant`. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
