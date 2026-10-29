package app.tinycam.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import app.tinycam.data.Skin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Skin art for the character skins: unicorn, shark, robot, fire truck, monster and ice cream.
// Same conventions as Skins.kt: cores are drawn into a pill-shaped rect of width cw with the
// face at the top, decorations outside the pill come from the "behind" functions.

internal fun Color.shade(f: Float) = Color(red * f, green * f, blue * f, alpha)

/** Drop of radius [r] around [c], pointing up. */
internal fun dropPath(c: Offset, r: Float) = Path().apply {
    moveTo(c.x, c.y - r * 1.3f)
    quadraticTo(c.x + r * 0.95f, c.y - r * 0.2f, c.x + r * 0.95f, c.y + r * 0.25f)
    arcTo(Rect(c.x - r * 0.95f, c.y - r * 0.7f, c.x + r * 0.95f, c.y + r * 1.2f), 0f, 180f, false)
    quadraticTo(c.x - r * 0.95f, c.y - r * 0.2f, c.x, c.y - r * 1.3f)
    close()
}

private fun DrawScope.drawGlossyHeart(c: Offset, s: Float, color: Color) {
    val heart = heartPath(c, s)
    drawPath(heart, Brush.verticalGradient(listOf(color.shade(1.08f), color, color.shade(0.8f)), c.y - s, c.y + s))
    drawPath(heart, color.shade(0.6f), style = Stroke(s * 0.14f, join = StrokeJoin.Round))
    drawOval(Color.White.copy(alpha = 0.7f), Offset(c.x - s * 0.62f, c.y - s * 0.55f), Size(s * 0.36f, s * 0.24f))
}

private fun DrawScope.drawWaterDrop(c: Offset, r: Float) {
    val drop = dropPath(c, r)
    drawPath(drop, Brush.verticalGradient(listOf(Color(0xFFBFDBFE), Color(0xFF3B82F6)), c.y - r * 1.3f, c.y + r * 1.2f))
    drawPath(drop, Color(0xFF1E3A8A).copy(alpha = 0.7f), style = Stroke(r * 0.16f, join = StrokeJoin.Round))
    drawOval(Color.White.copy(alpha = 0.8f), Offset(c.x - r * 0.5f, c.y - r * 0.05f), Size(r * 0.3f, r * 0.45f))
}

/** Round cartoon eye with a pupil that can look sideways, closing with a lid in [skinColor]. */
private fun DrawScope.drawRoundEye(e: Offset, r: Float, look: Float, lid: Float, skinColor: Color, line: Color) {
    drawCircle(Color.White, r, e)
    val pupil = e + Offset(look, r * 0.12f)
    drawCircle(Color(0xFF111827), r * 0.6f, pupil)
    drawCircle(Color.White, r * 0.22f, pupil + Offset(-r * 0.2f, -r * 0.22f))
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.1f, pupil + Offset(r * 0.22f, r * 0.2f))
    drawLid(Rect(e, r), lid, skinColor, line, r * 0.2f)
    drawCircle(line.copy(alpha = 1f - 0.8f * lid), r, e, style = Stroke(r * 0.16f))
}

/** Plate behind an emblem so it stands out from busy bodies. */
private fun DrawScope.drawEmblemPlate(c: Offset, r: Float, fill: Color, rim: Color) {
    drawCircle(Color.Black.copy(alpha = 0.12f), r * 1.03f, c + Offset(0f, r * 0.08f))
    drawCircle(fill, r, c)
    drawCircle(rim, r, c, style = Stroke(r * 0.07f))
}

// ---------------------------------------------------------------- Unicorn

internal val UnicornMane = listOf(
    Color(0xFFFF9EC7), Color(0xFFFFC48C), Color(0xFFFFE97A), Color(0xFFA8F0C6), Color(0xFF9AD8FF), Color(0xFFC9A8FF),
)
private val UnicornCoat = Color(0xFFFDF2F8)
private val UnicornLine = Color(0xFF7E3A8C)

internal fun DrawScope.drawUnicornCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    val cx = core.center.x
    drawRect(Brush.verticalGradient(listOf(Color.White, Color(0xFFFDE7F3), Color(0xFFF1D3FB)), core.top, core.bottom), core.topLeft, core.size)
    // Pastel forelock curling over the forehead
    for (i in 0..4) {
        val c = Offset(core.left + cw * (0.06f + 0.22f * i), core.top + cw * (0.06f + 0.05f * (i % 2)))
        drawCircle(UnicornMane[i], cw * 0.15f, c)
        drawCircle(UnicornMane[i].shade(0.82f), cw * 0.15f, c, style = Stroke(cw * 0.018f))
    }
    // Sparkles below the face twinkle one after another
    val sparkles = listOf(0.22f to 1.5f, 0.78f to 1.75f, 0.3f to 2.2f, 0.72f to 2.6f, 0.25f to 3.1f, 0.76f to 3.5f)
    sparkles.forEachIndexed { i, (fx, fy) ->
        val y = core.top + cw * fy
        if (y < core.bottom - cw * 0.2f) {
            val twinkle = 1f + 0.45f * alive * wave(idle, 2, i * 0.27f)
            drawSparkle(Offset(core.left + cw * fx, y), cw * 0.055f * twinkle, UnicornMane[(i + 4) % UnicornMane.size])
        }
    }
    // Big eyes with lashes
    val eyeY = core.top + cw * 0.5f
    val lid = blink(idle, 0.5f) * alive
    val look = cw * 0.018f * alive * wave(idle, 1, 0.1f)
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(cx + dir * cw * 0.2f, eyeY)
        val eye = Rect(e.x - cw * 0.085f, e.y - cw * 0.11f, e.x + cw * 0.085f, e.y + cw * 0.11f)
        drawOval(Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF2E1065)), eye.top, eye.bottom), eye.topLeft, eye.size)
        drawCircle(Color.White, cw * 0.034f, e + Offset(-cw * 0.03f + look, -cw * 0.045f))
        drawCircle(Color.White.copy(alpha = 0.75f), cw * 0.015f, e + Offset(cw * 0.03f + look, cw * 0.04f))
        drawLid(eye, lid, UnicornCoat, UnicornLine, cw * 0.022f)
        for (k in 0..2) {
            val a = -PI.toFloat() / 2 + dir * (0.55f + k * 0.38f)
            val from = e + Offset(cos(a) * cw * 0.08f, sin(a) * cw * 0.105f)
            drawLine(UnicornLine, from, from + polar(a, cw * 0.06f), cw * 0.02f, StrokeCap.Round)
        }
        drawOval(Color(0x66FF7EB6), Offset(e.x + dir * cw * 0.1f - cw * 0.07f, eyeY + cw * 0.14f), Size(cw * 0.14f, cw * 0.07f))
    }
    // Soft muzzle with nostrils and a smile
    val m = Offset(cx, core.top + cw * 0.87f)
    drawOval(Brush.verticalGradient(listOf(Color(0xFFFCE7F3), Color(0xFFF9C6E0)), m.y - cw * 0.13f, m.y + cw * 0.13f), Offset(m.x - cw * 0.23f, m.y - cw * 0.13f), Size(cw * 0.46f, cw * 0.26f))
    for (dir in listOf(-1f, 1f)) {
        drawOval(Color(0xFFD97FAE), Offset(m.x + dir * cw * 0.09f - cw * 0.018f, m.y - cw * 0.06f), Size(cw * 0.036f, cw * 0.05f))
    }
    drawArc(
        Color(0xFF9D174D), 25f, 130f, false, topLeft = Offset(m.x - cw * 0.08f, m.y - cw * 0.03f),
        size = Size(cw * 0.16f, cw * 0.1f), style = Stroke(cw * 0.024f, cap = StrokeCap.Round),
    )
}

/** Golden spiral horn with its base at [base] and the tip at [tip]. */
private fun DrawScope.drawHorn(base: Offset, tip: Offset, width: Float) {
    val horn = Path().apply {
        moveTo(base.x - width / 2, base.y)
        quadraticTo(base.x - width * 0.3f, (base.y + tip.y) / 2, tip.x, tip.y)
        quadraticTo(base.x + width * 0.3f, (base.y + tip.y) / 2, base.x + width / 2, base.y)
        close()
    }
    drawPath(horn, Brush.horizontalGradient(listOf(Color(0xFFFFF7C2), Color(0xFFFFD45E), Color(0xFFE9A23B)), base.x - width / 2, base.x + width / 2))
    clipPath(horn) {
        val len = base.y - tip.y
        for (k in 1..4) {
            val y = base.y - len * k / 5f
            drawLine(Color(0xFFB7791F).copy(alpha = 0.7f), Offset(base.x - width / 2, y + len * 0.08f), Offset(base.x + width / 2, y - len * 0.04f), width * 0.09f)
        }
    }
    drawPath(horn, Color(0xFFB7791F), style = Stroke(width * 0.08f, join = StrokeJoin.Round))
}

