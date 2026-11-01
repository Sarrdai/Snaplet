package app.tinycam.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import app.tinycam.R
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.tinycam.data.MediaItem
import app.tinycam.data.Thumbnails
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tinycam.data.DailyUsage
import app.tinycam.data.Settings
import app.tinycam.data.Side
import android.os.SystemClock
import android.widget.VideoView
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import app.tinycam.data.ParentLock
import app.tinycam.data.loadFullImage
import kotlinx.coroutines.delay
import app.tinycam.data.PhotoAspect
import app.tinycam.data.PhotoSize
import app.tinycam.data.Skin
import app.tinycam.data.VideoQuality
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt

private val Bg = Color(0xFF12151F)
private val Card = Color(0xFF1C2130)
private val Line = Color(0xFF2B3245)
private val Text = Color(0xFFEEF0F6)
private val Muted = Color(0xFF9AA3B8)
private val Accent = Color(0xFFFFC93C)

private val Title = TextStyle(fontFamily = KidFont, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = Text)
private val Heading = TextStyle(fontFamily = KidFont, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Text)
private val Body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, color = Muted)
private val Chip = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

private enum class FeatureId { VIDEO, GALLERY, SELFIE, ZOOM, EFFECTS, TIMER, SKINS }

private class Feature(
    val id: FeatureId,
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val info: Int,
    val get: (Settings) -> Boolean,
    val set: (Settings, Boolean) -> Settings,
)

private val Features = listOf(
    Feature(FeatureId.VIDEO, Icons.Video, R.string.feature_video, R.string.feature_video_info, { it.video }, { s, v -> s.copy(video = v) }),
    Feature(FeatureId.GALLERY, Icons.Grid, R.string.feature_gallery, R.string.feature_gallery_info, { it.gallery }, { s, v -> s.copy(gallery = v) }),
    Feature(FeatureId.SELFIE, Icons.Flip, R.string.feature_selfie, R.string.feature_selfie_info, { it.selfie }, { s, v -> s.copy(selfie = v) }),
    Feature(FeatureId.ZOOM, Icons.Search, R.string.feature_zoom, R.string.feature_zoom_info, { it.zoom }, { s, v -> s.copy(zoom = v) }),
    Feature(FeatureId.EFFECTS, Icons.Wand, R.string.feature_effects, R.string.feature_effects_info, { it.effects }, { s, v -> s.copy(effects = v) }),
    Feature(FeatureId.TIMER, Icons.Timer, R.string.feature_timer, R.string.feature_timer_info, { it.timer }, { s, v -> s.copy(timer = v) }),
    Feature(FeatureId.SKINS, Icons.Palette, R.string.feature_skins, R.string.feature_skins_info, { it.skinChoice }, { s, v -> s.copy(skinChoice = v) }),
)

