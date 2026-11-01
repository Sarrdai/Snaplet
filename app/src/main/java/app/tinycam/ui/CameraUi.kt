package app.tinycam.ui

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.tinycam.R
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
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import app.tinycam.data.Tokens
import app.tinycam.data.CaptureMode
import app.tinycam.data.MediaItem
import app.tinycam.data.Settings
import app.tinycam.data.Side
import app.tinycam.data.Skin
import app.tinycam.data.Thumbnails
import app.tinycam.data.cooldownMs
import app.tinycam.data.maxVideoMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class Captured(val item: MediaItem, val snapshot: ImageBitmap?)

/** What the UI needs from the camera. Implemented by CameraX in the app and by fakes in tests. */
interface CameraActions {
    fun takePhoto(effect: Effect): Captured?
    fun startVideo(): Boolean
    fun stopVideo(elapsedMs: Long): Captured?
    fun setZoom(linear: Float)
}

private enum class HintTarget { SHUTTER, MODE, GALLERY, SELFIE, EFFECTS, TIMER, SKINS }

private class Hint(val target: HintTarget, val kind: HintKind, @StringRes val text: Int)

private class Fly(val bitmap: ImageBitmap, val itemId: String) {
    val anim = Animatable(0f)
}

private sealed interface Overlay {
    data object None : Overlay
    data object Gallery : Overlay
    data object Effects : Overlay
    data object Skins : Overlay
    data class Preview(val item: MediaItem, val fromGallery: Boolean) : Overlay
}

/**
 * The kid-facing camera screen. Only the shutter, the thumbnail strip, the zoom knob and
 * hold-to-activate buttons react to touch; the live image itself is inert.
 *
 * @param filterPreview apply the effect's color filter to [preview] in Compose. The real camera
 *   filters its own view instead, so it passes false.
 */