/** Pointy ear leaning outwards by [dir], standing on [base]. */
private fun DrawScope.drawPointyEar(base: Offset, w: Float, h: Float, dir: Float, outer: Color, inner: Color, line: Color) {
    rotate(dir * 20f, pivot = base) {
        val ear = Path().apply {
            moveTo(base.x - w / 2, base.y)
            quadraticTo(base.x - w * 0.4f, base.y - h * 0.7f, base.x, base.y - h)
            quadraticTo(base.x + w * 0.4f, base.y - h * 0.7f, base.x + w / 2, base.y)
            close()
        }
        drawPath(ear, outer)
        val inside = Path().apply {
            moveTo(base.x - w * 0.25f, base.y)
            quadraticTo(base.x - w * 0.2f, base.y - h * 0.55f, base.x, base.y - h * 0.75f)
            quadraticTo(base.x + w * 0.2f, base.y - h * 0.55f, base.x + w * 0.25f, base.y)
            close()
        }
        drawPath(inside, inner)
        drawPath(ear, line, style = Stroke(w * 0.08f, join = StrokeJoin.Round))
    }
}

internal fun DrawScope.drawUnicornBehind(w: Float, h: Float, maneLeft: Boolean, idle: Float, alive: Float) {
    drawMane(w, h, maneLeft, idle, alive)
    for (dir in listOf(-1f, 1f)) drawPointyEar(Offset(w / 2 + dir * w * 0.3f, w * 0.16f), w * 0.2f, w * 0.26f, dir, UnicornCoat, Color(0xFFF9A8D4), UnicornLine)
    drawHorn(Offset(w / 2, w * 0.2f), Offset(w / 2, -w * 0.2f), w * 0.2f)
}

/** Pastel curls down the side of the pill; they sway in a ripple like the dino plates. */
private fun DrawScope.drawMane(w: Float, h: Float, left: Boolean, idle: Float, alive: Float) {
    var y = w * 0.3f
    var i = 0
    while (y < h - w * 0.35f) {
        val r = w * if (i % 2 == 0) 0.17f else 0.14f
        val sway = r * 0.2f * alive * wave(idle, 2, -i * 0.1f)
        val c = Offset(if (left) w * 0.06f - r * 0.6f - sway else w * 0.94f + r * 0.6f + sway, y)
        drawCurl(c, r, UnicornMane[i % UnicornMane.size])
        y += w * 0.21f
        i++
    }
}

private fun DrawScope.drawCurl(c: Offset, r: Float, color: Color) {
    drawCircle(Brush.verticalGradient(listOf(color.shade(1.05f), color.shade(0.88f)), c.y - r, c.y + r), r, c)
    drawArc(
        Color.White.copy(alpha = 0.6f), 200f, 110f, false, c - Offset(r * 0.6f, r * 0.6f), Size(r * 1.2f, r * 1.2f),
        style = Stroke(r * 0.16f, cap = StrokeCap.Round),
    )
    drawCircle(color.shade(0.72f), r, c, style = Stroke(r * 0.1f))
}

internal fun DrawScope.drawUnicornRoundBehind(c: Offset, r: Float, idle: Float, alive: Float) {
    var i = 0
    for (angle in listOf(150f, 172f, 194f, 216f, 238f)) {
        val sway = 4f * alive * wave(idle, 2, -i * 0.12f)
        val rad = Math.toRadians((angle + sway).toDouble()).toFloat()
        drawCurl(c + polar(rad, r * 0.98f), r * if (i % 2 == 0) 0.26f else 0.22f, UnicornMane[i % UnicornMane.size])
        i++
    }
    for (dir in listOf(-1f, 1f)) drawPointyEar(c + Offset(dir * r * 0.55f, -r * 0.72f), r * 0.32f, r * 0.42f, dir, UnicornCoat, Color(0xFFF9A8D4), UnicornLine)
    drawHorn(c + Offset(0f, -r * 0.72f), c + Offset(0f, -r * 1.45f), r * 0.32f)
}

internal fun DrawScope.drawUnicornEmblem(c: Offset, cw: Float, idle: Float, alive: Float) {
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), center = c, radius = cw * 0.42f), cw * 0.42f, c)
    val beat = 1f + 0.06f * alive * maxOf(pulse(idle, 0.15f, 0.08f), pulse(idle, 0.25f, 0.08f))
    scale(beat, pivot = c) { drawGlossyHeart(c, cw * 0.26f, Color(0xFFF472B6)) }
    drawSparkle(c + Offset(cw * 0.22f, -cw * 0.2f), cw * 0.06f * (1f + 0.4f * alive * wave(idle, 2)), Color(0xFFFFE97A))
}

// ---------------------------------------------------------------- Shark

private val SharkSkin = Color(0xFF6A93BA)
private val SharkLine = Color(0xFF1E3A5F)

internal fun DrawScope.drawSharkCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    val cx = core.center.x
    drawRect(
        Brush.horizontalGradient(
            0f to Color(0xFF3B6890), 0.5f to Color(0xFF86AFD2), 1f to Color(0xFF365F86),
            startX = core.left, endX = core.right,
        ),
        core.topLeft, core.size,
    )
    // Pale belly from the jaw down
    val belly = Rect(core.left + cw * 0.27f, core.top + cw * 0.95f, core.right - cw * 0.27f, core.bottom + cw * 0.5f)
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFEAF3FA), Color(0xFFB9D2E6).copy(alpha = 0.6f)), belly.top, belly.bottom),
        belly.topLeft, belly.size, CornerRadius(belly.width / 2),
    )
    // Gill slits on both sides
    for (dir in listOf(-1f, 1f)) {
        for (k in 0..2) {
            val x = cx + dir * cw * 0.4f
            val y = core.top + cw * (1.0f + 0.13f * k)
            drawLine(SharkLine.copy(alpha = 0.7f), Offset(x, y), Offset(x - dir * cw * 0.05f, y + cw * 0.09f), cw * 0.022f, StrokeCap.Round)
        }
    }
    // Bubbles rising along the belly while ready
    if (alive > 0f) {
        val rise = core.height - cw * 1.3f
        for (i in 0..2) {
            val t = (idle * 2 + i / 3f) % 1f
            val y = core.bottom - cw * 0.2f - rise * t
            val c = Offset(core.left + cw * (0.32f + 0.18f * i) + cw * 0.03f * wave(idle, 4, i * 0.3f), y)
            drawCircle(Color.White.copy(alpha = 0.7f * alive * sin01(t)), cw * (0.035f + 0.012f * i), c, style = Stroke(cw * 0.012f))
        }
    }
    // Snout with nostrils
    drawOval(Color.White.copy(alpha = 0.25f), Offset(cx - cw * 0.2f, core.top + cw * 0.06f), Size(cw * 0.4f, cw * 0.14f))
    for (dir in listOf(-1f, 1f)) {
        drawLine(SharkLine, Offset(cx + dir * cw * 0.08f, core.top + cw * 0.22f), Offset(cx + dir * cw * 0.12f, core.top + cw * 0.25f), cw * 0.022f, StrokeCap.Round)
    }
    // Eyes on the sides of the head
    val eyeY = core.top + cw * 0.42f
    val lid = blink(idle, 0.62f) * alive
    val look = cw * 0.02f * alive * wave(idle, 1, 0.4f)
    for (dir in listOf(-1f, 1f)) drawRoundEye(Offset(cx + dir * cw * 0.25f, eyeY), cw * 0.1f, look, lid, SharkSkin, SharkLine)
    // Big friendly grin with a row of teeth
    val top = core.top + cw * 0.64f
    val half = cw * 0.3f
    val mouth = Path().apply {
        moveTo(cx - half, top)
        quadraticTo(cx, top + cw * 0.12f, cx + half, top)
        quadraticTo(cx, top + cw * 0.5f, cx - half, top)
        close()
    }
    drawPath(mouth, Color(0xFF7F1D1D))
    clipPath(mouth) {
        drawOval(Color(0xFFF472B6), Offset(cx - cw * 0.14f, top + cw * 0.15f), Size(cw * 0.28f, cw * 0.16f))
        for (i in 0..6) {
            val x = cx - half * 0.86f + i * half * 1.72f / 6f
            val u = (x - cx) / half
            val y = top + cw * 0.06f * (1f - u * u)
            val tooth = Path().apply {
                moveTo(x - cw * 0.04f, y - cw * 0.01f)
                lineTo(x + cw * 0.04f, y - cw * 0.01f)
                lineTo(x, y + cw * 0.075f)
                close()
            }
            drawPath(tooth, Color.White)
        }
    }
    drawPath(mouth, SharkLine, style = Stroke(cw * 0.022f, join = StrokeJoin.Round))
}

private fun sin01(t: Float) = sin(PI.toFloat() * t)

