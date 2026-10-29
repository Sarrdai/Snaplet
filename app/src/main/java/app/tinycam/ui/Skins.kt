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
import androidx.compose.ui.graphics.drawscope.translate
import app.tinycam.data.Skin
import app.tinycam.data.Tokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
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
    Skin.UNICORN -> SkinStyle(
        track = Color(0xFF5B1A63),
        fill = listOf(Color(0xFFFF9EC7), Color(0xFFFFC48C), Color(0xFFFFE97A), Color(0xFFA8F0C6), Color(0xFF9AD8FF), Color(0xFFC9A8FF)),
        button = Color(0xFFC026D3), buttonInk = Color.White,
        buttonOn = Color.White, buttonOnInk = Color(0xFFA21CAF),
        holdFill = Color(0xCCF5D0FE), frame = Color(0xFFF5D0FE),
        backdrop = listOf(Color(0xFF86198F), Color(0xFF3B0A40)),
        accent = Color(0xFFF0ABFC), shadow = Color(0xFF701A75),
    )
    Skin.SHARK -> SkinStyle(
        track = Color(0xFF0B3B4A),
        fill = listOf(Color(0xFF0369A1), Color(0xFF0EA5E9), Color(0xFF7DD3FC)),
        button = Color(0xFF0F4C5C), buttonInk = Color(0xFFCCFBF1),
        buttonOn = Color(0xFF5EEAD4), buttonOnInk = Color(0xFF083344),
        holdFill = Color(0xCC2DD4BF), frame = Color(0xFF99F6E4),
        backdrop = listOf(Color(0xFF0E5A6E), Color(0xFF052530)),
        accent = Color(0xFF5EEAD4), shadow = Color(0xFF042F2E),
    )
    Skin.ROBOT -> SkinStyle(
        track = Color(0xFF1E293B),
        fill = listOf(Color(0xFF16A34A), Color(0xFF4ADE80), Color(0xFFBBF7D0)),
        button = Color(0xFF334155), buttonInk = Color(0xFFA5F3FC),
        buttonOn = Color(0xFF22D3EE), buttonOnInk = Color(0xFF083344),
        holdFill = Color(0xCC22D3EE), frame = Color(0xFFA5F3FC),
        backdrop = listOf(Color(0xFF1E293B), Color(0xFF0B1120)),
        accent = Color(0xFF22D3EE), shadow = Color(0xFF0B1120),
    )
    Skin.FIRE_TRUCK -> SkinStyle(
        track = Color(0xFF450A0A),
        fill = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6), Color(0xFF93C5FD)),
        button = Color(0xFF991B1B), buttonInk = Color(0xFFFEE2E2),
        buttonOn = Color(0xFFFACC15), buttonOnInk = Color(0xFF450A0A),
        holdFill = Color(0xCCFACC15), frame = Color(0xFFFDE68A),
        backdrop = listOf(Color(0xFF7F1D1D), Color(0xFF2A0A0A)),
        accent = Color(0xFFFACC15), shadow = Color(0xFF2A0A0A),
    )
    Skin.MONSTER -> SkinStyle(
        track = Color(0xFF172554),
        fill = listOf(Color(0xFF4D7C0F), Color(0xFF84CC16), Color(0xFFD9F99D)),
        button = Color(0xFF1E3A8A), buttonInk = Color(0xFFDBEAFE),
        buttonOn = Color(0xFFA3E635), buttonOnInk = Color(0xFF1A2E05),
        holdFill = Color(0xCCA3E635), frame = Color(0xFFBEF264),
        backdrop = listOf(Color(0xFF1E1B4B), Color(0xFF0B0A1F)),
        accent = Color(0xFFA3E635), shadow = Color(0xFF0B0A1F),
    )
    Skin.ICE_CREAM -> SkinStyle(
        track = Color(0xFF4A2511),
        fill = listOf(Color(0xFFEC4899), Color(0xFFF9A8D4), Color(0xFFFFF1F2)),
        button = Color(0xFF7C4A2D), buttonInk = Color(0xFFFFE4E6),
        buttonOn = Color(0xFFF9A8D4), buttonOnInk = Color(0xFF4A2511),
        holdFill = Color(0xCCF9A8D4), frame = Color(0xFFFBCFE8),
        backdrop = listOf(Color(0xFF5C3420), Color(0xFF21110A)),
        accent = Color(0xFFF9A8D4), shadow = Color(0xFF2B1408),
    )
}

enum class Emblem { SKIN, RECORD, STOP }

/** Length of one idle loop. Every idle motion repeats a whole number of times per loop, so it wraps seamlessly. */
const val ShutterIdleMs = 4800

/** Length of the sparkle burst when the button becomes ready. */
const val ShutterRewardMs = 750

