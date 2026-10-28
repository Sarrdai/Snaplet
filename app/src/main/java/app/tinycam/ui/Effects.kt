package app.tinycam.ui

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Frame { STARS, HEARTS, RAINBOW, PORTHOLE, LEAVES }

/** Fun looks for photos: either a color filter or a decorative frame. */
enum class Effect(val matrix: FloatArray?, val frame: Frame?) {
    NONE(null, null),
    BLACK_WHITE(saturation(0f), null),
    SEPIA(
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
        null,
    ),
    VIVID(saturation(1.8f), null),
    STARS(null, Frame.STARS),
    HEARTS(null, Frame.HEARTS),
    RAINBOW(null, Frame.RAINBOW),
    PORTHOLE(null, Frame.PORTHOLE),
    LEAVES(null, Frame.LEAVES),
}

private fun saturation(s: Float): FloatArray = ColorMatrix().apply { setToSaturation(s) }.values

/** Draws [block] through the effect's color filter, if it has one. */
inline fun DrawScope.withEffectColors(effect: Effect, block: DrawScope.() -> Unit) {
    val m = effect.matrix
    if (m == null) {
        block()
        return
    }
    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix(m)) }
    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), paint) }
    block()
    drawIntoCanvas { it.restore() }
}

/** Applies color filter and frame to a bitmap, e.g. the saved photo or the preview snapshot. */
fun renderEffect(src: Bitmap, effect: Effect): Bitmap {
    if (effect == Effect.NONE) return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    val paint = AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG)
    effect.matrix?.let { paint.colorFilter = ColorMatrixColorFilter(it) }
    canvas.drawBitmap(src, 0f, 0f, paint)
    effect.frame?.let { frame ->
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(canvas), Size(out.width.toFloat(), out.height.toFloat())) {
            drawFrame(frame)
        }
    }
    return out
}

fun DrawScope.drawFrame(frame: Frame) {
    when (frame) {
        Frame.STARS -> drawBorderShapes(listOf(Color(0xFFFFE14D), Color.White, Color(0xFFFFB3D9), Color(0xFF9BE7FF))) { c, r, color ->
            val star = starPath(c, r, r * 0.45f)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), c, r * 1.8f), r * 1.8f, c)
            drawPath(star, color)
            drawPath(star, Color.Black.copy(alpha = 0.12f), style = Stroke(r * 0.08f))
        }
        Frame.HEARTS -> drawBorderShapes(listOf(Color(0xFFFF4D8D), Color(0xFFFF8FB8), Color(0xFFE11D48), Color.White)) { c, r, color ->
            val heart = heartPath(c, r)
            drawPath(heart, color)
            drawPath(heart, Color.Black.copy(alpha = 0.12f), style = Stroke(r * 0.08f))
            drawCircle(Color.White.copy(alpha = 0.6f), r * 0.16f, c + Offset(-r * 0.4f, -r * 0.35f))
        }
        Frame.RAINBOW -> {
            val u = size.minDimension / 100f
            val colors = listOf(
                Color(0xFFFF5D5D), Color(0xFFFFA23A), Color(0xFFFFE14D),
                Color(0xFF5FD068), Color(0xFF4CB3FF), Color(0xFFB57BFF),
            )
            val band = u * 1.8f
            colors.forEachIndexed { i, c ->
                val inset = band * (i + 0.5f)
                drawRoundRect(
                    c, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2),
                    CornerRadius(u * 10f - inset), style = Stroke(band * 1.05f),
                )
            }
        }
        Frame.PORTHOLE -> {
            val u = size.minDimension / 100f
            val c = center
            val r = size.minDimension * 0.47f
            val outside = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addOval(Rect(c, r))
            }
            drawPath(outside, Brush.verticalGradient(listOf(Color(0xFF111A4A), Color(0xFF05071A))))
            val rnd = Random(3)
            repeat(70) {
                val p = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
                if ((p - c).getDistance() > r + u * 4) drawCircle(Color.White.copy(alpha = 0.3f + rnd.nextFloat() * 0.6f), u * (0.25f + rnd.nextFloat() * 0.4f), p)
            }
            drawCircle(
                Brush.sweepGradient(listOf(Color(0xFFF1F5F9), Color(0xFF8C9AB0), Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFFF1F5F9)), c),
                r + u * 2.5f, c, style = Stroke(u * 5f),
            )
            for (i in 0 until 16) {
                val a = i * PI / 8
                drawCircle(Color(0xFF475569), u * 0.9f, c + Offset((cos(a) * (r + u * 2.5f)).toFloat(), (sin(a) * (r + u * 2.5f)).toFloat()))
            }
        }
        Frame.LEAVES -> {
            val u = size.minDimension / 100f
            val rnd = Random(5)
            val corners = listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height))
            for (corner in corners) {
                repeat(9) {
                    val angle = rnd.nextFloat() * 360f
                    val dist = u * (4f + rnd.nextFloat() * 16f)
                    val toward = Offset(if (corner.x == 0f) 1f else -1f, if (corner.y == 0f) 1f else -1f)
                    val p = corner + Offset(toward.x * dist * rnd.nextFloat(), toward.y * dist * rnd.nextFloat())
                    drawLeaf(p, u * (7f + rnd.nextFloat() * 6f), angle, if (rnd.nextBoolean()) Color(0xFF2F9E44) else Color(0xFF69DB7C))
                }
            }
        }
    }
}