@Composable
fun ParentScreen(
    settings: Settings,
    usage: DailyUsage,
    items: List<MediaItem>,
    thumbs: Thumbnails,
    pinned: Boolean,
    deviceSecure: Boolean,
    hasPin: Boolean,
    onChange: (Settings) -> Unit,
    onSetPin: (String) -> Unit,
    onResetToday: () -> Unit,
    onShare: (List<MediaItem>) -> Unit,
    onDelete: (List<MediaItem>) -> Unit,
    onTogglePin: () -> Unit,
    onClose: () -> Unit,
    initialGalleryOpen: Boolean = false,
    /** Readable name of the export folder, null if none is set or access was lost. */
    exportFolder: String? = null,
    exportFailed: Boolean = false,
    onChooseExportFolder: () -> Unit = {},
) {
    val touches = remember { TouchCounter() }
    var galleryOpen by remember { mutableStateOf(initialGalleryOpen) }
    var pinSetup by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    if (galleryOpen) {
        ParentGallery(items, thumbs, onShare, onDelete, onClose = { galleryOpen = false })
        return
    }
    if (pinSetup) {
        PinSetup(
            onDone = { pin ->
                onSetPin(pin)
                onChange(settings.copy(parentLock = ParentLock.PIN))
                pinSetup = false
            },
            onCancel = { pinSetup = false },
        )
        return
    }
    Box(Modifier.fillMaxSize().background(Bg).trackTouches(touches)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(stringResource(R.string.parent_title), Modifier.weight(1f), style = Title)
                Button(stringResource(R.string.parent_done), primary = true, touches = touches, onClick = onClose)
            }

            Section(stringResource(R.string.section_design)) {
                SkinPicker(settings.skin, settings.side == Side.CENTER, touches) { onChange(settings.copy(skin = it)) }
            }
            Section(stringResource(R.string.section_side), stringResource(R.string.section_side_info)) {
                Options(Side.entries, settings.side, { it.label }, touches) { onChange(settings.copy(side = it)) }
            }
            Section(stringResource(R.string.section_features), stringResource(R.string.section_features_info)) {
                Features.forEach { f ->
                    val on = f.get(settings)
                    SwitchRow(stringResource(f.title), stringResource(f.info), on, touches, f.icon) { onChange(f.set(settings, it)) }
                    if (on && f.id == FeatureId.VIDEO) {
                        Indented {
                            SwitchRow(stringResource(R.string.video_limit), null, settings.videoLimitOn, touches) {
                                onChange(settings.copy(videoLimitOn = it))
                            }
                            if (settings.videoLimitOn) {
                                ValueSlider(settings.maxVideoSec, 5..180, 5, { stringResource(R.string.unit_seconds, it) }) { onChange(settings.copy(maxVideoSec = it)) }
                            }
                        }
                    }
                    if (on && f.id == FeatureId.TIMER) {
                        Indented {
                            BasicText(stringResource(R.string.countdown), style = Chip.copy(color = Text))
                            ValueSlider(settings.timerSec, 2..15, 1, { stringResource(R.string.unit_seconds, it) }) { onChange(settings.copy(timerSec = it)) }
                        }
                    }
                    if (on && f.id == FeatureId.SKINS) {
                        Indented {
                            BasicText(stringResource(R.string.kid_skins), style = Chip.copy(color = Text))
                            // At least one design always stays offered
                            MultiOptions(Skin.entries, settings.kidSkins, { it.label }, touches) { skin ->
                                val next = if (skin in settings.kidSkins) settings.kidSkins - skin else settings.kidSkins + skin
                                if (next.isNotEmpty()) onChange(settings.copy(kidSkins = next))
                            }
                        }
                    }
                }
            }
            Section(stringResource(R.string.section_limits)) {
                SwitchRow(stringResource(R.string.cooldown), stringResource(R.string.cooldown_info), settings.cooldownOn, touches) {
                    onChange(settings.copy(cooldownOn = it))
                }
                if (settings.cooldownOn) {
                    ValueSlider(settings.cooldownSec, 1..10, 1, { stringResource(R.string.unit_seconds, it) }) { onChange(settings.copy(cooldownSec = it)) }
                }
                SwitchRow(
                    stringResource(R.string.daily_captures),
                    stringResource(R.string.daily_captures_info, usage.captures),
                    settings.captureLimitOn, touches,
                ) { onChange(settings.copy(captureLimitOn = it)) }
                if (settings.captureLimitOn) {
                    ValueSlider(settings.dailyCaptures, 1..100, 1, { "$it" }) { onChange(settings.copy(dailyCaptures = it)) }
                }
                SwitchRow(
                    stringResource(R.string.daily_time),
                    stringResource(R.string.daily_time_info, (usage.usageMs / 60_000).toInt()),
                    settings.timeLimitOn, touches,
                ) { onChange(settings.copy(timeLimitOn = it)) }
                if (settings.timeLimitOn) {
                    ValueSlider(settings.dailyMinutes, 5..180, 5, { stringResource(R.string.unit_minutes, it) }) { onChange(settings.copy(dailyMinutes = it)) }
                }
                Button(stringResource(R.string.reset_today), touches = touches, onClick = onResetToday)
            }
            Section(stringResource(R.string.section_strip), stringResource(R.string.section_strip_info)) {
                ValueSlider(settings.stripCount, 1..8, 1, { pluralStringResource(R.plurals.strip_count, it, it) }) { onChange(settings.copy(stripCount = it)) }
            }
            Section(stringResource(R.string.section_photo_video), stringResource(R.string.section_photo_video_info)) {
                BasicText(stringResource(R.string.photo_aspect), style = Chip.copy(color = Text))
                Options(PhotoAspect.entries, settings.photoAspect, { it.label }, touches) { onChange(settings.copy(photoAspect = it)) }
                BasicText(stringResource(R.string.photo_size), style = Chip.copy(color = Text))
                Options(PhotoSize.entries, settings.photoSize, { it.label }, touches) { onChange(settings.copy(photoSize = it)) }
                BasicText(stringResource(R.string.video_quality), style = Chip.copy(color = Text))
                Options(VideoQuality.entries, settings.videoQuality, { it.label }, touches) { onChange(settings.copy(videoQuality = it)) }
            }
            Section(
                stringResource(R.string.section_media),
                stringResource(R.string.section_media_info, items.size),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(stringResource(R.string.media_view), primary = true, touches = touches) { galleryOpen = true }
                    Button(stringResource(R.string.media_share_all), touches = touches) { onShare(items.toList()) }
                    HoldButton(stringResource(R.string.media_delete_all), touches) { onDelete(items.toList()) }
                }
            }
            Section(stringResource(R.string.section_export), stringResource(R.string.section_export_info)) {
                SwitchRow(
                    stringResource(R.string.export_auto),
                    exportFolder?.let { stringResource(R.string.export_folder, it) },
                    settings.autoExport, touches,
                ) { on -> if (on && exportFolder == null) onChooseExportFolder() else onChange(settings.copy(autoExport = on)) }
                if (exportFailed && settings.autoExport) {
                    BasicText(stringResource(R.string.export_failed), style = Body.copy(color = Accent))
                }
                Button(stringResource(R.string.export_choose_folder), touches = touches, onClick = onChooseExportFolder)
            }
            Section(stringResource(R.string.section_access), stringResource(settings.parentLock.info)) {
                Options(ParentLock.entries, settings.parentLock, { it.label }, touches) { lock ->
                    when {
                        lock == ParentLock.PIN && !hasPin -> pinSetup = true
                        lock == ParentLock.DEVICE && !deviceSecure -> Unit
                        else -> onChange(settings.copy(parentLock = lock))
                    }
                }
                if (!deviceSecure) {
                    BasicText(
                        stringResource(R.string.no_screen_lock),
                        style = Body.copy(color = Accent),
                    )
                }
                if (settings.parentLock == ParentLock.PIN) {
                    Button(stringResource(R.string.pin_change), touches = touches) { pinSetup = true }
                }
            }
            Section(
                stringResource(R.string.section_pinning),
                stringResource(if (pinned) R.string.pinning_on_info else R.string.pinning_off_info, stringResource(R.string.app_name)),
            ) {
                Button(stringResource(if (pinned) R.string.pinning_unpin else R.string.pinning_pin), primary = !pinned, touches = touches, onClick = onTogglePin)
                SwitchRow(
                    stringResource(R.string.auto_pin),
                    stringResource(R.string.auto_pin_info),
                    settings.autoPin, touches,
                ) { onChange(settings.copy(autoPin = it)) }
            }
        }
    }
}