/**
 * Draws the pill-shaped shutter for [skin]. Decorations such as fins or spikes may extend
 * outside the bounds, so the caller must leave room around the pill.
 *
 * All motion comes in as phases so a fixed set of values always draws the same picture:
 *
 * @param progress cooldown or recording progress, 0..1; 1 means full.
 * @param inactive 0 = ready, 1 = not ready (desaturated and shrunk core, no idle motion).
 * @param spikesLeft side on which outward decorations (dino spikes) are drawn.
 * @param tokens remaining daily captures as symbols below the emblem, or null when unlimited.
 * @param idle position in the idle loop, 0..1 (blinking, breathing, twinkling); 0 is the resting pose.
 * @param press 0 = released, 1 = pressed down (squashed); negative values stretch, as on a spring back.
 * @param reward progress of the burst when the button becomes ready, 0..1; 0 and 1 draw nothing.
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
    idle: Float = 0f,
    press: Float = 0f,
    reward: Float = 0f,
) {
    val w = size.width
    val h = size.height
    val alive = 1f - inactive.coerceIn(0f, 1f)
    val pill = pillPath(Rect(0f, 0f, w, h))
    val inset = w * 0.11f
    val core = Rect(inset, inset, w - inset, h - inset)
    val cw = core.width

    // Squash towards the bottom while pressed, stretch while springing back
    scale(1f + 0.07f * press, 1f - 0.08f * press, pivot = Offset(w / 2, h)) {
        // Behind the pill
        when (skin) {
            Skin.SPACE -> {
                drawFins(w, h)
                drawExhaust(w, h, progress, alive * wave(idle, 12))
            }
            Skin.DINO -> drawSpikes(w, h, spikesLeft, idle, alive)
            Skin.LEOPARD -> for (dir in listOf(-1f, 1f)) {
                drawEar(Offset(w / 2 + dir * w * 0.33f, w * 0.13f), w * 0.17f, dir, earTwitch(idle, dir) * alive)
            }
            Skin.UNICORN -> drawUnicornBehind(w, h, spikesLeft, idle, alive)
            Skin.SHARK -> drawSharkBehind(w, h, spikesLeft, idle, alive)
            Skin.ROBOT -> drawRobotBehind(w, h, idle, alive)
            Skin.FIRE_TRUCK -> drawFireTruckBehind(w, h, idle, alive)
            Skin.MONSTER -> drawMonsterBehind(w, h, idle, alive)
            Skin.ICE_CREAM -> drawIceCreamBehind(w, h, idle, alive)
            else -> Unit
        }

        drawTrack(pill, style, w)
        val fillColors = if (emblem == Emblem.SKIN) style.fill else RecordFill
        drawFill(skin, pill, fillColors, progress, 0f, w, 0f, h, idle * alive)
        drawPath(pill, Color.White.copy(alpha = 0.35f), style = Stroke(w * 0.015f))

        // Core: the actual button face, slightly sunk while pressed
        val corePath = pillPath(core)
        val breath = 1f + 0.012f * alive * wave(idle, 2)
        translate(top = h * 0.01f * press.coerceAtLeast(0f)) {
            scale(coreScale * breath, pivot = core.center) {
                withDesaturation(inactive) {
                    clipPath(corePath) {
                        drawSkinCore(skin, core, idle, alive)
                        drawCoreShade(core, skin)
                        if (!metallic(skin)) drawGloss(core)
                    }
                    val center = emblemCenter(skin, core, roomBelow = tokens != null)
                    // On short pills (landscape) the capture symbols matter more than the decorative emblem
                    val tight = tokens != null && emblem == Emblem.SKIN && core.height < cw * 3.6f
                    if (tokens != null) {
                        val top = if (tight) core.top + cw * decorBottom(skin) else center.y + cw * 0.4f
                        drawTokens(skin, Rect(core.left + cw * 0.1f, top, core.right - cw * 0.1f, core.bottom - cw * 0.22f), tokens)
                    }
                    when (emblem) {
                        Emblem.SKIN -> if (!tight) drawSkinEmblem(skin, center, cw, idle, alive)
                        Emblem.RECORD -> drawRecordEmblem(center, cw, recording = false)
                        Emblem.STOP -> drawRecordEmblem(center, cw, recording = true)
                    }
                    drawPath(corePath, Color.Black.copy(alpha = 0.16f), style = Stroke(w * 0.02f))
                    if (press > 0f) drawPath(corePath, Color.Black.copy(alpha = 0.1f * press))
                }
            }
        }
        val level = h * (1f - progress.coerceIn(0f, 1f))
        drawLevelMarkers(skin, progress, level, inset / 2, w - inset / 2, w * 0.1f, idle * alive)
    }

    drawReward(skin, style, Offset(w / 2, w / 2), Offset(w / 2, h - w / 2), w / 2, reward)
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
        Skin.UNICORN -> 1.38f
        Skin.SHARK -> 1.42f
        Skin.ROBOT -> 1.55f
        Skin.FIRE_TRUCK -> 1.6f
        Skin.MONSTER -> 1.45f
        Skin.ICE_CREAM -> 1.5f
    }
    val maxY = maxOf(minY, core.bottom - cw * if (roomBelow) 1.25f else 0.9f)
    return Offset(core.center.x, core.center.y.coerceIn(minY, maxY))
}

private fun DrawScope.drawSkinCore(skin: Skin, core: Rect, idle: Float, alive: Float) = when (skin) {
    Skin.CLASSIC -> drawClassicCore(core)
    Skin.RAINBOW -> drawRainbowCore(core, idle, alive)
    Skin.SPACE -> drawRocketCore(core, idle, alive)
    Skin.DINO -> drawDinoCore(core, idle, alive)
    Skin.LEOPARD -> drawLeopardCore(core, idle, alive)
    Skin.UNICORN -> drawUnicornCore(core, idle, alive)
    Skin.SHARK -> drawSharkCore(core, idle, alive)
    Skin.ROBOT -> drawRobotCore(core, idle, alive)
    Skin.FIRE_TRUCK -> drawFireTruckCore(core, idle, alive)
    Skin.MONSTER -> drawMonsterCore(core, idle, alive)
    Skin.ICE_CREAM -> drawIceCreamCore(core, idle, alive)
}

/**
 * Round shutter for the centered layout. Uses the same skin art as the pill: the pill core is
 * drawn into a virtual rectangle shifted so that the face, rainbow or nose cone lands in the
 * circle. Remaining capture symbols sit in a row above the button. Phases as in [drawShutter].
 */
fun DrawScope.drawRoundShutter(
    skin: Skin,
    style: SkinStyle,
    progress: Float,
    inactive: Float,
    coreScale: Float,
    emblem: Emblem,
    tokens: Tokens? = null,
    idle: Float = 0f,
    press: Float = 0f,
    reward: Float = 0f,
) {
    val d = size.minDimension
    val c = center
    val r = d / 2
    val alive = 1f - inactive.coerceIn(0f, 1f)
    val ring = Path().apply { addOval(Rect(c, r)) }

    scale(1f + 0.07f * press, 1f - 0.08f * press, pivot = Offset(c.x, c.y + r)) {
        when (skin) {
            Skin.SPACE -> {
                drawFins(d, d)
                drawExhaust(d, d, progress, alive * wave(idle, 12))
            }
            Skin.DINO -> drawRoundPlates(c, r, idle, alive)
            Skin.LEOPARD -> for (dir in listOf(-1f, 1f)) {
                drawEar(c + Offset(dir * r * 0.62f, -r * 0.78f), r * 0.3f, dir, earTwitch(idle, dir) * alive)
            }
            Skin.UNICORN -> drawUnicornRoundBehind(c, r, idle, alive)
            Skin.SHARK -> drawSharkRoundBehind(c, r, idle, alive)
            Skin.ROBOT -> drawRobotRoundBehind(c, r, idle, alive)
            Skin.FIRE_TRUCK -> drawFireTruckRoundBehind(c, r, idle, alive)
            Skin.MONSTER -> drawMonsterRoundBehind(c, r, idle, alive)
            Skin.ICE_CREAM -> drawIceCreamRoundBehind(c, r, idle, alive)
            else -> Unit
        }

        drawTrack(ring, style, d * 0.86f)
        val fillColors = if (emblem == Emblem.SKIN) style.fill else RecordFill
        drawFill(skin, ring, fillColors, progress, c.x - r, c.x + r, c.y - r, c.y + r, idle * alive)
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
            Skin.UNICORN -> 0.2f
            Skin.SHARK -> 0.16f
            Skin.ROBOT -> 0.08f
            Skin.FIRE_TRUCK -> 0.12f
            Skin.MONSTER -> 0.16f
            Skin.ICE_CREAM -> 0.1f
        }
        val art = Rect(core.left, core.top - cw * shift, core.right, core.bottom + cw * 0.4f)
        val breath = 1f + 0.012f * alive * wave(idle, 2)
        translate(top = d * 0.012f * press.coerceAtLeast(0f)) {
            scale(coreScale * breath, pivot = c) {
                withDesaturation(inactive) {
                    clipPath(corePath) {
                        drawSkinCore(skin, art, idle, alive)
                        if (skin == Skin.LEOPARD) {
                            // Low on the sides only, the rest belongs to the face
                            val rnd = Random(4)
                            for (a in listOf(18f, 162f)) {
                                val rad = Math.toRadians(a.toDouble()).toFloat()
                                drawRosette(c + polar(rad, coreR * 0.86f), cw * 0.11f, cw, rnd)
                            }
                        }
                        drawCoreShade(Rect(core.left, core.top - cw * 0.1f, core.right, core.bottom), skin)
                        if (!metallic(skin)) drawGloss(Rect(core.left, core.top - cw * 0.3f, core.right, core.bottom + cw * 0.3f))
                    }
                    when (emblem) {
                        Emblem.SKIN -> when (skin) {
                            Skin.CLASSIC -> drawSkinEmblem(skin, c, cw * 0.95f, idle, alive)
                            Skin.RAINBOW -> drawSkinEmblem(skin, c + Offset(0f, cw * 0.2f), cw * 0.62f, idle, alive)
                            Skin.SPACE -> drawSkinEmblem(skin, c + Offset(0f, cw * 0.05f), cw * 0.7f, idle, alive)
                            Skin.DINO -> drawSkinEmblem(skin, c + Offset(0f, cw * 0.26f), cw * 0.5f, idle, alive)
                            // The face fills the circle
                            else -> Unit
                        }
                        Emblem.RECORD -> drawRecordEmblem(c + Offset(0f, cw * 0.12f), cw * 0.7f, recording = false)
                        Emblem.STOP -> drawRecordEmblem(c + Offset(0f, cw * 0.12f), cw * 0.7f, recording = true)
                    }
                    drawPath(corePath, Color.Black.copy(alpha = 0.16f), style = Stroke(d * 0.015f))
                    if (press > 0f) drawPath(corePath, Color.Black.copy(alpha = 0.1f * press))
                }
            }
        }
        // Markers sit on the middle of the ring where the level crosses it
        val dy = r - d * progress.coerceIn(0f, 1f)
        val mid = r * 0.9f
        if (abs(dy) < mid * 0.92f) {
            val dx = sqrt(mid * mid - dy * dy)
            drawLevelMarkers(skin, progress, c.y + dy, c.x - dx, c.x + dx, d * 0.08f, idle * alive)
        }
    }

    if (tokens != null) {
        // Row above the button, clear of plates and ears
        val rowW = d * 1.5f
        val cell = minOf(rowW / tokens.total, d * 0.2f)
        val y = -d * 0.32f
        drawTokens(skin, Rect(c.x - cell * tokens.total / 2f, y - cell / 2f, c.x + cell * tokens.total / 2f, y + cell / 2f), tokens)
    }
    drawReward(skin, style, c, c, r, reward)
}

