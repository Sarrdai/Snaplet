package app.tinycam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import app.tinycam.data.CaptureMode
import app.tinycam.data.Settings
import app.tinycam.data.Side
import app.tinycam.data.Thumbnails
import app.tinycam.ui.CameraActions
import app.tinycam.ui.Captured
import app.tinycam.ui.Effect
import app.tinycam.ui.KidCameraUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class VideoShutterTest {
    @get:Rule
    val compose = createComposeRule()

    private class FakeCamera : CameraActions {
        var photos = 0
        var videos = 0
        override fun takePhoto(effect: Effect): Captured? { photos++; return null }
        override fun startVideo(): Boolean { videos++; return true }
        override fun stopVideo(elapsedMs: Long): Captured? = null
        override fun setZoom(linear: Float) = Unit
    }

    @Test
    fun shutterFollowsModeSwitch() {
        val cam = FakeCamera()
        var mode by mutableStateOf(CaptureMode.PHOTO)
        val settings = Settings(video = true, side = Side.CENTER, cooldownOn = false)
        compose.setContent {
            KidCameraUi(settings, emptyList(), Thumbnails(), null, mode, { mode = it }, false, {}, Effect.NONE, {}, {}, cam, {}) {
                Box(Modifier.fillMaxSize())
            }
        }
        compose.waitForIdle()
        // Locate the shutter with a photo first
        var y = 0.98f
        while (cam.photos == 0 && y > 0.6f) {
            compose.onRoot().performTouchInput { down(Offset(width / 2f, height * y)); up() }
            compose.waitForIdle()
            y += -0.01f
        }
        assertEquals(1, cam.photos)
        mode = CaptureMode.VIDEO
        compose.waitForIdle()
        val hit = y + 0.01f
        compose.onRoot().performTouchInput { down(Offset(width / 2f, height * hit)); up() }
        compose.waitForIdle()
        assertEquals("photo taken in video mode", 1, cam.photos)
        assertEquals(1, cam.videos)
    }
}
