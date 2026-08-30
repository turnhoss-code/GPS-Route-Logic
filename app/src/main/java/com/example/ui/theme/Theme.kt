package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkNeonColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepDarkCanvas,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = PastelCyan,
    secondary = ElectricBlue,
    onSecondary = DeepDarkCanvas,
    secondaryContainer = SurfaceGlow,
    onSecondaryContainer = PastelBlue,
    tertiary = PastelLavender,
    onTertiary = DeepDarkCanvas,
    tertiaryContainer = SurfaceElevated,
    onTertiaryContainer = NeonPink,
    background = DeepDarkCanvas,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = PastelCyan,
    outline = CardBorderDark,
    outlineVariant = SurfaceGlow,
    error = HazardNeonRed,
    onError = Color.White
)

@Composable
fun GPSRouteLogicTheme(
    darkTheme: Boolean = true, // Dark theme on all pages with bright pastels or neons
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Standardize on dark neon/pastel automotive aesthetic across all views
    MaterialTheme(
        colorScheme = DarkNeonColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GPSRouteLogicTheme(darkTheme, dynamicColor, content)
}