@Composable
private fun Indented(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .background(Bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

/** Parent-area slider: tap or drag anywhere on the track; vertical scrolling still works. */
@Composable
private fun ValueSlider(value: Int, range: IntRange, step: Int, label: @Composable (Int) -> String, onChange: (Int) -> Unit) {
    val change by rememberUpdatedState(onChange)
    val current by rememberUpdatedState(value)
    val text = label(value)
    // Right-to-left languages fill the track from the right
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    fun snap(raw: Float) = (range.first + ((raw - range.first) / step).roundToInt() * step).coerceIn(range)
    fun valueAt(x: Float, width: Int): Int {
        val f = (x / width).coerceIn(0f, 1f)
        return snap(range.first + (if (rtl) 1f - f else f) * (range.last - range.first))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(
            Modifier
                .weight(1f)
                .height(40.dp)
                .semantics {
                    stateDescription = text
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        value.toFloat(), range.first.toFloat()..range.last.toFloat(), (range.last - range.first) / step - 1,
                    )
                    setProgress { v ->
                        val next = snap(v)
                        if (next != current) change(next)
                        true
                    }
                }
                .pointerInput(range, step, rtl) {
                    detectTapGestures { change(valueAt(it.x, size.width)) }
                }
                .pointerInput(range, step, rtl) {
                    detectHorizontalDragGestures { c, _ ->
                        c.consume()
                        val v = valueAt(c.position.x, size.width)
                        if (v != current) change(v)
                    }
                },
        ) {
            val f = (value - range.first) / (range.last - range.first).toFloat()
            val y = size.height / 2
            val track = 6.dp.toPx()
            val knob = 12.dp.toPx()
            val x = knob + (if (rtl) 1f - f else f) * (size.width - knob * 2)
            val start = if (rtl) size.width - knob else knob
            drawLine(Line, Offset(knob, y), Offset(size.width - knob, y), track, StrokeCap.Round)
            drawLine(Accent, Offset(start, y), Offset(x, y), track, StrokeCap.Round)
            drawCircle(Accent, knob, Offset(x, y))
            drawCircle(Ink, knob * 0.35f, Offset(x, y))
        }
        BasicText(text, Modifier.widthIn(min = 76.dp), style = Chip.copy(color = Text, textAlign = TextAlign.End))
    }
}

@Composable
private fun Section(title: String, info: String? = null, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(title, style = Heading)
        if (info != null) BasicText(info, style = Body)
        content()
    }
}

