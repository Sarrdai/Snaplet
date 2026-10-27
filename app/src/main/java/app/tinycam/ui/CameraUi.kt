package app.tinycam.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import app.tinycam.data.CaptureMode
import app.tinycam.data.MediaItem
import app.tinycam.data.Settings
import app.tinycam.data.Side
import app.tinycam.data.Thumbnails
import app.tinycam.data.maxVideoMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class Captured(val item: MediaItem, val snapshot: ImageBitmap?)

/** What the UI needs from the camera. Implemented by CameraX in the app and by fakes in tests. */
interface CameraActions {
    fun takePhoto(): Captured?
    fun startVideo(): Boolean
    fun stopVideo(elapsedMs: Long): Captured?
}

private enum class HintTarget { SHUTTER, MODE, GALLERY }

private class Hint(val target: HintTarget, val text: String)

private class Fly(val bitmap: ImageBitmap, val itemId: String) {
    val anim = Animatable(0f)
}

private sealed interface Overlay {
    data object None : Overlay
    data object Gallery : Overlay
    data class Preview(val item: MediaItem, val fromGallery: Boolean) : Overlay
}

private const val HOLD_HINT = "Gedrückt halten"
private const val EMPTY_HINT = "Filmrolle voll. Morgen geht's weiter!"

/**
 * The kid-facing camera screen. Only the shutter, the thumbnail strip and hold-to-activate
 * buttons react to touch; the live image itself is inert.
 */
