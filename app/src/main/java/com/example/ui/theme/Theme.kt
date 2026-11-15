package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Every colour pair below is chosen for readable contrast (onX on X >= 4.5:1), and every screen
 * uses MaterialTheme.colorScheme, so text can never end up white-on-white again.
 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF2754D8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5FF), onPrimaryContainer = Color(0xFF0B1F5C),
    secondary = Color(0xFF0B7A6B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F3EC), onSecondaryContainer = Color(0xFF053B33),
    tertiary = Color(0xFFB45309), onTertiary = Color.White,
    background = Color(0xFFF1F3F8), onBackground = Color(0xFF0F172A),
    surface = Color.White, onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8ECF4), onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F8FC),
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFEEF1F7), surfaceContainerHighest = Color(0xFFE8ECF4),
    outline = Color(0xFF8A95A8), outlineVariant = Color(0xFFD5DCE8),
    error = Color(0xFFB42318), onError = Color.White,
    errorContainer = Color(0xFFFEE4E2), onErrorContainer = Color(0xFF7A271A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9DB6FF), onPrimary = Color(0xFF0B1F5C),
    primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFF6FD6C4), onSecondary = Color(0xFF053B33),
    secondaryContainer = Color(0xFF0B4F45), onSecondaryContainer = Color(0xFFD3F3EC),
    tertiary = Color(0xFFF5B66B), onTertiary = Color(0xFF3B2100),
    background = Color(0xFF0E1218), onBackground = Color(0xFFE6E9F0),
    surface = Color(0xFF171C25), onSurface = Color(0xFFE6E9F0),
    surfaceVariant = Color(0xFF252C38), onSurfaceVariant = Color(0xFFA9B3C4),
    surfaceContainerLowest = Color(0xFF0E1218), surfaceContainerLow = Color(0xFF131822),
    surfaceContainer = Color(0xFF171C25), surfaceContainerHigh = Color(0xFF1E2430), surfaceContainerHighest = Color(0xFF252C38),
    outline = Color(0xFF7C879B), outlineVariant = Color(0xFF323A48),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(30.dp),
        ),
        content = content,
    )
}
