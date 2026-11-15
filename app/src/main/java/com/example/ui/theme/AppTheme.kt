package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Neumorphism (Soft UI) palette.
 * Rule of the style: background and surface share ONE tone; depth comes only from a
 * light highlight (top-left) and a dark shadow (bottom-right).
 */
object AppTheme {
    var isDarkTheme by mutableStateOf(false)

    val background: Color
        get() = if (isDarkTheme) Color(0xFF2A2D34) else Color(0xFFE3E8EF)

    /** Same tone as [background] on purpose - this is what makes surfaces look moulded. */
    val card: Color
        get() = background

    val shadowLight: Color
        get() = if (isDarkTheme) Color(0xFF363A43) else Color(0xFFFFFFFF)

    val shadowDark: Color
        get() = if (isDarkTheme) Color(0xFF1C1E23) else Color(0xFFA9B4C6)

    val primary: Color
        get() = Color(0xFF3D6BE0)

    val accent: Color
        get() = Color(0xFF2F9E8F)

    val text: Color
        get() = if (isDarkTheme) Color(0xFFE8EAEF) else Color(0xFF2B3340)

    val textSecondary: Color
        get() = if (isDarkTheme) Color(0xFF9AA1AE) else Color(0xFF627086)

    /** Soft engraved line instead of a hard border. */
    val border: Color
        get() = shadowDark.copy(alpha = 0.22f)
}
