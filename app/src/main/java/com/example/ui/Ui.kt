@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Card with a soft, wide shadow. Always uses theme surface + onSurface so text stays readable. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val shadowed = modifier.shadow(6.dp, shape, ambientColor = Color(0x261E293B), spotColor = Color(0x331E293B))
    if (onClick != null) {
        Surface(onClick = onClick, modifier = shadowed, shape = shape, color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column(content = content)
        }
    } else {
        Surface(modifier = shadowed, shape = shape, color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column(content = content)
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        trailing?.invoke()
    }
}

@Composable
fun Tag(text: String, container: Color, content: Color) {
    Text(
        text,
        color = content,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val paper = MaterialTheme.colorScheme.surface
        val line = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.size(width = 120.dp, height = 110.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(primary.copy(alpha = .14f), Offset(w * .06f, h * .1f), Size(w * .62f, h * .86f), androidx.compose.ui.geometry.CornerRadius(14f, 14f))
            drawRoundRect(paper, Offset(w * .18f, h * .02f), Size(w * .62f, h * .86f), androidx.compose.ui.geometry.CornerRadius(14f, 14f))
            drawRoundRect(line, Offset(w * .18f, h * .02f), Size(w * .62f, h * .86f), androidx.compose.ui.geometry.CornerRadius(14f, 14f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
            drawRect(primary, Offset(w * .26f, h * .14f), Size(w * .26f, h * .05f))
            var y = h * .28f
            repeat(4) { drawRect(line, Offset(w * .26f, y), Size(if (it == 3) w * .2f else w * .46f, h * .035f)); y += h * .1f }
            drawCircle(primary, w * .12f, Offset(w * .8f, h * .8f))
            drawRect(Color.White, Offset(w * .8f - w * .06f, h * .8f - 2f), Size(w * .12f, 4f))
            drawRect(Color.White, Offset(w * .8f - 2f, h * .8f - w * .06f), Size(4f, w * .12f))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

/** Colour dot for picking the resume accent. */
@Composable
fun Swatch(color: Color, selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = "$label colour" + if (selected) ", selected" else "" }
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Surface(onClick = onClick, shape = CircleShape, color = Color.Transparent, modifier = Modifier.fillMaxSize()) {}
    }
}

/** Tiny abstract page drawing so people can tell templates apart at a glance. */
@Composable
fun TemplateThumb(templateId: String, accent: Color, modifier: Modifier = Modifier) {
    val line = Color(0xFFCBD2DE)
    val dark = Color(0xFF334155)
    Canvas(modifier.background(Color.White, RoundedCornerShape(6.dp))) {
        val w = size.width
        val h = size.height
        fun bar(x: Float, y: Float, bw: Float, c: Color, bh: Float = h * 0.025f) =
            drawRect(c, Offset(x, y), Size(bw, bh))
        when (templateId) {
            "Minimal ATS" -> {
                bar(w * .1f, h * .08f, w * .5f, dark, h * .05f)
                bar(w * .1f, h * .17f, w * .8f, line)
                var y = h * .27f
                repeat(6) { bar(w * .1f, y, if (it % 3 == 0) w * .35f else w * .8f, if (it % 3 == 0) dark else line); y += h * .08f }
            }
            "Premium Creative" -> {
                drawRect(accent, Offset(0f, 0f), Size(w, h * .26f))
                bar(w * .1f, h * .1f, w * .5f, Color.White, h * .05f)
                var y = h * .36f
                repeat(5) { bar(w * .1f, y, if (it % 2 == 0) w * .35f else w * .8f, if (it % 2 == 0) accent else line); y += h * .09f }
            }
            "Developer Resume" -> {
                drawRect(Color(0xFF0F172A), Offset(0f, 0f), Size(w, h * .22f))
                bar(w * .1f, h * .09f, w * .4f, accent, h * .04f)
                var y = h * .32f
                repeat(6) { bar(w * .1f, y, w * .06f, accent); bar(w * .2f, y, if (it % 2 == 0) w * .6f else w * .45f, line); y += h * .085f }
            }
            "Executive Premium" -> {
                bar(w * .25f, h * .08f, w * .5f, dark, h * .05f)
                bar(w * .1f, h * .18f, w * .8f, Color(0xFFD4AF37), h * .012f)
                var y = h * .27f
                repeat(6) { bar(w * .1f, y, if (it % 3 == 0) w * .3f else w * .8f, if (it % 3 == 0) dark else line); y += h * .08f }
            }
            "Compact Two-Column" -> {
                drawRect(accent.copy(alpha = .18f), Offset(0f, 0f), Size(w * .34f, h))
                drawCircle(accent, w * .08f, Offset(w * .17f, h * .13f))
                var y = h * .26f
                repeat(5) { bar(w * .05f, y, w * .24f, line); y += h * .07f }
                bar(w * .42f, h * .08f, w * .45f, dark, h * .05f)
                y = h * .2f
                repeat(7) { bar(w * .42f, y, if (it % 3 == 0) w * .22f else w * .5f, if (it % 3 == 0) accent else line); y += h * .08f }
            }
            "Academic CV" -> {
                bar(w * .3f, h * .07f, w * .4f, dark, h * .04f)
                bar(w * .2f, h * .14f, w * .6f, line)
                var y = h * .24f
                repeat(3) {
                    bar(w * .1f, y, w * .3f, dark); bar(w * .1f, y + h * .03f, w * .8f, dark, h * .006f); y += h * .07f
                    repeat(2) { bar(w * .1f, y, w * .8f, line); y += h * .06f }
                    y += h * .03f
                }
            }
            "Classic Serif" -> {
                bar(w * .25f, h * .07f, w * .5f, dark, h * .05f)
                bar(w * .1f, h * .15f, w * .8f, dark, h * .012f)
                var y = h * .24f
                repeat(6) { bar(w * .1f, y, if (it % 3 == 0) w * .3f else w * .8f, if (it % 3 == 0) dark else line); y += h * .08f }
            }
            "Bold Header" -> {
                drawRect(accent, Offset(w * .05f, h * .05f), Size(w * .9f, h * .2f))
                bar(w * .1f, h * .1f, w * .4f, Color.White, h * .04f)
                drawCircle(Color.White, w * .06f, Offset(w * .8f, h * .15f))
                var y = h * .34f
                repeat(6) { bar(w * .1f, y, if (it % 3 == 0) w * .3f else w * .8f, if (it % 3 == 0) accent else line); y += h * .08f }
            }
            "Timeline" -> {
                bar(w * .1f, h * .08f, w * .45f, dark, h * .05f)
                drawRect(accent, Offset(w * .06f, h * .08f), Size(w * .02f, h * .05f))
                var y = h * .24f
                repeat(5) { bar(w * .1f, y, w * .2f, accent); bar(w * .38f, y, w * .5f, line); bar(w * .38f, y + h * .04f, w * .4f, line); y += h * .13f }
            }
            else -> { // Modern Professional: tinted sidebar + main column
                drawRect(accent.copy(alpha = .16f), Offset(0f, 0f), Size(w * .32f, h))
                bar(w * .06f, h * .1f, w * .2f, accent, h * .04f)
                var y = h * .22f
                repeat(5) { bar(w * .06f, y, w * .2f, line); y += h * .07f }
                bar(w * .4f, h * .1f, w * .45f, dark, h * .05f)
                y = h * .22f
                repeat(6) { bar(w * .4f, y, if (it % 3 == 0) w * .25f else w * .5f, if (it % 3 == 0) accent else line); y += h * .08f }
            }
        }
    }
}

/** WebView showing the exact HTML the PDF exporter prints. [onContentHeight] reports the page height in CSS px. */
@Composable
fun ResumeWebPreview(html: String, modifier: Modifier = Modifier, onContentHeight: ((Int) -> Unit)? = null) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                // JS is enabled only to read the page height; the content is our own escaped HTML.
                settings.javaScriptEnabled = onContentHeight != null
                settings.allowFileAccess = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        if (onContentHeight != null) {
                            view?.evaluateJavascript("(function(){return document.body.scrollHeight;})()") { v ->
                                v?.trim('"')?.toDoubleOrNull()?.let { onContentHeight(it.toInt()) }
                            }
                        }
                    }
                }
            }
        },
        update = { wv ->
            if (wv.tag != html) {
                wv.tag = html
                wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
    )
}

