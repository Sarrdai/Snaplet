package app.tinycam.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.tinycam.R
import app.tinycam.data.MediaItem
import app.tinycam.data.Skin
import app.tinycam.data.Thumbnails
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

val KidFont = FontFamily(
    Font(R.font.baloo2_medium, FontWeight.Medium),
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)

val Ink = Color(0xFF1B1F2A)

@Composable
fun Dp.asSp(): TextUnit = with(LocalDensity.current) { this@asSp.toSp() }

fun kidText(size: TextUnit, color: Color = Color.White, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = KidFont, fontWeight = weight, fontSize = size, color = color, lineHeight = size * 1.1f)

// ---------------------------------------------------------------- Touch protection

/** Number of fingers currently on the screen; more than one means palm or grabbing hand. */
class TouchCounter {
    var count = 0
}

/** Must sit on the root so it sees every pointer before the controls do. */
fun Modifier.trackTouches(counter: TouchCounter) = pointerInput(counter) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            counter.count = event.changes.count { it.pressed }
        }
    }
}

/** Fires on finger down (no waiting for release) unless several fingers touch the screen. */
@Composable
fun Modifier.pressDown(touches: TouchCounter, onPressedChange: (Boolean) -> Unit = {}, onPress: () -> Unit): Modifier {
    val press by rememberUpdatedState(onPress)
    val change by rememberUpdatedState(onPressedChange)
    return pointerInput(touches) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (touches.count > 1) return@awaitEachGesture
            down.consume()
            change(true)
            press()
            waitForUpOrCancellation()
            change(false)
        }
    }
}

/** A single-finger tap (down and up inside the element). */
@Composable
fun Modifier.singleTap(touches: TouchCounter, onTap: () -> Unit): Modifier {
    val tap by rememberUpdatedState(onTap)
    return pointerInput(touches) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (touches.count > 1) return@awaitEachGesture
            down.consume()
            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
            if (touches.count <= 1) {
                up.consume()
                tap()
            }
        }
    }
}

@Stable
class HoldState {
    var progress by mutableFloatStateOf(0f)
}

/**
 * Activates only after the element was held for [durationMs] with a single finger.
 * Releasing early reports [onTooShort] so the UI can explain the gesture.
 */
@Composable
fun Modifier.holdToActivate(
    state: HoldState,
    durationMs: Int,
    touches: TouchCounter,
    enabled: Boolean = true,
    onTooShort: () -> Unit = {},
    onComplete: () -> Unit,
): Modifier {
    val complete by rememberUpdatedState(onComplete)
    val tooShort by rememberUpdatedState(onTooShort)
    if (!enabled) return this
    return pointerInput(durationMs, touches) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (touches.count > 1) return@awaitEachGesture
                down.consume()
                var done = false
                val ticker = launch {
                    val start = withFrameMillis { it }
                    while (!done) {
                        val now = withFrameMillis { it }
                        state.progress = ((now - start) / durationMs.toFloat()).coerceAtMost(1f)
                        if (state.progress >= 1f) {
                            done = true
                            complete()
                        }
                    }
                }
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                    val pressed = event.changes.count { it.pressed }
                } while (pressed == 1 && touches.count <= 1)
                ticker.cancel()
                if (!done && state.progress < 0.5f) tooShort()
                state.progress = 0f
            }
        }
    }
}

// ---------------------------------------------------------------- Shutter

private val PillShape = RoundedCornerShape(50)

@Composable
fun Shutter(
    skin: Skin,
    style: SkinStyle,
    width: Dp,
    height: Dp,
    progress: () -> Float,
    ready: Boolean,
    emblem: Emblem,
    spikesLeft: Boolean,
    capturesLeft: Int?,
    unit: Dp,
    touches: TouchCounter,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val inactive = animateFloatAsState(if (ready) 0f else 1f, tween(220), label = "inactive")
    val pressScale by animateFloatAsState(
        if (pressed) 0.93f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "press",
    )
    val pop = remember { Animatable(1f) }
    var wasReady by remember { mutableStateOf(ready) }
    LaunchedEffect(ready) {
        // Small bounce when the camera becomes ready again
        if (ready && !wasReady) {
            pop.animateTo(1.08f, tween(110))
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
        wasReady = ready
    }
    val density = LocalDensity.current
    Box(
        modifier
            .size(width, height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(12.dp, PillShape, clip = false, ambientColor = style.shadow, spotColor = style.shadow)
            .pressDown(touches, onPressedChange = { pressed = it }, onPress = onPress)
            .drawBehind {
                drawShutter(
                    skin, style, progress(), inactive.value, (1f - 0.12f * inactive.value) * pop.value, emblem, spikesLeft,
                )
            },
    ) {
        if (capturesLeft != null) {
            val centerY = with(density) { (height * chipCenterFraction(skin, width.toPx(), height.toPx())).roundToPx() }
            CountChip(
                capturesLeft, unit,
                Modifier
                    .align(Alignment.TopCenter)
                    .layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) { p.place(0, centerY - p.height / 2) }
                    },
            )
        }
    }
}

@Composable
private fun CountChip(count: Int, unit: Dp, modifier: Modifier) {
    BasicText(
        count.toString(),
        modifier
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .padding(horizontal = unit * 2.2f, vertical = unit * 0.2f),
        style = kidText((unit * 5f).asSp(), Ink, FontWeight.ExtraBold),
    )
}

// ---------------------------------------------------------------- Rail buttons

