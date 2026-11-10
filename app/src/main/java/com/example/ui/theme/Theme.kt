package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = NavyDeep,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldBright,
    secondary = AmberAccent,
    onSecondary = NavyDeep,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberLight,
    tertiary = CyanInfo,
    background = NavyDeep,
    onBackground = SlateTextPrimaryDark,
    surface = NavySurface,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = NavyCard,
    onSurfaceVariant = SlateTextSecondaryDark,
    outline = Color(0xFF334E68),
    error = RoseAlert
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = CyanInfo,
    background = SlateBackgroundLight,
    onBackground = SlateTextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = SlateCardLight,
    onSurfaceVariant = SlateTextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = RoseAlert
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
