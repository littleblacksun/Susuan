package com.xingce.speedmath.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SkyColorScheme = lightColorScheme(
    primary = PrimarySky,
    onPrimary = SurfaceWhite,
    primaryContainer = SurfaceSkyTint,
    onPrimaryContainer = PrimarySkyText,
    inversePrimary = PrimarySkyLight,
    secondary = PrimarySkyText,
    onSecondary = SurfaceWhite,
    secondaryContainer = SurfaceIceBlue,
    onSecondaryContainer = TextPrimary,
    tertiary = StatusPass,
    onTertiary = SurfaceWhite,
    tertiaryContainer = StatusPassBg,
    onTertiaryContainer = TextPrimary,
    background = BgSkyWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceIceBlue,
    onSurfaceVariant = TextSecondary,
    surfaceTint = PrimarySky,
    inverseSurface = TextPrimary,
    inverseOnSurface = SurfaceWhite,
    error = StatusError,
    onError = SurfaceWhite,
    errorContainer = StatusErrorBg,
    onErrorContainer = TextPrimary,
    outline = BorderSky,
    outlineVariant = BorderSkySubtle,
    surfaceBright = SurfaceWhite,
    surfaceDim = SurfaceSkyTint,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = BgSkyWhite,
    surfaceContainer = SurfaceSelectedCard,
    surfaceContainerHigh = SurfaceIceBlue,
    surfaceContainerHighest = SurfaceSkyTint,
)

@Composable
fun SpeedMathTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SkyColorScheme, typography = SpeedMathTypography, content = content)
}