// ---------------------------------------------------------------- Shared parts

internal const val Tau = (2 * PI).toFloat()

/** Sine over the idle loop, [times] full periods per loop. */
internal fun wave(idle: Float, times: Int = 1, offset: Float = 0f) = sin(Tau * (idle * times + offset))

/** Triangle 0..1..0 lasting [length] of the idle loop, starting at [at]. */
internal fun pulse(idle: Float, at: Float, length: Float): Float {
    val t = (idle - at) / length
    return if (t in 0f..1f) 1f - abs(2f * t - 1f) else 0f
}

/** Eyelid closure 0..1: a quick double blink starting at [at]. */
internal fun blink(idle: Float, at: Float) = maxOf(pulse(idle, at, 0.045f), pulse(idle, at + 0.07f, 0.045f))

private fun earTwitch(idle: Float, dir: Float) = if (dir < 0) pulse(idle, 0.3f, 0.06f) else pulse(idle, 0.62f, 0.06f)

internal fun polar(angle: Float, r: Float) = Offset(cos(angle) * r, sin(angle) * r)

/** Dark outer edge for a clean silhouette, the track and a recessed inner rim. */
private fun DrawScope.drawTrack(outline: Path, style: SkinStyle, w: Float) {
    drawPath(outline, Color.Black.copy(alpha = 0.3f), style = Stroke(w * 0.035f))
    drawPath(outline, style.track)
    clipPath(outline) {
        drawPath(outline, Color.Black.copy(alpha = 0.22f), style = Stroke(w * 0.07f))
    }
}

private const val EdgeSteps = 72

/**
 * Fill rising from [bottom] to [top], clipped to [outline]. Its edge has a skin-specific shape
 * whose average height is the exact level, so progress stays readable.
 */
private fun DrawScope.drawFill(
    skin: Skin, outline: Path, colors: List<Color>, progress: Float,
    left: Float, right: Float, top: Float, bottom: Float, idle: Float,
) {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0.005f) return
    val brush = Brush.verticalGradient(colors, startY = bottom, endY = top)
    val width = right - left
    clipPath(outline) {
        if (p >= 0.995f) {
            drawRect(brush, Offset(left, top), Size(width, bottom - top))
            return@clipPath
        }
        val level = bottom - (bottom - top) * p
        // Flatten the edge close to empty and full so it never pokes out of the ends
        val amp = width * 0.09f * minOf(1f, p * 10f, (1f - p) * 10f)
        val area = Path().apply { moveTo(left, bottom) }
        val edge = Path()
        for (i in 0..EdgeSteps) {
            val u = i / EdgeSteps.toFloat()
            val x = left + width * u
            val y = level + amp * edgeOffset(skin, u, idle)
            area.lineTo(x, y)
            if (i == 0) edge.moveTo(x, y) else edge.lineTo(x, y)
        }
        area.lineTo(right, bottom)
        area.close()
        drawPath(area, brush)
        drawPath(edge, Color.White.copy(alpha = 0.65f), style = Stroke(width * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Skin-shaped markers riding the fill level on both sides, so the level stays easy to see next to the core. */
private fun DrawScope.drawLevelMarkers(skin: Skin, progress: Float, y: Float, xLeft: Float, xRight: Float, r: Float, idle: Float) {
    if (progress <= 0.005f || progress >= 0.995f) return
    for (i in 0..1) {
        val c = Offset(if (i == 0) xLeft else xRight, y + r * 0.15f * wave(idle, 4, i * 0.5f))
        when (skin) {
            Skin.CLASSIC -> {
                drawCircle(Color(0xFFFFD84D), r, c)
                drawCircle(Color(0xFFD99A00), r, c, style = Stroke(r * 0.2f))
                drawCircle(Color.White.copy(alpha = 0.8f), r * 0.3f, c - Offset(r * 0.3f, r * 0.3f))
            }
            Skin.RAINBOW -> drawCloud(c, r * 0.55f)
            Skin.SPACE -> {
                val star = starPath(c, r * 1.15f, r * 0.52f)
                drawPath(star, Color(0xFFFFE14D))
                drawPath(star, Color(0xFFC2410C), style = Stroke(r * 0.18f, join = StrokeJoin.Round))
            }
            Skin.DINO -> drawPlate(c + Offset(0f, r * 0.8f), r * 1.4f, r * 1.8f, r * 0.2f)
            Skin.LEOPARD -> {
                val drop = Path().apply {
                    moveTo(c.x, c.y - r * 1.25f)
                    quadraticTo(c.x + r * 0.9f, c.y - r * 0.2f, c.x + r * 0.9f, c.y + r * 0.25f)
                    arcTo(Rect(c.x - r * 0.9f, c.y - r * 0.65f, c.x + r * 0.9f, c.y + r * 1.15f), 0f, 180f, false)
                    quadraticTo(c.x - r * 0.9f, c.y - r * 0.2f, c.x, c.y - r * 1.25f)
                    close()
                }
                drawPath(drop, Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B)), c.y - r, c.y + r))
                drawPath(drop, Color(0xFFB45309), style = Stroke(r * 0.18f, join = StrokeJoin.Round))
                drawCircle(Color.White.copy(alpha = 0.8f), r * 0.22f, c + Offset(-r * 0.3f, r * 0.1f))
            }
            else -> drawCharacterMarker(skin, c, r)
        }
    }
}

/** Edge height at [u] (0..1 across) in amplitudes; negative is up. Averages roughly zero. */
private fun edgeOffset(skin: Skin, u: Float, idle: Float): Float = when (skin) {
    // Sloshing liquid
    Skin.CLASSIC -> 0.6f * sin(Tau * (u * 1.25f + idle * 2))
    // Puffy cloud tops
    Skin.RAINBOW -> {
        val f = u * 3.5f + 0.25f
        val x = 2f * (f - floor(f)) - 1f
        0.6f - 1.1f * sqrt(1f - x * x)
    }
    // Flickering flame tongues of different heights
    Skin.SPACE -> {
        val f = u * 4f
        val k = floor(f)
        val tri = 1f - abs(2f * (f - k) - 1f)
        val height = 0.8f + 0.25f * sin(k * 2.3f + Tau * idle * 3)
        0.5f - 1.4f * tri * tri * height
    }
    // Spiky like the back plates
    Skin.DINO -> {
        val f = u * 5f
        0.5f - (1f - abs(2f * (f - floor(f)) - 1f))
    }
    // Honey drips hanging down
    Skin.LEOPARD -> {
        var y = -0.4f
        for ((at, len, half) in LeopardDrips) {
            val x = (u - at) / half
            if (abs(x) < 1f) y += len * (1f + 0.12f * sin(Tau * (idle + at))) * sqrt(1f - x * x)
        }
        y
    }
    else -> characterEdge(skin, u, idle)
}

