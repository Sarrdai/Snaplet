package app.tinycam

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.tinycam.data.AppState
import app.tinycam.data.MediaItem
import app.tinycam.data.isTimeUp
import app.tinycam.ui.CameraScreen
import app.tinycam.ui.ParentScreen
import app.tinycam.ui.PermissionScreen
import app.tinycam.ui.SleepScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var app: AppState
    private var hasCamera by mutableStateOf(false)
    private var hasAudio by mutableStateOf(false)
    private var pinned by mutableStateOf(false)
    private var askedOnce = false

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = AppState(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        refreshPermissions()
        if (!hasCamera) requestPermissions()
        trackUsage()
        setContent { Root() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
            pinned = isPinned()
        }
    }

    @Composable
    private fun Root() {
        var parentOpen by remember { mutableStateOf(false) }
        val settings = app.settings
        val usage = app.usage
        Box(Modifier.fillMaxSize()) {
            when {
                !hasCamera -> PermissionScreen(::requestPermissions)
                settings.isTimeUp(usage) -> SleepScreen(settings.skin) { parentOpen = true }
                else -> CameraScreen(app, hasAudio) { parentOpen = true }
            }
            if (parentOpen) {
                ParentScreen(
                    settings = settings,
                    usage = usage,
                    items = app.items,
                    thumbs = app.thumbs,
                    pinned = pinned,
                    onChange = app::updateSettings,
                    onResetToday = app::resetToday,
                    onShare = ::share,
                    onDelete = app::delete,
                    onTogglePin = ::togglePin,
                    onClose = { parentOpen = false },
                )
            }
        }
    }

    private fun refreshPermissions() {
        hasCamera = granted(Manifest.permission.CAMERA)
        hasAudio = granted(Manifest.permission.RECORD_AUDIO)
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        if (askedOnce && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Permanently denied: only the system settings can grant it now
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
            return
        }
        askedOnce = true
        permissionRequest.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).run {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Counts foreground time for the daily usage limit. */
    private fun trackUsage() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = SystemClock.elapsedRealtime()
            try {
                while (true) {
                    delay(10_000)
                    val now = SystemClock.elapsedRealtime()
                    app.addUsageTime(now - last)
                    last = now
                }
            } finally {
                app.addUsageTime(SystemClock.elapsedRealtime() - last)
            }
        }
    }

    private fun isPinned() =
        getSystemService(ActivityManager::class.java).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    private fun togglePin() {
        if (isPinned()) stopLockTask() else startLockTask()
        pinned = isPinned()
    }

    private fun share(selection: List<MediaItem>) {
        if (selection.isEmpty()) return
        // Other apps cannot open while the screen is pinned
        if (isPinned()) stopLockTask()
        startActivity(app.media.shareIntent(selection))
    }
}
