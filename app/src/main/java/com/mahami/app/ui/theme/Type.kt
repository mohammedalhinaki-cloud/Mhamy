package com.mahami.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mahami.app.R

/**
 * Bundled Arabic font: IBM Plex Sans Arabic (OFL), four static weights
 * under res/font. No runtime font downloads.
 */
val PlexArabicFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

private val DefaultTypography = Typography()

/** Default Material 3 typography with every style switched to the bundled Arabic font. */
val MahamiTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = PlexArabicFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = PlexArabicFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = PlexArabicFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = PlexArabicFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = PlexArabicFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = PlexArabicFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = PlexArabicFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = PlexArabicFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = PlexArabicFontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = PlexArabicFontFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = PlexArabicFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = PlexArabicFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = PlexArabicFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = PlexArabicFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = PlexArabicFontFamily),
)