private val LeopardDrips = listOf(Triple(0.2f, 1.5f, 0.075f), Triple(0.52f, 0.9f, 0.07f), Triple(0.8f, 1.9f, 0.065f))

/** Darker bottom and edges inside [core] so the face reads as a raised dome. */
private fun DrawScope.drawCoreShade(core: Rect, skin: Skin) {
    val cw = core.width
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.16f), 0.18f to Color.Transparent,
            0.8f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.2f),
            startY = core.top, endY = core.bottom,
        ),
        core.topLeft, core.size,
    )
    // Rocket and robot already have their own cylinder shading
    if (metallic(skin)) return
    drawRect(
        Brush.horizontalGradient(
            0f to Color.Black.copy(alpha = 0.12f), 0.2f to Color.Transparent,
            0.78f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f),
            startX = core.left, endX = core.right,
        ),
        core.topLeft, core.size,
    )
    drawCircle(Color.White.copy(alpha = 0.5f), cw * 0.045f, Offset(core.left + cw * 0.3f, core.top + cw * 0.16f))
}

/** Sparkles flying out from a stadium outline between the cap centers [from] and [to]; [t] runs 0..1. */
private fun DrawScope.drawReward(skin: Skin, style: SkinStyle, from: Offset, to: Offset, radius: Float, t: Float) {
    if (t <= 0f || t >= 1f) return
    val out = 1f - (1f - t) * (1f - t)
    val fade = 1f - t
    // Flash ring growing out of the outline
    val grow = radius * 0.3f * out
    drawRoundRect(
        Color.White.copy(alpha = 0.8f * fade), Offset(from.x - radius - grow, from.y - radius - grow),
        Size((radius + grow) * 2, to.y - from.y + (radius + grow) * 2), CornerRadius(radius + grow),
        style = Stroke(radius * 0.12f * fade),
    )
    val n = if (to.y - from.y > radius) 12 else 8
    val size = radius * 0.24f * sin(PI.toFloat() * t)
    for (i in 0 until n) {
        val p = stadiumPoint(from, to, radius * (1.08f + 0.5f * out), i / n.toFloat() + 0.06f * t)
        val s = size * if (i % 2 == 0) 1f else 0.7f
        val color = when (i % 3) {
            0 -> style.accent
            1 -> Color.White
            else -> style.fill.last()
        }
        when (skin) {
            Skin.SPACE, Skin.RAINBOW -> rotate(t * 90f, pivot = p) {
                drawPath(starPath(p, s, s * 0.45f), if (skin == Skin.RAINBOW) RainbowBands[i % RainbowBands.size] else color)
            }
            else -> if (!drawCharacterReward(skin, p, s, i)) drawSparkle(p, s, color)
        }
    }
}

/** Point at fraction [u] around a vertical stadium of [radius] whose caps are centered at [top] and [bottom]. */
internal fun stadiumPoint(top: Offset, bottom: Offset, radius: Float, u: Float): Offset {
    val len = bottom.y - top.y
    val half = PI.toFloat() * radius
    var d = (u - floor(u)) * (2 * len + 2 * half)
    if (d < len) return Offset(top.x + radius, top.y + d)
    d -= len
    if (d < half) return bottom + polar(d / radius, radius)
    d -= half
    if (d < len) return Offset(top.x - radius, bottom.y - d)
    d -= len
    return top + polar(PI.toFloat() + d / radius, radius)
}

private fun DrawScope.drawRoundPlates(c: Offset, r: Float, idle: Float, alive: Float) {
    var i = 0
    for (angle in listOf(-60f, -36f, -12f, 12f, 36f, 60f)) {
        val h = r * if (i % 2 == 0) 0.42f else 0.32f
        val w = r * 0.36f
        val sway = 3f * alive * wave(idle, 2, i * 0.12f)
        rotate(angle + sway, pivot = c) {
            drawPlate(Offset(c.x, c.y - r * 0.9f), w, h, r * 0.04f)
        }
        i++
    }
}

/** One rounded back plate standing on [base], pointing up. */
private fun DrawScope.drawPlate(base: Offset, w: Float, h: Float, line: Float) {
    val brush = Brush.verticalGradient(listOf(Color(0xFFFFD166), Color(0xFFF77F00)), base.y - h, base.y)
    val p = Path().apply {
        moveTo(base.x - w / 2, base.y)
        cubicTo(base.x - w / 2, base.y - h * 0.6f, base.x - w * 0.2f, base.y - h, base.x, base.y - h)
        cubicTo(base.x + w * 0.2f, base.y - h, base.x + w / 2, base.y - h * 0.6f, base.x + w / 2, base.y)
        close()
    }
    drawPath(p, brush)
    // Light ridge along the middle for some depth
    drawLine(Color(0xFFFFF1C2).copy(alpha = 0.7f), Offset(base.x - w * 0.08f, base.y - h * 0.15f), Offset(base.x - w * 0.04f, base.y - h * 0.72f), line * 1.2f, StrokeCap.Round)
    drawPath(p, Color(0xFFB4410C).copy(alpha = 0.7f), style = Stroke(line, join = StrokeJoin.Round))
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
    Skin.RAINBOW, Skin.LEOPARD, Skin.UNICORN -> 1.15f
    Skin.SHARK, Skin.ROBOT -> 1.1f
    Skin.MONSTER, Skin.ICE_CREAM -> 1.2f
    Skin.FIRE_TRUCK -> 1.32f
}

/** Skins with a metal body that brings its own shading, so no gloss or dome shade on top. */
private fun metallic(skin: Skin) = skin == Skin.SPACE || skin == Skin.ROBOT

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
            listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f)),
            startY = top, endY = bottom,
        ),
        topLeft = Offset(left, top), size = Size(cw * 0.1f, bottom - top),
        cornerRadius = CornerRadius(cw * 0.05f),
    )
}

/** Closes an eye from the top: skin-colored lid over [eye] down to [amount] of its height, with a lash line. */
internal fun DrawScope.drawLid(eye: Rect, amount: Float, skinColor: Color, line: Color, stroke: Float) {
    if (amount <= 0.01f) return
    val y = eye.top + eye.height * amount
    clipPath(Path().apply { addOval(eye) }) {
        drawRect(skinColor, eye.topLeft, Size(eye.width, y - eye.top))
    }
    // Half chord of the oval at the lid edge
    val ny = (y - eye.center.y) / (eye.height / 2)
    val half = eye.width / 2 * sqrt((1f - ny * ny).coerceAtLeast(0f))
    drawLine(line, Offset(eye.center.x - half, y), Offset(eye.center.x + half, y), stroke, StrokeCap.Round)
}

// ---------------------------------------------------------------- Classic

private fun DrawScope.drawClassicCore(core: Rect) {
    val cw = core.width
    drawRect(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F3F8), Color(0xFFD9DEE8)), core.top, core.bottom), core.topLeft, core.size)
    // Viewfinder window at the top, like on a real camera
    val vf = Rect(core.center.x - cw * 0.15f, core.top + cw * 0.1f, core.center.x + cw * 0.15f, core.top + cw * 0.2f)
    drawRoundRect(Color(0xFF2A2F3D), vf.topLeft, vf.size, CornerRadius(cw * 0.04f))
    val glass = vf.deflate(cw * 0.018f)
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFF9DB4D8), Color(0xFF3B4A6B)), glass.topLeft, glass.bottomRight),
        glass.topLeft, glass.size, CornerRadius(cw * 0.025f),
    )
    drawLine(Color.White.copy(alpha = 0.7f), glass.topLeft + Offset(cw * 0.03f, glass.height * 0.75f), glass.topLeft + Offset(cw * 0.07f, glass.height * 0.2f), cw * 0.014f, StrokeCap.Round)
}