/** Scatters shapes along all four edges. */
private fun DrawScope.drawBorderShapes(colors: List<Color>, shape: DrawScope.(Offset, Float, Color) -> Unit) {
    val u = size.minDimension / 100f
    val rnd = Random(9)
    val step = u * 9f
    val band = u * 6f
    fun at(p: Offset) = shape(p, u * (2.8f + rnd.nextFloat() * 2.4f), colors[rnd.nextInt(colors.size)])
    var x = u * 4f
    while (x < size.width) {
        at(Offset(x, u * 4f + rnd.nextFloat() * band * 0.4f))
        at(Offset(x + step / 2, size.height - u * 4f - rnd.nextFloat() * band * 0.4f))
        x += step
    }
    var y = u * 12f
    while (y < size.height - u * 10f) {
        at(Offset(u * 4f + rnd.nextFloat() * band * 0.4f, y))
        at(Offset(size.width - u * 4f - rnd.nextFloat() * band * 0.4f, y + step / 2))
        y += step
    }
}

private fun heartPath(c: Offset, r: Float) = Path().apply {
    moveTo(c.x, c.y + r * 0.9f)
    cubicTo(c.x - r * 1.3f, c.y + r * 0.1f, c.x - r * 0.9f, c.y - r * 1.05f, c.x, c.y - r * 0.4f)
    cubicTo(c.x + r * 0.9f, c.y - r * 1.05f, c.x + r * 1.3f, c.y + r * 0.1f, c.x, c.y + r * 0.9f)
    close()
}

private fun DrawScope.drawLeaf(p: Offset, len: Float, angle: Float, color: Color) {
    rotate(angle, pivot = p) {
        val leaf = Path().apply {
            moveTo(p.x, p.y)
            quadraticTo(p.x + len * 0.5f, p.y - len * 0.35f, p.x + len, p.y)
            quadraticTo(p.x + len * 0.5f, p.y + len * 0.35f, p.x, p.y)
            close()
        }
        drawPath(leaf, color)
        drawLine(Color(0xFF1C4A27).copy(alpha = 0.5f), p, p + Offset(len * 0.9f, 0f), len * 0.05f, StrokeCap.Round)
    }
}

/** Tiny landscape used for the effect picker bubbles. */
fun DrawScope.drawMiniScene() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF4FA3E8), Color(0xFFBFE6F8))))
    drawCircle(Color(0xFFFFD34D), size.minDimension * 0.14f, Offset(size.width * 0.3f, size.height * 0.32f))
    drawCircle(Color(0xFF5FC259), size.width * 0.75f, Offset(size.width * 0.2f, size.height * 1.45f))
    drawCircle(Color(0xFF3C9A47), size.width * 0.7f, Offset(size.width * 0.95f, size.height * 1.5f))
    drawCircle(Color(0xFFFF6B6B), size.minDimension * 0.09f, Offset(size.width * 0.66f, size.height * 0.68f))
}
