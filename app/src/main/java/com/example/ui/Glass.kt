package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalGlass

/**
 * Parallax depth for the whole app. Any scrolling list or column inside [AuroraBackground]'s content reports how far
 * it moved, and the aurora blobs shift by different amounts, so the backdrop appears to sit behind the page.
 * No screen needs to know about it.
 */
@Stable
class ParallaxState {
    var offset by mutableFloatStateOf(0f)
        internal set

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            offset = (offset + consumed.y).coerceIn(-2400f, 0f)
            return Offset.Zero
        }
    }
}

@Composable
fun rememberParallax(): ParallaxState = remember { ParallaxState() }

/**
 * Animated gradient-mesh backdrop (Aurora UI + Gradient Mesh). Four soft colour blobs drift slowly and move with
 * scrolling at different speeds. In OLED mode the base is pure black and the blobs are dim.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    parallax: ParallaxState = rememberParallax(),
    content: @Composable BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora-t",
    )
    Box(
        modifier
            .fillMaxSize()
            .nestedScroll(parallax.connection)
            .drawBehind {
                // t and parallax.offset are read here, in the draw phase, so animating only redraws; it never recomposes.
                val w = size.width
                val h = size.height
                val p = parallax.offset
                val a = t
                val b = 1f - t
                drawRect(g.base)
                fun blob(color: Color, cx: Float, cy: Float, r: Float) = drawRect(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = g.meshAlpha), Color.Transparent),
                        center = Offset(cx, cy), radius = r,
                    ),
                )
                blob(g.mesh[0], w * (.15f + .12f * a), h * (.10f + .06f * b) + p * .10f, w * .95f)
                blob(g.mesh[1], w * (.90f - .12f * a), h * (.22f + .08f * a) + p * .18f, w * .85f)
                blob(g.mesh[2], w * (.75f + .08f * b), h * (.88f - .06f * a) + p * .28f, w * 1.0f)
                blob(g.mesh[3], w * (.05f + .10f * a), h * (.80f + .05f * b) + p * .22f, w * .80f)
            },
        content = content,
    )
}

/**
 * Soft drop shadow drawn only OUTSIDE the card shape, so it never shows through the translucent glass fill.
 * Pass Color.Transparent for no shadow (OLED).
 */
fun Modifier.softShadow(corner: Dp, color: Color): Modifier =
    if (color.alpha == 0f) this else drawBehind {
        val r = corner.toPx()
        val path = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r))) }
        clipPath(path, ClipOp.Difference) {
            val layers = 6
            for (i in 1..layers) {
                val spread = i * 2.4.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * (0.5f - i * 0.07f).coerceAtLeast(0.02f) * 0.5f),
                    topLeft = Offset(-spread, -spread + 7.dp.toPx()),
                    size = Size(size.width + spread * 2, size.height + spread * 2),
                    cornerRadius = CornerRadius(r + spread, r + spread),
                )
            }
        }
    }

/** Specular edge: bright at the top-left, fading to the bottom-right, like light catching a glass pane. */
@Composable
fun glassEdge(): Brush {
    val g = LocalGlass.current
    return Brush.linearGradient(listOf(g.borderHi, g.borderLo, g.borderHi.copy(alpha = g.borderHi.alpha * .45f)))
}

/** Iridescent edge, used while a card is pressed. */
@Composable
fun holoEdge(): Brush = Brush.linearGradient(LocalGlass.current.holo)

/** Light sheen across the upper-left of a glass surface. */
@Composable
fun glassSheen(): Brush {
    val g = LocalGlass.current
    return Brush.linearGradient(listOf(g.sheen, Color.Transparent), start = Offset.Zero, end = Offset(700f, 700f))
}

/** Thin bright line along the top of a glass bar (bottom navigation). */
fun Modifier.glassBarEdge(): Modifier = composed {
    val g = LocalGlass.current
    val line = g.borderHi
    drawBehind { drawRect(line, Offset.Zero, Size(size.width, 1.dp.toPx())) }
}