@Composable
fun ModeSwitch(
    videoSelected: Boolean,
    style: SkinStyle,
    unit: Dp,
    touches: TouchCounter,
    enabled: Boolean,
    onTooShort: () -> Unit,
    onToggle: () -> Unit,
) {
    val hold = remember { HoldState() }
    val shape = RoundedCornerShape(50)
    Column(
        Modifier
            .shadow(6.dp, shape, ambientColor = style.shadow, spotColor = style.shadow)
            .clip(shape)
            .background(style.button)
            .holdFillBehind(hold, style.holdFill)
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .holdToActivate(hold, 1500, touches, enabled, onTooShort, onToggle)
            .padding(unit * 1.1f),
        verticalArrangement = Arrangement.spacedBy(unit * 0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ModeSlot(Icons.Camera, !videoSelected, style, unit)
        ModeSlot(Icons.Video, videoSelected, style, unit)
    }
}

@Composable
private fun ModeSlot(icon: ImageVector, selected: Boolean, style: SkinStyle, unit: Dp) {
    Box(
        Modifier
            .size(unit * 11f)
            .then(if (selected) Modifier.shadow(3.dp, CircleShape).background(style.buttonOn, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            icon, null, Modifier.size(unit * 6.4f),
            colorFilter = ColorFilter.tint(if (selected) style.buttonOnInk else style.buttonInk.copy(alpha = 0.7f)),
        )
    }
}

@Composable
fun GalleryButton(style: SkinStyle, unit: Dp, touches: TouchCounter, onTooShort: () -> Unit, onOpen: () -> Unit) {
    val hold = remember { HoldState() }
    val shape = RoundedCornerShape(unit * 4.5f)
    Box(
        Modifier
            .size(unit * 15f)
            .shadow(6.dp, shape, ambientColor = style.shadow, spotColor = style.shadow)
            .clip(shape)
            .background(style.button)
            .holdFillBehind(hold, style.holdFill)
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .holdToActivate(hold, 1000, touches, onTooShort = onTooShort, onComplete = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Image(Icons.Grid, null, Modifier.size(unit * 7.5f), colorFilter = ColorFilter.tint(style.buttonInk))
    }
}

private fun Modifier.holdFillBehind(hold: HoldState, color: Color) = drawBehind {
    val p = hold.progress
    if (p > 0f) drawRect(color, topLeft = Offset(0f, size.height * (1 - p)), size = Size(size.width, size.height * p))
}

/** Parent entry: deliberately small and faint, needs a 3 second hold. */
@Composable
fun LockButton(style: SkinStyle, unit: Dp, touches: TouchCounter, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val hold = remember { HoldState() }
    Box(
        modifier
            .size(unit * 10f)
            .graphicsLayer { alpha = if (hold.progress > 0f) 0.95f else 0.4f }
            .background(Color(0x66000000), CircleShape)
            .drawBehind {
                if (hold.progress > 0f) {
                    val sw = size.width * 0.09f
                    drawArc(
                        style.accent, -90f, 360f * hold.progress, false,
                        topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw),
                        style = Stroke(sw, cap = StrokeCap.Round),
                    )
                }
            }
            .holdToActivate(hold, 3000, touches, onComplete = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Image(Icons.Lock, "Eltern-Bereich", Modifier.size(unit * 5f), colorFilter = ColorFilter.tint(Color.White))
    }
}

@Composable
fun CloseButton(unit: Dp, touches: TouchCounter, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(unit * 15f)
            .shadow(8.dp, CircleShape)
            .background(Color.White, CircleShape)
            .singleTap(touches, onClose),
        contentAlignment = Alignment.Center,
    ) {
        Image(Icons.Close, "Zurück", Modifier.size(unit * 7f), colorFilter = ColorFilter.tint(Ink))
    }
}

// ---------------------------------------------------------------- Hints

@Composable
fun HintBubble(text: String, unit: Dp, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier
            .shadow(8.dp, CircleShape)
            .background(Color.White, CircleShape)
            .padding(horizontal = unit * 3.6f, vertical = unit * 1.6f),
        style = kidText((unit * 4.2f).asSp(), Ink),
        softWrap = false,
    )
}

/** Places the content next to the anchor point it is aligned to, without taking up space. */
fun Modifier.beside(onLeft: Boolean, gap: Dp) = layout { measurable, _ ->
    val p = measurable.measure(Constraints())
    val g = gap.roundToPx()
    layout(0, 0) { p.place(if (onLeft) -p.width - g else g, -p.height / 2) }
}

// ---------------------------------------------------------------- Thumbnails

@Composable
fun Thumbnail(
    item: MediaItem,
    thumbs: Thumbnails,
    style: SkinStyle,
    unit: Dp,
    touches: TouchCounter,
    onOpen: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumb by produceState(thumbs.peek(item.id), item.id) {
        if (value == null) value = thumbs.load(item)
    }
    val shape = RoundedCornerShape(unit * 3f)
    Box(
        modifier
            .shadow(5.dp, shape, ambientColor = style.shadow, spotColor = style.shadow)
            .clip(shape)
            .background(Color(0xFF2A2F3D))
            .singleTap(touches) { onOpen(item) },
    ) {
        thumb?.let { Image(it.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (item.isVideo) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(unit * 1.2f)
                    .background(Color(0x99000000), CircleShape)
                    .padding(horizontal = unit * 1.4f, vertical = unit * 0.2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(unit * 0.6f),
            ) {
                Image(Icons.Play, null, Modifier.size(unit * 3f), colorFilter = ColorFilter.tint(Color.White))
                thumb?.durationMs?.let { BasicText(formatDuration(it), style = kidText((unit * 3.4f).asSp())) }
            }
        }
        Box(Modifier.fillMaxSize().border(unit * 0.7f, style.frame, shape))
    }
}

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

/** Layout unit: 1% of the short screen side, capped so tablets do not get giant controls. */
fun unitFor(widthDp: Float, heightDp: Float): Dp = (minOf(widthDp, heightDp) / 100f).coerceAtMost(4.6f).dp
