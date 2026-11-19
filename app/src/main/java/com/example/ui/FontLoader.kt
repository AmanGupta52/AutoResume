package com.example.ui

import android.content.Context
import android.util.Base64

/**
 * Professional fonts bundled in assets/fonts (Inter, Source Sans 3, Lora; all SIL Open Font License,
 * Latin subset). They are embedded into the resume HTML as base64 @font-face rules, so preview,
 * print and PDF look identical on every phone and never depend on the fonts the device happens to have.
 */
object FontLoader {
    const val INTER = "Inter"
    const val SOURCE = "Source Sans 3"
    const val LORA = "Lora"

    /** Labels shown in the editor, in order. "Default" means the template's own font. */
    val CHOICES = listOf("Default", "Inter", "Source Sans", "Lora")

    fun familyOfChoice(choice: String): String? = when (choice) {
        "Inter" -> INTER
        "Source Sans" -> SOURCE
        "Lora" -> LORA
        else -> null
    }

    fun stack(family: String): String = when (family) {
        LORA -> "'Lora', Georgia, 'Times New Roman', serif"
        SOURCE -> "'Source Sans 3', 'Segoe UI', Arial, sans-serif"
        else -> "'Inter', 'Segoe UI', Arial, sans-serif"
    }

    private class Face(val file: String, val weight: Int, val style: String, val format: String)

    private val faces = mapOf(
        INTER to listOf(
            Face("inter-regular.otf", 400, "normal", "opentype"),
            Face("inter-semibold.otf", 600, "normal", "opentype"),
            Face("inter-bold.otf", 700, "normal", "opentype"),
        ),
        SOURCE to listOf(
            Face("sourcesans3-regular.ttf", 400, "normal", "truetype"),
            Face("sourcesans3-semibold.ttf", 600, "normal", "truetype"),
            Face("sourcesans3-bold.ttf", 700, "normal", "truetype"),
        ),
        LORA to listOf(
            Face("lora-regular.ttf", 400, "normal", "truetype"),
            Face("lora-italic.ttf", 400, "italic", "truetype"),
            Face("lora-semibold.ttf", 600, "normal", "truetype"),
            Face("lora-bold.ttf", 700, "normal", "truetype"),
        ),
    )

    private val cache = HashMap<String, String>()

    /** CSS @font-face rules for one family (cached after the first call). */
    fun css(context: Context, family: String): String = synchronized(cache) {
        cache.getOrPut(family) {
            val sb = StringBuilder()
            faces[family].orEmpty().forEach { f ->
                try {
                    val bytes = context.applicationContext.assets.open("fonts/${f.file}").use { it.readBytes() }
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mime = if (f.format == "opentype") "font/otf" else "font/ttf"
                    sb.append("@font-face{font-family:'$family';font-weight:${f.weight};font-style:${f.style};")
                        .append("src:url(data:$mime;base64,$b64) format('${f.format}');}\n")
                } catch (e: Exception) {
                    // font missing: the fallback stack in stack() is used instead
                }
            }
            sb.toString()
        }
    }
}