/** Camera lens: knurled ring, coated glass and an iris that breathes slightly. */
private fun DrawScope.drawLens(c: Offset, cw: Float, idle: Float, alive: Float) {
    val r = cw * 0.3f
    drawCircle(Color.Black.copy(alpha = 0.12f), r * 1.04f, c + Offset(0f, r * 0.08f))
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFF4F6FA), Color(0xFF99A2B4)), c.y - r, c.y + r), r, c)
    for (i in 0 until 24) {
        val a = i * Tau / 24
        drawLine(Color(0xFF6B7488).copy(alpha = 0.55f), c + polar(a, r * 0.85f), c + polar(a, r * 0.96f), r * 0.045f)
    }
    drawCircle(Color(0xFF1F2330), r * 0.8f, c)
    val g = r * 0.66f
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFF7484B8), Color(0xFF2E3466), Color(0xFF0D0F1F)),
            center = c + Offset(-g * 0.25f, -g * 0.25f), radius = g * 1.3f,
        ),
        g, c,
    )
    drawCircle(Color(0xFF05060C), g * (0.42f + 0.05f * alive * wave(idle, 2)), c)
    // Coating reflections
    val arc = Size(g * 1.4f, g * 1.4f)
    drawArc(Color(0xFFA78BFA).copy(alpha = 0.6f), 15f, 65f, false, c - Offset(g * 0.7f, g * 0.7f), arc, style = Stroke(g * 0.11f, cap = StrokeCap.Round))
    drawArc(Color(0xFF6EE7B7).copy(alpha = 0.35f), 100f, 35f, false, c - Offset(g * 0.7f, g * 0.7f), arc, style = Stroke(g * 0.09f, cap = StrokeCap.Round))
    drawCircle(Color.White.copy(alpha = 0.9f), g * 0.17f, c + Offset(-g * 0.38f, -g * 0.38f))
    drawCircle(Color.White.copy(alpha = 0.55f), g * 0.08f, c + Offset(-g * 0.05f, -g * 0.55f))
    // Now and then a glint sweeps across the glass
    val t = (idle - 0.55f) / 0.18f
    if (alive > 0f && t in 0f..1f) {
        clipPath(Path().apply { addOval(Rect(c, g)) }) {
            rotate(30f, pivot = c) {
                drawRect(
                    Color.White.copy(alpha = 0.5f * alive * sin(PI.toFloat() * t)),
                    Offset(c.x - g * 1.4f + g * 2.8f * t, c.y - g * 1.5f), Size(g * 0.3f, g * 3f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Rainbow

private val RainbowBands = listOf(
    Color(0xFFFF5D5D), Color(0xFFFFA23A), Color(0xFFFFE14D), Color(0xFF5FD068), Color(0xFF4CB3FF),
)

private fun DrawScope.drawRainbowCore(core: Rect, idle: Float, alive: Float) {
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
    val outer = cw * 0.40f + band * 0.6f
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), center = arcCenter, radius = outer * 1.25f),
        outer * 1.25f, arcCenter,
    )
    RainbowBands.forEachIndexed { i, c ->
        val r = cw * 0.40f - i * band
        drawArc(
            c, 180f, 180f, false,
            topLeft = Offset(arcCenter.x - r, arcCenter.y - r), size = Size(r * 2, r * 2),
            style = Stroke(band * 1.05f),
        )
    }
    // Soft shine along the top of the bow
    drawArc(
        Color.White.copy(alpha = 0.35f), 200f, 60f, false,
        topLeft = Offset(arcCenter.x - outer + band * 0.35f, arcCenter.y - outer + band * 0.35f),
        size = Size((outer - band * 0.35f) * 2, (outer - band * 0.35f) * 2),
        style = Stroke(band * 0.3f, cap = StrokeCap.Round),
    )
    for (dx in listOf(-1f, 1f)) drawCloud(Offset(arcCenter.x + dx * cw * 0.34f, arcCenter.y + cw * 0.02f), cw * 0.085f)
    // Sparkles twinkling one after another
    val sparkles = listOf(0.22f to 0.62f, 0.78f to 0.70f, 0.30f to 0.86f, 0.74f to 0.92f, 0.2f to 0.4f, 0.82f to 0.45f)
    sparkles.forEachIndexed { i, (fx, fy) ->
        val y = core.top + core.height * fy
        val twinkle = 1f + 0.45f * alive * wave(idle, 2, i * 0.27f)
        if (y > arcCenter.y + cw * 0.2f) {
            drawSparkle(Offset(core.left + cw * fx, y), cw * (if (i % 2 == 0) 0.07f else 0.05f) * twinkle)
        }
    }
}

private fun DrawScope.drawCloud(c: Offset, r: Float) {
    // Lilac underside first, white body slightly above it
    drawCloudShape(c + Offset(0f, r * 0.22f), r, Color(0xFFD8C4FA))
    drawCloudShape(c, r, Color.White)
}

private fun DrawScope.drawCloudShape(c: Offset, r: Float, color: Color) {
    drawCircle(color, r, c)
    drawCircle(color, r * 0.8f, c + Offset(-r * 0.95f, r * 0.25f))
    drawCircle(color, r * 0.8f, c + Offset(r * 0.95f, r * 0.25f))
    drawOval(color, Offset(c.x - r * 1.7f, c.y), Size(r * 3.4f, r * 1.05f))
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

/** Smiling star that sways and blinks. */
private fun DrawScope.drawHappyStar(c: Offset, cw: Float, idle: Float, alive: Float) {
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = c, radius = cw * 0.45f),
        cw * 0.45f, c,
    )
    rotate(7f * alive * wave(idle, 1), pivot = c) {
        val sc = c + Offset(0f, cw * 0.02f)
        val star = starPath(sc, cw * 0.27f, cw * 0.125f)
        drawPath(star, Color(0xFFF59E0B), style = Stroke(cw * 0.08f, join = StrokeJoin.Round))
        drawPath(star, Brush.verticalGradient(listOf(Color(0xFFFFF08A), Color(0xFFFFD02E)), sc.y - cw * 0.27f, sc.y + cw * 0.2f))
        drawPath(star, Color(0xFFFFF08A), style = Stroke(cw * 0.04f, join = StrokeJoin.Round))
        drawSparkle(sc + Offset(-cw * 0.1f, -cw * 0.1f), cw * 0.045f)
        // Face
        val open = 1f - blink(idle, 0.7f) * alive
        for (dir in listOf(-1f, 1f)) {
            val e = sc + Offset(dir * cw * 0.065f, cw * 0.01f)
            val eh = cw * 0.06f * open.coerceAtLeast(0.15f)
            drawOval(Color(0xFF7C2D12), Offset(e.x - cw * 0.022f, e.y - eh / 2), Size(cw * 0.044f, eh))
        }
        drawArc(
            Color(0xFF7C2D12), 20f, 140f, false,
            topLeft = sc + Offset(-cw * 0.055f, cw * 0.01f), size = Size(cw * 0.11f, cw * 0.08f),
            style = Stroke(cw * 0.022f, cap = StrokeCap.Round),
        )
        for (dir in listOf(-1f, 1f)) drawCircle(Color(0x66FF5D8F), cw * 0.025f, sc + Offset(dir * cw * 0.12f, cw * 0.06f))
    }
}

// ---------------------------------------------------------------- Space

private val RocketRed = listOf(Color(0xFF9F1239), Color(0xFFF43F5E), Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF881337))

