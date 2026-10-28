package app.tinycam.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.tinycam.data.AppState
import app.tinycam.data.CaptureMode
import app.tinycam.data.MediaItem
import app.tinycam.data.Thumb
import app.tinycam.data.budgetTokens
import app.tinycam.data.decodeUpright
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Binds CameraX to the kid UI. Only one use case is active at a time, which works on every device. */
@Composable
fun CameraScreen(app: AppState, hasAudio: Boolean, onOpenParent: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settings = app.settings
    val audio by rememberUpdatedState(hasAudio)
    var mode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var selfie by remember { mutableStateOf(false) }
    var effect by remember { mutableStateOf(Effect.NONE) }
    if (!settings.video && mode == CaptureMode.VIDEO) mode = CaptureMode.PHOTO
    if (!settings.selfie && selfie) selfie = false
    if (!settings.effects && effect != Effect.NONE) effect = Effect.NONE

    val controller = remember {
        LifecycleCameraController(context).apply {
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            isPinchToZoomEnabled = false
            isTapToFocusEnabled = false
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(lifecycleOwner, controller) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    LaunchedEffect(mode) {
        controller.setEnabledUseCases(
            if (mode == CaptureMode.VIDEO) CameraController.VIDEO_CAPTURE else CameraController.IMAGE_CAPTURE,
        )
    }
    LaunchedEffect(selfie) {
        val selector = if (selfie) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        // Devices without a front camera simply stay on the back camera
        if (runCatching { controller.hasCamera(selector) }.getOrDefault(true)) controller.cameraSelector = selector
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    // SurfaceView is fastest; color filters need the TextureView path, so only use it when effects are on
    LaunchedEffect(settings.effects) {
        previewView.controller = null
        previewView.implementationMode =
            if (settings.effects) PreviewView.ImplementationMode.COMPATIBLE else PreviewView.ImplementationMode.PERFORMANCE
        previewView.controller = controller
    }
    val activeEffect = if (mode == CaptureMode.PHOTO) effect else Effect.NONE
    LaunchedEffect(activeEffect) {
        val m = activeEffect.matrix
        if (m == null) {
            previewView.setLayerType(View.LAYER_TYPE_NONE, null)
        } else {
            previewView.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = ColorMatrixColorFilter(m) })
        }
    }

    val actions = remember { CameraXActions(app, controller, previewView, context, scope, { audio }, { selfie }) }

    KidCameraUi(
        settings = settings,
        items = app.items,
        thumbs = app.thumbs,
        tokens = settings.budgetTokens(app.usage),
        mode = mode,
        onModeChange = { mode = it },
        selfie = selfie,
        onSelfieChange = { selfie = it },
        effect = effect,
        onEffectChange = { effect = it },
        actions = actions,
        onOpenParent = onOpenParent,
        filterPreview = false,
    ) {
        AndroidView({ previewView }, Modifier.fillMaxSize())
    }
}

private class CameraXActions(
    private val app: AppState,
    private val controller: LifecycleCameraController,
    private val previewView: PreviewView,
    context: Context,
    private val scope: CoroutineScope,
    private val hasAudio: () -> Boolean,
    private val selfie: () -> Boolean,
) : CameraActions {
    private val executor = ContextCompat.getMainExecutor(context)
    private var recording: Recording? = null
    private var pendingVideo: Pending? = null

    private class Pending(val item: MediaItem, val snapshot: Bitmap?)

    override fun takePhoto(effect: Effect): Captured {
        val item = app.media.newPhoto()
        val snapshot = previewView.bitmap?.let { renderEffect(it, effect) }
        rememberThumb(item, snapshot, null)
        app.addCapture(item)
        // Selfies are saved mirrored, exactly as the child saw them on screen
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = selfie() }
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(item.file).setMetadata(metadata).build(), executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (effect != Effect.NONE) scope.launch { applyEffect(item, effect) }
                }

                override fun onError(exception: ImageCaptureException) = drop(item)
            },
        )
        return Captured(item, snapshot?.asImageBitmap())
    }

    @SuppressLint("MissingPermission") // audio is only enabled when the permission was granted
    override fun startVideo(): Boolean {
        val item = app.media.newVideo()
        val snapshot = previewView.bitmap
        return runCatching {
            recording = controller.startRecording(
                FileOutputOptions.Builder(item.file).build(), AudioConfig.create(hasAudio()), executor,
            ) { event ->
                if (event is VideoRecordEvent.Finalize && event.hasError() && item.file.length() == 0L) {
                    pendingVideo = null
                    drop(item)
                }
            }
            pendingVideo = Pending(item, snapshot)
            true
        }.getOrDefault(false)
    }

    override fun stopVideo(elapsedMs: Long): Captured? {
        recording?.stop()
        recording = null
        val p = pendingVideo ?: return null
        pendingVideo = null
        rememberThumb(p.item, p.snapshot, elapsedMs)
        app.addCapture(p.item)
        return Captured(p.item, p.snapshot?.asImageBitmap())
    }

    override fun setZoom(linear: Float) {
        runCatching { controller.setLinearZoom(linear) }
    }

    /** Re-encodes the saved photo with the effect baked in. The original stays if memory runs out. */
    private suspend fun applyEffect(item: MediaItem, effect: Effect) = withContext(Dispatchers.Default) {
        runCatching {
            val src = decodeUpright(item.file, Int.MAX_VALUE) ?: return@runCatching
            val out = renderEffect(src, effect)
            src.recycle()
            item.file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            out.recycle()
        }
    }

    private fun drop(item: MediaItem) {
        if (item in app.items) app.dropCapture(item) else app.media.delete(item)
    }

    /** The live preview frame doubles as an instant thumbnail, so the strip never waits for disk. */
    private fun rememberThumb(item: MediaItem, snapshot: Bitmap?, durationMs: Long?) {
        snapshot ?: return
        val scale = 360f / maxOf(snapshot.width, snapshot.height)
        val small = Bitmap.createScaledBitmap(
            snapshot, (snapshot.width * scale).toInt().coerceAtLeast(1), (snapshot.height * scale).toInt().coerceAtLeast(1), true,
        )
        app.thumbs.put(item.id, Thumb(small.asImageBitmap(), durationMs))
    }
}
