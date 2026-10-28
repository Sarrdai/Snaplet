package app.tinycam

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
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
import app.tinycam.data.ParentLock
import app.tinycam.ui.ParentPinGate
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
    private var parentOpen by mutableStateOf(false)
    private var pinGate by mutableStateOf(false)
    private var askedOnce = false
    private var onDeviceUnlocked: (() -> Unit)? = null
    // While the device unlock is showing, our activity pauses and resumes; re-pinning then would kill the prompt.
    // Expires on its own in case the system never reports back.
    private var authUntil = 0L
    // Auto-pin only on a real start, not on every resume: parents who unpinned with the phone's PIN must stay out
    private var pinOnResume = true

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshPermissions() }

    // Android 8 and 9: the system's "confirm device credential" screen
    private val keyguardRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val success = onDeviceUnlocked
            onDeviceUnlocked = null
            if (result.resultCode == RESULT_OK) success?.let(::finishAuth) else finishAuth(null)
        }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pinOnResume = true // reopened from the launcher
    }

    override fun onResume() {
        super.onResume()
        if (pinOnResume) {
            pinOnResume = false
            pinIfWanted()
        }
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
        val settings = app.settings
        val usage = app.usage
        Box(Modifier.fillMaxSize()) {
            when {
                !hasCamera -> PermissionScreen(::requestPermissions)
                settings.isTimeUp(usage) -> SleepScreen(settings.skin, ::requestParentArea)
                else -> CameraScreen(app, hasAudio, ::requestParentArea)
            }
            if (pinGate) {
                ParentPinGate(
                    checkPin = app::checkPin,
                    deviceSecure = deviceSecure(),
                    onDeviceUnlock = { authenticateDevice { openParentArea() } },
                    onSuccess = ::openParentArea,
                    onCancel = { pinGate = false },
                )
            }
            if (parentOpen) {
                ParentScreen(
                    settings = settings,
                    usage = usage,
                    items = app.items,
                    thumbs = app.thumbs,
                    pinned = pinned,
                    deviceSecure = deviceSecure(),
                    hasPin = app.hasPin(),
                    onChange = app::updateSettings,
                    onSetPin = app::setPin,
                    onResetToday = app::resetToday,
                    onShare = ::share,
                    onDelete = app::delete,
                    onTogglePin = ::togglePin,
                    onClose = {
                        parentOpen = false
                        pinIfWanted()
                    },
                )
            }
        }
    }

    /** Called after the lock was held for 3 seconds. */
    private fun requestParentArea() {
        when (app.settings.parentLock) {
            ParentLock.HOLD -> openParentArea()
            ParentLock.DEVICE -> authenticateDevice(::openParentArea)
            ParentLock.PIN -> if (app.hasPin()) pinGate = true else openParentArea()
        }
    }

    private fun openParentArea() {
        pinGate = false
        parentOpen = true
    }

    private fun deviceSecure() = getSystemService(KeyguardManager::class.java).isDeviceSecure

    /** Asks for the phone's own screen lock (PIN, pattern, password or fingerprint). */
    private fun authenticateDevice(onSuccess: () -> Unit) {
        if (!deviceSecure()) {
            onSuccess()
            return
        }
        if (authRunning()) return // a second prompt on top of the first one crashes on some phones
        authUntil = SystemClock.elapsedRealtime() + 120_000
        val title = "Eltern-Bereich"
        val subtitle = "Mit der Bildschirmsperre des Handys entsperren"
        runCatching { showDevicePrompt(title, subtitle, onSuccess) }.onFailure { finishAuth(null) }
    }

    private fun showDevicePrompt(title: String, subtitle: String, onSuccess: () -> Unit) {
        when {
            Build.VERSION.SDK_INT >= 30 -> BiometricPrompt.Builder(this)
                .setTitle(title).setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
                .authenticate(CancellationSignal(), mainExecutor, promptCallback(onSuccess))
            Build.VERSION.SDK_INT == 29 -> @Suppress("DEPRECATION") BiometricPrompt.Builder(this)
                .setTitle(title).setSubtitle(subtitle)
                .setDeviceCredentialAllowed(true)
                .build()
                .authenticate(CancellationSignal(), mainExecutor, promptCallback(onSuccess))
            else -> {
                @Suppress("DEPRECATION")
                val intent = getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent(title, subtitle)
                if (intent == null) {
                    finishAuth(onSuccess)
                } else {
                    onDeviceUnlocked = onSuccess
                    keyguardRequest.launch(intent)
                }
            }
        }
    }

    @RequiresApi(28)
    private fun promptCallback(onSuccess: () -> Unit) = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = finishAuth(onSuccess)
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = finishAuth(null)
    }

    /** Opens the parent area on success before re-pinning is allowed again, otherwise re-pins. */
    private fun finishAuth(onSuccess: (() -> Unit)?) {
        onSuccess?.invoke()
        authUntil = 0L
        pinIfWanted()
    }

    private fun authRunning() = SystemClock.elapsedRealtime() < authUntil

    /** Re-pins the app when parents enabled automatic pinning and are not in the parent area. */
    private fun pinIfWanted() {
        if (!app.settings.autoPin || parentOpen || pinGate || authRunning() || isPinned()) return
        runCatching { startLockTask() }
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