private fun DrawScope.drawFin(fin: Path, from: Float, to: Float, horizontal: Boolean) {
    val colors = listOf(Color(0xFF7FA6CB), Color(0xFF3F6A92))
    drawPath(fin, if (horizontal) Brush.horizontalGradient(colors, from, to) else Brush.verticalGradient(colors, from, to))
    drawPath(fin, SharkLine.copy(alpha = 0.8f), style = Stroke(abs(to - from) * 0.05f, join = StrokeJoin.Round))
}

internal fun DrawScope.drawSharkBehind(w: Float, h: Float, finLeft: Boolean, idle: Float, alive: Float) {
    // Dorsal fin on the outer side, swept back towards the tail
    val dir = if (finLeft) -1f else 1f
    val bx = if (finLeft) w * 0.16f else w * 0.84f
    val len = minOf(w * 1.4f, h * 0.45f)
    val y0 = h * 0.3f
    val sway = 5f * alive * wave(idle, 2)
    rotate(dir * sway, pivot = Offset(bx, y0 + len / 2)) {
        val tip = Offset(bx + dir * w * 0.44f, y0 + len * 0.88f)
        val fin = Path().apply {
            moveTo(bx, y0)
            quadraticTo(bx + dir * w * 0.36f, y0 + len * 0.3f, tip.x, tip.y)
            quadraticTo(bx + dir * w * 0.14f, y0 + len * 0.75f, bx, y0 + len)
            close()
        }
        drawFin(fin, bx, tip.x, horizontal = true)
        drawPath(
            Path().apply {
                moveTo(bx + dir * w * 0.05f, y0 + len * 0.12f)
                quadraticTo(bx + dir * w * 0.3f, y0 + len * 0.34f, bx + dir * w * 0.38f, y0 + len * 0.76f)
            },
            Color.White.copy(alpha = 0.4f), style = Stroke(w * 0.03f, cap = StrokeCap.Round),
        )
    }
    // Tail fin below the pill, wagging slowly
    rotate(4f * alive * wave(idle, 1), pivot = Offset(w / 2, h - w * 0.1f)) {
        val tail = Path().apply {
            moveTo(w * 0.32f, h - w * 0.12f)
            quadraticTo(w * 0.18f, h + w * 0.02f, w * 0.06f, h + w * 0.2f)
            quadraticTo(w * 0.3f, h + w * 0.12f, w * 0.5f, h + w * 0.04f)
            quadraticTo(w * 0.7f, h + w * 0.12f, w * 0.94f, h + w * 0.2f)
            quadraticTo(w * 0.82f, h + w * 0.02f, w * 0.68f, h - w * 0.12f)
            close()
        }
        drawFin(tail, h - w * 0.12f, h + w * 0.2f, horizontal = false)
    }
}

internal fun DrawScope.drawSharkRoundBehind(c: Offset, r: Float, idle: Float, alive: Float) {
    rotate(4f * alive * wave(idle, 2), pivot = c + Offset(0f, -r * 0.8f)) {
        val tip = c + Offset(r * 0.3f, -r * 1.5f)
        val fin = Path().apply {
            moveTo(c.x - r * 0.42f, c.y - r * 0.8f)
            quadraticTo(c.x - r * 0.05f, c.y - r * 1.05f, tip.x, tip.y)
            quadraticTo(c.x + r * 0.18f, c.y - r * 1.05f, c.x + r * 0.42f, c.y - r * 0.8f)
            close()
        }
        drawFin(fin, tip.y, c.y - r * 0.8f, horizontal = false)
        drawPath(
            Path().apply {
                moveTo(c.x - r * 0.3f, c.y - r * 0.9f)
                quadraticTo(c.x - r * 0.02f, c.y - r * 1.08f, tip.x - r * 0.05f, tip.y + r * 0.12f)
            },
            Color.White.copy(alpha = 0.4f), style = Stroke(r * 0.05f, cap = StrokeCap.Round),
        )
    }
    for (dir in listOf(-1f, 1f)) {
        val fin = Path().apply {
            moveTo(c.x + dir * r * 0.8f, c.y + r * 0.1f)
            quadraticTo(c.x + dir * r * 1.2f, c.y + r * 0.3f, c.x + dir * r * 1.32f, c.y + r * 0.62f)
            quadraticTo(c.x + dir * r * 1.0f, c.y + r * 0.55f, c.x + dir * r * 0.7f, c.y + r * 0.5f)
            close()
        }
        drawFin(fin, c.y + r * 0.1f, c.y + r * 0.62f, horizontal = false)
    }
}

/** Little yellow fish swimming in place. */
internal fun DrawScope.drawFishEmblem(c: Offset, cw: Float, idle: Float, alive: Float) {
    drawEmblemPlate(c, cw * 0.32f, Color(0xFFE0F2FE), Color(0xFF7DD3FC))
    rotate(6f * alive * wave(idle, 2), pivot = c) { drawFish(c + Offset(cw * 0.02f, 0f), cw * 0.2f, Color(0xFFFFC93C)) }
}

private fun DrawScope.drawFish(c: Offset, s: Float, color: Color) {
    val tail = Path().apply {
        moveTo(c.x + s * 0.55f, c.y)
        lineTo(c.x + s * 1.05f, c.y - s * 0.45f)
        lineTo(c.x + s * 1.05f, c.y + s * 0.45f)
        close()
    }
    drawPath(tail, color.shade(0.85f))
    drawPath(tail, color.shade(0.55f), style = Stroke(s * 0.08f, join = StrokeJoin.Round))
    val body = Rect(c.x - s * 0.8f, c.y - s * 0.5f, c.x + s * 0.7f, c.y + s * 0.5f)
    drawOval(Brush.verticalGradient(listOf(color.shade(1.1f), color, color.shade(0.85f)), body.top, body.bottom), body.topLeft, body.size)
    drawOval(color.shade(0.55f), body.topLeft, body.size, style = Stroke(s * 0.08f))
    drawArc(color.shade(0.7f), 300f, 120f, false, Offset(c.x - s * 0.1f, c.y - s * 0.3f), Size(s * 0.5f, s * 0.6f), style = Stroke(s * 0.07f, cap = StrokeCap.Round))
    drawCircle(Color(0xFF111827), s * 0.13f, c + Offset(-s * 0.42f, -s * 0.08f))
    drawCircle(Color.White, s * 0.05f, c + Offset(-s * 0.46f, -s * 0.12f))
}

private fun DrawScope.drawBubble(c: Offset, r: Float) {
    drawCircle(Color(0x6693C5FD), r, c)
    drawCircle(Color.White.copy(alpha = 0.9f), r, c, style = Stroke(r * 0.16f))
    drawCircle(Color.White, r * 0.25f, c + Offset(-r * 0.35f, -r * 0.35f))
}

// ---------------------------------------------------------------- Robot

private val RobotLed = Color(0xFF67E8F9)

