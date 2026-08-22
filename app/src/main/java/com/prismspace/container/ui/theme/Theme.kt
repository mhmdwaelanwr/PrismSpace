package com.prismspace.container.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryLight,
    
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryLight,
    
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TertiaryLight,
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed,
    
    background = BackgroundDark,
    onBackground = TextPrimary,
    
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    outline = Outline,
    outlineVariant = OutlineLight,
    scrim = Color.Black.copy(alpha = 0.7f),
)

@Composable
fun PrismSpaceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = PrismSpaceTypography,
        shapes = PrismSpaceShapes,
        content = content
    )
}

// ===== GRADIENT BRUSHES =====

/** Cyan to Purple gradient - Primary theme gradient */
val CyanToPurpleGradient = Brush.linearGradient(
    colors = listOf(
        PrimaryLight,
        SecondaryLight
    )
)

/** Purple to Cyan gradient - Reversed for variety */
val PurpleToCyanGradient = Brush.linearGradient(
    colors = listOf(
        SecondaryLight,
        PrimaryLight
    )
)

/** Diagonal gradient - Cyan bottom-left to Purple top-right */
val DiagonalGradient = Brush.linearGradient(
    colors = listOf(
        PrimaryLight,
        TertiaryLight,
        SecondaryLight
    )
)

/** Radial gradient centered - For spotlight effects */
val RadialGradient = Brush.radialGradient(
    colors = listOf(
        PrimaryLight.copy(alpha = 0.8f),
        PrimaryDark.copy(alpha = 0.3f)
    )
)

// Legacy aliases for backward compatibility
val CyanToPurple = CyanToPurpleGradient
val PurpleToCyan = PurpleToCyanGradient

