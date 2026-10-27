package app.tinycam.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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

private val LevelInfo = mapOf(
    1 to "Nur Fotos: Auslöser und Vorschau-Leiste.",
    2 to "Zusätzlich Videos. Der Umschalter muss 1,5 s gehalten werden.",
    3 to "Zusätzlich die Galerie mit allen Bildern (1 s halten).",
)

@Composable
fun ParentScreen(
    settings: Settings,
    usage: DailyUsage,
    itemCount: Int,
    pinned: Boolean,
    onChange: (Settings) -> Unit,
    onResetToday: () -> Unit,
    onShareAll: () -> Unit,
    onDeleteAll: () -> Unit,
    onTogglePin: () -> Unit,
    onClose: () -> Unit,
) {
    val touches = remember { TouchCounter() }
    BackHandler(onBack = onClose)
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
            Section("Lernstufe", LevelInfo[settings.level]) {
                Options(listOf(1, 2, 3), settings.level, { "Stufe $it" }, touches) { onChange(settings.copy(level = it)) }
            }
            Section("Auslöser-Seite", "Auf welcher Seite die Hand des Kindes den Auslöser am besten erreicht.") {
                Options(Side.entries, settings.side, { it.label }, touches) { onChange(settings.copy(side = it)) }
            }
            Section("Pause zwischen Aufnahmen", "Verhindert viele Bilder durch wiederholtes Drücken.") {
                Options(listOf(0, 1, 3, 5), settings.cooldownSec, { if (it == 0) "Aus" else "$it s" }, touches) {
                    onChange(settings.copy(cooldownSec = it))
                }
            }
            Section("Aufnahmen pro Tag", "Fotos und Videos zusammen. Heute: ${usage.captures}") {
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
            Section("Fotos & Videos", "$itemCount gespeichert. Die Aufnahmen liegen nur in der App.") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button("Alle teilen …", touches = touches, onClick = onShareAll)
                    HoldButton("Alle löschen (3 s halten)", touches, onDeleteAll)
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
