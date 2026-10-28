package app.tinycam.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import app.tinycam.data.Skin
import app.tinycam.data.Tokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Colors of everything around the camera image for one skin. */
@Immutable
class SkinStyle(
    val track: Color,
    /** Cooldown fill, bottom to top. */
    val fill: List<Color>,
    val button: Color,
    val buttonInk: Color,
    val buttonOn: Color,
    val buttonOnInk: Color,
    val holdFill: Color,
    val frame: Color,
    /** Overlay background, top to bottom. */
    val backdrop: List<Color>,
    val accent: Color,
    val shadow: Color,
)

private val RecordFill = listOf(Color(0xFFB91C1C), Color(0xFFFF4D5E), Color(0xFFFF8A8A))

fun styleFor(skin: Skin): SkinStyle = when (skin) {
    Skin.CLASSIC -> SkinStyle(
        track = Color.White.copy(alpha = 0.28f),
        fill = listOf(Color(0xFFFFB800), Color(0xFFFFD84D)),
        button = Color(0xFF2A2F3D), buttonInk = Color.White,
        buttonOn = Color.White, buttonOnInk = Color(0xFF1B1F2A),
        holdFill = Color(0xAAFFC93C), frame = Color.White,
        backdrop = listOf(Color(0xFF1B1F2A), Color(0xFF0C0E13)),
        accent = Color(0xFFFFC93C), shadow = Color.Black,
    )
    Skin.RAINBOW -> SkinStyle(
        track = Color(0xFF3B1A70),
        fill = listOf(
            Color(0xFFFF5D5D), Color(0xFFFFA23A), Color(0xFFFFE14D),
            Color(0xFF5FD068), Color(0xFF4CB3FF), Color(0xFFB57BFF),
        ),
        button = Color(0xFF5B21B6), buttonInk = Color.White,
        buttonOn = Color.White, buttonOnInk = Color(0xFF6D28D9),
        holdFill = Color(0xCCD8B4FE), frame = Color(0xFFE9D5FF),
        backdrop = listOf(Color(0xFF4C1D95), Color(0xFF1E0B3D)),
        accent = Color(0xFFD8B4FE), shadow = Color(0xFF6D28D9),
    )
    Skin.SPACE -> SkinStyle(
        track = Color(0xFF1E1B4B),
        fill = listOf(Color(0xFFFF3D2E), Color(0xFFFF9F1C), Color(0xFFFFE66D)),
        button = Color(0xFF232060), buttonInk = Color(0xFFBAE6FD),
        buttonOn = Color(0xFF7DD3FC), buttonOnInk = Color(0xFF0C1445),
        holdFill = Color(0xCC38BDF8), frame = Color(0xFFBAE6FD),
        backdrop = listOf(Color(0xFF111A4A), Color(0xFF05071A)),
        accent = Color(0xFF7DD3FC), shadow = Color(0xFF0C1445),
    )
    Skin.DINO -> SkinStyle(
        track = Color(0xFF1C4A27),
        fill = listOf(Color(0xFF2F9E44), Color(0xFF69DB7C), Color(0xFFC0F5B5)),
        button = Color(0xFF1E5A2C), buttonInk = Color(0xFFD3F9D8),
        buttonOn = Color(0xFFFFD166), buttonOnInk = Color(0xFF1C4A27),
        holdFill = Color(0xCCFFD166), frame = Color(0xFFC0F5B5),
        backdrop = listOf(Color(0xFF1F5130), Color(0xFF0A2312)),
        accent = Color(0xFFFFD166), shadow = Color(0xFF0A2312),
    )
    Skin.LEOPARD -> SkinStyle(
        track = Color(0xFF3B2412),
        fill = listOf(Color(0xFFC2410C), Color(0xFFF59E0B), Color(0xFFFDE68A)),
        button = Color(0xFF4A2E17), buttonInk = Color(0xFFFDE7C2),
        buttonOn = Color(0xFFF6C35B), buttonOnInk = Color(0xFF3B2412),
        holdFill = Color(0xCCF6C35B), frame = Color(0xFFF6C35B),
        backdrop = listOf(Color(0xFF4A2D15), Color(0xFF1C1008)),
        accent = Color(0xFFF6C35B), shadow = Color(0xFF1C1008),
    )
}

enum class Emblem { SKIN, RECORD, STOP }

/**
 * Draws the pill-shaped shutter for [skin]. Decorations such as fins or spikes may extend
 * outside the bounds, so the caller must leave room around the pill.
 *
 * @param progress cooldown or recording progress, 0..1; 1 means full.
 * @param inactive 0 = ready, 1 = not ready (desaturated and shrunk core).
 * @param spikesLeft side on which outward decorations (dino spikes) are drawn.
 * @param tokens remaining daily captures as symbols below the emblem, or null when unlimited.
 */
