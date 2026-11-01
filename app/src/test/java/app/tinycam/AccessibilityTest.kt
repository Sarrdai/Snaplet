package app.tinycam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import app.tinycam.data.CaptureMode
import app.tinycam.data.DailyUsage
import app.tinycam.data.Settings
import app.tinycam.data.Thumbnails
import app.tinycam.ui.CameraActions
import app.tinycam.ui.Captured
import app.tinycam.ui.Effect
import app.tinycam.ui.KidCameraUi
import app.tinycam.ui.ParentScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Screen readers must be able to use the controls that touch users operate with taps and holds. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w400dp-h860dp-xxhdpi")
class AccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private class FakeCamera : CameraActions {
        var photos = 0
        override fun takePhoto(effect: Effect): Captured? { photos++; return null }
        override fun startVideo() = false
        override fun stopVideo(elapsedMs: Long): Captured? = null
        override fun setZoom(linear: Float) = Unit
    }

    @Test
    fun kidCameraOffersActions() {
        val cam = FakeCamera()
        var parentOpened = false
        compose.setContent {
            KidCameraUi(
                Settings(cooldownOn = false), emptyList(), Thumbnails(), null, CaptureMode.PHOTO, {}, false, {},
                Effect.NONE, {}, {}, cam, { parentOpened = true },
            ) { Box(Modifier.fillMaxSize()) }
        }
        compose.onNodeWithContentDescription("Shutter").assertHasClickAction().performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, cam.photos)
        // The 3 s hold becomes the screen reader's long press
        compose.onNodeWithContentDescription("Parent area")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .performSemanticsAction(SemanticsActions.OnLongClick)
        assertTrue(parentOpened)
    }

    @Test
    fun parentSwitchesReportState() {
        compose.setContent {
            ParentScreen(Settings(), DailyUsage(), emptyList(), Thumbnails(), false, true, false, {}, {}, {}, {}, {}, {}, {})
        }
        assertTrue(compose.onAllNodes(isToggleable()).fetchSemanticsNodes().isNotEmpty())
    }
}
