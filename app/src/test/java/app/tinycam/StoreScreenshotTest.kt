package app.tinycam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import app.tinycam.ui.Captured
import app.tinycam.ui.Effect
import app.tinycam.ui.Emblem
import app.tinycam.ui.KidCameraUi
import app.tinycam.ui.ParentScreen
import app.tinycam.ui.SkinChooser
import app.tinycam.ui.TouchCounter
import app.tinycam.ui.drawBackdrop
import app.tinycam.ui.drawShutter
import app.tinycam.ui.kidText
import app.tinycam.ui.styleFor
import app.tinycam.ui.unitFor
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Google Play listing images for every app language, written to app/build/store/<locale>/.
 * Run `./gradlew recordRoborazziDebug --tests '*StoreScreenshot*'`; pass `-PstoreCamera=/path/photo.jpg`
 * to show that photo as the camera image instead of the drawn stand-in scene.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class StoreScreenshotTest(private val locale: String) {
    companion object {
        private val Tagline = mapOf(
            "en" to "The first camera for little photographers",
            "de" to "Die erste Kamera für kleine Fotografen",
        )
    }

    @get:Rule
    val compose = createComposeRule()

    private val out get() = "build/store/$locale"
    private val camera: ImageBitmap? = System.getProperty("store.camera")?.takeIf { it.isNotBlank() }?.let { path ->
        requireNotNull(BitmapFactory.decodeFile(path)) { "Cannot read camera image $path" }.asImageBitmap()
    }
    private val thumbs = Thumbnails()
    private val items = (0 until 6).map { i ->
        MediaItem(File("IMG_${1000 + i}.${if (i == 1) "mp4" else "jpg"}"), isVideo = i == 1).also {
            thumbs.put(it.id, Thumb(camera?.let { c -> crop(c, i) } ?: sceneBitmap(320, 320, i), if (i == 1) 12_000 else null))
        }
    }

    private object NoCamera : CameraActions {
        override fun takePhoto(effect: Effect): Captured? = null
        override fun startVideo() = false
        override fun stopVideo(elapsedMs: Long): Captured? = null
        override fun setZoom(linear: Float) = Unit
    }

    private val allFeatures = Settings(video = true, gallery = true, selfie = true, zoom = true, effects = true, timer = true, skinChoice = true)

    private fun kidCamera(settings: Settings, tokens: Tokens? = null, overlay: @Composable () -> Unit = {}) {
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                KidCameraUi(
                    settings, items, thumbs, tokens, CaptureMode.PHOTO, {}, false, {}, Effect.NONE, {}, {}, NoCamera, {},
                ) { CameraImage() }
                overlay()
            }
        }
    }

    @Composable
    private fun CameraImage() {
        val image = camera
        if (image != null) {
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize()) { drawScene(0) }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("$out/$name.png")

    @Test
    fun phone1Simple() {
        kidCamera(Settings(skin = Skin.RAINBOW))
        capture("phone_1_simple")
    }

    @Test
    fun phone2AllFeatures() {
        kidCamera(allFeatures.copy(skin = Skin.DINO, captureLimitOn = true), Tokens(3, 5))
        capture("phone_2_all_features")
    }

    @Test
    fun phone3SkinChooser() {
        val settings = allFeatures.copy(skin = Skin.SPACE)
        kidCamera(settings) {
            SkinChooser(Skin.entries, settings.skin, false, styleFor(settings.skin), unitFor(360f, 640f), TouchCounter(), {}, {})
        }
        capture("phone_3_skin_chooser")
    }

    @Test
    fun phone4RoundShutter() {
        kidCamera(allFeatures.copy(skin = Skin.UNICORN, side = Side.CENTER), Tokens(4, 6))
        capture("phone_4_round_shutter")
    }

    @Test
    fun phone5Parent() {
        compose.setContent {
            ParentScreen(
                Settings(video = true, gallery = true, timer = true, effects = true, skinChoice = true, skin = Skin.SHARK),
                DailyUsage("d", 4, 600_000), items, thumbs, false, true, true,
                {}, {}, {}, {}, {}, {}, {},
            )
        }
        capture("phone_5_parent")
    }

    @Test
    fun phone6ParentGallery() {
        compose.setContent {
            ParentScreen(Settings(), DailyUsage(), items, thumbs, false, true, false, {}, {}, {}, {}, {}, {}, {}, initialGalleryOpen = true)
        }
        capture("phone_6_parent_gallery")
    }

    /** 1024 x 500 px banner on top of the store page. */
    @Test
    @Config(qualifiers = "+w1024dp-h500dp-mdpi")
    fun featureGraphic() {
        val skins = listOf(Skin.RAINBOW, Skin.DINO, Skin.SPACE, Skin.UNICORN)
        compose.setContent {
            Box(Modifier.fillMaxSize().drawBehind { drawBackdrop(Skin.RAINBOW, styleFor(Skin.RAINBOW)) }) {
                Row(Modifier.fillMaxSize().padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicText(
                            RuntimeEnvironment.getApplication().getString(R.string.app_name),
                            style = kidText(96.sp, weight = FontWeight.ExtraBold),
                        )
                        BasicText(Tagline.getValue(locale), style = kidText(34.sp, Color.White.copy(alpha = 0.92f), FontWeight.Bold))
                    }
                    // Decorations (spikes, manes) stick out of the pill, so the pills keep generous gaps
                    Row(horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                        skins.forEach { skin ->
                            Canvas(Modifier.size(width = 64.dp, height = 290.dp)) {
                                drawShutter(skin, styleFor(skin), 1f, 0f, 1f, Emblem.SKIN, true)
                            }
                        }
                    }
                }
            }
        }
        capture("feature_graphic")
    }

    /** 512 x 512 px store icon: the adaptive launcher icon full-bleed; Play adds the rounded mask itself. */
    @Test
    fun icon() {
        if (locale != "en") return
        val context = RuntimeEnvironment.getApplication()
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        for (layer in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            ContextCompat.getDrawable(context, layer)!!.apply { setBounds(0, 0, 512, 512) }.draw(canvas)
        }
        File("build/store").mkdirs()
        File("build/store/icon_512.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

// One subclass per app language (values-<lang>); phone screenshots are 9:16 at 1080 x 1920 px
@Config(sdk = [35], qualifiers = "en-w360dp-h640dp-xxhdpi")
class StoreScreenshotEn : StoreScreenshotTest("en")

@Config(sdk = [35], qualifiers = "de-w360dp-h640dp-xxhdpi")
class StoreScreenshotDe : StoreScreenshotTest("de")

/** Different square sections of the camera image, so the preview strip shows varied pictures. */
private fun crop(source: ImageBitmap, seed: Int): ImageBitmap {
    val side = minOf(source.width, source.height) * (0.5f + 0.08f * (seed % 4))
    val x = ((source.width - side) * (seed * 0.37f % 1f)).toInt()
    val y = ((source.height - side) * (seed * 0.61f % 1f)).toInt()
    val out = ImageBitmap(320, 320)
    Canvas(out).drawImageRect(source, IntOffset(x, y), IntSize(side.toInt(), side.toInt()), IntOffset.Zero, IntSize(320, 320), Paint())
    return out
}
