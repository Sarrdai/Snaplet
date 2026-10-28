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
import app.tinycam.data.Skin

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

private class Feature(
    val icon: ImageVector,
    val title: String,
    val info: String,
    val get: (Settings) -> Boolean,
    val set: (Settings, Boolean) -> Settings,
)

private val Features = listOf(
    Feature(Icons.Video, "Videos", "Umschalter Foto/Video, 1,5 s halten.", { it.video }, { s, v -> s.copy(video = v) }),
    Feature(Icons.Grid, "Galerie", "Alle Bilder als Raster ansehen, 1 s halten.", { it.gallery }, { s, v -> s.copy(gallery = v) }),
    Feature(Icons.Flip, "Selfie-Kamera", "Zwischen Rück- und Frontkamera wechseln, 1 s halten.", { it.selfie }, { s, v -> s.copy(selfie = v) }),
    Feature(Icons.Search, "Zoom", "Großer Schieberegler. Bewegt sich nur, wenn man den Knopf zieht.", { it.zoom }, { s, v -> s.copy(zoom = v) }),
    Feature(Icons.Wand, "Filter & Rahmen", "Schwarz-Weiß, Sepia, Bunt und lustige Rahmen für Fotos.", { it.effects }, { s, v -> s.copy(effects = v) }),
    Feature(Icons.Timer, "Selbstauslöser", "Mit großem Countdown. Erneutes Tippen bricht ab.", { it.timer }, { s, v -> s.copy(timer = v) }),
)

@Composable
fun ParentScreen(
    settings: Settings,
    usage: DailyUsage,
    items: List<MediaItem>,
    thumbs: Thumbnails,
    pinned: Boolean,
    onChange: (Settings) -> Unit,
    onResetToday: () -> Unit,
    onShare: (List<MediaItem>) -> Unit,
    onDelete: (List<MediaItem>) -> Unit,
    onTogglePin: () -> Unit,
    onClose: () -> Unit,
    initialGalleryOpen: Boolean = false,
) {
    val touches = remember { TouchCounter() }
    var galleryOpen by remember { mutableStateOf(initialGalleryOpen) }
    BackHandler(onBack = onClose)
    if (galleryOpen) {
        ParentGallery(items, thumbs, onShare, onDelete, onClose = { galleryOpen = false })
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
                SkinPicker(settings.skin, touches) { onChange(settings.copy(skin = it)) }
            }
            Section("Funktionen", "Fotografieren geht immer. Alles andere einzeln einschalten, wenn das Kind so weit ist.") {
                Features.forEach { f ->
                    FeatureRow(f, f.get(settings), touches) { onChange(f.set(settings, it)) }
                }
            }
            if (settings.timer) {
                Section("Selbstauslöser-Dauer") {
                    Options(listOf(3, 5, 10), settings.timerSec, { "$it s" }, touches) { onChange(settings.copy(timerSec = it)) }
                }
            }
            Section("Auslöser-Seite", "Auf welcher Seite die Hand des Kindes den Auslöser am besten erreicht.") {
                Options(Side.entries, settings.side, { it.label }, touches) { onChange(settings.copy(side = it)) }
            }
            Section("Pause zwischen Aufnahmen", "Verhindert viele Bilder durch wiederholtes Drücken.") {
                Options(listOf(0, 1, 3, 5), settings.cooldownSec, { if (it == 0) "Aus" else "$it s" }, touches) {
                    onChange(settings.copy(cooldownSec = it))
                }
            }
            Section(
                "Aufnahmen pro Tag",
                "Fotos und Videos zusammen. Der Auslöser zeigt die restlichen als Symbole, die leer werden. Heute: ${usage.captures}",
            ) {
                Options(listOf(0, 10, 20, 50), settings.dailyCaptures, { if (it == 0) "Aus" else "$it" }, touches) {
                    onChange(settings.copy(dailyCaptures = it))
                }
            }
            Section("Nutzungszeit pro Tag", "Heute genutzt: ${usage.usageMs / 60_000} min") {
                Options(listOf(0, 15, 30, 60), settings.dailyMinutes, { if (it == 0) "Aus" else "$it min" }, touches) {
                    onChange(settings.copy(dailyMinutes = it))
                }
            }
            Section("Maximale Videolänge") {
                Options(listOf(10, 30, 60, 0), settings.maxVideoSec, { if (it == 0) "Aus" else "$it s" }, touches) {
                    onChange(settings.copy(maxVideoSec = it))
                }
            }
            Section("Bilder in der Vorschau-Leiste") {
                Options(listOf(3, 5, 7), settings.stripCount, { "$it" }, touches) { onChange(settings.copy(stripCount = it)) }
            }
            Section("Heutige Limits", "Setzt Aufnahmen und Nutzungszeit für heute auf null.") {
                Button("Zurücksetzen", touches = touches, onClick = onResetToday)
            }
            Section(
                "Fotos & Videos",
                "${items.size} gespeichert. Die Aufnahmen liegen nur in der App. Zum Sichern auswählen und teilen, z. B. an Google Fotos, Drive oder per Nachricht.",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button("Auswählen …", primary = true, touches = touches) { galleryOpen = true }
                    Button("Alle teilen …", touches = touches) { onShare(items.toList()) }
                    HoldButton("Alle löschen (3 s halten)", touches) { onDelete(items.toList()) }
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
            }
        }
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
private fun FeatureRow(feature: Feature, on: Boolean, touches: TouchCounter, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .singleTap(touches) { onToggle(!on) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(42.dp).background(if (on) Accent else Line, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(feature.icon, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(if (on) Ink else Text))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(feature.title, style = Chip.copy(color = Text))
            BasicText(feature.info, style = Body.copy(fontSize = 13.sp, lineHeight = 18.sp))
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
private fun SkinPicker(selected: Skin, touches: TouchCounter, onSelect: (Skin) -> Unit) {
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
                    inset(left = size.width * 0.25f, top = 0f, right = size.width * 0.25f, bottom = size.height * 0.08f) {
                        drawShutter(skin, style, 1f, 0f, 1f, Emblem.SKIN, spikesLeft = true)
                    }
                }
                BasicText(skin.label, style = Chip.copy(color = if (on) Accent else Text))
            }
        }
    }
}