@Composable
fun KidCameraUi(
    settings: Settings,
    items: List<MediaItem>,
    thumbs: Thumbnails,
    tokens: Tokens?,
    mode: CaptureMode,
    onModeChange: (CaptureMode) -> Unit,
    selfie: Boolean,
    onSelfieChange: (Boolean) -> Unit,
    effect: Effect,
    onEffectChange: (Effect) -> Unit,
    onSkinChange: (Skin) -> Unit,
    actions: CameraActions,
    onOpenParent: () -> Unit,
    modifier: Modifier = Modifier,
    filterPreview: Boolean = true,
    initialCooldown: Float = 1f,
    initialCountdownMs: Long? = null,
    preview: @Composable () -> Unit,
) {
    val touches = remember { TouchCounter() }
    val style = remember(settings.skin) { styleFor(settings.skin) }
    val scope = rememberCoroutineScope()

    val cooldown = remember { Animatable(initialCooldown) }
    val ready by remember { derivedStateOf { cooldown.value >= 1f } }
    val shake = remember { Animatable(0f) }
    var recordingStart by remember { mutableStateOf<Long?>(null) }
    var recordingElapsed by remember { mutableLongStateOf(0L) }
    var countdownEnd by remember { mutableStateOf(initialCountdownMs?.let { SystemClock.uptimeMillis() + it }) }
    var countdownLeft by remember { mutableLongStateOf(initialCountdownMs ?: 0L) }
    var timerOn by remember { mutableStateOf(initialCountdownMs != null) }
    var zoom by remember { mutableFloatStateOf(0f) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var hint by remember { mutableStateOf<Hint?>(null) }
    var fly by remember { mutableStateOf<Fly?>(null) }
    var flyTarget by remember { mutableStateOf<Rect?>(null) }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val recording = recordingStart != null
    val activeEffect = if (settings.effects && mode == CaptureMode.PHOTO) effect else Effect.NONE
    val timerActive = settings.timer && timerOn

    fun startCooldown() {
        val ms = settings.cooldownMs()
        if (ms <= 0) return
        scope.launch {
            cooldown.snapTo(0f)
            cooldown.animateTo(1f, tween(ms, easing = LinearEasing))
        }
    }

    // No white flash: the snapshot flying into the strip is feedback enough and gentler on kids' eyes
    fun showCaptured(c: Captured) {
        val bitmap = c.snapshot ?: return
        val f = Fly(bitmap, c.item.id)
        flyTarget = null
        fly = f
        scope.launch {
            f.anim.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
            if (fly === f) fly = null
        }
    }

    fun holdHint(target: HintTarget) {
        hint = Hint(target, HintKind.HOLD, R.string.hint_hold)
    }

    fun capture() {
        if (mode == CaptureMode.PHOTO) {
            actions.takePhoto(activeEffect)?.let {
                showCaptured(it)
                startCooldown()
            }
        } else if (actions.startVideo()) {
            recordingElapsed = 0
            recordingStart = SystemClock.uptimeMillis()
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
            countdownEnd != null -> countdownEnd = null // tapping again cancels the self-timer
            !ready -> Unit
            tokens?.filled == 0 -> {
                hint = Hint(HintTarget.SHUTTER, HintKind.EMPTY, R.string.see_you_tomorrow)
                scope.launch { for (x in listOf(-2.5f, 2.5f, -2f, 2f, -1f, 0f)) shake.animateTo(x, tween(45)) }
            }
            timerActive -> {
                countdownLeft = settings.timerSec * 1000L
                countdownEnd = now + countdownLeft
            }
            else -> capture()
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
    LaunchedEffect(countdownEnd) {
        val end = countdownEnd ?: return@LaunchedEffect
        while (true) {
            countdownLeft = end - SystemClock.uptimeMillis()
            if (countdownLeft <= 0) {
                countdownEnd = null
                capture()
                break
            }
            delay(30)
        }
    }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(2200)
            hint = null
        }
    }
    // A new lens starts wide; a disabled zoom must not stay zoomed in
    LaunchedEffect(selfie, settings.zoom) {
        zoom = 0f
        actions.setZoom(0f)
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
        val center = settings.side == Side.CENTER
        // Centered layout keeps its tools on the right, so it behaves like a right-hand layout otherwise
        val right = settings.side != Side.LEFT
        val roundD = minOf(u * 30f, screenH * 0.32f)
        val edge = u * 6.5f
        val railGap = u * 3f
        val railBottom = screenH * 0.07f
        // The mode switch is stacked above the shutter and must still fit on short landscape screens
        val aboveShutter = if (settings.video) u * 25f + railGap else 0.dp
        val pillH = minOf(
            if (portrait) screenH * 0.3f else screenH * 0.5f,
            pillW * 4.4f,
            screenH - railBottom - aboveShutter - u * 6f,
        )
        val outer = if (right) Alignment.BottomEnd else Alignment.BottomStart
        val opposite = if (right) Alignment.BottomStart else Alignment.BottomEnd
        fun Modifier.fromSide(sideGap: Dp, bottom: Dp, onShutterSide: Boolean = true) =
            if (right == onShutterSide) padding(end = sideGap, bottom = bottom) else padding(start = sideGap, bottom = bottom)

        val matrix = activeEffect.matrix
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (filterPreview && matrix != null) {
                        Modifier.drawWithContent { withEffectColors(activeEffect) { this@drawWithContent.drawContent() } }
                    } else {
                        Modifier
                    },
                ),
        ) { preview() }
        activeEffect.frame?.let { frame -> Canvas(Modifier.fillMaxSize()) { drawFrame(frame) } }

        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Thumbnail strip, newest next to the shutter
            val reserve = if (portrait || center) u * 4f else edge + pillW + u * 5f
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

            if (settings.zoom) {
                ZoomSlider(
                    zoom, { zoom = it; actions.setZoom(it) }, style, u,
                    if (center) minOf(screenH * 0.3f, u * 45f) else minOf(pillH, screenH * 0.42f), touches,
                    Modifier.align(opposite).fromSide(u * 5f, maxOf(railBottom, u * 18f), onShutterSide = false),
                )
            }

            val tools = buildList<@Composable () -> Unit> {
                if (settings.gallery && !recording) add {
                    HintAnchor(hint, HintTarget.GALLERY, right, u) {
                        ToolButton(Icons.Grid, stringResource(R.string.feature_gallery), false, style, u, touches, { holdHint(HintTarget.GALLERY) }) {
                            overlay = Overlay.Gallery
                        }
                    }
                }
                if (settings.selfie && !recording) add {
                    HintAnchor(hint, HintTarget.SELFIE, right, u) {
                        ToolButton(Icons.Flip, stringResource(R.string.feature_selfie), selfie, style, u, touches, { holdHint(HintTarget.SELFIE) }) {
                            onSelfieChange(!selfie)
                        }
                    }
                }
                if (settings.effects && mode == CaptureMode.PHOTO) add {
                    HintAnchor(hint, HintTarget.EFFECTS, right, u) {
                        ToolButton(Icons.Wand, stringResource(R.string.feature_effects), effect != Effect.NONE, style, u, touches, { holdHint(HintTarget.EFFECTS) }) {
                            overlay = Overlay.Effects
                        }
                    }
                }
                if (settings.timer && !recording) add {
                    HintAnchor(hint, HintTarget.TIMER, right, u) {
                        ToolButton(Icons.Timer, stringResource(R.string.feature_timer), timerOn, style, u, touches, { holdHint(HintTarget.TIMER) }) {
                            timerOn = !timerOn
                        }
                    }
                }
                if (settings.skinChoice && !recording) add {
                    HintAnchor(hint, HintTarget.SKINS, right, u) {
                        ToolButton(Icons.Palette, stringResource(R.string.feature_skins), false, style, u, touches, { holdHint(HintTarget.SKINS) }) {
                            overlay = Overlay.Skins
                        }
                    }
                }
            }
            val modeSwitch: @Composable () -> Unit = {
                if (settings.video) {
                    HintAnchor(hint, HintTarget.MODE, right, u) {
                        ModeSwitch(
                            videoSelected = mode == CaptureMode.VIDEO, style = style, unit = u, touches = touches,
                            enabled = !recording && countdownEnd == null, onTooShort = { holdHint(HintTarget.MODE) },
                        ) {
                            onModeChange(if (mode == CaptureMode.PHOTO) CaptureMode.VIDEO else CaptureMode.PHOTO)
                        }
                    }
                }
            }
            val shutter: @Composable () -> Unit = {
                HintAnchor(hint, HintTarget.SHUTTER, right, u) {
                    val maxMs = settings.maxVideoMs()
                    Shutter(
                        skin = settings.skin, style = style,
                        width = if (center) roundD else pillW, height = if (center) roundD else pillH,
                        progress = {
                            if (recordingStart != null) maxMs?.let { 1f - recordingElapsed / it.toFloat() } ?: 1f
                            else cooldown.value
                        },
                        ready = (ready && tokens?.filled != 0) || recording || countdownEnd != null,
                        emblem = when {
                            recording -> Emblem.STOP
                            mode == CaptureMode.VIDEO -> Emblem.RECORD
                            else -> Emblem.SKIN
                        },
                        spikesLeft = right,
                        tokens = tokens,
                        // A lambda, not ::onShutter: Compose memoizes local function references, which kept the
                        // first mode and settings, so the shutter took photos in video mode
                        unit = u, touches = touches, onPress = { onShutter() },
                        shake = { shake.value },
                        round = center,
                    )
                }
            }

            // Tools stay below the thumbnail strip; on short screens they spread into more columns
            val toolsPerColumn = ((screenH - railBottom - u * 24f + railGap) / (u * TOOL_SIZE + railGap)).toInt().coerceAtLeast(1)

            if (center) {
                // Portrait has room to stack everything in one column; landscape needs two
                val corner = Modifier.align(Alignment.BottomEnd).padding(end = u * 5f, bottom = railBottom)
                if (portrait) {
                    Column(corner, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(railGap)) {
                        modeSwitch()
                        tools.forEach { it() }
                    }
                } else {
                    Row(corner, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(railGap)) {
                        modeSwitch()
                        ToolColumns(tools, toolsPerColumn, towardsEnd = false, railGap)
                    }
                }
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = railBottom)) { shutter() }
            } else {
                // Tool column next to the shutter; leaves room for the dino plates in between
                ToolColumns(
                    tools, toolsPerColumn, towardsEnd = !right, railGap,
                    Modifier.align(outer).fromSide(edge + pillW + u * 9f, railBottom),
                )
                // Drawn after the tools so its hints appear on top of them
                Column(
                    Modifier.align(outer).fromSide(edge, railBottom),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(railGap),
                ) {
                    modeSwitch()
                    shutter()
                }
            }

            LockButton(
                style, u, touches, onOpenParent,
                Modifier
                    .align(opposite)
                    .padding(u * 4f),
            )
        }

        if (countdownEnd != null) {
            Countdown(countdownLeft, settings.timerSec, style, u, Modifier.align(Alignment.Center))
        }

        fly?.let { f -> FlyingSnapshot(f, flyTarget, style.frame, u) }

        when (val o = overlay) {
            Overlay.None -> Unit
            Overlay.Gallery -> GalleryOverlay(
                items, thumbs, settings.skin, style, u, touches,
                onOpen = { overlay = Overlay.Preview(it, fromGallery = true) },
                onClose = { overlay = Overlay.None },
            )
            Overlay.Effects -> EffectPicker(
                effect, style, u, touches,
                onSelect = onEffectChange,
                onClose = { overlay = Overlay.None },
            )
            Overlay.Skins -> SkinChooser(
                Skin.entries.filter { it in settings.kidSkins }, settings.skin, settings.side == Side.CENTER, style, u, touches,
                onSelect = onSkinChange,
                onClose = { overlay = Overlay.None },
            )
            is Overlay.Preview -> MediaPreview(
                o.item, thumbs.revision, settings.skin, style, u, touches,
                onClose = { overlay = if (o.fromGallery) Overlay.Gallery else Overlay.None },
            )
        }
    }
}