@Composable
fun KidCameraUi(
    settings: Settings,
    items: List<MediaItem>,
    thumbs: Thumbnails,
    capturesLeft: Int?,
    mode: CaptureMode,
    onModeChange: (CaptureMode) -> Unit,
    actions: CameraActions,
    onOpenParent: () -> Unit,
    modifier: Modifier = Modifier,
    initialCooldown: Float = 1f,
    preview: @Composable () -> Unit,
) {
    val touches = remember { TouchCounter() }
    val style = remember(settings.skin) { styleFor(settings.skin) }
    val scope = rememberCoroutineScope()

    val cooldown = remember { Animatable(initialCooldown) }
    val ready by remember { derivedStateOf { cooldown.value >= 1f } }
    var recordingStart by remember { mutableStateOf<Long?>(null) }
    var recordingElapsed by remember { mutableLongStateOf(0L) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var hint by remember { mutableStateOf<Hint?>(null) }
    var fly by remember { mutableStateOf<Fly?>(null) }
    var flyTarget by remember { mutableStateOf<Rect?>(null) }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val flash = remember { Animatable(0f) }

    fun startCooldown() {
        if (settings.cooldownSec <= 0) return
        scope.launch {
            cooldown.snapTo(0f)
            cooldown.animateTo(1f, tween(settings.cooldownSec * 1000, easing = LinearEasing))
        }
    }

    fun showCaptured(c: Captured) {
        scope.launch {
            flash.snapTo(0.75f)
            flash.animateTo(0f, tween(220))
        }
        val bitmap = c.snapshot ?: return
        val f = Fly(bitmap, c.item.id)
        flyTarget = null
        fly = f
        scope.launch {
            f.anim.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
            if (fly === f) fly = null
        }
    }

    fun stopRecording() {
        val start = recordingStart ?: return
        recordingStart = null
        actions.stopVideo(SystemClock.uptimeMillis() - start)?.let(::showCaptured)
        startCooldown()
    }

    fun onShutter() {
        val now = SystemClock.uptimeMillis()
        val start = recordingStart
        when {
            start != null -> if (now - start >= 1000) stopRecording() // a double tap must not end the clip
            !ready -> Unit
            capturesLeft == 0 -> hint = Hint(HintTarget.SHUTTER, EMPTY_HINT)
            mode == CaptureMode.PHOTO -> actions.takePhoto()?.let {
                showCaptured(it)
                startCooldown()
            }
            actions.startVideo() -> {
                recordingElapsed = 0
                recordingStart = now
            }
        }
    }

    LaunchedEffect(recordingStart) {
        val start = recordingStart ?: return@LaunchedEffect
        val max = settings.maxVideoMs()
        while (true) {
            recordingElapsed = SystemClock.uptimeMillis() - start
            if (max != null && recordingElapsed >= max) {
                stopRecording()
                break
            }
            delay(100)
        }
    }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(1800)
            hint = null
        }
    }

    // Back never leaves the camera; it only closes overlays.
    BackHandler(enabled = true) {
        overlay = when (val o = overlay) {
            is Overlay.Preview -> if (o.fromGallery) Overlay.Gallery else Overlay.None
            else -> Overlay.None
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .trackTouches(touches)
            .onGloballyPositioned { rootCoords = it },
    ) {
        val screenH = maxHeight
        val portrait = maxHeight > maxWidth
        val u = unitFor(maxWidth.value, maxHeight.value)
        val pillW = u * 21f
        val right = settings.side == Side.RIGHT
        val edge = u * 6.5f
        val recording = recordingStart != null
        val railGap = u * 3f
        val railBottom = screenH * 0.07f
        // Whatever is stacked above the shutter must still fit on short landscape screens
        val aboveShutter = (if (settings.galleryUnlocked) u * 15f + railGap else 0.dp) +
            (if (settings.videoUnlocked) u * 25f + railGap else 0.dp)
        val pillH = minOf(
            if (portrait) screenH * 0.3f else screenH * 0.5f,
            pillW * 4.4f,
            screenH - railBottom - aboveShutter - u * 6f,
        )

        preview()

        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Thumbnail strip, newest next to the shutter
            val reserve = if (portrait) u * 4f else edge + pillW + u * 5f
            ThumbnailStrip(
                items = items, thumbs = thumbs, style = style, unit = u, touches = touches,
                count = settings.stripCount, right = right, portrait = portrait, hiddenId = fly?.itemId,
                onTargetPositioned = { coords -> rootCoords?.let { flyTarget = it.localBoundingBoxOf(coords) } },
                onOpen = { overlay = Overlay.Preview(it, fromGallery = false) },
                modifier = Modifier
                    .padding(top = u * 4f, start = if (right) u * 4f else reserve, end = if (right) reserve else u * 4f)
                    .fillMaxWidth()
                    .height(u * 17f),
            )

            if (recording) {
                RecordingTimer(recordingElapsed, u, Modifier.align(Alignment.TopCenter).padding(top = u * 25f))
            }

            Column(
                Modifier
                    .align(if (right) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(bottom = railBottom, start = if (right) 0.dp else edge, end = if (right) edge else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(railGap),
            ) {
                if (settings.galleryUnlocked && !recording) {
                    RailItem(hint, HintTarget.GALLERY, right, u) {
                        GalleryButton(style, u, touches, onTooShort = { hint = Hint(HintTarget.GALLERY, HOLD_HINT) }) {
                            overlay = Overlay.Gallery
                        }
                    }
                }
                if (settings.videoUnlocked) {
                    RailItem(hint, HintTarget.MODE, right, u) {
                        ModeSwitch(
                            videoSelected = mode == CaptureMode.VIDEO, style = style, unit = u, touches = touches,
                            enabled = !recording, onTooShort = { hint = Hint(HintTarget.MODE, HOLD_HINT) },
                        ) {
                            onModeChange(if (mode == CaptureMode.PHOTO) CaptureMode.VIDEO else CaptureMode.PHOTO)
                        }
                    }
                }
                RailItem(hint, HintTarget.SHUTTER, right, u) {
                    val maxMs = settings.maxVideoMs()
                    Shutter(
                        skin = settings.skin, style = style, width = pillW, height = pillH,
                        progress = {
                            if (recordingStart != null) maxMs?.let { 1f - recordingElapsed / it.toFloat() } ?: 1f
                            else cooldown.value
                        },
                        ready = (ready && capturesLeft != 0) || recording,
                        emblem = when {
                            recording -> Emblem.STOP
                            mode == CaptureMode.VIDEO -> Emblem.RECORD
                            else -> Emblem.SKIN
                        },
                        spikesLeft = right,
                        capturesLeft = capturesLeft,
                        unit = u, touches = touches, onPress = ::onShutter,
                    )
                }
            }

            LockButton(
                style, u, touches, onOpenParent,
                Modifier
                    .align(if (right) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(u * 4f),
            )
        }

        if (flash.value > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = flash.value }.background(Color.White))
        }
        fly?.let { f -> FlyingSnapshot(f, flyTarget, style.frame, u) }

        when (val o = overlay) {
            Overlay.None -> Unit
            Overlay.Gallery -> GalleryOverlay(
                items, thumbs, settings.skin, style, u, touches,
                onOpen = { overlay = Overlay.Preview(it, fromGallery = true) },
                onClose = { overlay = Overlay.None },
            )
            is Overlay.Preview -> MediaPreview(
                o.item, settings.skin, style, u, touches,
                onClose = { overlay = if (o.fromGallery) Overlay.Gallery else Overlay.None },
            )
        }
    }
}

