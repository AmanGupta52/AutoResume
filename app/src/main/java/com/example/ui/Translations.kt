package com.example.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/** App language. Resume content is never translated, only the app interface. */
object Lang {
    var code by mutableStateOf("en")
    val options = listOf("en" to "English", "hi" to "हिन्दी (Hindi)")
}

/** Looks the text up for the current language; anything not in the dictionary stays as written. */
@Composable
fun tr(text: String): String {
    val c = Lang.code
    return if (c == "hi") HindiStrings[text] ?: text else text
}

/**
 * Declared in this package on purpose: it takes priority over the material3.* star import, so every
 * Text(...) call in the app is translated without touching each call site.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    androidx.compose.material3.Text(
        text = tr(text), modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
        textAlign = textAlign, overflow = overflow, maxLines = maxLines, style = style,
    )
}