@Composable
private fun <T> Options(values: List<T>, selected: T, label: (T) -> Int, touches: TouchCounter, onSelect: (T) -> Unit) =
    MultiOptions(values, setOf(selected), label, touches, single = true, onToggle = onSelect)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiOptions(
    values: List<T>,
    selected: Set<T>,
    label: (T) -> Int,
    touches: TouchCounter,
    single: Boolean = false,
    onToggle: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            val on = v in selected
            BasicText(
                stringResource(label(v)),
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Accent else Bg)
                    .border(1.dp, if (on) Accent else Line, RoundedCornerShape(50))
                    .singleTap(touches, if (single) Role.RadioButton else Role.Checkbox) { onToggle(v) }
                    .semantics { if (single) { this.selected = on } else { toggleableState = ToggleableState(on) } }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = Chip.copy(color = if (on) Ink else Text),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    info: String?,
    on: Boolean,
    touches: TouchCounter,
    icon: ImageVector? = null,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .singleTap(touches, Role.Switch) { onToggle(!on) }
            .semantics { toggleableState = ToggleableState(on) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier.size(42.dp).background(if (on) Accent else Line, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(icon, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(if (on) Ink else Text))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(title, style = Chip.copy(color = Text))
            if (info != null) BasicText(info, style = Body.copy(fontSize = 13.sp, lineHeight = 18.sp))
        }
        Switch(on)
    }
}

@Composable
private fun Switch(on: Boolean) {
    val x by animateDpAsState(if (on) 22.dp else 0.dp, label = "switch")
    Box(
        Modifier
            .size(width = 50.dp, height = 28.dp)
            .background(if (on) Accent else Line, RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        Box(Modifier.offset(x = x).size(22.dp).background(if (on) Ink else Muted, CircleShape))
    }
}

@Composable
private fun Button(text: String, primary: Boolean = false, touches: TouchCounter, onClick: () -> Unit) {
    BasicText(
        text,
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) Accent else Line)
            .singleTap(touches, onTap = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        style = Chip.copy(color = if (primary) Ink else Text),
    )
}

