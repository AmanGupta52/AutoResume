package com.example.ui

import com.example.data.Resume

/**
 * Extra design settings, stored inside the existing `fontSize` column so no database migration
 * is needed (and duplicating / backing up a resume carries them automatically).
 * Format: "Standard;font=Lora;tight=0;order=summary,experience,..."
 * Old values such as plain "Compact" still parse correctly.
 */
data class DesignOptions(
    val size: String = "Standard",
    val font: String = "Default",
    val order: List<String> = DEFAULT_ORDER,
    val tight: Boolean = false,
) {
    fun encode(): String = "$size;font=$font;tight=${if (tight) 1 else 0};order=${order.joinToString(",")}"

    companion object {
        val DEFAULT_ORDER = listOf("summary", "experience", "education", "skills", "projects", "certifications", "languages", "achievements")
        val SECTION_TITLES = mapOf(
            "summary" to "Summary", "experience" to "Experience", "education" to "Education", "skills" to "Skills",
            "projects" to "Projects", "certifications" to "Certifications", "languages" to "Languages",
            "achievements" to "Awards and interests",
        )
        val FONTS = FontLoader.CHOICES

        fun parse(raw: String): DesignOptions {
            val parts = raw.split(";")
            val size = parts.firstOrNull()?.takeIf { it in listOf("Compact", "Standard", "Large") } ?: "Standard"
            var font = "Default"
            var tight = false
            var order = DEFAULT_ORDER
            parts.drop(1).forEach { p ->
                val kv = p.split("=", limit = 2)
                if (kv.size == 2) when (kv[0]) {
                    "font" -> font = when (kv[1]) {
                        in FONTS -> kv[1]
                        "Sans" -> "Inter"      // values saved by older versions
                        "Serif" -> "Lora"
                        else -> "Default"
                    }
                    "tight" -> tight = kv[1] == "1"
                    "order" -> {
                        val given = kv[1].split(",").filter { it in DEFAULT_ORDER }.distinct()
                        order = given + DEFAULT_ORDER.filter { it !in given }
                    }
                }
            }
            return DesignOptions(size, font, order, tight)
        }

        fun parse(r: Resume): DesignOptions = parse(r.fontSize)
    }
}