@Composable
private fun HintAnchor(hint: Hint?, target: HintTarget, right: Boolean, unit: Dp, content: @Composable () -> Unit) {
    Box {
        content()
        if (hint?.target == target) {
            HintBubble(
                hint.kind, stringResource(hint.text), unit,
                Modifier
                    .align(if (right) Alignment.CenterStart else Alignment.CenterEnd)
                    .beside(onLeft = right, gap = unit * 3f),
            )
        }
    }
}

/** [tools] in bottom-aligned columns of at most [perColumn]; the first column sits nearest the shutter. */
@Composable
private fun ToolColumns(tools: List<@Composable () -> Unit>, perColumn: Int, towardsEnd: Boolean, gap: Dp, modifier: Modifier = Modifier) {
    val columns = tools.chunked(perColumn)
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (column in if (towardsEnd) columns else columns.asReversed()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
                column.forEach { it() }
            }
        }
    }
}

/** Big self-timer: a number for those who can read it, a draining ring and dots for everyone else. */
@Composable
private fun Countdown(leftMs: Long, totalSec: Int, style: SkinStyle, unit: Dp, modifier: Modifier) {
    val secs = ((leftMs + 999) / 1000).toInt().coerceAtLeast(1)
    val fraction = (leftMs / (totalSec * 1000f)).coerceIn(0f, 1f)
    // The number pops at the start of every second
    val pop = 1f + 0.18f * ((leftMs % 1000) / 1000f).let { it * it * it }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(unit * 4f)) {
        Box(
            Modifier
                .size(unit * 50f)
                .drawBehind {
                    val sw = size.minDimension * 0.08f
                    drawCircle(Color(0x73000000))
                    drawArc(
                        style.accent, -90f, 360f * fraction, false,
                        topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw),
                        style = Stroke(sw, cap = StrokeCap.Round),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                secs.toString(),
                Modifier.graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                },
                style = kidText((unit * 28f).asSp(), weight = FontWeight.ExtraBold)
                    .copy(shadow = Shadow(Color(0x66000000), Offset(0f, 6f), 12f)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(unit * 2.5f)) {
            repeat(totalSec) { i ->
                Box(
                    Modifier
                        .size(unit * 5f)
                        .shadow(3.dp, CircleShape)
                        .background(if (i < secs) style.accent else Color(0x66FFFFFF), CircleShape),
                )
            }
        }
    }
}