fun DrawScope.drawShutter(
    skin: Skin,
    style: SkinStyle,
    progress: Float,
    inactive: Float,
    coreScale: Float,
    emblem: Emblem,
    spikesLeft: Boolean,
    tokens: Tokens? = null,
) {
    val w = size.width
    val h = size.height
    val pill = pillPath(Rect(0f, 0f, w, h))
    val inset = w * 0.11f
    val core = Rect(inset, inset, w - inset, h - inset)
    val cw = core.width

    // Behind the pill
    when (skin) {
        Skin.SPACE -> {
            drawFins(w, h)
            drawExhaust(w, h, progress)
        }
        Skin.DINO -> drawSpikes(w, h, spikesLeft)
        else -> Unit
    }

    // Track with an inner rim
    drawPath(pill, style.track)
    clipPath(pill) {
        drawPath(pill, Color.Black.copy(alpha = 0.22f), style = Stroke(w * 0.07f))
    }

    // Fill rising from the bottom
    val fillColors = if (emblem == Emblem.SKIN) style.fill else RecordFill
    val fillTop = h * (1f - progress.coerceIn(0f, 1f))
    clipPath(pill) {
        drawRect(
            Brush.verticalGradient(fillColors, startY = h, endY = 0f),
            topLeft = Offset(0f, fillTop), size = Size(w, h - fillTop),
        )
        if (progress in 0.01f..0.99f) {
            drawLine(Color.White.copy(alpha = 0.6f), Offset(0f, fillTop), Offset(w, fillTop), w * 0.025f)
        }
    }
    drawPath(pill, Color.White.copy(alpha = 0.35f), style = Stroke(w * 0.015f))

    // Core: the actual button face
    val corePath = pillPath(core)
    scale(coreScale, pivot = core.center) {
        withDesaturation(inactive) {
            clipPath(corePath) {
                drawSkinCore(skin, core)
                if (skin != Skin.SPACE) drawGloss(core)
            }
            val center = emblemCenter(skin, core, roomBelow = tokens != null)
            // On short pills (landscape) the capture symbols matter more than the decorative emblem
            val tight = tokens != null && emblem == Emblem.SKIN && core.height < cw * 3.6f
            if (tokens != null) {
                val top = if (tight) core.top + cw * decorBottom(skin) else center.y + cw * 0.4f
                drawTokens(skin, Rect(core.left + cw * 0.1f, top, core.right - cw * 0.1f, core.bottom - cw * 0.22f), tokens)
            }
            when (emblem) {
                Emblem.SKIN -> if (!tight) drawSkinEmblem(skin, center, cw)
                Emblem.RECORD -> drawRecordEmblem(center, cw, recording = false)
                Emblem.STOP -> drawRecordEmblem(center, cw, recording = true)
            }
            drawPath(corePath, Color.Black.copy(alpha = 0.14f), style = Stroke(w * 0.02f))
        }
    }
}

/** Centered, but below the face or nose cone and with room for the capture symbols underneath. */
private fun emblemCenter(skin: Skin, core: Rect, roomBelow: Boolean): Offset {
    val cw = core.width
    val minY = core.top + cw * when (skin) {
        Skin.CLASSIC -> 0.5f
        Skin.SPACE -> 1.25f
        Skin.DINO -> 1.3f
        Skin.RAINBOW -> 1.42f
        Skin.LEOPARD -> 1.45f
    }
    val maxY = maxOf(minY, core.bottom - cw * if (roomBelow) 1.25f else 0.9f)
    return Offset(core.center.x, core.center.y.coerceIn(minY, maxY))
}

private fun DrawScope.drawSkinCore(skin: Skin, core: Rect) = when (skin) {
    Skin.CLASSIC -> drawClassicCore(core)
    Skin.RAINBOW -> drawRainbowCore(core)
    Skin.SPACE -> drawRocketCore(core)
    Skin.DINO -> drawDinoCore(core)
    Skin.LEOPARD -> drawLeopardCore(core)
}

/**
 * Round shutter for the centered layout. Uses the same skin art as the pill: the pill core is
 * drawn into a virtual rectangle shifted so that the face, rainbow or nose cone lands in the
 * circle. Remaining capture symbols sit in a row above the button.
 */