private fun DrawScope.drawRocketCore(core: Rect, idle: Float, alive: Float) {
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
    // Nose cone with a darker tip, a seam and the belly stripe
    drawRect(red, core.topLeft, Size(cw, cw * 0.82f))
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF4C0519).copy(alpha = 0.55f), Color.Transparent), core.top, core.top + cw * 0.35f),
        core.topLeft, Size(cw, cw * 0.35f),
    )
    drawRect(Color(0xFF334155).copy(alpha = 0.55f), Offset(core.left, core.top + cw * 0.82f), Size(cw, cw * 0.06f))
    drawRect(Color.White.copy(alpha = 0.5f), Offset(core.left, core.top + cw * 0.88f), Size(cw, cw * 0.012f))
    val stripeTop = core.bottom - cw * 0.78f
    drawRect(red, Offset(core.left, stripeTop), Size(cw, cw * 0.18f))
    drawRect(Color.White.copy(alpha = 0.35f), Offset(core.left, stripeTop), Size(cw, cw * 0.02f))
    // Panel seam and rivets
    val rivet = Color(0xFF64748B)
    drawLine(Color(0xFF94A3B8).copy(alpha = 0.6f), Offset(core.left + cw * 0.72f, core.top + cw * 0.9f), Offset(core.left + cw * 0.72f, stripeTop), cw * 0.012f)
    for (i in 1..4) {
        val x = core.left + cw * i / 5f
        drawCircle(rivet, cw * 0.018f, Offset(x, core.top + cw * 0.96f))
        drawCircle(rivet, cw * 0.018f, Offset(x, stripeTop - cw * 0.08f))
        drawCircle(Color.White.copy(alpha = 0.6f), cw * 0.007f, Offset(x - cw * 0.006f, core.top + cw * 0.954f))
    }
    // Highlight on the nose and a beacon that blinks while ready
    drawOval(Color.White.copy(alpha = 0.45f), Offset(core.left + cw * 0.22f, core.top + cw * 0.18f), Size(cw * 0.12f, cw * 0.4f))
    val beacon = Offset(core.center.x + cw * 0.18f, core.top + cw * 0.6f)
    val on = alive * (0.5f + 0.5f * wave(idle, 3))
    if (on > 0.01f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xCCFFF3B0), Color.Transparent), center = beacon, radius = cw * 0.11f), cw * 0.11f * on, beacon)
    }
    drawCircle(Color(0xFFFFE66D).copy(alpha = 0.5f + 0.5f * on), cw * 0.035f, beacon)
    drawCircle(Color(0xFF881337), cw * 0.035f, beacon, style = Stroke(cw * 0.012f))
}

private fun DrawScope.drawPorthole(c: Offset, cw: Float, idle: Float, alive: Float) {
    drawCircle(Color.Black.copy(alpha = 0.15f), cw * 0.35f, c + Offset(0f, cw * 0.03f))
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFF8C9AB0)), c.y - cw * 0.34f, c.y + cw * 0.34f), cw * 0.34f, c)
    drawCircle(Color(0xFF475569), cw * 0.34f, c, style = Stroke(cw * 0.025f))
    for (i in 0 until 8) {
        val a = i * Tau / 8
        drawCircle(Color(0xFF64748B), cw * 0.02f, c + polar(a, cw * 0.29f))
    }
    drawCircle(Color(0xFF334155), cw * 0.235f, c)
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0369A1), Color(0xFF0C2D57)),
            center = c + Offset(-cw * 0.08f, -cw * 0.08f), radius = cw * 0.3f,
        ),
        cw * 0.22f, c,
    )
    // Tiny stars seen through the window
    drawSparkle(c + Offset(cw * 0.09f, cw * 0.07f), cw * 0.03f * (1f + 0.4f * alive * wave(idle, 2)), Color.White.copy(alpha = 0.8f))
    drawCircle(Color.White.copy(alpha = 0.6f), cw * 0.01f, c + Offset(-cw * 0.02f, cw * 0.13f))
    drawArc(
        Color.White.copy(alpha = 0.85f), 200f, 70f, false,
        topLeft = c - Offset(cw * 0.16f, cw * 0.16f), size = Size(cw * 0.32f, cw * 0.32f),
        style = Stroke(cw * 0.04f, cap = StrokeCap.Round),
    )
    val t = (idle - 0.2f) / 0.18f
    if (alive > 0f && t in 0f..1f) {
        clipPath(Path().apply { addOval(Rect(c, cw * 0.22f)) }) {
            rotate(30f, pivot = c) {
                drawRect(
                    Color.White.copy(alpha = 0.45f * alive * sin(PI.toFloat() * t)),
                    Offset(c.x - cw * 0.32f + cw * 0.64f * t, c.y - cw * 0.35f), Size(cw * 0.07f, cw * 0.7f),
                )
            }
        }
    }
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
        // Light leading edge
        val edge = Path().apply {
            moveTo(x(w * 0.12f), h * 0.64f)
            quadraticTo(x(-w * 0.13f), h * 0.74f, x(-w * 0.14f), h * 0.88f)
        }
        drawPath(edge, Color.White.copy(alpha = 0.4f), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
        drawPath(p, Color(0xFF881337).copy(alpha = 0.7f), style = Stroke(w * 0.02f, join = StrokeJoin.Round))
    }
}

/** Exhaust flame below the rocket; [flicker] in -1..1 varies its length. */
private fun DrawScope.drawExhaust(w: Float, h: Float, progress: Float, flicker: Float) {
    if (progress <= 0.02f) return
    val fh = w * 0.42f * progress * (1f + 0.1f * flicker)
    val top = h * 0.96f
    fun flame(width: Float, height: Float) = Path().apply {
        val cx = w / 2
        moveTo(cx - width / 2, top)
        cubicTo(cx - width / 2, top + height * 0.5f, cx - width * 0.1f, top + height * 0.8f, cx, top + height)
        cubicTo(cx + width * 0.1f, top + height * 0.8f, cx + width / 2, top + height * 0.5f, cx + width / 2, top)
        close()
    }
    drawPath(flame(w * 0.5f, fh), Brush.verticalGradient(listOf(Color(0xFFFF6B1A), Color(0x00FF3D2E)), top, top + fh))
    drawPath(flame(w * 0.28f, fh * (0.7f - 0.06f * flicker)), Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0x00FFD23F)), top, top + fh * 0.7f))
}

// ---------------------------------------------------------------- Dino

private val DinoDark = Color(0xFF1F4D27)