@Composable
private fun HoldButton(text: String, touches: TouchCounter, onComplete: () -> Unit) {
    val hold = remember { HoldState() }
    BasicText(
        text,
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF4A1D24))
            .drawBehind {
                drawRect(Color(0xFFE5484D), size = Size(size.width * hold.progress, size.height))
            }
            .holdToActivate(hold, 3000, touches, onComplete = onComplete)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        style = Chip.copy(color = Color(0xFFFFD6D9)),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkinPicker(selected: Skin, round: Boolean, touches: TouchCounter, onSelect: (Skin) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Skin.entries.forEach { skin ->
            val on = skin == selected
            val style = remember(skin) { styleFor(skin) }
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Bg)
                    .border(if (on) 2.5.dp else 1.dp, if (on) Accent else Line, RoundedCornerShape(16.dp))
                    .singleTap(touches, Role.RadioButton) { onSelect(skin) }
                    .semantics { this.selected = on }
                    .width(112.dp)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Canvas(Modifier.size(width = 56.dp, height = 110.dp)) {
                    if (round) {
                        inset(left = size.width * 0.1f, top = size.height * 0.3f, right = size.width * 0.1f, bottom = size.height * 0.3f) {
                            drawRoundShutter(skin, style, 1f, 0f, 1f, Emblem.SKIN)
                        }
                    } else {
                        inset(left = size.width * 0.25f, top = 0f, right = size.width * 0.25f, bottom = size.height * 0.08f) {
                            drawShutter(skin, style, 1f, 0f, 1f, Emblem.SKIN, spikesLeft = true)
                        }
                    }
                }
                BasicText(stringResource(skin.label), style = Chip.copy(color = if (on) Accent else Text))
            }
        }
    }
}

/** Pick single photos and videos to share or delete. Tap opens the large view, the circle selects. */
@Composable
private fun ParentGallery(
    items: List<MediaItem>,
    thumbs: Thumbnails,
    onShare: (List<MediaItem>) -> Unit,
    onDelete: (List<MediaItem>) -> Unit,
    onClose: () -> Unit,
) {
    val touches = remember { TouchCounter() }
    val selected = remember { mutableStateListOf<String>() }
    var viewing by remember { mutableStateOf<Int?>(null) }
    val style = remember { styleFor(Skin.CLASSIC) }
    // Forget selections of items that were deleted meanwhile
    selected.retainAll(items.map { it.id }.toSet())
    val chosen = items.filter { it.id in selected }
    val allSelected = items.isNotEmpty() && chosen.size == items.size

    fun toggle(item: MediaItem) {
        if (!selected.remove(item.id)) selected.add(item.id)
    }

    val start = viewing
    if (start != null && items.isNotEmpty()) {
        ParentViewer(items, thumbs.revision, start.coerceIn(items.indices), selected, ::toggle) { viewing = null }
        return
    }
    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize().background(Bg).trackTouches(touches).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(stringResource(R.string.back), touches = touches, onClick = onClose)
            BasicText(
                if (chosen.isEmpty()) stringResource(R.string.section_media) else stringResource(R.string.selected_count, chosen.size),
                Modifier.weight(1f), style = Heading,
            )
            Button(stringResource(if (allSelected) R.string.select_none else R.string.select_all), touches = touches) {
                if (allSelected) selected.clear() else selected.addAll(items.map { it.id }.filterNot { it in selected })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (items.isEmpty()) {
                BasicText(stringResource(R.string.no_media), Modifier.align(Alignment.Center), style = Body)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(104.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items.size, key = { items[it].id }) { index ->
                    val item = items[index]
                    val on = item.id in selected
                    val shape = RoundedCornerShape(12.dp)
                    Box(Modifier.aspectRatio(1f)) {
                        Thumbnail(item, thumbs, style, 3.4.dp, touches, { viewing = index }, Modifier.fillMaxSize())
                        if (on) Box(Modifier.fillMaxSize().border(3.dp, Accent, shape).background(Color(0x33000000), shape))
                        SelectCircle(on, touches, Modifier.align(Alignment.TopEnd)) { toggle(item) }
                    }
                }
            }
        }
        SelectionBar(chosen, touches, onShare) {
            onDelete(chosen)
            selected.clear()
        }
    }
}

