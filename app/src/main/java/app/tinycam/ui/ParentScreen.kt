package app.tinycam.ui

import androidx.activity.compose.BackHandler
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

private enum class FeatureId { VIDEO, GALLERY, SELFIE, ZOOM, EFFECTS, TIMER }

private class Feature(
    val id: FeatureId,
    val icon: ImageVector,
    val title: String,
    val info: String,
    val get: (Settings) -> Boolean,
    val set: (Settings, Boolean) -> Settings,
)

private val Features = listOf(
    Feature(FeatureId.VIDEO, Icons.Video, "Videos", "Umschalter Foto/Video, 1,5 s halten.", { it.video }, { s, v -> s.copy(video = v) }),
    Feature(FeatureId.GALLERY, Icons.Grid, "Galerie", "Alle Bilder als Raster ansehen, 1 s halten.", { it.gallery }, { s, v -> s.copy(gallery = v) }),
    Feature(FeatureId.SELFIE, Icons.Flip, "Selfie-Kamera", "Zwischen Rück- und Frontkamera wechseln, 1 s halten.", { it.selfie }, { s, v -> s.copy(selfie = v) }),
    Feature(FeatureId.ZOOM, Icons.Search, "Zoom", "Großer Schieberegler. Bewegt sich nur, wenn man den Knopf zieht.", { it.zoom }, { s, v -> s.copy(zoom = v) }),
    Feature(FeatureId.EFFECTS, Icons.Wand, "Filter & Rahmen", "Schwarz-Weiß, Sepia, Bunt und lustige Rahmen für Fotos.", { it.effects }, { s, v -> s.copy(effects = v) }),
    Feature(FeatureId.TIMER, Icons.Timer, "Selbstauslöser", "Mit großem Countdown. Erneutes Tippen bricht ab.", { it.timer }, { s, v -> s.copy(timer = v) }),
)