/** Non-interactive miniature of the real first page, for the resume list. */
@Composable
fun ResumeMiniPreview(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setOnTouchListener { _, _ -> true }
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setBackgroundColor(android.graphics.Color.WHITE)
            }
        },
        update = { wv ->
            if (wv.tag != html) {
                wv.tag = html
                wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
    )
}

val TemplateNames = listOf(
    "Modern Professional", "Minimal ATS", "Premium Creative", "Developer Resume", "Executive Premium",
    "Compact Two-Column", "Academic CV", "Classic Serif", "Bold Header", "Timeline",
)
val TemplateBlurbs = mapOf(
    "Modern Professional" to "Sidebar + main column. Good all-rounder.",
    "Minimal ATS" to "Plain single column. Safest for applicant systems.",
    "Premium Creative" to "Colour header band. For design and marketing roles.",
    "Developer Resume" to "Monospace accents. For engineers.",
    "Executive Premium" to "Serif type, gold rule. For senior roles.",
    "Compact Two-Column" to "Education first, skills in a side column. Great for freshers.",
    "Academic CV" to "Serif, education and honours first. For research and teaching.",
    "Classic Serif" to "Traditional centred header. Conservative industries.",
    "Bold Header" to "Dark colour band with photo. Stands out.",
    "Timeline" to "Dates in a left column. Easy to scan a career.",
)

/** Opens the system share sheet for a file we created in the cache directory. */
fun shareFile(context: android.content.Context, file: java.io.File, mime: String, subject: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mime
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Save or share"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot share the file: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}