fun DrawScope.drawRoundShutter(
    skin: Skin,
    style: SkinStyle,
    progress: Float,
    inactive: Float,
    coreScale: Float,
    emblem: Emblem,
    tokens: Tokens? = null,
) {
    val d = size.minDimension
    val c = center
    val r = d / 2
    val ring = Path().apply { addOval(Rect(c, r)) }

    when (skin) {
        Skin.SPACE -> {
            drawFins(d, d)
            drawExhaust(d, d, progress)
        }
        Skin.DINO -> drawRoundPlates(c, r)
        Skin.LEOPARD -> for (dir in listOf(-1f, 1f)) {
            val ear = c + Offset(dir * r * 0.62f, -r * 0.78f)
            drawCircle(Color(0xFFE39B3A), r * 0.3f, ear)
            drawCircle(Color(0xFFF7B6C0), r * 0.16f, ear)
            drawCircle(LeopardDark, r * 0.3f, ear, style = Stroke(r * 0.05f))
        }
        else -> Unit
    }

    drawPath(ring, style.track)
    clipPath(ring) { drawPath(ring, Color.Black.copy(alpha = 0.22f), style = Stroke(d * 0.06f)) }
    val fillColors = if (emblem == Emblem.SKIN) style.fill else RecordFill
    val fillTop = d * (1f - progress.coerceIn(0f, 1f))
    clipPath(ring) {
        drawRect(Brush.verticalGradient(fillColors, startY = d, endY = 0f), Offset(0f, fillTop), Size(d, d - fillTop))
        if (progress in 0.01f..0.99f) drawLine(Color.White.copy(alpha = 0.6f), Offset(0f, fillTop), Offset(d, fillTop), d * 0.02f)
    }
    drawPath(ring, Color.White.copy(alpha = 0.35f), style = Stroke(d * 0.012f))

    val coreR = r * 0.8f
    val core = Rect(c, coreR)
    val cw = core.width
    val corePath = Path().apply { addOval(core) }
    // Virtual pill core: shifted up so the skin's top decoration ends up inside the circle
    val shift = when (skin) {
        Skin.CLASSIC -> 0f
        Skin.RAINBOW -> 0.5f
        Skin.SPACE -> 0.55f
        Skin.DINO -> 0.3f
        Skin.LEOPARD -> 0.25f
    }
    val art = Rect(core.left, core.top - cw * shift, core.right, core.bottom + cw * 0.4f)
    scale(coreScale, pivot = c) {
        withDesaturation(inactive) {
            clipPath(corePath) {
                drawSkinCore(skin, art)
                if (skin == Skin.LEOPARD) {
                    val rnd = Random(4)
                    for (a in listOf(25f, 70f, 110f, 155f, 200f, 340f)) {
                        val rad = Math.toRadians(a.toDouble())
                        drawRosette(c + Offset((cos(rad) * coreR * 0.82f).toFloat(), (sin(rad) * coreR * 0.82f).toFloat()), cw * 0.13f, cw, rnd)
                    }
                }
                if (skin != Skin.SPACE) drawGloss(Rect(core.left, core.top - cw * 0.3f, core.right, core.bottom + cw * 0.3f))
            }
            when (emblem) {
                Emblem.SKIN -> when (skin) {
                    Skin.CLASSIC -> drawSkinEmblem(skin, c, cw * 0.95f)
                    Skin.RAINBOW -> drawSkinEmblem(skin, c + Offset(0f, cw * 0.2f), cw * 0.62f)
                    Skin.SPACE -> drawSkinEmblem(skin, c + Offset(0f, cw * 0.05f), cw * 0.7f)
                    Skin.DINO -> drawSkinEmblem(skin, c + Offset(0f, cw * 0.24f), cw * 0.55f)
                    Skin.LEOPARD -> Unit
                }
                Emblem.RECORD -> drawRecordEmblem(c + Offset(0f, cw * 0.12f), cw * 0.7f, recording = false)
                Emblem.STOP -> drawRecordEmblem(c + Offset(0f, cw * 0.12f), cw * 0.7f, recording = true)
            }
            drawPath(corePath, Color.Black.copy(alpha = 0.14f), style = Stroke(d * 0.015f))
        }
    }

    if (tokens != null) {
        // Row above the button, clear of plates and ears
        val rowW = d * 1.5f
        val cell = minOf(rowW / tokens.total, d * 0.2f)
        val y = -d * 0.32f
        drawTokens(skin, Rect(c.x - cell * tokens.total / 2f, y - cell / 2f, c.x + cell * tokens.total / 2f, y + cell / 2f), tokens)
    }
}

private fun DrawScope.drawRoundPlates(c: Offset, r: Float) {
    val brush = Brush.verticalGradient(listOf(Color(0xFFFFD166), Color(0xFFF77F00)), c.y - r * 1.4f, c.y - r * 0.6f)
    var i = 0
    for (angle in listOf(-60f, -36f, -12f, 12f, 36f, 60f)) {
        val h = r * if (i % 2 == 0) 0.42f else 0.32f
        val w = r * 0.36f
        rotate(angle, pivot = c) {
            val base = c.y - r * 0.9f
            val p = Path().apply {
                moveTo(c.x - w / 2, base)
                cubicTo(c.x - w / 2, base - h * 0.6f, c.x - w * 0.2f, base - h, c.x, base - h)
                cubicTo(c.x + w * 0.2f, base - h, c.x + w / 2, base - h * 0.6f, c.x + w / 2, base)
                close()
            }
            drawPath(p, brush)
            drawPath(p, Color(0xFFC2410C).copy(alpha = 0.55f), style = Stroke(r * 0.04f, join = StrokeJoin.Round))
        }
        i++
    }
}

private fun DrawScope.drawRosette(c: Offset, r: Float, cw: Float, rnd: Random) {
    drawCircle(Color(0xCCB4621F), r * 0.7f, c)
    val start = rnd.nextFloat() * 90f
    for (k in 0 until 4) {
        drawArc(
            LeopardDark, start + k * 90f, 50f + rnd.nextFloat() * 20f, false,
            topLeft = c - Offset(r, r), size = Size(r * 2, r * 2),
            style = Stroke(cw * 0.05f, cap = StrokeCap.Round),
        )
    }
}

/** Where the face, rainbow or nose cone ends, in core widths from the top. */
private fun decorBottom(skin: Skin) = when (skin) {
    Skin.CLASSIC -> 0.25f
    Skin.SPACE, Skin.DINO -> 1.0f
    Skin.RAINBOW, Skin.LEOPARD -> 1.15f
}

private fun pillPath(r: Rect) = Path().apply {
    addRoundRect(RoundRect(r, CornerRadius(minOf(r.width, r.height) / 2f)))
}