@Composable
private fun RailItem(hint: Hint?, target: HintTarget, right: Boolean, unit: Dp, content: @Composable () -> Unit) {
    Box {
        content()
        if (hint?.target == target) {
            HintBubble(
                hint.text, unit,
                Modifier
                    .align(if (right) Alignment.CenterStart else Alignment.CenterEnd)
                    .beside(onLeft = right, gap = unit * 3f),
            )
        }
    }
}

@Composable
private fun ThumbnailStrip(
    items: List<MediaItem>,
    thumbs: Thumbnails,
    style: SkinStyle,
    unit: Dp,
    touches: TouchCounter,
    count: Int,
    right: Boolean,
    portrait: Boolean,
    hiddenId: String?,
    onTargetPositioned: (LayoutCoordinates) -> Unit,
    onOpen: (MediaItem) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier) {
        val ratio = if (portrait) 1f else 4f / 3f
        val gap = unit * 1.8f
        val thumbH = maxHeight
        val thumbW = thumbH * ratio
        val fit = max(0, ((maxWidth + gap) / (thumbW + gap)).toInt())
        val shown = items.take(min(count, fit))
        Row(
            Modifier.align(if (right) Alignment.CenterEnd else Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (item in if (right) shown.asReversed() else shown) {
                key(item.id) {
                    val isTarget = item.id == hiddenId
                    Thumbnail(
                        item, thumbs, style, unit, touches, onOpen,
                        Modifier
                            .height(thumbH)
                            .aspectRatio(ratio)
                            .graphicsLayer { alpha = if (isTarget) 0f else 1f }
                            .then(if (isTarget) Modifier.onGloballyPositioned(onTargetPositioned) else Modifier),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingTimer(elapsedMs: Long, unit: Dp, modifier: Modifier) {
    Row(
        modifier
            .background(Color(0x99000000), CircleShape)
            .padding(horizontal = unit * 3.5f, vertical = unit * 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(unit * 2f),
    ) {
        val blinkOn = (elapsedMs / 500) % 2 == 0L
        Box(Modifier.size(unit * 3.4f).background(Color(0xFFFF4D5E).copy(alpha = if (blinkOn) 1f else 0.3f), CircleShape))
        BasicText(formatDuration(elapsedMs), style = kidText((unit * 6f).asSp()))
    }
}

/** The frozen frame flies from full screen into its slot in the strip, so kids see where it went. */
@Composable
private fun FlyingSnapshot(fly: Fly, target: Rect?, frame: Color, unit: Dp) {
    Canvas(Modifier.fillMaxSize()) {
        val end = target ?: return@Canvas
        val t = fly.anim.value
        val full = Rect(Offset.Zero, size)
        val r = lerp(full, end, t)
        val radius = lerp(0f, unit.toPx() * 3f, t)
        val clip = Path().apply { addRoundRect(RoundRect(r, CornerRadius(radius))) }
        val img = fly.bitmap
        val scale = max(r.width / img.width, r.height / img.height)
        val srcW = (r.width / scale).toInt().coerceIn(1, img.width)
        val srcH = (r.height / scale).toInt().coerceIn(1, img.height)
        clipPath(clip) {
            drawImage(
                img,
                srcOffset = IntOffset((img.width - srcW) / 2, (img.height - srcH) / 2), srcSize = IntSize(srcW, srcH),
                dstOffset = IntOffset(r.left.toInt(), r.top.toInt()), dstSize = IntSize(r.width.toInt(), r.height.toInt()),
            )
        }
        if (t > 0.5f) {
            drawRoundRect(
                frame.copy(alpha = (t - 0.5f) * 2f), r.topLeft, r.size, CornerRadius(radius),
                style = Stroke(unit.toPx() * 0.7f),
            )
        }
    }
}
