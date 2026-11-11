package com.mahami.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The single place for DIRECTIONAL icons.
 *
 * Rule for every screen in this app: never use `Icons.Filled.Arrow*` /
 * `KeyboardArrow*` directly — always use these aliases. They are backed by
 * `Icons.AutoMirrored.*`, which automatically flips when the layout direction
 * is RTL (the whole app is RTL), so arrows always point the correct way.
 *
 * Non-directional icons (Add, Check, Calendar, …) can be used as-is.
 */
object RtlIcons {

    /** Top-app-bar back arrow. */
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack

    /** Arrow pointing "backward" (toward content start). */
    val NavigateBefore: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft

    /** Arrow pointing "forward" (toward content end). */
    val NavigateNext: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
}