private inline fun DrawScope.withDesaturation(amount: Float, block: DrawScope.() -> Unit) {
    if (amount <= 0.001f) {
        block()
        return
    }
    val matrix = ColorMatrix().apply { setToSaturation(1f - 0.75f * amount) }
    val dim = 1f - 0.2f * amount
    matrix.timesAssign(ColorMatrix().apply { setToScale(dim, dim, dim, 1f) })
    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(matrix) }
    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size).inflate(size.width), paint) }
    block()
    drawIntoCanvas { it.restore() }
}

private fun DrawScope.drawGloss(core: Rect) {
    val cw = core.width
    val left = core.left + cw * 0.12f
    val top = core.top + cw * 0.32f
    val bottom = core.bottom - cw * 0.32f
    if (bottom <= top) return
    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f)),
            startY = top, endY = bottom,
        ),
        topLeft = Offset(left, top), size = Size(cw * 0.11f, bottom - top),
        cornerRadius = CornerRadius(cw * 0.06f),
    )
}

// ---------------------------------------------------------------- Classic

private fun DrawScope.drawClassicCore(core: Rect) {
    drawRect(Brush.verticalGradient(listOf(Color.White, Color(0xFFE4E8F0)), core.top, core.bottom), core.topLeft, core.size)
}

// ---------------------------------------------------------------- Rainbow

private val RainbowBands = listOf(
    Color(0xFFFF5D5D), Color(0xFFFFA23A), Color(0xFFFFE14D), Color(0xFF5FD068), Color(0xFF4CB3FF),
)

private fun DrawScope.drawRainbowCore(core: Rect) {
    val cw = core.width
    drawRect(
        Brush.radialGradient(
            listOf(Color(0xFFE9D5FF), Color(0xFFB57BFF), Color(0xFF8B3FF0), Color(0xFF5B21B6)),
            center = Offset(core.center.x, core.top + cw * 0.7f), radius = core.height * 0.95f,
        ),
        core.topLeft, core.size,
    )
    // Small rainbow with clouds at the top
    val arcCenter = Offset(core.center.x, core.top + cw * 1.02f)
    val band = cw * 0.075f
    RainbowBands.forEachIndexed { i, c ->
        val r = cw * 0.40f - i * band
        drawArc(
            c, 180f, 180f, false,
            topLeft = Offset(arcCenter.x - r, arcCenter.y - r), size = Size(r * 2, r * 2),
            style = Stroke(band * 1.05f),
        )
    }
    for (dx in listOf(-1f, 1f)) drawCloud(Offset(arcCenter.x + dx * cw * 0.34f, arcCenter.y + cw * 0.02f), cw * 0.085f)
    // Sparkles
    val sparkles = listOf(0.22f to 0.62f, 0.78f to 0.70f, 0.30f to 0.86f, 0.74f to 0.92f)
    sparkles.forEachIndexed { i, (fx, fy) ->
        val y = core.top + core.height * fy
        if (y > arcCenter.y + cw * 0.2f) drawSparkle(Offset(core.left + cw * fx, y), cw * (if (i % 2 == 0) 0.07f else 0.05f))
    }
}

private fun DrawScope.drawCloud(c: Offset, r: Float) {
    val white = Color.White
    drawCircle(white, r, c)
    drawCircle(white, r * 0.8f, c + Offset(-r * 0.95f, r * 0.25f))
    drawCircle(white, r * 0.8f, c + Offset(r * 0.95f, r * 0.25f))
    drawOval(white, Offset(c.x - r * 1.7f, c.y), Size(r * 3.4f, r * 1.05f))
}

internal fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color = Color.White) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}

internal fun starPath(c: Offset, outer: Float, inner: Float) = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = c.x + (r * cos(a)).toFloat()
        val y = c.y + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// ---------------------------------------------------------------- Space

private val RocketRed = listOf(Color(0xFF9F1239), Color(0xFFF43F5E), Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF881337))

private fun DrawScope.drawRocketCore(core: Rect) {
    val cw = core.width
    drawRect(
        Brush.horizontalGradient(
            0f to Color(0xFF5B6B86), 0.3f to Color(0xFFD4DCE8), 0.45f to Color.White,
            0.7f to Color(0xFFC6D0DE), 1f to Color(0xFF5E6C88),
            startX = core.left, endX = core.right,
        ),
        core.topLeft, core.size,
    )
    val red = Brush.horizontalGradient(RocketRed, core.left, core.right)
    // Nose cone and belly stripe
    drawRect(red, core.topLeft, Size(cw, cw * 0.82f))
    drawRect(Color(0xFF334155).copy(alpha = 0.55f), Offset(core.left, core.top + cw * 0.82f), Size(cw, cw * 0.06f))
    val stripeTop = core.bottom - cw * 0.78f
    drawRect(red, Offset(core.left, stripeTop), Size(cw, cw * 0.18f))
    // Panel rivets along the seams
    val rivet = Color(0xFF64748B)
    for (i in 1..4) {
        val x = core.left + cw * i / 5f
        drawCircle(rivet, cw * 0.018f, Offset(x, core.top + cw * 0.96f))
        drawCircle(rivet, cw * 0.018f, Offset(x, stripeTop - cw * 0.08f))
    }
    // Highlight on the nose
    drawOval(Color.White.copy(alpha = 0.45f), Offset(core.left + cw * 0.22f, core.top + cw * 0.18f), Size(cw * 0.12f, cw * 0.4f))
}