private fun DrawScope.drawDinoCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFA3E88F), Color(0xFF5FC259), Color(0xFF3C9A47)), core.top, core.bottom),
        core.topLeft, core.size,
    )
    // Pale belly below the face
    val belly = Rect(core.left + cw * 0.17f, core.top + cw * 1.15f, core.right - cw * 0.17f, core.bottom + cw * 0.3f)
    drawRoundRect(Color(0xFFDDF7C8).copy(alpha = 0.55f), belly.topLeft, belly.size, CornerRadius(belly.width / 2))
    var by = belly.top + cw * 0.3f
    while (by < belly.bottom - cw * 0.3f) {
        drawLine(Color(0xFF8BCB78).copy(alpha = 0.6f), Offset(belly.left + cw * 0.12f, by), Offset(belly.right - cw * 0.12f, by), cw * 0.02f, StrokeCap.Round)
        by += cw * 0.28f
    }
    // Scales: soft spots along the sides
    val rnd = Random(11)
    var y = core.top + cw * 1.25f
    var row = 0
    while (y < core.bottom) {
        for (fx in if (row % 2 == 0) listOf(0.1f, 0.9f) else listOf(0.14f, 0.86f)) {
            val r = cw * (0.06f + rnd.nextFloat() * 0.04f)
            val c = Offset(core.left + cw * (fx + (rnd.nextFloat() - 0.5f) * 0.06f), y + (rnd.nextFloat() - 0.5f) * cw * 0.1f)
            drawCircle(Color(0x8C2E7D38), r, c)
            drawCircle(Color(0x66D3F9C9), r * 0.4f, c - Offset(r * 0.3f, r * 0.3f))
        }
        y += cw * 0.36f
        row++
    }
    // Head spots and nostrils
    drawCircle(Color(0x732E7D38), cw * 0.05f, Offset(core.left + cw * 0.3f, core.top + cw * 0.1f))
    drawCircle(Color(0x732E7D38), cw * 0.035f, Offset(core.left + cw * 0.68f, core.top + cw * 0.08f))
    drawOval(DinoDark, Offset(core.center.x - cw * 0.15f, core.top + cw * 0.23f), Size(cw * 0.07f, cw * 0.045f))
    drawOval(DinoDark, Offset(core.center.x + cw * 0.08f, core.top + cw * 0.23f), Size(cw * 0.07f, cw * 0.045f))
    // Big eyes that look around and blink
    val eyeY = core.top + cw * 0.6f
    val look = cw * 0.025f * alive * wave(idle, 1)
    val lid = blink(idle, 0.84f) * alive
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(core.center.x + dir * cw * 0.2f, eyeY)
        val er = cw * 0.15f
        drawCircle(Color.White, er, e)
        val pupil = e + Offset(-dir * cw * 0.02f + look, cw * 0.025f)
        drawCircle(Color(0xFF14261A), cw * 0.085f, pupil)
        drawCircle(Color.White, cw * 0.032f, pupil + Offset(-cw * 0.03f, -cw * 0.03f))
        drawCircle(Color.White.copy(alpha = 0.7f), cw * 0.014f, pupil + Offset(cw * 0.035f, cw * 0.03f))
        drawLid(Rect(e, er), lid, Color(0xFF86D575), DinoDark, cw * 0.03f)
        drawCircle(DinoDark.copy(alpha = 1f - 0.8f * lid), er, e, style = Stroke(cw * 0.025f))
        drawOval(Color(0x73FF8FAB), Offset(e.x - cw * 0.08f, eyeY + cw * 0.18f), Size(cw * 0.16f, cw * 0.08f))
    }
    // Smile with one little tooth
    val mouth = Rect(core.center.x - cw * 0.12f, eyeY + cw * 0.02f, core.center.x + cw * 0.12f, eyeY + cw * 0.24f)
    val tooth = Path().apply {
        moveTo(mouth.center.x + cw * 0.03f, mouth.bottom - cw * 0.012f)
        lineTo(mouth.center.x + cw * 0.075f, mouth.bottom - cw * 0.022f)
        lineTo(mouth.center.x + cw * 0.055f, mouth.bottom + cw * 0.035f)
        close()
    }
    drawPath(tooth, Color.White)
    drawArc(
        Color(0xFF14261A), 25f, 130f, false, topLeft = mouth.topLeft, size = mouth.size,
        style = Stroke(cw * 0.035f, cap = StrokeCap.Round),
    )
}

/** Three-toed dinosaur track with claws. */
private fun DrawScope.drawFootprint(center: Offset, cw: Float, color: Color) {
    val c = center + Offset(0f, cw * 0.1f)
    drawCircle(Color(0xFFDDF7C8).copy(alpha = 0.6f), cw * 0.34f, center)
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

private fun DrawScope.drawSpikes(w: Float, h: Float, left: Boolean, idle: Float, alive: Float) {
    val pitch = w * 0.3f
    val depth = w * 0.3f
    // Rounded back plates, alternating big and small like a stegosaurus; they sway in a ripple
    var y = h * 0.1f
    var i = 0
    while (y + pitch < h * 0.92f) {
        val base = if (left) w * 0.14f else w * 0.86f
        val d = if (i % 2 == 0) depth else depth * 0.72f
        val sway = 4f * alive * wave(idle, 2, -i * 0.1f)
        rotate(if (left) -sway else sway, pivot = Offset(base, y + pitch / 2)) {
            drawPlate(Offset(base, y + pitch / 2), pitch * 0.96f, w * 0.14f + d, w * 0.02f, if (left) -90f else 90f)
        }
        y += pitch
        i++
    }
}

/** [drawPlate] rotated by [angle] around its base, so it points sideways. */
private fun DrawScope.drawPlate(base: Offset, w: Float, h: Float, line: Float, angle: Float) {
    rotate(angle, pivot = base) { drawPlate(base, w, h, line) }
}

// ---------------------------------------------------------------- Leopard

private val LeopardDark = Color(0xFF3B2412)
private val LeopardFur = Color(0xFFF7BF66)

private fun DrawScope.drawEar(c: Offset, r: Float, dir: Float, twitch: Float) {
    rotate(dir * 14f * twitch, pivot = c + Offset(-dir * r * 0.3f, r * 0.8f)) {
        drawCircle(Brush.verticalGradient(listOf(Color(0xFFF2B055), Color(0xFFD9852B)), c.y - r, c.y + r), r, c)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC9D2), Color(0xFFF29AAB)), center = c, radius = r * 0.55f), r * 0.52f, c + Offset(0f, r * 0.08f))
        drawCircle(LeopardDark, r, c, style = Stroke(r * 0.16f))
    }
}

