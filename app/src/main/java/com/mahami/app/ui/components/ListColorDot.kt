package com.mahami.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Palette offered when creating a list (stored as TaskList.colorHex). */
val ListColorPalette = listOf(
    "#C0392B", // red (brand)
    "#E67E22", // orange
    "#F1C40F", // yellow
    "#27AE60", // green
    "#16A085", // teal
    "#2980B9", // blue
    "#8E44AD", // purple
    "#7F8C8D", // gray
)

/** Parses a stored "#RRGGBB" list color. Palette values are always valid. */
fun parseListColor(colorHex: String): Color =
    Color(android.graphics.Color.parseColor(colorHex))

/** Small filled circle showing a list's color. */
@Composable
fun ListColorDot(
    colorHex: String,
    size: Dp = 12.dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size).background(parseListColor(colorHex), CircleShape))
}