private fun DrawScope.drawPorthole(c: Offset, cw: Float) {
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFF8C9AB0)), c.y - cw * 0.34f, c.y + cw * 0.34f), cw * 0.34f, c)
    drawCircle(Color(0xFF475569), cw * 0.34f, c, style = Stroke(cw * 0.025f))
    for (i in 0 until 8) {
        val a = i * PI / 4
        drawCircle(Color(0xFF64748B), cw * 0.02f, c + Offset((cos(a) * cw * 0.29f).toFloat(), (sin(a) * cw * 0.29f).toFloat()))
    }
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0369A1), Color(0xFF0C2D57)),
            center = c + Offset(-cw * 0.08f, -cw * 0.08f), radius = cw * 0.3f,
        ),
        cw * 0.23f, c,
    )
    drawArc(
        Color.White.copy(alpha = 0.85f), 200f, 70f, false,
        topLeft = c - Offset(cw * 0.16f, cw * 0.16f), size = Size(cw * 0.32f, cw * 0.32f),
        style = Stroke(cw * 0.04f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawFins(w: Float, h: Float) {
    val brush = Brush.verticalGradient(RocketRed.take(3).reversed(), h * 0.6f, h)
    for (mirror in listOf(false, true)) {
        fun x(v: Float) = if (mirror) w - v else v
        val p = Path().apply {
            moveTo(x(w * 0.2f), h * 0.6f)
            quadraticTo(x(-w * 0.2f), h * 0.72f, x(-w * 0.2f), h * 0.88f)
            lineTo(x(-w * 0.2f), h * 0.99f)
            quadraticTo(x(w * 0.02f), h * 0.9f, x(w * 0.25f), h * 0.93f)
            close()
        }
        drawPath(p, brush)
        drawPath(p, Color(0xFF881337).copy(alpha = 0.6f), style = Stroke(w * 0.02f, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawExhaust(w: Float, h: Float, progress: Float) {
    if (progress <= 0.02f) return
    val fh = w * 0.42f * progress
    val top = h * 0.96f
    fun flame(width: Float, height: Float) = Path().apply {
        val cx = w / 2
        moveTo(cx - width / 2, top)
        cubicTo(cx - width / 2, top + height * 0.5f, cx - width * 0.1f, top + height * 0.8f, cx, top + height)
        cubicTo(cx + width * 0.1f, top + height * 0.8f, cx + width / 2, top + height * 0.5f, cx + width / 2, top)
        close()
    }
    drawPath(flame(w * 0.5f, fh), Brush.verticalGradient(listOf(Color(0xFFFF6B1A), Color(0x00FF3D2E)), top, top + fh))
    drawPath(flame(w * 0.28f, fh * 0.7f), Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0x00FFD23F)), top, top + fh * 0.7f))
}

// ---------------------------------------------------------------- Dino

private fun DrawScope.drawDinoCore(core: Rect) {
    val cw = core.width
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFA3E88F), Color(0xFF5FC259), Color(0xFF3C9A47)), core.top, core.bottom),
        core.topLeft, core.size,
    )
    // Scales: soft spots below the face
    val rnd = Random(11)
    var y = core.top + cw * 1.25f
    var row = 0
    while (y < core.bottom) {
        for (fx in if (row % 2 == 0) listOf(0.2f, 0.78f) else listOf(0.5f)) {
            val r = cw * (0.07f + rnd.nextFloat() * 0.05f)
            val c = Offset(core.left + cw * (fx + (rnd.nextFloat() - 0.5f) * 0.1f), y + (rnd.nextFloat() - 0.5f) * cw * 0.1f)
            drawCircle(if (rnd.nextBoolean()) Color(0x8C2E7D38) else Color(0x80D3F9C9), r, c)
        }
        y += cw * 0.36f
        row++
    }
    // Face
    val eyeY = core.top + cw * 0.62f
    drawOval(Color(0xFF235A2B), Offset(core.center.x - cw * 0.16f, core.top + cw * 0.24f), Size(cw * 0.07f, cw * 0.045f))
    drawOval(Color(0xFF235A2B), Offset(core.center.x + cw * 0.09f, core.top + cw * 0.24f), Size(cw * 0.07f, cw * 0.045f))
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(core.center.x + dir * cw * 0.2f, eyeY)
        drawCircle(Color.White, cw * 0.14f, e)
        drawCircle(Color(0xFF235A2B), cw * 0.14f, e, style = Stroke(cw * 0.025f))
        val pupil = e + Offset(-dir * cw * 0.025f, cw * 0.025f)
        drawCircle(Color(0xFF14261A), cw * 0.08f, pupil)
        drawCircle(Color.White, cw * 0.03f, pupil + Offset(-cw * 0.03f, -cw * 0.03f))
        drawOval(Color(0x73FF8FAB), Offset(e.x - cw * 0.08f, eyeY + cw * 0.17f), Size(cw * 0.16f, cw * 0.08f))
    }
    drawArc(
        Color(0xFF14261A), 25f, 130f, false,
        topLeft = Offset(core.center.x - cw * 0.12f, eyeY + cw * 0.02f), size = Size(cw * 0.24f, cw * 0.22f),
        style = Stroke(cw * 0.035f, cap = StrokeCap.Round),
    )
}

