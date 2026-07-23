package com.citecircle.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────────────
// Brand color tokens (spec-defined)
// ──────────────────────────────────────────────────────────────────────────────

object CcColors {
    // Primary palette
    val InkNavy       = Color(0xFF1B2A4A)   // Primary text, app bars, scholarly anchor
    val CircleBlue    = Color(0xFF3E63DD)   // Primary brand — buttons, active nav, links
    val HighlighterYellow = Color(0xFFFFC53D) // Signature accent — FAB, citation counts, selection
    val SeafoamTeal   = Color(0xFF2AB3A6)   // Success, verified researcher, AI-approved
    val CoralPop      = Color(0xFFFF6B6B)   // Notifications, likes/endorsements, alerts
    val EmeraldGreen  = Color(0xFF2E7D32)   // Success green for methodology index
    val AcademicGold  = Color(0xFFD4AF37)   // Academic accent for graph borders and nodes
    val DeepBurgundy  = Color(0xFF6B1D2F)   // Deep burgundy for icons and reset buttons
    val PaperCream    = Color(0xFFFAF8F3)   // App background — warm paper
    val MarginGray    = Color(0xFF8A94A6)   // Secondary text, dividers, timestamps
    val CardWhite     = Color(0xFFFFFFFF)   // Card background in light mode

    // Light mode extras
    val SurfaceLight  = Color(0xFFFAF8F3)   // Same as PaperCream
    val OnSurfaceLight = InkNavy
    val OutlineLight  = Color(0xFFDDD9D0)   // Subtle dividers

    // Dark mode palette
    val InkNavyDark       = Color(0xFFE8EDF7)   // Light text on dark surfaces
    val CircleBlueDark    = Color(0xFF7B9FF5)   // Brightened for dark bg contrast
    val HighlighterYellowDark = Color(0xFFFFD166) // Brightened yellow
    val SeafoamTealDark   = Color(0xFF4FD1C5)   // Brightened teal
    val CoralPopDark      = Color(0xFFFF8F8F)   // Softened coral for dark
    val SurfaceDark       = Color(0xFF121827)   // Navy-tinted dark surface
    val SurfaceVariantDark = Color(0xFF1E2B42)  // Card bg in dark mode
    val MarginGrayDark    = Color(0xFFAAB3C5)   // Lighter gray for readability
    val OutlineDark       = Color(0xFF2E3E5C)   // Dark dividers

    // Avatar ring colors (by role)
    val ProfessorRing     = InkNavy
    val VerifiedRing      = SeafoamTeal
    val StudentRing       = HighlighterYellow
}

// ──────────────────────────────────────────────────────────────────────────────
// Material 3 color schemes
// ──────────────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = CcColors.CircleBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE6FF),
    onPrimaryContainer = CcColors.InkNavy,
    secondary = CcColors.SeafoamTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF2EF),
    onSecondaryContainer = Color(0xFF003733),
    tertiary = CcColors.CoralPop,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDD9),
    onTertiaryContainer = Color(0xFF410009),
    error = CcColors.CoralPop,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = CcColors.PaperCream,
    onBackground = CcColors.InkNavy,
    surface = CcColors.CardWhite,
    onSurface = CcColors.InkNavy,
    surfaceVariant = Color(0xFFF1EEE7),
    onSurfaceVariant = CcColors.MarginGray,
    outline = CcColors.OutlineLight,
    outlineVariant = Color(0xFFEAE6DF),
    scrim = Color(0xFF000000),
    inverseSurface = CcColors.InkNavy,
    inverseOnSurface = Color(0xFFF0ECE2),
    inversePrimary = CcColors.CircleBlueDark,
    surfaceTint = CcColors.CircleBlue
)

private val DarkColorScheme = darkColorScheme(
    primary = CcColors.CircleBlueDark,
    onPrimary = CcColors.InkNavy,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = CcColors.CircleBlueDark,
    secondary = CcColors.SeafoamTealDark,
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF004F4A),
    onSecondaryContainer = CcColors.SeafoamTealDark,
    tertiary = CcColors.CoralPopDark,
    onTertiary = Color(0xFF680013),
    tertiaryContainer = Color(0xFF93001D),
    onTertiaryContainer = CcColors.CoralPopDark,
    error = CcColors.CoralPopDark,
    onError = Color(0xFF680013),
    errorContainer = Color(0xFF93001D),
    onErrorContainer = CcColors.CoralPopDark,
    background = CcColors.SurfaceDark,
    onBackground = CcColors.InkNavyDark,
    surface = CcColors.SurfaceVariantDark,
    onSurface = CcColors.InkNavyDark,
    surfaceVariant = Color(0xFF2A3A55),
    onSurfaceVariant = CcColors.MarginGrayDark,
    outline = CcColors.OutlineDark,
    outlineVariant = Color(0xFF3A4E6B),
    scrim = Color(0xFF000000),
    inverseSurface = CcColors.InkNavyDark,
    inverseOnSurface = CcColors.InkNavy,
    inversePrimary = CcColors.CircleBlue,
    surfaceTint = CcColors.CircleBlueDark
)

// ──────────────────────────────────────────────────────────────────────────────
// Extended color composition local (brand colors outside M3 slots)
// ──────────────────────────────────────────────────────────────────────────────

data class CcExtendedColors(
    val highlighterYellow: Color,
    val paperCream: Color,
    val coralPop: Color,
    val marginGray: Color,
    val inkNavy: Color,
    val seafoamTeal: Color,
    val cardBackground: Color,
    val highlighterYellowAlpha: Color,  // 30% alpha for sweep background
    val divider: Color
)

private val LightExtendedColors = CcExtendedColors(
    highlighterYellow = CcColors.HighlighterYellow,
    paperCream = CcColors.PaperCream,
    coralPop = CcColors.CoralPop,
    marginGray = CcColors.MarginGray,
    inkNavy = CcColors.InkNavy,
    seafoamTeal = CcColors.SeafoamTeal,
    cardBackground = CcColors.CardWhite,
    highlighterYellowAlpha = CcColors.HighlighterYellow.copy(alpha = 0.35f),
    divider = CcColors.OutlineLight
)

private val DarkExtendedColors = CcExtendedColors(
    highlighterYellow = CcColors.HighlighterYellowDark,
    paperCream = CcColors.SurfaceDark,
    coralPop = CcColors.CoralPopDark,
    marginGray = CcColors.MarginGrayDark,
    inkNavy = CcColors.InkNavyDark,
    seafoamTeal = CcColors.SeafoamTealDark,
    cardBackground = CcColors.SurfaceVariantDark,
    highlighterYellowAlpha = CcColors.HighlighterYellowDark.copy(alpha = 0.30f),
    divider = CcColors.OutlineDark
)

val LocalCcColors = staticCompositionLocalOf { LightExtendedColors }

// Extension for convenient access
val MaterialTheme.ccColors: CcExtendedColors
    @Composable get() = LocalCcColors.current

// ──────────────────────────────────────────────────────────────────────────────
// Theme composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun BrightScholarTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalCcColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CcTypography,
            shapes = CcShapes,
            content = content
        )
    }
}