/** 3x3 grid of looks; each bubble previews its effect on a tiny landscape. */
@Composable
internal fun EffectPicker(
    selected: Effect,
    style: SkinStyle,
    unit: Dp,
    touches: TouchCounter,
    onSelect: (Effect) -> Unit,
    onClose: () -> Unit,
) {
    val back = stringResource(R.string.cd_back)
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).singleTap(touches, role = null, onTap = onClose)
            .semantics { contentDescription = back },
        contentAlignment = Alignment.Center,
    ) {
        val panel = RoundedCornerShape(unit * 7f)
        Column(
            Modifier
                .shadow(16.dp, panel)
                .background(style.button, panel)
                .border(1.dp, Color.White.copy(alpha = 0.25f), panel)
                .padding(unit * 4f),
            verticalArrangement = Arrangement.spacedBy(unit * 3f),
        ) {
            Effect.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(unit * 3f)) {
                    row.forEach { e ->
                        val shape = RoundedCornerShape(unit * 5f)
                        val on = e == selected
                        val name = stringResource(e.label)
                        Canvas(
                            Modifier
                                .size(unit * 22f)
                                .shadow(if (on) 10.dp else 3.dp, shape)
                                .clip(shape)
                                .border(if (on) unit * 1.4f else unit * 0.4f, if (on) style.buttonOn else Color.White.copy(alpha = 0.4f), shape)
                                .singleTap(touches, Role.RadioButton) { onSelect(e) }
                                .semantics {
                                    contentDescription = name
                                    this.selected = on
                                },
                        ) {
                            withEffectColors(e) { drawMiniScene() }
                            e.frame?.let { drawFrame(it) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The skins parents offered, each previewed as its shutter. A tap restyles the whole camera
 * right away, so the child sees the result behind the panel before closing it.
 */
@Composable
internal fun SkinChooser(
    skins: List<Skin>,
    selected: Skin,
    round: Boolean,
    style: SkinStyle,
    unit: Dp,
    touches: TouchCounter,
    onSelect: (Skin) -> Unit,
    onClose: () -> Unit,
) {
    val back = stringResource(R.string.cd_back)
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color(0x99000000)).singleTap(touches, role = null, onTap = onClose)
            .semantics { contentDescription = back },
        contentAlignment = Alignment.Center,
    ) {
        // Tall pill previews only fit two rows on a landscape screen
        val columns = if (maxHeight > maxWidth) 3 else 6
        val panel = RoundedCornerShape(unit * 7f)
        Column(
            Modifier
                .shadow(16.dp, panel)
                .background(style.button, panel)
                .border(1.dp, Color.White.copy(alpha = 0.25f), panel)
                .padding(unit * 4f),
            verticalArrangement = Arrangement.spacedBy(unit * 3f),
        ) {
            skins.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(unit * 3f)) {
                    row.forEach { skin ->
                        val shape = RoundedCornerShape(unit * 5f)
                        val on = skin == selected
                        val skinStyle = remember(skin) { styleFor(skin) }
                        val name = stringResource(skin.label)
                        Canvas(
                            Modifier
                                .size(unit * 22f, unit * if (round) 22f else 34f)
                                .shadow(if (on) 10.dp else 3.dp, shape)
                                .clip(shape)
                                .background(Color(0x33FFFFFF))
                                .border(if (on) unit * 1.4f else unit * 0.4f, if (on) style.buttonOn else Color.White.copy(alpha = 0.4f), shape)
                                .singleTap(touches, Role.RadioButton) { onSelect(skin) }
                                .semantics {
                                    contentDescription = name
                                    this.selected = on
                                },
                        ) {
                            if (round) {
                                inset(size.width * 0.14f) { drawRoundShutter(skin, skinStyle, 1f, 0f, 1f, Emblem.SKIN) }
                            } else {
                                inset(size.width * 0.27f, size.height * 0.06f) {
                                    drawShutter(skin, skinStyle, 1f, 0f, 1f, Emblem.SKIN, spikesLeft = true)
                                }
                            }
                        }
                    }
                }
            }
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