/** Pick single photos and videos to share or delete. A tap toggles the selection. */
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
    val style = remember { styleFor(Skin.CLASSIC) }
    BackHandler(onBack = onClose)
    // Forget selections of items that were deleted meanwhile
    selected.retainAll(items.map { it.id }.toSet())
    val chosen = items.filter { it.id in selected }
    val allSelected = items.isNotEmpty() && chosen.size == items.size

    fun toggle(item: MediaItem) {
        if (!selected.remove(item.id)) selected.add(item.id)
    }

    Column(Modifier.fillMaxSize().background(Bg).trackTouches(touches).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button("Zurück", touches = touches, onClick = onClose)
            BasicText(
                if (chosen.isEmpty()) "Bilder auswählen" else "${chosen.size} ausgewählt",
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
                items(items, key = { it.id }) { item ->
                    val on = item.id in selected
                    val shape = RoundedCornerShape(12.dp)
                    Box(Modifier.aspectRatio(1f)) {
                        Thumbnail(item, thumbs, style, 3.4.dp, touches, ::toggle, Modifier.fillMaxSize())
                        if (on) Box(Modifier.fillMaxSize().border(3.dp, Accent, shape).background(Color(0x33000000), shape))
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(26.dp)
                                .background(if (on) Accent else Color(0x59000000), CircleShape)
                                .border(2.dp, if (on) Accent else Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) Image(Icons.Check, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(Ink))
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Card).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button("Teilen (${chosen.size}) …", primary = chosen.isNotEmpty(), touches = touches) {
                if (chosen.isNotEmpty()) onShare(chosen)
            }
            HoldButton("Löschen (${chosen.size}) – 3 s halten", touches) {
                if (chosen.isNotEmpty()) {
                    onDelete(chosen)
                    selected.clear()
                }
            }
        }
    }
}
