package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Aurora Glass. Three looks: light, dark and OLED (pure black).
 * Every colour pair is chosen for readable contrast (onX on X >= 4.5:1), and every screen uses
 * MaterialTheme.colorScheme, so text can never end up white-on-white.
 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF4351EE), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E4FF), onPrimaryContainer = Color(0xFF0E1659),
    secondary = Color(0xFF0B7A6B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F3EC), onSecondaryContainer = Color(0xFF053B33),
    tertiary = Color(0xFF9A4DDB), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E3FF), onTertiaryContainer = Color(0xFF3A0F66),
    background = Color(0xFFEEF0FA), onBackground = Color(0xFF0E1430),
    surface = Color(0xFFFBFBFF), onSurface = Color(0xFF0E1430),
    surfaceVariant = Color(0xFFE6E9F6), onSurfaceVariant = Color(0xFF515B7A),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F7FD),
    surfaceContainer = Color(0xFFF1F3FB), surfaceContainerHigh = Color(0xFFEBEEF9), surfaceContainerHighest = Color(0xFFE4E8F5),
    outline = Color(0xFF8790AC), outlineVariant = Color(0xFFD3D9EC),
    error = Color(0xFFCF2347), onError = Color.White,
    errorContainer = Color(0xFFFFE1E7), onErrorContainer = Color(0xFF7A0F26),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9FB0FF), onPrimary = Color(0xFF0B1240),
    primaryContainer = Color(0xFF2A3597), onPrimaryContainer = Color(0xFFDFE4FF),
    secondary = Color(0xFF6FD6C4), onSecondary = Color(0xFF053B33),
    secondaryContainer = Color(0xFF0B4F45), onSecondaryContainer = Color(0xFFD3F3EC),
    tertiary = Color(0xFFD2B0FF), onTertiary = Color(0xFF3A0F66),
    tertiaryContainer = Color(0xFF53268A), onTertiaryContainer = Color(0xFFF2E3FF),
    background = Color(0xFF0A0C1C), onBackground = Color(0xFFEEF0FF),
    surface = Color(0xFF14183A), onSurface = Color(0xFFEEF0FF),
    surfaceVariant = Color(0xFF242A55), onSurfaceVariant = Color(0xFFA9B1D6),
    surfaceContainerLowest = Color(0xFF080A18), surfaceContainerLow = Color(0xFF10132E),
    surfaceContainer = Color(0xFF14183A), surfaceContainerHigh = Color(0xFF1B2048), surfaceContainerHighest = Color(0xFF242A55),
    outline = Color(0xFF7D86AC), outlineVariant = Color(0xFF2E3566),
    error = Color(0xFFFF8DA1), onError = Color(0xFF5E0016),
    errorContainer = Color(0xFF8A1030), onErrorContainer = Color(0xFFFFDAE0),
)

/** Same as dark, but every surface is pure black or near black so OLED pixels switch off. */
private val OledScheme = DarkScheme.copy(
    background = Color.Black, surface = Color(0xFF0A0A0C),
    surfaceVariant = Color(0xFF1A1A20), outlineVariant = Color(0xFF2A2A32),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF050507),
    surfaceContainer = Color(0xFF0C0C0F), surfaceContainerHigh = Color(0xFF141418), surfaceContainerHighest = Color(0xFF1C1C22),
)

/** Tokens for the glass look that Material's colour scheme has no slot for. */
@Immutable
data class GlassTokens(
    val dark: Boolean,
    val oled: Boolean,
    val base: Color,            // page colour under the aurora
    val mesh: List<Color>,      // four aurora blobs
    val meshAlpha: Float,
    val fill: Color,            // glass card fill
    val barFill: Color,         // bottom bar / sheets, a little more opaque
    val field: Color,           // text field fill
    val sheen: Color,           // top-left light on a card
    val borderHi: Color,        // bright edge (top-left)
    val borderLo: Color,        // faint edge (bottom-right)
    val shadow: Color,          // soft drop shadow, Transparent = none
    val holo: List<Color>,
)

private val LightGlass = GlassTokens(
    dark = false, oled = false, base = AuroraPalette.LightBase, mesh = AuroraPalette.LightMesh, meshAlpha = .6f,
    fill = Color(0xB8FFFFFF), barFill = Color(0xE6FFFFFF), field = Color(0xBFFFFFFF),
    sheen = Color(0x99FFFFFF), borderHi = Color(0xF2FFFFFF), borderLo = Color(0x33283C96),
    shadow = Color(0x4D2C388C), holo = AuroraPalette.Holo,
)
private val DarkGlass = GlassTokens(
    dark = true, oled = false, base = AuroraPalette.DarkBase, mesh = AuroraPalette.DarkMesh, meshAlpha = .42f,
    fill = Color(0x8A1A1F40), barFill = Color(0xE6141838), field = Color(0x8C080A1E),
    sheen = Color(0x1AFFFFFF), borderHi = Color(0x4DFFFFFF), borderLo = Color(0x14FFFFFF),
    shadow = Color(0x99000000), holo = AuroraPalette.Holo,
)
private val OledGlass = DarkGlass.copy(
    oled = true, base = AuroraPalette.OledBase, meshAlpha = .24f,
    fill = Color(0x0FFFFFFF), barFill = Color(0xF2050507), field = Color(0x12FFFFFF),
    sheen = Color(0x10FFFFFF), borderHi = Color(0x3DFFFFFF), borderLo = Color(0x0FFFFFFF),
    shadow = Color.Transparent,
)

val LocalGlass = staticCompositionLocalOf { LightGlass }

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    oled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        darkTheme && oled -> OledScheme
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    val glass = when {
        darkTheme && oled -> OledGlass
        darkTheme -> DarkGlass
        else -> LightGlass
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalGlass provides glass) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp), extraLarge = RoundedCornerShape(32.dp),
            ),
            content = content,
        )
    }
}
