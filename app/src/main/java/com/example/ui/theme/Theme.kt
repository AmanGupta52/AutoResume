package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Reading AppTheme.* here keeps Material defaults (text fields, dialogs, menus) in the soft palette.
  AppTheme.isDarkTheme = darkTheme
  val scheme =
    if (darkTheme) {
      darkColorScheme(
        primary = AppTheme.primary, secondary = AppTheme.accent,
        background = AppTheme.background, surface = AppTheme.background,
        surfaceVariant = AppTheme.background, surfaceContainer = AppTheme.background,
        surfaceContainerHigh = AppTheme.background, surfaceContainerHighest = AppTheme.background,
        onBackground = AppTheme.text, onSurface = AppTheme.text, onSurfaceVariant = AppTheme.textSecondary,
        outline = AppTheme.shadowDark, outlineVariant = AppTheme.shadowDark,
      )
    } else {
      lightColorScheme(
        primary = AppTheme.primary, secondary = AppTheme.accent,
        background = AppTheme.background, surface = AppTheme.background,
        surfaceVariant = AppTheme.background, surfaceContainer = AppTheme.background,
        surfaceContainerHigh = AppTheme.background, surfaceContainerHighest = AppTheme.background,
        onBackground = AppTheme.text, onSurface = AppTheme.text, onSurfaceVariant = AppTheme.textSecondary,
        outline = AppTheme.shadowDark, outlineVariant = AppTheme.shadowDark,
      )
    }
  val shapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp), small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )
  MaterialTheme(colorScheme = scheme, typography = Typography, shapes = shapes, content = content)
}
