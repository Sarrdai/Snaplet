package app.tinycam

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.runtime.CompositionLocalProvider
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
import app.tinycam.data.MediaExporter
import app.tinycam.data.MediaItem
import app.tinycam.data.ParentLock
import app.tinycam.ui.ParentPinGate
import app.tinycam.data.isTimeUp
import app.tinycam.ui.CameraScreen
import app.tinycam.ui.HoldIndicator
import app.tinycam.ui.HoldState
import app.tinycam.ui.LocalHoldIndicator
import app.tinycam.ui.ParentScreen
import app.tinycam.ui.PermissionScreen
import app.tinycam.ui.SleepScreen
import app.tinycam.ui.styleFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var app: AppState
    private var hasCamera by mutableStateOf(false)
    private var hasAudio by mutableStateOf(false)
    private var pinned by mutableStateOf(false)
    private var parentOpen by mutableStateOf(false)
    private var hasCameraBlocked by mutableStateOf(false)
    // Runs after the parent PIN was entered; null while the PIN gate is closed
    private var pinGateAction by mutableStateOf<(() -> Unit)?>(null)
    private var askedOnce = false
    // The system dialog is showing; the activity resumes underneath it before the answer arrives
    private var permissionPending = false
    private var onDeviceUnlocked: (() -> Unit)? = null
    // While the device unlock is showing, our activity pauses and resumes; re-pinning then would kill the prompt.
    // Expires on its own in case the system never reports back.
    private var authUntil = 0L
    // Answers from an earlier, abandoned prompt can arrive much later; only the current attempt counts
    private var authId = 0
    // The unlock waits until unpinning has settled; if Android locked the phone meanwhile, that unlock is the check
    private var pendingUnlock: (() -> Unit)? = null
    private var lockedWhileUnpinning = false
    private var repinOnFail = false
    // Auto-pin only on a real start, not on every resume: parents who unpinned with the phone's PIN must stay out
    private var pinOnResume = true

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionPending = false
            refreshPermissions()
        }

    // System folder picker for the automatic export; access is kept via a persisted URI permission
    private val exportFolderRequest =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
            if (tree != null) runCatching { app.setExportFolder(tree) }
        }

    // Android 8 and 9: the system's "confirm device credential" screen
    private val keyguardRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val success = onDeviceUnlocked
            onDeviceUnlocked = null
            finishAuth(authId, success.takeIf { result.resultCode == RESULT_OK })
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
        refreshPermissions() // parents may have granted the camera in the system settings
        if (pinOnResume) {
            pinOnResume = false
            pinIfWanted()
        }
    }

    override fun onStop() {
        super.onStop()
        if (pendingUnlock != null && keyguard().isKeyguardLocked) lockedWhileUnpinning = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
            pinned = isPinned()
            continueUnlock()
        }
    }

    @Composable
    private fun Root() {
        val holdIndicator = remember { HoldState() }
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalHoldIndicator provides holdIndicator) { Screens() }
            HoldIndicator(holdIndicator, remember(app.settings.skin) { styleFor(app.settings.skin) }.accent)
        }
    }

    @Composable
    private fun Screens() {
        val settings = app.settings
        val usage = app.usage
        Box(Modifier.fillMaxSize()) {
            // The child's screens stay left-to-right in every language: "shutter left/right" means the physical side
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                when {
                    !hasCamera -> PermissionScreen(hasCameraBlocked, ::requestPermissions, ::openAppSettings)
                    settings.isTimeUp(usage) -> SleepScreen(settings.skin, ::requestParentArea)
                    else -> CameraScreen(app, hasAudio, ::requestParentArea)
                }
            }
            pinGateAction?.let { action ->
                ParentPinGate(
                    checkPin = app::checkPin,
                    deviceSecure = deviceSecure(),
                    // The PIN gate stays open behind the device prompt in case that is cancelled
                    onDeviceUnlock = {
                        authenticateDevice {
                            pinGateAction = null
                            action()
                        }
                    },
                    onSuccess = {
                        pinGateAction = null
                        action()
                    },
                    onCancel = { pinGateAction = null },
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
                    exportFolder = settings.exportFolder?.takeIf { app.exportFolderUsable() }
                        ?.let { MediaExporter.label(Uri.parse(it)) },
                    exportFailed = app.exportFailed || (settings.autoExport && !app.exportFolderUsable()),
                    onChooseExportFolder = ::chooseExportFolder,
                    onClose = {
                        parentOpen = false
                        pinIfWanted()
                    },
                )
            }
        }
    }

    /** Called after the lock was held for 3 seconds. */
    private fun requestParentArea() = withParentCheck { parentOpen = true }

    /** Runs [action] once the parent check chosen in the parent area passed (the 3 s hold before counts for HOLD). */
    private fun withParentCheck(action: () -> Unit) {
        when (app.settings.parentLock) {
            ParentLock.HOLD -> action()
            ParentLock.DEVICE -> authenticateDevice(action)
            ParentLock.PIN -> if (app.hasPin()) pinGateAction = action else action()
        }
    }

    private fun keyguard() = getSystemService(KeyguardManager::class.java)

    private fun deviceSecure() = keyguard().isDeviceSecure

    /**
     * Asks for the phone's own screen lock (PIN, pattern, password or fingerprint). A pinned screen blocks
     * the unlock dialog, so the app unpins first. If Android then locks the phone ("ask for PIN before
     * unpinning"), unlocking it is the check.
     */
    private fun authenticateDevice(onSuccess: () -> Unit) {
        if (!deviceSecure()) {
            onSuccess()
            return
        }
        // A new attempt replaces one that never answered (the dialog covers the app while it is really showing)
        repinOnFail = isPinned() || (authRunning() && repinOnFail)
        val id = ++authId
        authUntil = SystemClock.elapsedRealtime() + 120_000
        pendingUnlock = null
        if (!repinOnFail) {
            showDevicePrompt(id, onSuccess)
            return
        }
        pendingUnlock = onSuccess
        lockedWhileUnpinning = false
        runCatching { stopLockTask() }
        // Without a lock screen no focus change follows, so check again once unpinning has settled
        window.decorView.postDelayed(::continueUnlock, 800)
    }

    /** Second half of [authenticateDevice], once the screen is unpinned and belongs to the app again. */
    private fun continueUnlock() {
        val next = pendingUnlock ?: return
        if (!hasWindowFocus() || keyguard().isKeyguardLocked) return
        pendingUnlock = null
        if (lockedWhileUnpinning) finishAuth(authId, next) else showDevicePrompt(authId, next)
    }

    private fun showDevicePrompt(id: Int, onSuccess: () -> Unit) {
        val title = getString(R.string.parent_title)
        val subtitle = getString(R.string.device_prompt_subtitle)
        runCatching {
            when {
                Build.VERSION.SDK_INT >= 30 -> BiometricPrompt.Builder(this)
                    .setTitle(title).setSubtitle(subtitle)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                    .authenticate(CancellationSignal(), mainExecutor, promptCallback(id, onSuccess))
                Build.VERSION.SDK_INT == 29 -> @Suppress("DEPRECATION") BiometricPrompt.Builder(this)
                    .setTitle(title).setSubtitle(subtitle)
                    .setDeviceCredentialAllowed(true)
                    .build()
                    .authenticate(CancellationSignal(), mainExecutor, promptCallback(id, onSuccess))
                else -> {
                    @Suppress("DEPRECATION")
                    val intent = keyguard().createConfirmDeviceCredentialIntent(title, subtitle)
                    if (intent == null) {
                        finishAuth(id, onSuccess)
                    } else {
                        onDeviceUnlocked = onSuccess
                        keyguardRequest.launch(intent)
                    }
                }
            }
        }.onFailure { finishAuth(id, null) }
    }

    @RequiresApi(28)
    private fun promptCallback(id: Int, onSuccess: () -> Unit) = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = finishAuth(id, onSuccess)
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = finishAuth(id, null)
    }

    /** Opens the parent area on success; on failure restores the pinning only if the attempt removed it. */
    private fun finishAuth(id: Int, onSuccess: (() -> Unit)?) {
        if (id != authId) return
        authId++
        authUntil = 0L
        onSuccess?.invoke()
        if (onSuccess == null && repinOnFail && !isPinned()) runCatching { startLockTask() }
        repinOnFail = false
    }

    private fun authRunning() = SystemClock.elapsedRealtime() < authUntil

    /** Re-pins the app when parents enabled automatic pinning and are not in the parent area. */
    private fun pinIfWanted() {
        if (!app.settings.autoPin || parentOpen || pinGateAction != null || authRunning() || isPinned()) return
        runCatching { startLockTask() }
    }

    private fun refreshPermissions() {
        hasCamera = granted(Manifest.permission.CAMERA)
        hasAudio = granted(Manifest.permission.RECORD_AUDIO)
        // Android no longer shows its dialog: only the system settings can grant the camera now
        hasCameraBlocked = askedOnce && !permissionPending && !hasCamera && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        if (hasCameraBlocked || permissionPending) return
        askedOnce = true
        permissionPending = true
        permissionRequest.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    /** Leaves the app for the system settings, so it sits behind the parent check (Families policy). */
    private fun openAppSettings() = withParentCheck {
        // Other apps cannot open while the screen is pinned
        if (isPinned()) stopLockTask()
        startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
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

    private fun chooseExportFolder() {
        // The picker cannot open while the screen is pinned
        if (isPinned()) stopLockTask()
        runCatching { exportFolderRequest.launch(app.settings.exportFolder?.let(Uri::parse)) }
    }

    private fun share(selection: List<MediaItem>) {
        if (selection.isEmpty()) return
        // Other apps cannot open while the screen is pinned
        if (isPinned()) stopLockTask()
        startActivity(app.media.shareIntent(selection))
    }
}