internal fun DrawScope.drawRobotCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    val cx = core.center.x
    drawRect(
        Brush.horizontalGradient(
            0f to Color(0xFF5F6E84), 0.28f to Color(0xFFCBD5E1), 0.45f to Color(0xFFF1F5F9),
            0.7f to Color(0xFFB6C2D1), 1f to Color(0xFF56657C),
            startX = core.left, endX = core.right,
        ),
        core.topLeft, core.size,
    )
    // Screen face with a bezel
    val screen = Rect(core.left + cw * 0.14f, core.top + cw * 0.2f, core.right - cw * 0.14f, core.top + cw * 0.92f)
    val bezel = screen.inflate(cw * 0.035f)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF334155)), bezel.top, bezel.bottom), bezel.topLeft, bezel.size, CornerRadius(cw * 0.17f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF070B16)), screen.top, screen.bottom), screen.topLeft, screen.size, CornerRadius(cw * 0.13f))
    // LED eyes scan left and right and blink
    val eyeY = screen.top + screen.height * 0.4f
    val lid = blink(idle, 0.3f) * alive
    val scan = cw * 0.03f * alive * wave(idle, 1)
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(cx + dir * cw * 0.16f + scan, eyeY)
        val ew = cw * 0.13f
        val eh = cw * 0.19f * (1f - 0.85f * lid)
        drawCircle(Brush.radialGradient(listOf(RobotLed.copy(alpha = 0.45f), Color.Transparent), center = e, radius = cw * 0.17f), cw * 0.17f, e)
        drawRoundRect(RobotLed, e - Offset(ew / 2, eh / 2), Size(ew, eh), CornerRadius(ew * 0.4f))
        drawRoundRect(Color.White.copy(alpha = 0.8f), e - Offset(ew * 0.3f, eh * 0.35f), Size(ew * 0.22f, eh * 0.3f), CornerRadius(ew * 0.1f))
    }
    // Pixel smile
    val px = cw * 0.05f
    val mouthY = screen.top + screen.height * 0.78f
    for (i in -2..2) {
        val y = mouthY - if (abs(i) == 2) px else 0f
        drawRect(RobotLed, Offset(cx + i * px * 1.2f - px / 2, y - px / 2), Size(px, px))
    }
    drawLine(Color.White.copy(alpha = 0.18f), Offset(screen.left + cw * 0.06f, screen.bottom - cw * 0.08f), Offset(screen.left + cw * 0.2f, screen.top + cw * 0.08f), cw * 0.04f, StrokeCap.Round)
    // Panel seam with bolts and status lights running one after another
    val seam = core.top + cw * 1.04f
    drawLine(Color(0xFF334155).copy(alpha = 0.5f), Offset(core.left, seam), Offset(core.right, seam), cw * 0.015f)
    drawLine(Color.White.copy(alpha = 0.5f), Offset(core.left, seam + cw * 0.015f), Offset(core.right, seam + cw * 0.015f), cw * 0.01f)
    for (dir in listOf(-1f, 1f)) drawBolt(Offset(cx + dir * cw * 0.36f, seam + cw * 0.14f), cw * 0.045f)
    val lights = listOf(Color(0xFFF87171), Color(0xFFFACC15), Color(0xFF4ADE80))
    lights.forEachIndexed { i, color ->
        val c = Offset(cx + (i - 1) * cw * 0.16f, seam + cw * 0.14f)
        val on = alive * maxOf(0f, wave(idle, 3, -i / 3f))
        drawCircle(Color(0xFF1E293B), cw * 0.05f, c)
        drawCircle(color.copy(alpha = 0.35f + 0.65f * on), cw * 0.035f, c)
        if (on > 0.05f) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.5f * on), Color.Transparent), center = c, radius = cw * 0.09f), cw * 0.09f, c)
    }
    // Vents at the bottom
    for (k in 0..2) {
        val y = core.bottom - cw * (0.32f + 0.11f * k)
        if (y > seam + cw * 0.4f) {
            drawLine(Color(0xFF334155).copy(alpha = 0.6f), Offset(cx - cw * 0.2f, y), Offset(cx + cw * 0.2f, y), cw * 0.04f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawBolt(c: Offset, r: Float) {
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFF64748B)), c.y - r, c.y + r), r, c)
    drawCircle(Color(0xFF334155), r, c, style = Stroke(r * 0.18f))
    rotate(35f, pivot = c) { drawLine(Color(0xFF334155), c - Offset(r * 0.55f, 0f), c + Offset(r * 0.55f, 0f), r * 0.25f, StrokeCap.Round) }
}

/** Antenna with a blinking ball and ear bolts on both sides of the head. */
private fun DrawScope.drawRobotHead(antennaBase: Offset, antennaTop: Offset, ball: Float, ears: List<Offset>, ear: Size, idle: Float, alive: Float) {
    drawLine(Color(0xFF475569), antennaBase, antennaTop, ball * 0.5f, StrokeCap.Round)
    drawLine(Color(0xFFCBD5E1), antennaBase - Offset(ball * 0.08f, 0f), antennaTop - Offset(ball * 0.08f, 0f), ball * 0.15f, StrokeCap.Round)
    val on = alive * maxOf(0f, wave(idle, 2))
    if (on > 0.05f) drawCircle(Brush.radialGradient(listOf(Color(0x99F87171), Color.Transparent), center = antennaTop, radius = ball * 2.4f), ball * 2.4f * on, antennaTop)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFECACA), Color(0xFFEF4444), Color(0xFF991B1B)), center = antennaTop - Offset(ball * 0.3f, ball * 0.3f), radius = ball * 1.3f), ball, antennaTop)
    drawCircle(Color(0xFF7F1D1D), ball, antennaTop, style = Stroke(ball * 0.15f))
    for (e in ears) {
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFF64748B)), e.y - ear.height / 2, e.y + ear.height / 2),
            e - Offset(ear.width / 2, ear.height / 2), ear, CornerRadius(ear.width * 0.3f),
        )
        drawRoundRect(Color(0xFF334155), e - Offset(ear.width / 2, ear.height / 2), ear, CornerRadius(ear.width * 0.3f), style = Stroke(ear.width * 0.1f))
        drawCircle(Color(0xFF334155), ear.width * 0.16f, e)
    }
}

internal fun DrawScope.drawRobotBehind(w: Float, h: Float, idle: Float, alive: Float) {
    val earY = w * 0.11f + w * 0.78f * 0.55f
    drawRobotHead(
        Offset(w / 2, w * 0.12f), Offset(w / 2, -w * 0.11f), w * 0.065f,
        listOf(Offset(0f, earY), Offset(w, earY)), Size(w * 0.2f, w * 0.26f), idle, alive,
    )
}

internal fun DrawScope.drawRobotRoundBehind(c: Offset, r: Float, idle: Float, alive: Float) {
    drawRobotHead(
        c + Offset(0f, -r * 0.8f), c + Offset(0f, -r * 1.32f), r * 0.12f,
        listOf(c + Offset(-r, 0f), c + Offset(r, 0f)), Size(r * 0.36f, r * 0.5f), idle, alive,
    )
}

private fun gearPath(c: Offset, outer: Float, inner: Float, teeth: Int) = Path().apply {
    val n = teeth * 4
    for (i in 0 until n) {
        val r = if (i % 4 == 1 || i % 4 == 2) outer else inner
        val p = c + polar(i * Tau / n, r)
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** Orange gear that turns once per idle loop. */
internal fun DrawScope.drawGearEmblem(c: Offset, cw: Float, idle: Float, alive: Float) {
    drawEmblemPlate(c, cw * 0.33f, Color(0xFF1E293B), Color(0xFF64748B))
    rotate(360f * idle * alive, pivot = c) {
        val gear = gearPath(c, cw * 0.25f, cw * 0.19f, 8)
        drawPath(gear, Brush.verticalGradient(listOf(Color(0xFFFCD34D), Color(0xFFF59E0B), Color(0xFFB45309)), c.y - cw * 0.25f, c.y + cw * 0.25f))
        drawPath(gear, Color(0xFF78350F), style = Stroke(cw * 0.02f, join = StrokeJoin.Round))
        drawCircle(Color(0xFF1E293B), cw * 0.07f, c)
        drawCircle(Color(0xFF78350F), cw * 0.07f, c, style = Stroke(cw * 0.02f))
    }
    drawArc(Color.White.copy(alpha = 0.5f), 200f, 60f, false, c - Offset(cw * 0.16f, cw * 0.16f), Size(cw * 0.32f, cw * 0.32f), style = Stroke(cw * 0.03f, cap = StrokeCap.Round))
}

private fun lightningPath(c: Offset, r: Float) = Path().apply {
    moveTo(c.x + r * 0.25f, c.y - r * 1.2f)
    lineTo(c.x - r * 0.6f, c.y + r * 0.15f)
    lineTo(c.x - r * 0.02f, c.y + r * 0.15f)
    lineTo(c.x - r * 0.25f, c.y + r * 1.2f)
    lineTo(c.x + r * 0.6f, c.y - r * 0.2f)
    lineTo(c.x + r * 0.02f, c.y - r * 0.2f)
    close()
}

// ---------------------------------------------------------------- Fire truck

internal fun DrawScope.drawFireTruckCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    val cx = core.center.x
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626), Color(0xFFB91C1C)), core.top, core.bottom), core.topLeft, core.size)
    // Windshield with the truck's eyes
    val glass = Rect(core.left + cw * 0.13f, core.top + cw * 0.2f, core.right - cw * 0.13f, core.top + cw * 0.66f)
    val frame = glass.inflate(cw * 0.035f)
    drawRoundRect(Color(0xFF7F1D1D), frame.topLeft, frame.size, CornerRadius(cw * 0.14f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFF7DD3FC)), glass.top, glass.bottom), glass.topLeft, glass.size, CornerRadius(cw * 0.11f))
    val lid = blink(idle, 0.75f) * alive
    val look = cw * 0.02f * alive * wave(idle, 1, 0.6f)
    for (dir in listOf(-1f, 1f)) drawRoundEye(Offset(cx + dir * cw * 0.16f, glass.center.y + cw * 0.01f), cw * 0.1f, look, lid, Color(0xFFBAE6FD), Color(0xFF1E3A8A))
    drawLine(Color.White.copy(alpha = 0.7f), Offset(glass.right - cw * 0.12f, glass.top + cw * 0.06f), Offset(glass.right - cw * 0.05f, glass.top + cw * 0.16f), cw * 0.03f, StrokeCap.Round)
    // Smile, grill and headlights
    drawArc(
        Color(0xFF450A0A), 25f, 130f, false, topLeft = Offset(cx - cw * 0.12f, core.top + cw * 0.66f),
        size = Size(cw * 0.24f, cw * 0.16f), style = Stroke(cw * 0.032f, cap = StrokeCap.Round),
    )
    val grill = Rect(cx - cw * 0.2f, core.top + cw * 0.9f, cx + cw * 0.2f, core.top + cw * 1.08f)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8)), grill.top, grill.bottom), grill.topLeft, grill.size, CornerRadius(cw * 0.04f))
    for (k in 1..4) {
        val x = grill.left + grill.width * k / 5f
        drawLine(Color(0xFF475569), Offset(x, grill.top + cw * 0.035f), Offset(x, grill.bottom - cw * 0.035f), cw * 0.018f, StrokeCap.Round)
    }
    for (dir in listOf(-1f, 1f)) {
        val l = Offset(cx + dir * cw * 0.34f, grill.center.y)
        drawCircle(Brush.radialGradient(listOf(Color(0x88FEF08A), Color.Transparent), center = l, radius = cw * 0.15f), cw * 0.15f, l)
        drawCircle(Color(0xFFFEF9C3), cw * 0.07f, l)
        drawCircle(Color(0xFF9CA3AF), cw * 0.07f, l, style = Stroke(cw * 0.02f))
    }
    // Golden stripe and the ladder along the body
    val stripe = core.top + cw * 1.2f
    drawRect(Color(0xFFFACC15), Offset(core.left, stripe), Size(cw, cw * 0.1f))
    drawRect(Color.White.copy(alpha = 0.5f), Offset(core.left, stripe), Size(cw, cw * 0.02f))
    drawRect(Color(0xFFA16207).copy(alpha = 0.6f), Offset(core.left, stripe + cw * 0.085f), Size(cw, cw * 0.015f))
    val top = stripe + cw * 0.3f
    val bottom = core.bottom - cw * 0.3f
    if (bottom > top) {
        var y = top + cw * 0.12f
        while (y < bottom) {
            drawLine(Color(0xFFCBD5E1), Offset(cx - cw * 0.22f, y), Offset(cx + cw * 0.22f, y), cw * 0.04f)
            y += cw * 0.24f
        }
        for (dir in listOf(-1f, 1f)) {
            val x = cx + dir * cw * 0.24f
            drawLine(Color(0xFF7F1D1D).copy(alpha = 0.4f), Offset(x + cw * 0.015f, top + cw * 0.015f), Offset(x + cw * 0.015f, bottom + cw * 0.015f), cw * 0.06f, StrokeCap.Round)
            drawLine(Color(0xFFF1F5F9), Offset(x, top), Offset(x, bottom), cw * 0.06f, StrokeCap.Round)
        }
    }
}

