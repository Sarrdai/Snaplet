package app.tinycam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.tinycam.data.CaptureMode
import app.tinycam.data.DailyUsage
import app.tinycam.data.MediaItem
import app.tinycam.data.Settings
import app.tinycam.data.Side
import app.tinycam.data.Skin
import app.tinycam.data.Thumb
import app.tinycam.data.Thumbnails
import app.tinycam.data.Tokens
import app.tinycam.ui.CameraActions
import app.tinycam.ui.Effect
import app.tinycam.ui.EffectPicker
import app.tinycam.ui.Captured
import app.tinycam.ui.GalleryOverlay
import app.tinycam.ui.KidCameraUi
import app.tinycam.ui.ParentScreen
import app.tinycam.ui.SleepScreen
import app.tinycam.ui.TouchCounter
import app.tinycam.ui.styleFor
import app.tinycam.ui.unitFor
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.sin

/**
 * Renders the main screens for every skin. Run `./gradlew recordRoborazziDebug` and look at
 * app/build/screenshots to review the design without a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val thumbs = Thumbnails()
    private val items = (0 until 6).map { i ->
        MediaItem(File("IMG_${1000 + i}.${if (i == 1) "mp4" else "jpg"}"), isVideo = i == 1).also {
            thumbs.put(it.id, Thumb(sceneBitmap(320, 320, i), if (i == 1) 12_000 else null))
        }
    }

    private object NoCamera : CameraActions {
        override fun takePhoto(effect: Effect): Captured? = null
        override fun startVideo() = false
        override fun stopVideo(elapsedMs: Long): Captured? = null
        override fun setZoom(linear: Float) = Unit
    }

    private val allFeatures = Settings(video = true, gallery = true, selfie = true, zoom = true, effects = true, timer = true)

    private fun renderSkins(
        prefix: String,
        settingsFor: (Skin) -> Settings,
        cooldown: Float = 1f,
        tokens: Tokens? = null,
        effect: Effect = Effect.NONE,
        countdownMs: Long? = null,
    ) {
        var settings by mutableStateOf(settingsFor(Skin.CLASSIC))
        compose.setContent {
            KidCameraUi(
                settings, items, thumbs, tokens, CaptureMode.PHOTO, {}, false, {}, effect, {}, NoCamera, {},
                initialCooldown = cooldown, initialCountdownMs = countdownMs,
            ) { Canvas(Modifier.fillMaxSize()) { drawScene(0) } }
        }
        Skin.entries.forEach {
            settings = settingsFor(it)
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/screenshots/${prefix}_${it.name.lowercase()}.png")
        }
    }

    @Test
    fun portrait() = renderSkins("portrait", { allFeatures.copy(skin = it, dailyCaptures = 5) }, tokens = Tokens(3, 5))

    @Test
    @Config(qualifiers = "w860dp-h400dp-land-xxhdpi")
    fun landscape() = renderSkins("landscape", { allFeatures.copy(skin = it, side = Side.LEFT) }, tokens = Tokens(7, 10))

    @Test
    fun portraitCooling() = renderSkins("cooling", { Settings(skin = it, side = Side.LEFT) }, cooldown = 0.45f)

    @Test
    fun countdown() = renderSkins("countdown", { Settings(skin = it, timer = true) }, countdownMs = 2400)

    @Test
    fun frame() = renderSkins("frame", { Settings(skin = it, effects = true) }, effect = Effect.STARS)

    @Test
    fun effectPicker() {
        compose.setContent {
            EffectPicker(Effect.HEARTS, styleFor(Skin.RAINBOW), unitFor(400f, 860f), TouchCounter(), {}, {})
        }
        compose.onRoot().captureRoboImage("build/screenshots/effects_picker.png")
    }

    @Test
    fun overlays() {
        var skin by mutableStateOf(Skin.CLASSIC)
        var sleep by mutableStateOf(false)
        compose.setContent {
            if (sleep) SleepScreen(skin) {}
            else GalleryOverlay(items, thumbs, skin, styleFor(skin), unitFor(400f, 860f), TouchCounter(), {}, {})
        }
        for (s in listOf(Skin.SPACE, Skin.DINO)) {
            skin = s
            sleep = false
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/screenshots/gallery_${s.name.lowercase()}.png")
            sleep = true
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/screenshots/sleep_${s.name.lowercase()}.png")
        }
    }

    @Test
    fun parent() {
        compose.setContent {
            ParentScreen(Settings(video = true, timer = true, dailyCaptures = 20), DailyUsage("d", 4, 600_000), 37, false, {}, {}, {}, {}, {}, {})
        }
        compose.onRoot().captureRoboImage("build/screenshots/parent.png")
    }
}

private fun sceneBitmap(w: Int, h: Int, seed: Int): ImageBitmap {
    val bmp = ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bmp), Size(w.toFloat(), h.toFloat())) { drawScene(seed) }
    return bmp
}

/** Stand-in for the camera image: sky, sun, hills and a ball. */
private fun DrawScope.drawScene(seed: Int) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF5AA8E6 + seed * 0x050300), Color(0xFFCFEAF7))))
    drawCircle(Color(0xFFFFD34D), minOf(w, h) * 0.09f, Offset(w * (0.2f + seed * 0.1f), h * 0.3f))
    for ((y, c) in listOf(0.68f to Color(0xFF6CC070), 0.8f to Color(0xFF3F9A55))) {
        val p = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                lineTo(x, h * y + sin(x / w * 7 + seed) * h * 0.03f)
                x += w / 40
            }
            lineTo(w, h)
            close()
        }
        drawPath(p, c)
    }
    drawCircle(Color(0xFFFF6B6B), minOf(w, h) * 0.06f, Offset(w * 0.55f, h * 0.62f))
}
