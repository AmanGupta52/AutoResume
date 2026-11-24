package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/** Resume accent colours. The hex values match ResumeHtmlHelper so swatches equal the printed colour. */
object ResumeAccents {
    val all: List<Pair<String, Color>> = listOf(
        "Blue" to Color(0xFF0A84FF),
        "Green" to Color(0xFF30D158),
        "Cyan" to Color(0xFF5AC8FA),
        "Slate" to Color(0xFF8E8E93),
        "Dark" to Color(0xFF1F2937),
    )
    fun of(id: String): Color = all.firstOrNull { it.first == id }?.second ?: all.last().second
}

/** Aurora Glass palette: mesh-gradient blobs, holographic edge and glass tints. */
object AuroraPalette {
    // light mesh
    val LightBase = Color(0xFFEEF0FA)
    val LightMesh = listOf(Color(0xFF7F9BFF), Color(0xFFB99BFF), Color(0xFF74E6D6), Color(0xFFFFB5D6))
    // dark mesh
    val DarkBase = Color(0xFF0A0C1C)
    val DarkMesh = listOf(Color(0xFF4A67FF), Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFEC4899))
    // OLED reuses the dark mesh, dimmer, on pure black
    val OledBase = Color(0xFF000000)

    /** Iridescent sweep used for pressed-card edges and highlights. */
    val Holo = listOf(
        Color(0xFF5B8CFF), Color(0xFF9A6BFF), Color(0xFFFF7AD9), Color(0xFFFFB86B), Color(0xFF4FE0D0),
    )
}