/** Light bar with two blue lights flashing in turn. */
private fun DrawScope.drawSiren(c: Offset, w: Float, idle: Float, alive: Float) {
    val bar = Rect(c.x - w / 2, c.y - w * 0.05f, c.x + w / 2, c.y + w * 0.16f)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF94A3B8), Color(0xFF334155)), bar.top, bar.bottom), bar.topLeft, bar.size, CornerRadius(w * 0.06f))
    for (dir in listOf(-1f, 1f)) {
        val l = Offset(c.x + dir * w * 0.24f, c.y - w * 0.03f)
        val r = w * 0.2f
        val on = alive * maxOf(0f, dir * wave(idle, 6))
        if (on > 0.05f) drawCircle(Brush.radialGradient(listOf(Color(0xAA60A5FA), Color.Transparent), center = l, radius = r * 2.6f), r * 2.6f * on, l)
        drawArc(
            Brush.verticalGradient(listOf(Color(0xFFBFDBFE), Color(0xFF1D4ED8).copy(alpha = 1f)), l.y - r, l.y),
            180f, 180f, true, l - Offset(r, r), Size(r * 2, r * 2),
        )
        drawArc(Color(0xFF60A5FA).copy(alpha = on), 180f, 180f, true, l - Offset(r, r), Size(r * 2, r * 2))
        drawArc(Color(0xFF1E3A8A), 180f, 180f, true, l - Offset(r, r), Size(r * 2, r * 2), style = Stroke(w * 0.025f, join = StrokeJoin.Round))
        drawCircle(Color.White.copy(alpha = 0.7f), r * 0.18f, l + Offset(-r * 0.35f, -r * 0.5f))
    }
}

private fun DrawScope.drawWheel(c: Offset, r: Float) {
    drawCircle(Color(0xFF111827), r, c)
    drawCircle(Color(0xFF374151), r * 0.86f, c, style = Stroke(r * 0.06f))
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8)), c.y - r * 0.5f, c.y + r * 0.5f), r * 0.5f, c)
    for (i in 0 until 5) drawCircle(Color(0xFF475569), r * 0.07f, c + polar(i * Tau / 5, r * 0.3f))
    drawCircle(Color(0xFF475569), r * 0.12f, c)
}

internal fun DrawScope.drawFireTruckBehind(w: Float, h: Float, idle: Float, alive: Float) {
    val r = w * 0.2f
    val rear = h - w * 0.34f
    for (x in listOf(w * 0.05f, w * 0.95f)) {
        drawWheel(Offset(x, rear), r)
        if (rear - w * 1.4f > w * 1.2f) drawWheel(Offset(x, w * 1.3f), r)
    }
    drawSiren(Offset(w / 2, w * 0.02f), w * 0.62f, idle, alive)
}

internal fun DrawScope.drawFireTruckRoundBehind(c: Offset, r: Float, idle: Float, alive: Float) {
    for (dir in listOf(-1f, 1f)) drawWheel(c + Offset(dir * r * 0.72f, r * 0.72f), r * 0.32f)
    drawSiren(c + Offset(0f, -r * 0.95f), r * 1.0f, idle, alive)
}

private fun flamePath(c: Offset, s: Float) = Path().apply {
    moveTo(c.x, c.y - s)
    cubicTo(c.x + s * 0.25f, c.y - s * 0.55f, c.x + s * 0.75f, c.y - s * 0.3f, c.x + s * 0.65f, c.y + s * 0.25f)
    cubicTo(c.x + s * 0.6f, c.y + s * 0.65f, c.x + s * 0.3f, c.y + s * 0.8f, c.x, c.y + s * 0.8f)
    cubicTo(c.x - s * 0.3f, c.y + s * 0.8f, c.x - s * 0.62f, c.y + s * 0.6f, c.x - s * 0.62f, c.y + s * 0.2f)
    cubicTo(c.x - s * 0.62f, c.y - s * 0.15f, c.x - s * 0.35f, c.y - s * 0.3f, c.x - s * 0.3f, c.y - s * 0.55f)
    cubicTo(c.x - s * 0.1f, c.y - s * 0.35f, c.x - s * 0.05f, c.y - s * 0.7f, c.x, c.y - s)
    close()
}

/** Golden fire brigade badge with a flickering flame. */
internal fun DrawScope.drawBadgeEmblem(c: Offset, cw: Float, idle: Float, alive: Float) {
    val r = cw * 0.3f
    drawCircle(Color.Black.copy(alpha = 0.15f), r * 1.05f, c + Offset(0f, r * 0.1f))
    for (i in 0 until 8) {
        rotate(i * 45f, pivot = c) {
            drawRoundRect(Color(0xFFD97706), Offset(c.x - r * 0.18f, c.y - r * 1.12f), Size(r * 0.36f, r * 0.5f), CornerRadius(r * 0.1f))
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFACC15), Color(0xFFD97706)), center = c - Offset(r * 0.3f, r * 0.3f), radius = r * 1.4f), r, c)
    drawCircle(Color(0xFF92400E), r, c, style = Stroke(r * 0.08f))
    drawCircle(Color(0xFF92400E).copy(alpha = 0.5f), r * 0.8f, c, style = Stroke(r * 0.04f))
    val flicker = 1f + 0.07f * alive * wave(idle, 4)
    scale(1f, flicker, pivot = c + Offset(0f, r * 0.5f)) {
        drawPath(flamePath(c + Offset(0f, r * 0.05f), r * 0.6f), Brush.verticalGradient(listOf(Color(0xFFF97316), Color(0xFFDC2626)), c.y - r * 0.6f, c.y + r * 0.6f))
        drawPath(flamePath(c + Offset(0f, r * 0.25f), r * 0.32f), Color(0xFFFDE047))
    }
}

// ---------------------------------------------------------------- Monster

private val MonsterFur = Color(0xFF4F8EF7)
private val MonsterDark = Color(0xFF1E3A8A)

