package app.tinycam.ui

import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.tinycam.data.MediaItem
import app.tinycam.data.Skin
import app.tinycam.data.Thumbnails
import app.tinycam.data.loadFullImage

@Composable
fun GalleryOverlay(
    items: List<MediaItem>,
    thumbs: Thumbnails,
    skin: Skin,
    style: SkinStyle,
    unit: Dp,
    touches: TouchCounter,
    onOpen: (MediaItem) -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize().drawBehind { drawBackdrop(skin, style) }.singleTap(touches) {}) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(unit * 30f),
            contentPadding = PaddingValues(start = unit * 5f, end = unit * 5f, top = unit * 24f, bottom = unit * 8f),
            horizontalArrangement = Arrangement.spacedBy(unit * 3f),
            verticalArrangement = Arrangement.spacedBy(unit * 3f),
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            items(items, key = { it.id }) {
                Thumbnail(it, thumbs, style, unit, touches, onOpen, Modifier.fillMaxWidth().aspectRatio(1f))
            }
        }
        CloseButton(
            unit, touches, onClose,
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(unit * 5f),
        )
    }
}

/** Full-size photo or video. One tap anywhere closes it. */
@Composable
fun MediaPreview(item: MediaItem, skin: Skin, style: SkinStyle, unit: Dp, touches: TouchCounter, onClose: () -> Unit) {
    val maxPx = with(LocalDensity.current) {
        val c = LocalConfiguration.current
        maxOf(c.screenWidthDp, c.screenHeightDp).dp.roundToPx()
    }
    Box(
        Modifier.fillMaxSize().drawBehind { drawBackdrop(skin, style) },
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(unit * 4f)
        Box(Modifier.fillMaxSize(0.9f), contentAlignment = Alignment.Center) {
            if (item.isVideo) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(item.file.path)
                            setOnPreparedListener { it.start() }
                        }
                    },
                    onRelease = { it.stopPlayback() },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val image by produceState<ImageBitmap?>(null, item.id) { value = loadFullImage(item.file, maxPx) }
                image?.let {
                    Image(
                        it, null,
                        Modifier
                            .aspectRatio(it.width / it.height.toFloat())
                            .shadow(12.dp, shape)
                            .clip(shape)
                            .border(unit * 1f, style.frame, shape),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        // Transparent layer on top so the video view cannot swallow the closing tap
        Box(Modifier.fillMaxSize().singleTap(touches, onClose))
    }
}

/** Shown when the daily usage time is used up. Parents can still get in via the lock. */
@Composable
fun SleepScreen(skin: Skin, onOpenParent: () -> Unit) {
    val style = remember(skin) { styleFor(skin) }
    val touches = remember { TouchCounter() }
    BoxWithConstraints(Modifier.fillMaxSize().trackTouches(touches).drawBehind { drawBackdrop(skin, style) }) {
        val u = unitFor(maxWidth.value, maxHeight.value)
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = u * 8f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(u * 44f)) { drawMoon(Offset(size.width / 2, size.height / 2), size.minDimension * 0.3f) }
            BasicText(
                "Die Kamera schläft jetzt",
                style = kidText((u * 9f).asSp(), weight = FontWeight.ExtraBold).copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(u * 2f))
            BasicText(
                "Morgen geht's weiter!",
                style = kidText((u * 6f).asSp(), Color.White.copy(alpha = 0.8f), FontWeight.Medium).copy(textAlign = TextAlign.Center),
            )
        }
        LockButton(
            style, u, touches, onOpenParent,
            Modifier.align(Alignment.BottomStart).windowInsetsPadding(WindowInsets.safeDrawing).padding(u * 4f),
        )
    }
}

/** For parents: the camera permission is needed once. */
@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    val touches = remember { TouchCounter() }
    BoxWithConstraints(
        Modifier.fillMaxSize().trackTouches(touches).drawBehind { drawBackdrop(Skin.RAINBOW, styleFor(Skin.RAINBOW)) },
    ) {
        val u = unitFor(maxWidth.value, maxHeight.value)
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = u * 8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(u * 4f),
        ) {
            BasicText(
                "TinyCam braucht die Kamera",
                style = kidText((u * 8f).asSp(), weight = FontWeight.ExtraBold).copy(textAlign = TextAlign.Center),
            )
            BasicText(
                "Liebe Eltern: Bitte einmal Kamera und Mikrofon erlauben. Das Mikrofon wird nur für Videos benutzt.",
                style = kidText((u * 4.4f).asSp(), Color.White.copy(alpha = 0.85f), FontWeight.Medium).copy(textAlign = TextAlign.Center),
            )
            BasicText(
                "Erlauben",
                Modifier
                    .shadow(8.dp, RoundedCornerShape(50))
                    .background(Color.White, RoundedCornerShape(50))
                    .singleTap(touches, onRequest)
                    .padding(horizontal = u * 8f, vertical = u * 2.5f),
                style = kidText((u * 6f).asSp(), Ink, FontWeight.ExtraBold),
            )
        }
    }
}