/** Three-toed dinosaur track with claws. */
private fun DrawScope.drawFootprint(center: Offset, cw: Float, color: Color) {
    val c = center + Offset(0f, cw * 0.1f)
    drawOval(color, Offset(c.x - cw * 0.12f, c.y - cw * 0.09f), Size(cw * 0.24f, cw * 0.2f))
    for (angle in listOf(-34f, 0f, 34f)) {
        rotate(angle, pivot = c) {
            drawOval(color, Offset(c.x - cw * 0.065f, c.y - cw * 0.27f), Size(cw * 0.13f, cw * 0.26f))
            val claw = Path().apply {
                moveTo(c.x - cw * 0.05f, c.y - cw * 0.24f)
                lineTo(c.x, c.y - cw * 0.35f)
                lineTo(c.x + cw * 0.05f, c.y - cw * 0.24f)
                close()
            }
            drawPath(claw, color)
        }
    }
}

private fun DrawScope.drawSpikes(w: Float, h: Float, left: Boolean) {
    val pitch = w * 0.3f
    val depth = w * 0.3f
    val brush = Brush.horizontalGradient(
        if (left) listOf(Color(0xFFFFD166), Color(0xFFF77F00)) else listOf(Color(0xFFF77F00), Color(0xFFFFD166)),
        startX = if (left) -depth else w, endX = if (left) w * 0.1f else w + depth,
    )
    // Rounded back plates, alternating big and small like a stegosaurus
    var y = h * 0.1f
    var i = 0
    while (y + pitch < h * 0.92f) {
        val base = if (left) w * 0.14f else w * 0.86f
        val d = if (i % 2 == 0) depth else depth * 0.72f
        val tip = if (left) base - w * 0.14f - d else base + w * 0.14f + d
        val mid = base + (tip - base) * 0.62f
        val p = Path().apply {
            moveTo(base, y)
            cubicTo(mid, y - pitch * 0.06f, tip, y + pitch * 0.22f, tip, y + pitch * 0.5f)
            cubicTo(tip, y + pitch * 0.78f, mid, y + pitch * 1.06f, base, y + pitch)
            close()
        }
        drawPath(p, brush)
        drawPath(p, Color(0xFFC2410C).copy(alpha = 0.55f), style = Stroke(w * 0.02f, join = StrokeJoin.Round))
        y += pitch
        i++
    }
}

// ---------------------------------------------------------------- Leopard

private val LeopardDark = Color(0xFF3B2412)