private val LockInfo = mapOf(
    ParentLock.HOLD to "Wer das Schloss 3 s hält, kommt hinein.",
    ParentLock.DEVICE to "Nach dem Halten fragt Android die Bildschirmsperre des Handys ab (PIN, Muster oder Fingerabdruck). Ideal für ein geteiltes Handy.",
    ParentLock.PIN to "Eigene 4-stellige PIN, unabhängig von der Handy-Sperre. Vergessen? Mit der Geräte-Sperre kommt man trotzdem hinein.",
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
                BasicText("Eltern-Bereich", Modifier.weight(1f), style = Title)
                Button("Fertig", primary = true, touches = touches, onClick = onClose)
            }

            Section("Design") {
                SkinPicker(settings.skin, settings.side == Side.CENTER, touches) { onChange(settings.copy(skin = it)) }
            }
            Section("Auslöser-Position", "Links oder rechts: Pille an der Seite. Mitte: runder Auslöser unten in der Mitte.") {
                Options(Side.entries, settings.side, { it.label }, touches) { onChange(settings.copy(side = it)) }
            }
            Section("Funktionen", "Fotografieren geht immer. Alles andere einzeln einschalten, wenn das Kind so weit ist.") {
                Features.forEach { f ->
                    val on = f.get(settings)
                    SwitchRow(f.title, f.info, on, touches, f.icon) { onChange(f.set(settings, it)) }
                    if (on && f.id == FeatureId.VIDEO) {
                        Indented {
                            SwitchRow("Videolänge begrenzen", null, settings.videoLimitOn, touches) {
                                onChange(settings.copy(videoLimitOn = it))
                            }
                            if (settings.videoLimitOn) {
                                ValueSlider(settings.maxVideoSec, 5..180, 5, { "$it s" }) { onChange(settings.copy(maxVideoSec = it)) }
                            }
                        }
                    }
                    if (on && f.id == FeatureId.TIMER) {
                        Indented {
                            BasicText("Countdown", style = Chip.copy(color = Text))
                            ValueSlider(settings.timerSec, 2..15, 1, { "$it s" }) { onChange(settings.copy(timerSec = it)) }
                        }
                    }
                }
            }
            Section("Grenzen") {
                SwitchRow("Pause zwischen Aufnahmen", "Verhindert viele Bilder durch wiederholtes Drücken.", settings.cooldownOn, touches) {
                    onChange(settings.copy(cooldownOn = it))
                }
                if (settings.cooldownOn) {
                    ValueSlider(settings.cooldownSec, 1..10, 1, { "$it s" }) { onChange(settings.copy(cooldownSec = it)) }
                }
                SwitchRow(
                    "Aufnahmen pro Tag",
                    "Fotos und Videos zusammen. Der Auslöser zeigt die restlichen als Symbole. Heute: ${usage.captures}",
                    settings.captureLimitOn, touches,
                ) { onChange(settings.copy(captureLimitOn = it)) }
                if (settings.captureLimitOn) {
                    ValueSlider(settings.dailyCaptures, 1..100, 1, { "$it" }) { onChange(settings.copy(dailyCaptures = it)) }
                }
                SwitchRow(
                    "Nutzungszeit pro Tag",
                    "Danach schläft die Kamera bis morgen. Heute genutzt: ${usage.usageMs / 60_000} min",
                    settings.timeLimitOn, touches,
                ) { onChange(settings.copy(timeLimitOn = it)) }
                if (settings.timeLimitOn) {
                    ValueSlider(settings.dailyMinutes, 5..180, 5, { "$it min" }) { onChange(settings.copy(dailyMinutes = it)) }
                }
                Button("Heutige Aufnahmen und Zeit zurücksetzen", touches = touches, onClick = onResetToday)
            }
            Section("Vorschau-Leiste", "Wie viele der letzten Bilder über dem Kamerabild zu sehen sind.") {
                ValueSlider(settings.stripCount, 1..8, 1, { "$it Bilder" }) { onChange(settings.copy(stripCount = it)) }
            }
            Section("Foto & Video", "Kleinere Formate sparen Speicher. 1:1 wird nach der Aufnahme quadratisch zugeschnitten.") {
                BasicText("Foto-Format", style = Chip.copy(color = Text))
                Options(PhotoAspect.entries, settings.photoAspect, { it.label }, touches) { onChange(settings.copy(photoAspect = it)) }
                BasicText("Foto-Größe", style = Chip.copy(color = Text))
                Options(PhotoSize.entries, settings.photoSize, { it.label }, touches) { onChange(settings.copy(photoSize = it)) }
                BasicText("Video-Qualität", style = Chip.copy(color = Text))
                Options(VideoQuality.entries, settings.videoQuality, { it.label }, touches) { onChange(settings.copy(videoQuality = it)) }
            }
            Section(
                "Fotos & Videos",
                "${items.size} gespeichert. Die Aufnahmen liegen nur in der App. Zum Sichern auswählen und teilen, z. B. an Google Fotos, Drive oder per Nachricht.",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button("Ansehen & auswählen …", primary = true, touches = touches) { galleryOpen = true }
                    Button("Alle teilen …", touches = touches) { onShare(items.toList()) }
                    HoldButton("Alle löschen (3 s halten)", touches) { onDelete(items.toList()) }
                }
            }
            Section("Zugang zum Eltern-Bereich", LockInfo[settings.parentLock]) {
                Options(ParentLock.entries, settings.parentLock, { it.label }, touches) { lock ->
                    when {
                        lock == ParentLock.PIN && !hasPin -> pinSetup = true
                        lock == ParentLock.DEVICE && !deviceSecure -> Unit
                        else -> onChange(settings.copy(parentLock = lock))
                    }
                }
                if (!deviceSecure) {
                    BasicText(
                        "Auf diesem Handy ist keine Bildschirmsperre eingerichtet. „Geräte-Sperre“ geht erst, wenn eine eingerichtet ist.",
                        style = Body.copy(color = Accent),
                    )
                }
                if (settings.parentLock == ParentLock.PIN) {
                    Button("PIN ändern", touches = touches) { pinSetup = true }
                }
            }
            Section(
                "App fixieren",
                if (pinned) {
                    "TinyCam ist fixiert. Kinder können die App nicht verlassen."
                } else {
                    "Fixiert TinyCam auf dem Bildschirm (Android-Bildschirmfixierung). Lösen geht hier oder mit Zurück + Übersicht gleichzeitig halten."
                },
            ) {
                Button(if (pinned) "Fixierung lösen" else "App fixieren", primary = !pinned, touches = touches, onClick = onTogglePin)
                SwitchRow(
                    "Automatisch fixieren",
                    "Beim Start und beim Verlassen des Eltern-Bereichs. Android fragt jedes Mal kurz nach einer Bestätigung. " +
                        "Tipp: In den Android-Einstellungen unter Sicherheit → App fixieren „Vor dem Lösen nach PIN fragen“ einschalten.",
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
private fun ValueSlider(value: Int, range: IntRange, step: Int, label: (Int) -> String, onChange: (Int) -> Unit) {
    val change by rememberUpdatedState(onChange)
    val current by rememberUpdatedState(value)
    fun valueAt(x: Float, width: Int): Int {
        val raw = range.first + (x / width).coerceIn(0f, 1f) * (range.last - range.first)
        return (range.first + ((raw - range.first) / step).roundToInt() * step).coerceIn(range)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(
            Modifier
                .weight(1f)
                .height(40.dp)
                .pointerInput(range, step) {
                    detectTapGestures { change(valueAt(it.x, size.width)) }
                }
                .pointerInput(range, step) {
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
            val x = knob + f * (size.width - knob * 2)
            drawLine(Line, Offset(knob, y), Offset(size.width - knob, y), track, StrokeCap.Round)
            drawLine(Accent, Offset(knob, y), Offset(x, y), track, StrokeCap.Round)
            drawCircle(Accent, knob, Offset(x, y))
            drawCircle(Ink, knob * 0.35f, Offset(x, y))
        }
        BasicText(label(value), Modifier.widthIn(min = 76.dp), style = Chip.copy(color = Text, textAlign = TextAlign.End))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Options(values: List<T>, selected: T, label: (T) -> String, touches: TouchCounter, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            val on = v == selected
            BasicText(
                label(v),
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Accent else Bg)
                    .border(1.dp, if (on) Accent else Line, RoundedCornerShape(50))
                    .singleTap(touches) { onSelect(v) }
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
            .singleTap(touches) { onToggle(!on) }
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
            .singleTap(touches, onClick)
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
                    .singleTap(touches) { onSelect(skin) }
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
                BasicText(skin.label, style = Chip.copy(color = if (on) Accent else Text))
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
        ParentViewer(items, start.coerceIn(items.indices), selected, ::toggle) { viewing = null }
        return
    }
    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize().background(Bg).trackTouches(touches).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button("Zurück", touches = touches, onClick = onClose)
            BasicText(
                if (chosen.isEmpty()) "Fotos & Videos" else "${chosen.size} ausgewählt",
                Modifier.weight(1f), style = Heading,
            )
            Button(if (allSelected) "Keine" else "Alle", touches = touches) {
                if (allSelected) selected.clear() else selected.addAll(items.map { it.id }.filterNot { it in selected })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (items.isEmpty()) {
                BasicText("Noch keine Aufnahmen.", Modifier.align(Alignment.Center), style = Body)
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
    Box(modifier.size(48.dp).singleTap(touches, onToggle), contentAlignment = Alignment.Center) {
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
        Button("Teilen (${chosen.size}) …", primary = chosen.isNotEmpty(), touches = touches) {
            if (chosen.isNotEmpty()) onShare(chosen)
        }
        HoldButton("Löschen (${chosen.size}) – 3 s halten", touches) {
            if (chosen.isNotEmpty()) onDelete()
        }
    }
}

/** Full-screen view for picking: swipe between photos and videos, select the current one. */
@Composable
private fun ParentViewer(
    items: List<MediaItem>,
    start: Int,
    selected: List<String>,
    onToggle: (MediaItem) -> Unit,
    onClose: () -> Unit,
) {
    val touches = remember { TouchCounter() }
    val pager = rememberPagerState(start) { items.size }
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
                    val image by produceState<ImageBitmap?>(null, item.id) { value = loadFullImage(item.file, maxPx) }
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
            Button("Zurück", touches = touches, onClick = onClose)
            BasicText("${pager.currentPage + 1} / ${items.size}", Modifier.weight(1f), style = Heading)
            if (current != null) {
                val on = current.id in selected
                Button(if (on) "✓ Ausgewählt" else "Auswählen", primary = on, touches = touches) { onToggle(current) }
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
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(Modifier.size(18.dp).background(if (i < entered.length) Accent else Line, CircleShape))
            }
        }
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .size(76.dp)
                            .then(if (key.isEmpty()) Modifier else Modifier.background(Card, CircleShape).border(1.dp, Line, CircleShape))
                            .singleTap(touches) {
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
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(key, style = Title.copy(fontSize = 28.sp))
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
        title = if (first == null) "Neue PIN" else "PIN wiederholen",
        message = if (mismatch) "Die PINs waren verschieden. Bitte neu eingeben." else "4 Ziffern, nur für den Eltern-Bereich.",
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
        Button("Abbrechen", touches = touches, onClick = onCancel)
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
        title = "Eltern-PIN",
        message = when {
            waitSec > 0 -> "Zu viele Versuche. Bitte $waitSec s warten."
            failures > 0 -> "Falsche PIN."
            else -> "Bitte PIN eingeben."
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
            if (deviceSecure) Button("PIN vergessen? Mit Geräte-Sperre", touches = touches, onClick = onDeviceUnlock)
            Button("Abbrechen", touches = touches, onClick = onCancel)
        }
    }
}