internal fun DrawScope.drawMonsterCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    val cx = core.center.x
    drawRect(Brush.verticalGradient(listOf(Color(0xFF7DB4FB), Color(0xFF3B82F6), Color(0xFF1D4ED8)), core.top, core.bottom), core.topLeft, core.size)
    // Shaggy fur: short strokes in lighter and darker blue
    val rnd = Random(23)
    repeat((core.height / cw * 16).toInt()) { i ->
        val p = Offset(core.left + rnd.nextFloat() * cw, core.top + rnd.nextFloat() * core.height)
        val a = Math.toRadians(70.0 + rnd.nextFloat() * 40.0).toFloat()
        val color = if (i % 2 == 0) Color(0x4DBFDBFE) else Color(0x4D1E3A8A)
        drawLine(color, p, p + polar(a, cw * 0.07f), cw * 0.025f, StrokeCap.Round)
    }
    // Spots on the belly
    for ((fx, fy, fr) in listOf(Triple(0.2f, 1.6f, 0.08f), Triple(0.78f, 1.9f, 0.1f), Triple(0.3f, 2.5f, 0.07f), Triple(0.74f, 2.9f, 0.08f), Triple(0.22f, 3.4f, 0.09f))) {
        val y = core.top + cw * fy
        if (y < core.bottom) drawCircle(Color(0x597C3AED), cw * fr, Offset(core.left + cw * fx, y))
    }
    // One big eye that looks around
    val e = Offset(cx, core.top + cw * 0.46f)
    val er = cw * 0.24f
    drawCircle(MonsterDark.copy(alpha = 0.45f), er * 1.14f, e)
    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFDDE5F0)), center = e - Offset(er * 0.3f, er * 0.3f), radius = er * 1.4f), er, e)
    val look = Offset(cw * 0.06f * alive * wave(idle, 1), cw * 0.03f * alive * wave(idle, 1, 0.25f))
    val iris = e + look
    drawCircle(Brush.radialGradient(listOf(Color(0xFFD9F99D), Color(0xFF65A30D)), center = iris, radius = er * 0.55f), er * 0.52f, iris)
    drawCircle(Color(0xFF111827), er * 0.26f, iris)
    drawCircle(Color.White, er * 0.12f, iris + Offset(-er * 0.18f, -er * 0.2f))
    val lid = blink(idle, 0.22f) * alive
    drawLid(Rect(e, er), lid, MonsterFur, MonsterDark, cw * 0.03f)
    drawCircle(MonsterDark.copy(alpha = 1f - 0.8f * lid), er, e, style = Stroke(cw * 0.025f))
    // Bushy brow that lifts while looking
    drawArc(
        MonsterDark, 205f, 130f, false, topLeft = e - Offset(er * 1.15f, er * (1.3f + 0.1f * alive * wave(idle, 2))),
        size = Size(er * 2.3f, er * 2f), style = Stroke(cw * 0.06f, cap = StrokeCap.Round),
    )
    // Wide grin with two fangs
    val top = core.top + cw * 0.84f
    val half = cw * 0.3f
    val mouth = Path().apply {
        moveTo(cx - half, top)
        quadraticTo(cx, top + cw * 0.08f, cx + half, top)
        quadraticTo(cx, top + cw * 0.5f, cx - half, top)
        close()
    }
    drawPath(mouth, Color(0xFF3B0A2A))
    clipPath(mouth) { drawOval(Color(0xFFF472B6), Offset(cx - cw * 0.13f, top + cw * 0.15f), Size(cw * 0.26f, cw * 0.16f)) }
    for (dir in listOf(-1f, 1f)) {
        val x = cx + dir * cw * 0.15f
        val y = top + cw * 0.035f
        val fang = Path().apply {
            moveTo(x - cw * 0.05f, y - cw * 0.01f)
            lineTo(x + cw * 0.05f, y - cw * 0.01f)
            lineTo(x + dir * cw * 0.01f, y + cw * 0.1f)
            close()
        }
        drawPath(fang, Color.White)
        drawPath(fang, MonsterDark.copy(alpha = 0.4f), style = Stroke(cw * 0.012f, join = StrokeJoin.Round))
    }
    drawPath(mouth, MonsterDark, style = Stroke(cw * 0.024f, join = StrokeJoin.Round))
}

private fun DrawScope.drawMonsterHorn(base: Offset, len: Float, width: Float, dir: Float) {
    val tip = base + Offset(dir * len * 0.45f, -len)
    val horn = Path().apply {
        moveTo(base.x - width / 2, base.y)
        quadraticTo(base.x - width / 2 + dir * len * 0.05f, base.y - len * 0.6f, tip.x, tip.y)
        quadraticTo(base.x + width / 2 + dir * len * 0.25f, base.y - len * 0.45f, base.x + width / 2, base.y)
        close()
    }
    drawPath(horn, Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFE9C46A)), tip.y, base.y))
    clipPath(horn) {
        for (k in 1..3) {
            val y = base.y - len * k * 0.22f
            drawLine(Color(0xFFB08A3E).copy(alpha = 0.6f), Offset(base.x - width, y), Offset(base.x + width, y - len * 0.06f), width * 0.1f)
        }
    }
    drawPath(horn, Color(0xFF8A6A2B), style = Stroke(width * 0.1f, join = StrokeJoin.Round))
}

/** Fur tufts around an outline given by [point] (u 0..1 around it), wiggling a little. */
private fun DrawScope.drawFuzz(n: Int, r: Float, idle: Float, alive: Float, point: (Float, Float) -> Offset) {
    for (i in 0 until n) {
        val grow = 1f + 0.04f * alive * wave(idle, 4, i * 0.37f)
        drawCircle(if (i % 2 == 0) Color(0xFF1D4ED8) else Color(0xFF2563EB), r, point(i / n.toFloat(), grow))
    }
}

internal fun DrawScope.drawMonsterBehind(w: Float, h: Float, idle: Float, alive: Float) {
    val n = ((2 * (h - w) + PI.toFloat() * w) / (w * 0.09f)).toInt()
    drawFuzz(n, w * 0.07f, idle, alive) { u, grow -> stadiumPoint(Offset(w / 2, w / 2), Offset(w / 2, h - w / 2), w / 2 * grow, u) }
    for (dir in listOf(-1f, 1f)) drawMonsterHorn(Offset(w / 2 + dir * w * 0.26f, w * 0.18f), w * 0.34f, w * 0.15f, dir)
}

internal fun DrawScope.drawMonsterRoundBehind(c: Offset, r: Float, idle: Float, alive: Float) {
    drawFuzz(28, r * 0.13f, idle, alive) { u, grow -> c + polar(u * Tau, r * grow) }
    for (dir in listOf(-1f, 1f)) drawMonsterHorn(c + Offset(dir * r * 0.48f, -r * 0.62f), r * 0.65f, r * 0.26f, dir)
}

/** Wrapped candy, monsters love those. */
internal fun DrawScope.drawCandyEmblem(c: Offset, cw: Float, idle: Float, alive: Float) {
    rotate(-15f + 8f * alive * wave(idle, 2), pivot = c) {
        val r = cw * 0.17f
        for (dir in listOf(-1f, 1f)) {
            val end = Path().apply {
                moveTo(c.x + dir * r * 0.8f, c.y)
                lineTo(c.x + dir * r * 1.9f, c.y - r * 0.75f)
                quadraticTo(c.x + dir * r * 1.7f, c.y, c.x + dir * r * 1.9f, c.y + r * 0.75f)
                close()
            }
            drawPath(end, Color(0xFFF472B6))
            drawPath(end, Color(0xFF9D174D), style = Stroke(r * 0.1f, join = StrokeJoin.Round))
        }
        val ball = Path().apply { addOval(Rect(c, r)) }
        drawPath(ball, Color.White)
        clipPath(ball) {
            rotate(40f, pivot = c) {
                for (k in -3..3 step 2) drawRect(Color(0xFFF472B6), Offset(c.x + k * r * 0.35f, c.y - r * 1.5f), Size(r * 0.35f, r * 3f))
            }
        }
        drawCircle(Color(0xFF9D174D), r, c, style = Stroke(r * 0.1f))
        drawCircle(Color.White.copy(alpha = 0.8f), r * 0.2f, c + Offset(-r * 0.4f, -r * 0.4f))
    }
}

private fun DrawScope.drawEyeball(c: Offset, r: Float, index: Int) {
    drawCircle(Color.White, r, c)
    val look = polar(index * 1.9f, r * 0.22f)
    drawCircle(Color(0xFF84CC16), r * 0.5f, c + look)
    drawCircle(Color(0xFF111827), r * 0.24f, c + look)
    drawCircle(Color.White, r * 0.1f, c + look + Offset(-r * 0.12f, -r * 0.12f))
    drawCircle(MonsterDark, r, c, style = Stroke(r * 0.14f))
}

// ---------------------------------------------------------------- Ice cream

/** Light and shaded color of each scoop: strawberry, mint, vanilla, chocolate. */
private val Scoops = listOf(
    Color(0xFFFBC4DE) to Color(0xFFF07AAE),
    Color(0xFFB8F2D8) to Color(0xFF4CC99A),
    Color(0xFFFFF8E1) to Color(0xFFF0D68E),
    Color(0xFFC08A60) to Color(0xFF7C4A2D),
)
private val Sprinkles = listOf(Color(0xFFF43F5E), Color(0xFFFACC15), Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFA78BFA), Color.White)
private val Waffle = listOf(Color(0xFFF6CD8C), Color(0xFFD99A4E))