/** Generous touch target around the visible circle so selecting is easy. */
@Composable
private fun SelectCircle(on: Boolean, touches: TouchCounter, modifier: Modifier, onToggle: () -> Unit) {
    val label = stringResource(R.string.select)
    Box(
        modifier
            .size(48.dp)
            .singleTap(touches, Role.Checkbox, onTap = onToggle)
            .semantics {
                contentDescription = label
                toggleableState = ToggleableState(on)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(if (on) Accent else Color(0x59000000), CircleShape)
                .border(2.dp, if (on) Accent else Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (on) Image(Icons.Check, null, Modifier.size(19.dp), colorFilter = ColorFilter.tint(Ink))
        }
    }
}

@Composable
private fun SelectionBar(chosen: List<MediaItem>, touches: TouchCounter, onShare: (List<MediaItem>) -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Card).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(stringResource(R.string.share_count, chosen.size), primary = chosen.isNotEmpty(), touches = touches) {
            if (chosen.isNotEmpty()) onShare(chosen)
        }
        HoldButton(stringResource(R.string.delete_count, chosen.size), touches) {
            if (chosen.isNotEmpty()) onDelete()
        }
    }
}

/** Full-screen view for picking: swipe between photos and videos, select the current one, compare filter and original. */
@Composable
private fun ParentViewer(
    items: List<MediaItem>,
    revision: Int,
    start: Int,
    selected: List<String>,
    onToggle: (MediaItem) -> Unit,
    onClose: () -> Unit,
) {
    val touches = remember { TouchCounter() }
    val pager = rememberPagerState(start) { items.size }
    var showOriginal by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    val maxPx = with(LocalDensity.current) {
        val c = LocalConfiguration.current
        maxOf(c.screenWidthDp, c.screenHeightDp).dp.roundToPx()
    }
    Box(Modifier.fillMaxSize().background(Color.Black).trackTouches(touches)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { items[it].id }) { page ->
            val item = items[page]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (item.isVideo) {
                    if (pager.currentPage == page) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(item.file.path)
                                    setOnPreparedListener { it.isLooping = true; it.start() }
                                }
                            },
                            onRelease = { it.stopPlayback() },
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        )
                    }
                } else {
                    val file = if (showOriginal && item.original.exists()) item.original else item.file
                    val image by produceState<ImageBitmap?>(null, file, revision) { value = loadFullImage(file, maxPx) }
                    image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                }
            }
        }
        val current = items.getOrNull(pager.currentPage)
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(stringResource(R.string.back), touches = touches, onClick = onClose)
            BasicText(stringResource(R.string.page_position, pager.currentPage + 1, items.size), Modifier.weight(1f), style = Heading)
            val hasOriginal = remember(current?.id, revision) { current != null && !current.isVideo && current.original.exists() }
            if (hasOriginal) {
                Button(stringResource(if (showOriginal) R.string.with_filter else R.string.original), touches = touches) { showOriginal = !showOriginal }
            }
            if (current != null) {
                val on = current.id in selected
                Button(stringResource(if (on) R.string.selected else R.string.select), primary = on, touches = touches) { onToggle(current) }
            }
        }
    }
}