private fun DrawScope.drawLeopardCore(core: Rect) {
    val cw = core.width
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFFCD98A), Color(0xFFF5AE4E), Color(0xFFD9852B)), core.top, core.bottom),
        core.topLeft, core.size,
    )
    val rnd = Random(7)
    // Rosettes: broken dark rings around a darker center
    var y = core.top + cw * 1.3f
    var row = 0
    while (y < core.bottom + cw * 0.2f) {
        for (fx in if (row % 2 == 0) listOf(0.18f, 0.74f) else listOf(0.46f, 1.0f)) {
            val c = Offset(core.left + cw * (fx + (rnd.nextFloat() - 0.5f) * 0.08f), y + (rnd.nextFloat() - 0.5f) * cw * 0.08f)
            val r = cw * (0.12f + rnd.nextFloat() * 0.03f)
            drawCircle(Color(0xCCB4621F), r * 0.7f, c)
            val start = rnd.nextFloat() * 90f
            for (k in 0 until 4) {
                drawArc(
                    LeopardDark, start + k * 90f, 50f + rnd.nextFloat() * 20f, false,
                    topLeft = c - Offset(r, r), size = Size(r * 2, r * 2),
                    style = Stroke(cw * 0.055f, cap = StrokeCap.Round),
                )
            }
        }
        drawCircle(LeopardDark, cw * 0.028f, Offset(core.left + cw * (0.3f + rnd.nextFloat() * 0.4f), y + cw * 0.24f))
        y += cw * 0.48f
        row++
    }
    // Forehead dots
    for ((fx, fy) in listOf(0.38f to 0.2f, 0.5f to 0.14f, 0.62f to 0.2f, 0.3f to 0.34f, 0.7f to 0.34f)) {
        drawCircle(LeopardDark, cw * 0.025f, Offset(core.left + cw * fx, core.top + cw * fy))
    }
    // Face
    val eyeY = core.top + cw * 0.6f
    val muzzle = Offset(core.center.x, eyeY + cw * 0.32f)
    drawOval(Color(0xFFFFF3DC), Offset(muzzle.x - cw * 0.24f, muzzle.y - cw * 0.13f), Size(cw * 0.48f, cw * 0.3f))
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(core.center.x + dir * cw * 0.2f, eyeY)
        drawOval(
            Brush.radialGradient(listOf(Color(0xFFD9F99D), Color(0xFF65A30D)), center = e, radius = cw * 0.12f),
            Offset(e.x - cw * 0.12f, e.y - cw * 0.085f), Size(cw * 0.24f, cw * 0.17f),
        )
        drawOval(LeopardDark, Offset(e.x - cw * 0.12f, e.y - cw * 0.085f), Size(cw * 0.24f, cw * 0.17f), style = Stroke(cw * 0.025f))
        drawOval(Color(0xFF111111), Offset(e.x - cw * 0.025f, e.y - cw * 0.07f), Size(cw * 0.05f, cw * 0.14f))
        drawCircle(Color.White, cw * 0.022f, e + Offset(-cw * 0.045f, -cw * 0.035f))
        // Whiskers
        for (k in -1..1) {
            val from = Offset(muzzle.x + dir * cw * 0.14f, muzzle.y + cw * 0.04f + k * cw * 0.05f)
            drawLine(LeopardDark.copy(alpha = 0.55f), from, from + Offset(dir * cw * 0.32f, k * cw * 0.06f), cw * 0.014f, StrokeCap.Round)
        }
    }
    val noseY = muzzle.y - cw * 0.08f
    val nose = Path().apply {
        moveTo(muzzle.x - cw * 0.08f, noseY)
        lineTo(muzzle.x + cw * 0.08f, noseY)
        quadraticTo(muzzle.x + cw * 0.04f, noseY + cw * 0.08f, muzzle.x, noseY + cw * 0.09f)
        quadraticTo(muzzle.x - cw * 0.04f, noseY + cw * 0.08f, muzzle.x - cw * 0.08f, noseY)
        close()
    }
    drawPath(nose, Color(0xFFE0788A))
    for (dir in listOf(-1f, 1f)) {
        drawArc(
            LeopardDark, 0f, 180f, false,
            topLeft = Offset(muzzle.x + (if (dir < 0) -cw * 0.1f else 0f), noseY + cw * 0.05f),
            size = Size(cw * 0.1f, cw * 0.08f), style = Stroke(cw * 0.022f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawPaw(c: Offset, cw: Float, color: Color) {
    drawOval(color, Offset(c.x - cw * 0.13f, c.y - cw * 0.02f), Size(cw * 0.26f, cw * 0.2f))
    for ((dx, dy) in listOf(-0.17f to -0.06f, -0.06f to -0.16f, 0.06f to -0.16f, 0.17f to -0.06f)) {
        drawOval(color, Offset(c.x + cw * dx - cw * 0.05f, c.y + cw * dy - cw * 0.065f), Size(cw * 0.1f, cw * 0.13f))
    }
}

// ---------------------------------------------------------------- Emblems

private fun DrawScope.drawSkinEmblem(skin: Skin, c: Offset, cw: Float) {
    when (skin) {
        Skin.CLASSIC -> {
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFDDE2EC), Color(0xFFB8C0CF)), c.y - cw * 0.3f, c.y + cw * 0.3f), cw * 0.3f, c)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF4B5568), Color(0xFF1B1F2A)), center = c, radius = cw * 0.21f),
                cw * 0.21f, c,
            )
            drawCircle(Color.White.copy(alpha = 0.8f), cw * 0.05f, c + Offset(-cw * 0.07f, -cw * 0.07f))
        }
        Skin.RAINBOW -> {
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = c, radius = cw * 0.45f),
                cw * 0.45f, c,
            )
            val star = starPath(c + Offset(0f, cw * 0.02f), cw * 0.27f, cw * 0.125f)
            drawPath(star, Color(0xFFFFE14D))
            drawPath(star, Color(0xFFFFE14D), style = Stroke(cw * 0.06f, join = StrokeJoin.Round))
            drawPath(star, Color(0xFFF59E0B), style = Stroke(cw * 0.02f, join = StrokeJoin.Round))
            drawSparkle(c + Offset(-cw * 0.07f, -cw * 0.04f), cw * 0.05f)
        }
        Skin.SPACE -> drawPorthole(c, cw)
        Skin.DINO -> drawFootprint(c, cw, Color(0xFF235A2B).copy(alpha = 0.8f))
        Skin.LEOPARD -> {
            drawCircle(Color(0xFFFFF3DC).copy(alpha = 0.9f), cw * 0.3f, c)
            drawPaw(c + Offset(0f, cw * 0.03f), cw, LeopardDark)
        }
    }
}

private fun DrawScope.drawRecordEmblem(c: Offset, cw: Float, recording: Boolean) {
    drawCircle(Color.White, cw * 0.33f, c)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFF9A9A), Color(0xFFE11D48)), center = c - Offset(cw * 0.06f, cw * 0.06f), radius = cw * 0.3f),
        cw * 0.26f, c,
    )
    if (recording) {
        val s = cw * 0.2f
        drawRoundRect(Color.White, c - Offset(s / 2, s / 2), Size(s, s), CornerRadius(cw * 0.04f))
    }
}

// ---------------------------------------------------------------- Capture symbols

/** Lays out [tokens] in the largest grid that fits [area]; used ones are drawn as empty outlines. */
private fun DrawScope.drawTokens(skin: Skin, area: Rect, tokens: Tokens) {
    val n = tokens.total
    if (n <= 0 || area.height <= 0f) return
    var cell = 0f
    var cols = 1
    for (c in 1..n) {
        val rows = (n + c - 1) / c
        val s = minOf(area.width / c, area.height / rows)
        if (s > cell) {
            cell = s
            cols = c
        }
    }
    val rows = (n + cols - 1) / cols
    val origin = area.center - Offset(cols * cell / 2f, rows * cell / 2f)
    // Light plate so the symbols stay readable on busy skins (rosettes, rocket stripes)
    val pad = cell * 0.12f
    drawRoundRect(
        Color.White.copy(alpha = 0.78f), origin - Offset(pad, pad), Size(cols * cell + pad * 2, rows * cell + pad * 2),
        CornerRadius(cell * 0.45f),
    )
    for (i in 0 until n) {
        val c = origin + Offset((i % cols) * cell + cell / 2f, (i / cols) * cell + cell / 2f)
        drawToken(skin, c, cell * 0.4f, i, filled = i < tokens.filled)
    }
}

