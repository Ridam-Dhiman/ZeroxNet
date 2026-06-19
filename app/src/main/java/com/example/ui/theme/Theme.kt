package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GeoPrimary,
    onPrimary = Color.White,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoDarkBlue,
    secondary = GeoSignalGreen,
    onSecondary = Color.White,
    background = GeoBg,
    onBackground = GeoText,
    surface = GeoSurface,
    onSurface = GeoDarkBlue,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoTextMuted,
    outline = GeoOutline,
    outlineVariant = GeoDivider,
    error = GeoSosRed,
    onError = Color.White,
    errorContainer = GeoSosBg,
    onErrorContainer = GeoSosText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