private fun DrawScope.drawLeopardCore(core: Rect, idle: Float, alive: Float) {
    val cw = core.width
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFFCD98A), Color(0xFFF5AE4E), Color(0xFFD9852B)), core.top, core.bottom),
        core.topLeft, core.size,
    )
    val rnd = Random(7)
    // Rosettes: broken dark rings around a darker center
    var y = core.top + cw * 1.35f
    var row = 0
    while (y < core.bottom + cw * 0.2f) {
        for (fx in if (row % 2 == 0) listOf(0.18f, 0.74f) else listOf(0.46f, 1.0f)) {
            val c = Offset(core.left + cw * (fx + (rnd.nextFloat() - 0.5f) * 0.08f), y + (rnd.nextFloat() - 0.5f) * cw * 0.08f)
            val r = cw * (0.11f + rnd.nextFloat() * 0.03f)
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
        drawCircle(LeopardDark, cw * 0.028f, Offset(core.left + cw * (0.3f + rnd.nextFloat() * 0.4f), y + cw * 0.24f))
        y += cw * 0.48f
        row++
    }
    // Forehead dots
    for ((fx, fy) in listOf(0.38f to 0.2f, 0.5f to 0.14f, 0.62f to 0.2f, 0.3f to 0.32f, 0.7f to 0.32f)) {
        drawCircle(LeopardDark, cw * 0.025f, Offset(core.left + cw * fx, core.top + cw * fy))
    }
    // Face: cream muzzle and cheeks with pink blush
    val eyeY = core.top + cw * 0.58f
    val muzzle = Offset(core.center.x, eyeY + cw * 0.32f)
    drawOval(Color(0xFFFFF3DC), Offset(muzzle.x - cw * 0.27f, muzzle.y - cw * 0.14f), Size(cw * 0.54f, cw * 0.32f))
    for (dir in listOf(-1f, 1f)) {
        drawOval(Color(0x59FF8FAB), Offset(muzzle.x + dir * cw * 0.3f - cw * 0.07f, muzzle.y - cw * 0.12f), Size(cw * 0.14f, cw * 0.07f))
    }
    val lid = blink(idle, 0.4f) * alive
    val look = cw * 0.02f * alive * wave(idle, 1, 0.25f)
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(core.center.x + dir * cw * 0.2f, eyeY)
        val eye = Rect(e.x - cw * 0.13f, e.y - cw * 0.095f, e.x + cw * 0.13f, e.y + cw * 0.095f)
        drawOval(
            Brush.radialGradient(listOf(Color(0xFFEFFFC2), Color(0xFF84CC16), Color(0xFF4D7C0F)), center = e, radius = cw * 0.13f),
            eye.topLeft, eye.size,
        )
        drawOval(Color(0xFF111111), Offset(e.x - cw * 0.025f + look, e.y - cw * 0.075f), Size(cw * 0.05f, cw * 0.15f))
        drawCircle(Color.White, cw * 0.024f, e + Offset(-cw * 0.05f + look, -cw * 0.035f))
        drawLid(eye, lid, LeopardFur, LeopardDark, cw * 0.028f)
        drawOval(LeopardDark.copy(alpha = 1f - 0.8f * lid), eye.topLeft, eye.size, style = Stroke(cw * 0.028f))
        // Little tear line to the muzzle, typical for big cats
        drawLine(LeopardDark.copy(alpha = 0.6f), Offset(e.x - dir * cw * 0.07f, e.y + cw * 0.09f), Offset(e.x - dir * cw * 0.1f, e.y + cw * 0.17f), cw * 0.02f, StrokeCap.Round)
        // Whiskers and whisker dots
        for (k in -1..1) {
            val from = Offset(muzzle.x + dir * cw * 0.15f, muzzle.y + cw * 0.05f + k * cw * 0.05f)
            drawLine(LeopardDark.copy(alpha = 0.55f), from, from + Offset(dir * cw * 0.32f, k * cw * 0.06f), cw * 0.014f, StrokeCap.Round)
            drawCircle(LeopardDark.copy(alpha = 0.6f), cw * 0.012f, Offset(muzzle.x + dir * cw * (0.1f + 0.02f * (k + 1)), muzzle.y + cw * 0.02f + k * cw * 0.03f))
        }
    }
    val noseY = muzzle.y - cw * 0.09f
    val nose = Path().apply {
        moveTo(muzzle.x - cw * 0.085f, noseY)
        lineTo(muzzle.x + cw * 0.085f, noseY)
        quadraticTo(muzzle.x + cw * 0.04f, noseY + cw * 0.085f, muzzle.x, noseY + cw * 0.095f)
        quadraticTo(muzzle.x - cw * 0.04f, noseY + cw * 0.085f, muzzle.x - cw * 0.085f, noseY)
        close()
    }
    drawPath(nose, Color(0xFFE0788A))
    drawOval(Color.White.copy(alpha = 0.5f), Offset(muzzle.x - cw * 0.04f, noseY + cw * 0.01f), Size(cw * 0.05f, cw * 0.025f))
    drawPath(nose, Color(0xFF9F3B4E), style = Stroke(cw * 0.012f, join = StrokeJoin.Round))
    for (dir in listOf(-1f, 1f)) {
        drawArc(
            LeopardDark, 0f, 180f, false,
            topLeft = Offset(muzzle.x + (if (dir < 0) -cw * 0.1f else 0f), noseY + cw * 0.06f),
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

private fun DrawScope.drawSkinEmblem(skin: Skin, c: Offset, cw: Float, idle: Float, alive: Float) {
    when (skin) {
        Skin.CLASSIC -> drawLens(c, cw, idle, alive)
        Skin.RAINBOW -> drawHappyStar(c, cw, idle, alive)
        Skin.SPACE -> drawPorthole(c, cw, idle, alive)
        Skin.DINO -> drawFootprint(c, cw, DinoDark.copy(alpha = 0.8f))
        Skin.LEOPARD -> {
            drawCircle(Color.Black.copy(alpha = 0.12f), cw * 0.31f, c + Offset(0f, cw * 0.025f))
            drawCircle(Color(0xFFFFF3DC), cw * 0.3f, c)
            drawCircle(Color(0xFFE6B877), cw * 0.3f, c, style = Stroke(cw * 0.02f))
            drawPaw(c + Offset(0f, cw * 0.03f), cw, LeopardDark)
            drawOval(Color(0xFFF29AAB), Offset(c.x - cw * 0.07f, c.y + cw * 0.04f), Size(cw * 0.14f, cw * 0.09f))
        }
        Skin.UNICORN -> drawUnicornEmblem(c, cw, idle, alive)
        Skin.SHARK -> drawFishEmblem(c, cw, idle, alive)
        Skin.ROBOT -> drawGearEmblem(c, cw, idle, alive)
        Skin.FIRE_TRUCK -> drawBadgeEmblem(c, cw, idle, alive)
        Skin.MONSTER -> drawCandyEmblem(c, cw, idle, alive)
        Skin.ICE_CREAM -> drawStrawberryEmblem(c, cw, idle, alive)
    }
}

private fun DrawScope.drawRecordEmblem(c: Offset, cw: Float, recording: Boolean) {
    drawCircle(Color.Black.copy(alpha = 0.15f), cw * 0.34f, c + Offset(0f, cw * 0.025f))
    drawCircle(Color.White, cw * 0.33f, c)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFF9A9A), Color(0xFFE11D48)), center = c - Offset(cw * 0.06f, cw * 0.06f), radius = cw * 0.3f),
        cw * 0.26f, c,
    )
    if (recording) {
        val s = cw * 0.2f
        drawRoundRect(Color.White, c - Offset(s / 2, s / 2), Size(s, s), CornerRadius(cw * 0.04f))
    } else {
        drawCircle(Color.White.copy(alpha = 0.6f), cw * 0.05f, c - Offset(cw * 0.09f, cw * 0.09f))
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
    val plateTopLeft = origin - Offset(pad, pad)
    val plateSize = Size(cols * cell + pad * 2, rows * cell + pad * 2)
    drawRoundRect(Color.White.copy(alpha = 0.85f), plateTopLeft, plateSize, CornerRadius(cell * 0.45f))
    drawRoundRect(Color.Black.copy(alpha = 0.12f), plateTopLeft, plateSize, CornerRadius(cell * 0.45f), style = Stroke(cell * 0.05f))
    for (i in 0 until n) {
        val c = origin + Offset((i % cols) * cell + cell / 2f, (i / cols) * cell + cell / 2f)
        drawToken(skin, c, cell * 0.4f, i, filled = i < tokens.filled)
    }
}

private fun DrawScope.drawToken(skin: Skin, c: Offset, r: Float, index: Int, filled: Boolean) {
    val empty = Color.Black.copy(alpha = 0.32f)
    val outline = Stroke(r * 0.18f, join = StrokeJoin.Round)
    when (skin) {
        Skin.RAINBOW, Skin.SPACE -> {
            val star = starPath(c, r * 1.1f, r * 0.5f)
            if (filled) {
                drawPath(star, if (skin == Skin.RAINBOW) RainbowBands[index % RainbowBands.size] else Color(0xFFFFE14D))
                drawPath(star, Color(0xFF1E1B4B).copy(alpha = 0.4f), style = Stroke(r * 0.12f, join = StrokeJoin.Round))
                drawCircle(Color.White.copy(alpha = 0.6f), r * 0.16f, c + Offset(-r * 0.2f, -r * 0.2f))
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
                drawOval(DinoDark.copy(alpha = 0.55f), egg.topLeft, egg.size, style = Stroke(r * 0.12f))
            } else {
                drawOval(empty, egg.topLeft, egg.size, style = outline)
            }
        }
        Skin.LEOPARD -> {
            if (filled) {
                drawCircle(Color(0xFFFFF3DC), r, c)
                drawCircle(Color(0xFFD9852B), r, c, style = Stroke(r * 0.12f))
                drawPaw(c + Offset(0f, r * 0.1f), r * 2.6f, LeopardDark)
            } else {
                drawCircle(empty, r, c, style = outline)
            }
        }
        Skin.CLASSIC -> {
            if (filled) {
                drawCircle(Color(0xFFFFC93C), r, c)
                drawCircle(Color(0xFFD99A00), r, c, style = Stroke(r * 0.14f))
                drawCircle(Color.White.copy(alpha = 0.65f), r * 0.3f, c + Offset(-r * 0.3f, -r * 0.3f))
            } else {
                drawCircle(empty, r, c, style = outline)
            }
        }
        else -> drawCharacterToken(skin, c, r, index, filled)
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
        else -> drawCharacterBackdrop(skin, rnd, unit)
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
