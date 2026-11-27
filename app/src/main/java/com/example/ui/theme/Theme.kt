package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val MotionStoryDarkColorScheme = darkColorScheme(
    primary = StudioAmber,
    onPrimary = StudioVoid,
    primaryContainer = StudioAmberDim,
    onPrimaryContainer = StudioAmber,
    secondary = StudioTeal,
    onSecondary = StudioVoid,
    secondaryContainer = StudioTealDim,
    onSecondaryContainer = StudioTeal,
    tertiary = StudioViolet,
    onTertiary = StudioVoid,
    background = StudioVoid,
    onBackground = StudioTextPrimary,
    surface = StudioCharcoal,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioPanel,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLowest = StudioVoid,
    surfaceContainerLow = StudioCharcoal,
    surfaceContainer = StudioPanel,
    surfaceContainerHigh = StudioElevated,
    surfaceContainerHighest = StudioSurfaceHigh,
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle,
    error = StudioCoral,
    onError = StudioVoid,
    errorContainer = StudioCoralDim,
    onErrorContainer = StudioCoral
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun MotionStoryStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MotionStoryDarkColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