private fun DrawScope.drawWaffle(area: Path, top: Float, bottom: Float, left: Float, right: Float, cell: Float) {
    drawPath(area, Brush.verticalGradient(Waffle, top, bottom))
    clipPath(area) {
        val h = bottom - top
        var x = left - h
        while (x < right + h) {
            drawLine(Color(0xFFB7793A).copy(alpha = 0.7f), Offset(x, top), Offset(x + h, bottom), cell * 0.12f)
            drawLine(Color(0xFFB7793A).copy(alpha = 0.7f), Offset(x, top), Offset(x - h, bottom), cell * 0.12f)
            x += cell
        }
    }
}

internal fun DrawScope.drawIceCreamCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    val cx = core.center.x
    // Waffle cone at the bottom, scoops stacked on top of it
    val coneTop = core.bottom - cw * if (core.height > cw * 3.2f) 1.0f else 0.55f
    drawWaffle(Path().apply { addRect(Rect(core.left, coneTop, core.right, core.bottom)) }, coneTop, core.bottom, core.left, core.right, cw * 0.2f)
    drawRect(Color(0xFF8A5523).copy(alpha = 0.35f), Offset(core.left, coneTop), Size(cw, cw * 0.06f))
    val n = (((coneTop - core.top) / (cw * 1.2f)).roundToInt()).coerceIn(1, 4)
    val h = (coneTop - core.top) / n
    // From the bottom up, so each scoop's melting rim overlaps the one below
    for (i in n - 1 downTo 0) {
        val (light, dark) = Scoops[i % Scoops.size]
        val top = core.top + i * h
        val bottom = top + h
        val scoop = Path().apply {
            moveTo(core.left, top - cw * 0.3f)
            lineTo(core.right, top - cw * 0.3f)
            lineTo(core.right, bottom)
            for (k in 3 downTo 0) {
                val x0 = core.left + cw * (k + 1) / 4f
                val x1 = core.left + cw * k / 4f
                quadraticTo((x0 + x1) / 2, bottom + cw * 0.2f, x1, bottom)
            }
            close()
        }
        drawPath(scoop, Brush.verticalGradient(listOf(light, light, dark), top, bottom + cw * 0.1f))
        drawPath(scoop, dark.shade(0.75f).copy(alpha = 0.5f), style = Stroke(cw * 0.025f))
        // A drip running down, slowly getting longer and shorter
        val dx = core.left + cw * (0.28f + 0.44f * ((i * 0.61f) % 1f))
        val len = cw * (0.16f + 0.05f * alive * wave(idle, 1, i * 0.3f))
        drawRoundRect(dark, Offset(dx - cw * 0.045f, bottom), Size(cw * 0.09f, len), CornerRadius(cw * 0.045f))
        drawCircle(dark, cw * 0.06f, Offset(dx, bottom + len - cw * 0.02f))
        drawOval(Color.White.copy(alpha = 0.35f), Offset(core.left + cw * 0.12f, top + cw * 0.1f), Size(cw * 0.16f, minOf(h * 0.5f, cw * 0.5f)))
    }
    // Sprinkles on the top scoop, away from the face
    val rnd = Random(5)
    repeat(14) { i ->
        val p = Offset(core.left + cw * (0.1f + rnd.nextFloat() * 0.8f), core.top + rnd.nextFloat() * minOf(h, cw * 1.3f))
        val a = rnd.nextFloat() * Tau
        val nearFace = abs(p.x - cx) < cw * 0.36f && p.y > core.top + cw * 0.36f && p.y < core.top + cw * 0.86f
        if (!nearFace) {
            drawLine(Sprinkles[i % Sprinkles.size], p, p + polar(a, cw * 0.07f), cw * 0.035f, StrokeCap.Round)
        }
    }
    // Happy little face on the top scoop
    val eyeY = core.top + cw * 0.56f
    val lid = blink(idle, 0.46f) * alive
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(cx + dir * cw * 0.17f, eyeY)
        val eye = Rect(e.x - cw * 0.055f, e.y - cw * 0.075f, e.x + cw * 0.055f, e.y + cw * 0.075f)
        drawOval(Color(0xFF3B1A1A), eye.topLeft, eye.size)
        drawCircle(Color.White, cw * 0.025f, e + Offset(-cw * 0.018f, -cw * 0.03f))
        drawCircle(Color.White.copy(alpha = 0.7f), cw * 0.012f, e + Offset(cw * 0.02f, cw * 0.03f))
        drawLid(eye, lid, Scoops[0].first, Color(0xFF3B1A1A), cw * 0.02f)
        drawOval(Color(0x80FF6FA3), Offset(e.x + dir * cw * 0.07f - cw * 0.07f, eyeY + cw * 0.09f), Size(cw * 0.14f, cw * 0.07f))
    }
    val m = Offset(cx, eyeY + cw * 0.11f)
    val mouth = Path().apply {
        moveTo(m.x - cw * 0.07f, m.y)
        quadraticTo(m.x, m.y + cw * 0.01f, m.x + cw * 0.07f, m.y)
        quadraticTo(m.x, m.y + cw * 0.14f, m.x - cw * 0.07f, m.y)
        close()
    }
    drawPath(mouth, Color(0xFF7A1F3D))
    drawPath(mouth, Color(0xFF3B1A1A), style = Stroke(cw * 0.018f, join = StrokeJoin.Round))
}

private fun DrawScope.drawCherry(c: Offset, r: Float, idle: Float, alive: Float) {
    rotate(6f * alive * wave(idle, 2), pivot = c) {
        drawPath(
            Path().apply {
                moveTo(c.x, c.y - r * 0.6f)
                quadraticTo(c.x + r * 0.1f, c.y - r * 1.4f, c.x + r * 0.8f, c.y - r * 1.75f)
            },
            Color(0xFF4D7C0F), style = Stroke(r * 0.18f, cap = StrokeCap.Round),
        )
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFF8A8A), Color(0xFFDC2626), Color(0xFF7F1D1D)), center = c - Offset(r * 0.3f, r * 0.3f), radius = r * 1.4f), r, c)
        drawCircle(Color(0xFF7F1D1D), r, c, style = Stroke(r * 0.1f))
        drawOval(Color.White.copy(alpha = 0.75f), Offset(c.x - r * 0.55f, c.y - r * 0.55f), Size(r * 0.4f, r * 0.28f))
    }
}

internal fun DrawScope.drawIceCreamBehind(w: Float, h: Float, idle: Float, alive: Float) {
    drawCherry(Offset(w / 2, w * 0.02f), w * 0.13f, idle, alive)
}

internal fun DrawScope.drawIceCreamRoundBehind(c: Offset, r: Float, idle: Float, alive: Float) {
    val cone = Path().apply {
        moveTo(c.x - r * 0.78f, c.y + r * 0.5f)
        lineTo(c.x + r * 0.78f, c.y + r * 0.5f)
        quadraticTo(c.x + r * 0.1f, c.y + r * 1.4f, c.x, c.y + r * 1.5f)
        quadraticTo(c.x - r * 0.1f, c.y + r * 1.4f, c.x - r * 0.78f, c.y + r * 0.5f)
        close()
    }
    drawWaffle(cone, c.y + r * 0.5f, c.y + r * 1.5f, c.x - r, c.x + r, r * 0.26f)
    drawPath(cone, Color(0xFF8A5523), style = Stroke(r * 0.04f, join = StrokeJoin.Round))
    drawCherry(c + Offset(0f, -r * 0.98f), r * 0.24f, idle, alive)
}

/** Strawberry on a cream plate. */
internal fun DrawScope.drawStrawberryEmblem(c: Offset, cw: Float, idle: Float, alive: Float) {
    drawEmblemPlate(c, cw * 0.32f, Color(0xFFFFF8E1), Color(0xFFF0D68E))
    val s = cw * 0.2f
    val b = c + Offset(0f, s * 0.15f)
    rotate(5f * alive * wave(idle, 2), pivot = b) {
        val berry = Path().apply {
            moveTo(b.x, b.y + s)
            cubicTo(b.x - s * 1.15f, b.y + s * 0.2f, b.x - s * 0.95f, b.y - s * 0.8f, b.x, b.y - s * 0.62f)
            cubicTo(b.x + s * 0.95f, b.y - s * 0.8f, b.x + s * 1.15f, b.y + s * 0.2f, b.x, b.y + s)
            close()
        }
        drawPath(berry, Brush.verticalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFDC2626)), b.y - s, b.y + s))
        drawPath(berry, Color(0xFF991B1B), style = Stroke(s * 0.08f, join = StrokeJoin.Round))
        for ((dx, dy) in listOf(-0.4f to -0.2f, 0f to -0.25f, 0.4f to -0.2f, -0.25f to 0.15f, 0.25f to 0.15f, 0f to 0.5f)) {
            drawOval(Color(0xFFFDE68A), Offset(b.x + s * dx - s * 0.05f, b.y + s * dy - s * 0.07f), Size(s * 0.1f, s * 0.14f))
        }
        for (a in listOf(-60f, -20f, 20f, 60f)) {
            rotate(a, pivot = b + Offset(0f, -s * 0.62f)) {
                drawOval(Color(0xFF22C55E), Offset(b.x - s * 0.12f, b.y - s * 1.12f), Size(s * 0.24f, s * 0.5f))
            }
        }
    }
}

