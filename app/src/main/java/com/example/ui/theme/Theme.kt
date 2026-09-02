package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = ImmersiveLavender,
    onSecondary = Color.Black,
    tertiary = ImmersivePrimaryLight,
    background = ChatDarkBg,
    onBackground = Color(0xFFF1F5F9), // slate-100
    surface = ChatDarkSurface,
    onSurface = Color(0xFFE2E8F0), // slate-200
    surfaceVariant = ChatDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8), // slate-400
    outline = ChatDarkBorder
  )

private val LightColorScheme =
  darkColorScheme( // Prioritize the dark immersive aesthetic even in light mode
    primary = ImmersivePrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = ImmersiveLavender,
    onSecondary = Color.Black,
    tertiary = ImmersivePrimaryLight,
    background = ChatDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = ChatDarkSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = ChatDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = ChatDarkBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
