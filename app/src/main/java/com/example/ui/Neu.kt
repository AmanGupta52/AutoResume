package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import kotlin.math.max
import kotlin.math.min

/*
 * Neumorphism (Soft UI) building blocks.
 *
 * `Card` and `Button` are declared in this package ON PURPOSE: every screen in com.example.ui
 * that used material3 Card/Button now gets the soft raised look without per-screen edits
 * (same-package declarations win over the material3.* star import).
 */

/** Neutral = grey/white/black-ish. Neutral container colours become the shared soft surface. */
private fun isNeutral(c: Color): Boolean {
    val mx = max(c.red, max(c.green, c.blue))
    val mn = min(c.red, min(c.green, c.blue))
    return (mx - mn) < 0.12f
}

private fun surfaceFor(c: Color): Color =
    if (isNeutral(c)) AppTheme.card else c.compositeOver(AppTheme.card)

/** Raised (extruded) look: light shadow top-left + dark shadow bottom-right. */
fun Modifier.neuRaised(
    shape: Shape = RoundedCornerShape(20.dp),
    depth: Dp = 6.dp,
    color: Color = AppTheme.card,
): Modifier = this.drawBehind {
    val d = depth.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }.asAndroidPath()
    drawIntoCanvas { canvas ->
        val p = Paint().asFrameworkPaint()
        p.isAntiAlias = true
        p.color = color.toArgb()
        p.setShadowLayer(d * 1.7f, d, d, AppTheme.shadowDark.toArgb())
        canvas.nativeCanvas.drawPath(path, p)
        p.setShadowLayer(d * 1.7f, -d, -d, AppTheme.shadowLight.toArgb())
        canvas.nativeCanvas.drawPath(path, p)
    }
}

/** Pressed-in / engraved look for fields, tracks and pressed buttons. */
fun Modifier.neuInset(shape: Shape = RoundedCornerShape(14.dp)): Modifier =
    this.clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    AppTheme.shadowDark.copy(alpha = 0.50f),
                    AppTheme.card,
                    AppTheme.shadowLight.copy(alpha = 0.70f),
                )
            ),
            shape,
        )
        .border(1.dp, AppTheme.shadowDark.copy(alpha = 0.18f), shape)

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = surfaceFor(colors.containerColor)
    Column(
        modifier = modifier.neuRaised(shape, 5.dp, container).clip(shape).background(container, shape),
        content = content,
    )
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = surfaceFor(colors.containerColor)
    Column(
        modifier = modifier
            .neuRaised(shape, 5.dp, container)
            .clip(shape)
            .background(container, shape)
            .clickable(enabled = enabled, onClick = onClick),
        content = content,
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val neutral = isNeutral(colors.containerColor)
    val container = if (neutral) AppTheme.card else colors.containerColor
    val contentColor = if (neutral) AppTheme.text else colors.contentColor
    val base = modifier
        .defaultMinSize(minHeight = 44.dp)
        .let { if (pressed) it.neuInset(shape) else it.neuRaised(shape, 5.dp, container).clip(shape).background(container, shape) }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                modifier = base
                    .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
                    .padding(contentPadding),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Selectable pill: raised when idle, pressed-in + accent text when selected. */
@Composable
fun NeuChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val base = if (selected) modifier.neuInset(shape)
    else modifier.neuRaised(shape, 4.dp).clip(shape).background(AppTheme.card, shape)
    androidx.compose.material3.Text(
        text = label,
        color = if (selected) AppTheme.primary else AppTheme.text,
        style = MaterialTheme.typography.labelLarge,
        modifier = base.clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Round colour swatch that sinks in when chosen. */
@Composable
fun NeuSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val shape = androidx.compose.foundation.shape.CircleShape
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .let { if (selected) it.neuInset(shape) else it.neuRaised(shape, 3.dp).clip(shape).background(AppTheme.card, shape) }
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(22.dp).clip(shape).background(color, shape)
        )
    }
}

/** WebView showing the exact HTML that the PDF exporter prints. */
@Composable
fun ResumeWebPreview(html: String, modifier: Modifier = Modifier) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