// ---------------------------------------------------------------- Shared lookups for the character skins

/** Capture symbol for the character skins: hearts, fish, batteries, drops, eyeballs and cones. */
internal fun DrawScope.drawCharacterToken(skin: Skin, c: Offset, r: Float, index: Int, filled: Boolean) {
    val empty = Color.Black.copy(alpha = 0.32f)
    val outline = Stroke(r * 0.18f, join = StrokeJoin.Round)
    when (skin) {
        Skin.UNICORN -> {
            val heart = heartPath(c + Offset(0f, r * 0.05f), r * 0.95f)
            if (filled) drawGlossyHeart(c + Offset(0f, r * 0.05f), r * 0.95f, UnicornMane[index % UnicornMane.size].shade(0.95f))
            else drawPath(heart, empty, style = outline)
        }
        Skin.SHARK -> if (filled) {
            drawFish(c - Offset(r * 0.12f, 0f), r * 0.85f, Color(0xFFFFC93C))
        } else {
            drawOval(empty, Offset(c.x - r * 0.8f, c.y - r * 0.5f), Size(r * 1.6f, r), style = outline)
        }
        Skin.ROBOT -> {
            val body = Rect(c.x - r * 0.95f, c.y - r * 0.55f, c.x + r * 0.75f, c.y + r * 0.55f)
            drawRoundRect(if (filled) Color(0xFF334155) else empty, Offset(body.right, c.y - r * 0.22f), Size(r * 0.2f, r * 0.44f), CornerRadius(r * 0.06f))
            if (filled) {
                drawRoundRect(Color(0xFF1E293B), body.topLeft, body.size, CornerRadius(r * 0.18f))
                val inner = body.deflate(r * 0.14f)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFBBF7D0), Color(0xFF22C55E)), inner.top, inner.bottom), inner.topLeft, inner.size, CornerRadius(r * 0.08f))
                drawPath(lightningPath(c - Offset(r * 0.1f, 0f), r * 0.35f), Color.White)
            } else {
                drawRoundRect(empty, body.topLeft, body.size, CornerRadius(r * 0.18f), style = outline)
            }
        }
        Skin.FIRE_TRUCK -> if (filled) drawWaterDrop(c + Offset(0f, r * 0.05f), r * 0.75f)
        else drawPath(dropPath(c + Offset(0f, r * 0.05f), r * 0.75f), empty, style = outline)
        Skin.MONSTER -> if (filled) drawEyeball(c, r * 0.9f, index) else drawCircle(empty, r * 0.9f, c, style = outline)
        Skin.ICE_CREAM -> {
            val cone = Path().apply {
                moveTo(c.x - r * 0.6f, c.y)
                lineTo(c.x + r * 0.6f, c.y)
                lineTo(c.x, c.y + r * 1.15f)
                close()
            }
            if (filled) {
                drawPath(cone, Color(0xFFE0A458))
                drawPath(cone, Color(0xFF8A5523), style = Stroke(r * 0.12f, join = StrokeJoin.Round))
                val (light, dark) = Scoops[index % 3]
                val scoop = c - Offset(0f, r * 0.15f)
                drawCircle(Brush.verticalGradient(listOf(light, dark), scoop.y - r * 0.65f, scoop.y + r * 0.6f), r * 0.62f, scoop)
                drawCircle(dark.shade(0.7f), r * 0.62f, scoop, style = Stroke(r * 0.12f))
            } else {
                drawPath(cone, empty, style = outline)
                drawCircle(empty, r * 0.62f, c - Offset(0f, r * 0.15f), style = outline)
            }
        }
        else -> Unit
    }
}

/** Marker riding the fill level next to the core. */
internal fun DrawScope.drawCharacterMarker(skin: Skin, c: Offset, r: Float) {
    when (skin) {
        Skin.UNICORN -> drawGlossyHeart(c, r * 1.05f, Color(0xFFF472B6))
        Skin.SHARK -> drawBubble(c, r * 0.9f)
        Skin.ROBOT -> {
            val bolt = lightningPath(c, r * 1.05f)
            drawPath(bolt, Color(0xFFFDE047))
            drawPath(bolt, Color(0xFFA16207), style = Stroke(r * 0.18f, join = StrokeJoin.Round))
        }
        Skin.FIRE_TRUCK -> drawWaterDrop(c, r * 0.9f)
        Skin.MONSTER -> drawEyeball(c, r * 0.95f, 1)
        Skin.ICE_CREAM -> drawCherry(c, r * 0.8f, 0f, 0f)
        else -> Unit
    }
}

/** Edge height of the fill for the character skins, as in edgeOffset. */
internal fun characterEdge(skin: Skin, u: Float, idle: Float): Float = when (skin) {
    // Glittery ripple
    Skin.UNICORN -> 0.45f * sin(Tau * (u * 3f + idle)) + 0.2f * sin(Tau * (u * 7f - idle * 2))
    // Choppy sea with pointed crests
    Skin.SHARK -> 0.5f - 1.2f * (1f - abs(sin(PI.toFloat() * (u * 3f + idle))))
    // Charging bars
    Skin.ROBOT -> if (floor(u * 6f + idle * 2f).toInt() % 2 == 0) -0.45f else 0.45f
    // Water splashes of different heights
    Skin.FIRE_TRUCK -> {
        val f = u * 5f
        val k = floor(f)
        val x = (f - k - 0.5f) / 0.14f
        0.35f - 1.5f * exp(-x * x) * (0.8f + 0.3f * sin(k * 1.7f + Tau * idle * 2))
    }
    // Lumpy, wobbling goo
    Skin.MONSTER -> 0.5f * sin(Tau * (u * 2.3f + idle)) + 0.35f * sin(Tau * (u * 5.1f - idle * 2) + 1f)
    // Melting scoop rims hanging down
    Skin.ICE_CREAM -> {
        val f = u * 4f
        val x = 2f * (f - floor(f)) - 1f
        -0.55f + sqrt(1f - x * x)
    }
    else -> 0f
}

/** Full-screen overlay backgrounds for the character skins. */
internal fun DrawScope.drawCharacterBackdrop(skin: Skin, rnd: Random, unit: Float) {
    fun spot() = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
    when (skin) {
        Skin.UNICORN -> repeat(40) { i ->
            val c = spot()
            if (i % 3 == 0) drawPath(heartPath(c, unit * 1.6f), UnicornMane[i % UnicornMane.size].copy(alpha = 0.18f))
            else drawSparkle(c, unit * (0.6f + rnd.nextFloat()), Color.White.copy(alpha = 0.35f))
        }
        Skin.SHARK -> repeat(40) {
            drawCircle(Color.White.copy(alpha = 0.12f + rnd.nextFloat() * 0.12f), unit * (0.5f + rnd.nextFloat() * 1.5f), spot(), style = Stroke(unit * 0.3f))
        }
        Skin.ROBOT -> repeat(18) {
            val a = spot()
            val b = a + Offset(unit * (6f + rnd.nextFloat() * 10f), 0f)
            val d = b + Offset(0f, unit * (rnd.nextFloat() - 0.5f) * 16f)
            val trace = Color(0x2622D3EE)
            drawLine(trace, a, b, unit * 0.5f)
            drawLine(trace, b, d, unit * 0.5f)
            drawCircle(trace, unit * 0.9f, a)
            drawCircle(trace, unit * 0.9f, d)
        }
        Skin.FIRE_TRUCK -> repeat(40) {
            drawCircle(Color(0xFFFDBA74).copy(alpha = 0.1f + rnd.nextFloat() * 0.25f), unit * (0.2f + rnd.nextFloat() * 0.5f), spot())
        }
        Skin.MONSTER -> repeat(26) {
            drawCircle(Color(0x1A93C5FD), unit * (1.5f + rnd.nextFloat() * 2.5f), spot())
        }
        Skin.ICE_CREAM -> repeat(60) { i ->
            val c = spot()
            drawLine(Sprinkles[i % Sprinkles.size].copy(alpha = 0.25f), c, c + polar(rnd.nextFloat() * Tau, unit * 2f), unit * 0.8f, StrokeCap.Round)
        }
        else -> Unit
    }
}

/** Reward particle for the character skins, or false to use the default sparkle. */
internal fun DrawScope.drawCharacterReward(skin: Skin, p: Offset, s: Float, i: Int): Boolean {
    when (skin) {
        Skin.UNICORN -> drawPath(heartPath(p, s * 0.8f), UnicornMane[i % UnicornMane.size])
        Skin.SHARK -> drawBubble(p, s * 0.6f)
        Skin.ICE_CREAM -> drawLine(Sprinkles[i % Sprinkles.size], p - Offset(s * 0.5f, s * 0.3f), p + Offset(s * 0.5f, s * 0.3f), s * 0.35f, StrokeCap.Round)
        else -> return false
    }
    return true
}
