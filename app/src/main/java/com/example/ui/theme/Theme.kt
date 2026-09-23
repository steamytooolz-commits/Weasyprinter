package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PyBlueDarkPrimary,
    onPrimary = PyBlueDarkOnPrimary,
    primaryContainer = PyBlueDarkContainer,
    onPrimaryContainer = PyBlueDarkOnContainer,
    secondary = PyGoldDarkSecondary,
    onSecondary = PyGoldDarkOnSecondary,
    secondaryContainer = PyGoldDarkContainer,
    onSecondaryContainer = PyGoldDarkOnContainer,
    tertiary = PyEmeraldDarkTertiary,
    onTertiary = PyEmeraldDarkOnTertiary,
    tertiaryContainer = PyEmeraldDarkContainer,
    onTertiaryContainer = PyEmeraldDarkOnContainer,
    background = PyBackgroundDark,
    surface = PySurfaceDark,
    surfaceVariant = PySurfaceVariantDark,
    onBackground = PyOnBackgroundDark,
    onSurface = PyOnSurfaceDark,
    onSurfaceVariant = PyOnSurfaceVariantDark,
    outline = PyOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PyBluePrimary,
    onPrimary = PyBlueOnPrimary,
    primaryContainer = PyBlueContainer,
    onPrimaryContainer = PyBlueOnContainer,
    secondary = PyGoldSecondary,
    onSecondary = PyGoldOnSecondary,
    secondaryContainer = PyGoldContainer,
    onSecondaryContainer = PyGoldOnContainer,
    tertiary = PyEmeraldTertiary,
    onTertiary = PyEmeraldOnTertiary,
    tertiaryContainer = PyEmeraldContainer,
    onTertiaryContainer = PyEmeraldOnContainer,
    background = PyBackgroundLight,
    surface = PySurfaceLight,
    surfaceVariant = PySurfaceVariantLight,
    onBackground = PyOnBackgroundLight,
    onSurface = PyOnSurfaceLight,
    onSurfaceVariant = PyOnSurfaceVariantLight,
    outline = PyOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature Python design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