/** Four-digit keypad used to create and check the parent PIN. */
@Composable
private fun PinPad(title: String, message: String?, enabled: Boolean = true, onComplete: (String) -> Unit, extra: @Composable () -> Unit = {}) {
    val touches = remember { TouchCounter() }
    var entered by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .trackTouches(touches)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BasicText(title, style = Title.copy(fontSize = 26.sp, textAlign = TextAlign.Center))
        BasicText(message ?: " ", style = Body.copy(textAlign = TextAlign.Center))
        Row(Modifier.semantics { contentDescription = "${entered.length} / 4" }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(Modifier.size(18.dp).background(if (i < entered.length) Accent else Line, CircleShape))
            }
        }
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        val deleteLabel = stringResource(R.string.cd_delete_digit)
        // Keypads keep their 1-2-3 order in right-to-left languages too
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        Box(
                            Modifier
                                .size(76.dp)
                                .then(if (key.isEmpty()) Modifier else Modifier.background(Card, CircleShape).border(1.dp, Line, CircleShape))
                                .singleTap(touches, accessible = key.isNotEmpty()) {
                                    if (!enabled || key.isEmpty()) return@singleTap
                                    if (key == "⌫") {
                                        entered = entered.dropLast(1)
                                    } else if (entered.length < 4) {
                                        entered += key
                                        if (entered.length == 4) {
                                            onComplete(entered)
                                            entered = ""
                                        }
                                    }
                                }
                                .then(if (key == "⌫") Modifier.semantics { contentDescription = deleteLabel } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(key, style = Title.copy(fontSize = 28.sp))
                        }
                    }
                }
            }
        }
        extra()
    }
}

@Composable
private fun PinSetup(onDone: (String) -> Unit, onCancel: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var mismatch by remember { mutableStateOf(false) }
    val touches = remember { TouchCounter() }
    BackHandler(onBack = onCancel)
    PinPad(
        title = stringResource(if (first == null) R.string.pin_new else R.string.pin_repeat),
        message = stringResource(if (mismatch) R.string.pin_mismatch else R.string.pin_setup_info),
        onComplete = { pin ->
            val f = first
            when {
                f == null -> {
                    first = pin
                    mismatch = false
                }
                f == pin -> onDone(pin)
                else -> {
                    first = null
                    mismatch = true
                }
            }
        },
    ) {
        Button(stringResource(R.string.cancel), touches = touches, onClick = onCancel)
    }
}

/**
 * Asks for the parent PIN before the parent area opens. After five wrong tries input pauses for
 * 30 seconds. Parents who forgot the PIN can use the device lock instead, if the phone has one.
 */
@Composable
fun ParentPinGate(
    checkPin: (String) -> Boolean,
    deviceSecure: Boolean,
    onDeviceUnlock: () -> Unit,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val touches = remember { TouchCounter() }
    var failures by remember { mutableIntStateOf(0) }
    var lockedUntil by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(lockedUntil) {
        while (SystemClock.elapsedRealtime() < lockedUntil) {
            now = SystemClock.elapsedRealtime()
            delay(250)
        }
        now = SystemClock.elapsedRealtime()
    }
    val waitSec = ((lockedUntil - now + 999) / 1000).coerceAtLeast(0)
    BackHandler(onBack = onCancel)
    PinPad(
        title = stringResource(R.string.pin_gate_title),
        message = when {
            waitSec > 0 -> stringResource(R.string.pin_wait, waitSec.toInt())
            failures > 0 -> stringResource(R.string.pin_wrong)
            else -> stringResource(R.string.pin_enter)
        },
        enabled = waitSec == 0L,
        onComplete = { pin ->
            if (checkPin(pin)) {
                onSuccess()
            } else {
                failures++
                if (failures % 5 == 0) lockedUntil = SystemClock.elapsedRealtime() + 30_000
            }
        },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (deviceSecure) Button(stringResource(R.string.pin_forgot), touches = touches, onClick = onDeviceUnlock)
            Button(stringResource(R.string.cancel), touches = touches, onClick = onCancel)
        }
    }
}
