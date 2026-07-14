package com.riftbound.recon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Riftbound aesthetic: Void and Magic (Deep Purple, Violet, Teal, Gold)
val DarkBackground = Color(0xFF0F111A)
val DarkSurface = Color(0xFF161A26)
val DarkSurfaceVariant = Color(0xFF22283A)
val PrimaryPurple = Color(0xFF9F5CFC)
val SecondaryTeal = Color(0xFF00E5FF)
val AccentGold = Color(0xFFFFD700)
val TextPrimary = Color(0xFFF1F3F9)
val TextSecondary = Color(0xFFA0A5C0)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryTeal,
    tertiary = AccentGold,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryTeal,
    tertiary = AccentGold,
    background = Color(0xFFF6F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EDF5),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1E212B),
    onSurface = Color(0xFF1E212B),
    onSurfaceVariant = Color(0xFF6B7280)
)

@Composable
fun RiftboundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