private fun DrawScope.drawToken(skin: Skin, c: Offset, r: Float, index: Int, filled: Boolean) {
    val empty = Color.Black.copy(alpha = 0.3f)
    val outline = Stroke(r * 0.18f, join = StrokeJoin.Round)
    when (skin) {
        Skin.RAINBOW, Skin.SPACE -> {
            val star = starPath(c, r * 1.1f, r * 0.5f)
            if (filled) {
                drawPath(star, if (skin == Skin.RAINBOW) RainbowBands[index % RainbowBands.size] else Color(0xFFFFE14D))
                drawPath(star, Color(0xFF1E1B4B).copy(alpha = 0.35f), style = Stroke(r * 0.1f, join = StrokeJoin.Round))
            } else {
                drawPath(star, empty, style = outline)
            }
        }
        Skin.DINO -> {
            val egg = Rect(c.x - r * 0.75f, c.y - r, c.x + r * 0.75f, c.y + r)
            if (filled) {
                drawOval(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF0C2)), egg.top, egg.bottom), egg.topLeft, egg.size)
                drawCircle(Color(0xFF5FC259), r * 0.2f, c + Offset(-r * 0.25f, -r * 0.3f))
                drawCircle(Color(0xFF5FC259), r * 0.14f, c + Offset(r * 0.3f, r * 0.25f))
                drawOval(Color(0xFF235A2B).copy(alpha = 0.5f), egg.topLeft, egg.size, style = Stroke(r * 0.1f))
            } else {
                drawOval(empty, egg.topLeft, egg.size, style = outline)
            }
        }
        Skin.LEOPARD -> {
            if (filled) {
                drawCircle(Color(0xFFFFF3DC), r, c)
                drawPaw(c + Offset(0f, r * 0.1f), r * 2.6f, LeopardDark)
            } else {
                drawCircle(empty, r, c, style = outline)
            }
        }
        Skin.CLASSIC -> {
            if (filled) {
                drawCircle(Color(0xFFFFC93C), r, c)
                drawCircle(Color.White.copy(alpha = 0.6f), r * 0.3f, c + Offset(-r * 0.3f, -r * 0.3f))
            } else {
                drawCircle(empty, r, c, style = outline)
            }
        }
    }
}

// ---------------------------------------------------------------- Backdrops

/** Decorative full-screen background for overlays (gallery, sleep screen). */
fun DrawScope.drawBackdrop(skin: Skin, style: SkinStyle) {
    drawRect(Brush.verticalGradient(style.backdrop))
    val rnd = Random(42)
    val unit = minOf(size.width, size.height) / 100f
    when (skin) {
        Skin.SPACE, Skin.RAINBOW -> repeat(90) {
            val c = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
            val big = rnd.nextFloat() < 0.12f
            if (big) drawSparkle(c, unit * 1.6f, Color.White.copy(alpha = 0.7f))
            else drawCircle(Color.White.copy(alpha = 0.25f + rnd.nextFloat() * 0.5f), unit * (0.2f + rnd.nextFloat() * 0.35f), c)
        }
        Skin.DINO -> repeat(26) {
            val c = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
            rotate(rnd.nextFloat() * 360f, pivot = c) {
                drawOval(Color(0x1AC0F5B5), Offset(c.x - unit * 3, c.y - unit * 1.2f), Size(unit * 6, unit * 2.4f))
            }
        }
        Skin.LEOPARD -> repeat(30) {
            val c = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
            val r = unit * (3f + rnd.nextFloat() * 2f)
            for (k in 0 until 4) {
                drawArc(
                    Color(0x14F6C35B), rnd.nextFloat() * 90f + k * 90f, 55f, false,
                    topLeft = c - Offset(r, r), size = Size(r * 2, r * 2), style = Stroke(unit * 1.2f, cap = StrokeCap.Round),
                )
            }
        }
        Skin.CLASSIC -> Unit
    }
}

/** Crescent moon used on the sleep screen. */
fun DrawScope.drawMoon(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0x40FFE9A8), Color.Transparent), center = c, radius = r * 1.8f), r * 1.8f, c)
    val moon = Path().apply { addOval(Rect(c, r)) }
    val bite = Path().apply { addOval(Rect(c + Offset(r * 0.45f, -r * 0.3f), r * 0.85f)) }
    drawPath(
        Path.combine(PathOperation.Difference, moon, bite),
        Brush.verticalGradient(listOf(Color(0xFFFFF4C2), Color(0xFFFFD45E)), c.y - r, c.y + r),
    )
    drawSparkle(c + Offset(r * 1.25f, r * 0.55f), r * 0.16f, Color(0xFFFFF4C2))
    drawSparkle(c + Offset(r * 0.95f, -r * 1.05f), r * 0.1f, Color(0xFFFFF4C2))
}
