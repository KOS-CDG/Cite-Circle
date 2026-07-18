package com.citecircle.app.core.designsystem

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.R

// ──────────────────────────────────────────────────────────────────────────────
// Google Fonts provider
// ──────────────────────────────────────────────────────────────────────────────

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// ──────────────────────────────────────────────────────────────────────────────
// Fraunces — Display/Headline (warm scholarly serif)
// ──────────────────────────────────────────────────────────────────────────────

private val FrauncesFont = GoogleFont("Fraunces")

val FrauncesFamily = FontFamily(
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.Bold)
)

// ──────────────────────────────────────────────────────────────────────────────
// Plus Jakarta Sans — Body/UI (friendly, rounded, highly legible)
// ──────────────────────────────────────────────────────────────────────────────

private val PlusJakartaSansFont = GoogleFont("Plus Jakarta Sans")

val PlusJakartaSansFamily = FontFamily(
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Bold)
)

// ──────────────────────────────────────────────────────────────────────────────
// JetBrains Mono — Citations, DOIs, code (bundled as fallback)
// ──────────────────────────────────────────────────────────────────────────────

private val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Medium)
)

// ──────────────────────────────────────────────────────────────────────────────
// Type scale — Display 32sp / Headline 24sp / Title 18sp / Body 15sp / Label 12sp
// Line height factor 1.4–1.5
// ──────────────────────────────────────────────────────────────────────────────

val CcTypography = Typography(
    // Display — Fraunces, large serif headings
    displayLarge = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    // Headline — Fraunces, 24–32sp
    headlineLarge = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 48.sp  // ~1.5x
    ),
    headlineMedium = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 40.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 34.sp  // ~1.4x
    ),
    // Title — Plus Jakarta Sans, 18sp
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 31.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp  // ~1.4x
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Body — Plus Jakarta Sans, 15sp
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp  // ~1.47x
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    // Label — Plus Jakarta Sans, 12sp
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

// ──────────────────────────────────────────────────────────────────────────────
// Shapes — 12–16dp corner radii (index cards on a desk)
// ──────────────────────────────────────────────────────────────────────────────

val CcShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)
