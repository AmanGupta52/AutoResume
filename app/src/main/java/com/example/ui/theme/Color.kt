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
