package com.riftbound.recon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────────
// Riftbound Aesthetic: "Void & Magic"
// See DESIGN_SYSTEM.md for the full specification and steering rules.
// ──────────────────────────────────────────────────────────────────

// Core palette — Dark theme
val DarkBackground = Color(0xFF0F111A)
val DarkSurface = Color(0xFF161A26)
val DarkSurfaceVariant = Color(0xFF22283A)
val DarkOutlineVariant = Color(0xFF2E3348)
val PrimaryPurple = Color(0xFF9F5CFC)
val SecondaryTeal = Color(0xFF00BFA6)
val AccentGold = Color(0xFFFFD700)
val ErrorRed = Color(0xFFFF5252)
val TextPrimary = Color(0xFFF1F3F9)
val TextSecondary = Color(0xFFA0A5C0)

// Core palette — Light theme adjustments
val LightPrimary = Color(0xFF7B3BDB)
val LightSecondary = Color(0xFF009B86)
val LightTertiary = Color(0xFFE6A800)
val LightBackground = Color(0xFFF6F8FC)
val LightSurface = Color.White
val LightSurfaceVariant = Color(0xFFE9EDF5)
val LightOnBackground = Color(0xFF1E212B)
val LightOnSurfaceVariant = Color(0xFF6B7280)

// ──────────────────────────────────────────────────────────────────
// Functional Colors: Scanner Console / Terminal Logs
// ──────────────────────────────────────────────────────────────────
val ConsoleBackground = Color.Black.copy(alpha = 0.85f)
val LogSuccess = Color(0xFF00E676)       // Green — "SUCESSO"
val LogError = Color(0xFFFF5252)         // Red — "FALHA", "Erro"
val LogMetadata = Color(0xFF00E5FF)      // Cyan — "Cód. Rodapé", "Energia Runa"
val LogBoundary = Color(0xFFFFEB3B)      // Yellow — "INICIANDO", "FIM"
val LogDefault = Color.LightGray

// ──────────────────────────────────────────────────────────────────
// Functional Colors: Compendium Card Thumbnails
// ──────────────────────────────────────────────────────────────────
val EnergyCostBadge = Color(0xFF8C52FF)

/** Gradient base color per Riftbound set for card thumbnails. */
val SetGradientColors = mapOf(
    "Origins" to Color(0xFF1E3A8A),
    "Proving Grounds" to Color(0xFF7F1D1D),
    "Spiritforged" to Color(0xFF14532D),
    "Vendetta" to Color(0xFF831843),
    "Unleashed" to Color(0xFF701A75),
)
val SetGradientDefault = Color(0xFF334155)

// ──────────────────────────────────────────────────────────────────
// Color Schemes
// ──────────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryTeal,
    tertiary = AccentGold,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRed,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground,
    onSurfaceVariant = LightOnSurfaceVariant
)

// ──────────────────────────────────────────────────────────────────
// Typography
// ──────────────────────────────────────────────────────────────────
private val RiftboundTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 16.sp
    )
)

// ──────────────────────────────────────────────────────────────────
// Theme Composable
// ──────────────────────────────────────────────────────────────────
@Composable
fun RiftboundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RiftboundTypography,
        content = content
    )
}
