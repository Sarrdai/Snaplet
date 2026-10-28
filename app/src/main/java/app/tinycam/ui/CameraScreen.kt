package app.tinycam.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.ExifInterface
import android.view.View
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
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
import app.tinycam.data.PhotoAspect
import app.tinycam.data.PhotoSize
import app.tinycam.data.VideoQuality
import app.tinycam.data.Thumb
import app.tinycam.data.budgetTokens
import app.tinycam.data.decodeUpright
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            isPinchToZoomEnabled = false
            isTapToFocusEnabled = false
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(lifecycleOwner, controller) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    // The phone's own photo processing (multi-frame HDR, night, denoise), run by the vendor camera stack on the device
    var extensions by remember { mutableStateOf<ExtensionsManager?>(null) }
    LaunchedEffect(Unit) {
        extensions = runCatching {
            ExtensionsManager.getInstanceAsync(context, ProcessCameraProvider.getInstance(context).await()).await()
        }.getOrNull()
    }
    LaunchedEffect(mode, selfie, extensions) {
        val wanted = if (selfie) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        // Devices without a front camera simply stay on the back camera
        val lens = if (runCatching { controller.hasCamera(wanted) }.getOrDefault(true)) wanted else CameraSelector.DEFAULT_BACK_CAMERA
        val photo = mode == CaptureMode.PHOTO
        // Extensions cannot record video: leave them before enabling video, enter them after enabling photos
        if (!photo) controller.cameraSelector = lens
        controller.setEnabledUseCases(if (photo) CameraController.IMAGE_CAPTURE else CameraController.VIDEO_CAPTURE)
        if (photo) {
            val enhanced = extensions?.let { runCatching { enhancedSelector(it, lens) }.getOrNull() }
            if (enhanced == null || runCatching { controller.cameraSelector = enhanced }.isFailure) controller.cameraSelector = lens
        }
    }
    LaunchedEffect(settings.photoAspect, settings.photoSize) {
        controller.imageCaptureResolutionSelector = photoResolution(settings.photoAspect, settings.photoSize)
    }
    LaunchedEffect(settings.videoQuality) {
        val q = when (settings.videoQuality) {
            VideoQuality.SD -> Quality.SD
            VideoQuality.HD -> Quality.HD
            VideoQuality.FHD -> Quality.FHD
        }
        controller.videoCaptureQualitySelector = QualitySelector.from(q, FallbackStrategy.lowerQualityOrHigherThan(q))
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

    val square = settings.photoAspect == PhotoAspect.SQUARE
    val actions = remember { CameraXActions(app, controller, previewView, context, scope, { audio }, { selfie }, { square }) }

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
    private val square: () -> Boolean,
) : CameraActions {
    private val executor = ContextCompat.getMainExecutor(context)
    private var recording: Recording? = null
    private var pendingVideo: Pending? = null

    private class Pending(val item: MediaItem, val snapshot: Bitmap?)

    override fun takePhoto(effect: Effect): Captured {
        val item = app.media.newPhoto()
        val crop = square()
        val snapshot = previewView.bitmap?.let { renderEffect(if (crop) centerSquare(it) else it, effect) }
        rememberThumb(item, snapshot, null)
        app.addCapture(item)
        // Selfies are saved mirrored, exactly as the child saw them on screen
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = selfie() }
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(item.file).setMetadata(metadata).build(), executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (effect != Effect.NONE || crop) scope.launch { postProcess(item, effect, crop) }
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

    /** Re-encodes the saved photo cropped and with the effect baked in. The original stays if memory runs out. */
    private suspend fun postProcess(item: MediaItem, effect: Effect, crop: Boolean) = withContext(Dispatchers.Default) {
        runCatching {
            val exif = ExifInterface(item.file.path)
            val decoded = decodeUpright(item.file, Int.MAX_VALUE) ?: return@runCatching
            val src = if (crop) centerSquare(decoded) else decoded
            val out = renderEffect(src, effect)
            if (src !== out) src.recycle()
            item.file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            out.recycle()
            keepExif(exif, item.file.path)
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

/** Scene-aware auto mode if the phone offers it, otherwise its HDR mode. */
private fun enhancedSelector(manager: ExtensionsManager, lens: CameraSelector): CameraSelector? =
    listOf(ExtensionMode.AUTO, ExtensionMode.HDR)
        .firstOrNull { manager.isExtensionAvailable(lens, it) }
        ?.let { manager.getExtensionEnabledCameraSelector(lens, it) }

private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { c ->
    addListener({ c.resumeWith(runCatching { get() }) }, Runnable::run)
}

/** Re-encoding drops EXIF; restore capture date and camera data. The pixels are already upright. */
private fun keepExif(original: ExifInterface, path: String) {
    val tags = listOf(
        ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_ISO_SPEED_RATINGS, ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FLASH, ExifInterface.TAG_WHITE_BALANCE,
    )
    val target = ExifInterface(path)
    for (tag in tags) original.getAttribute(tag)?.let { target.setAttribute(tag, it) }
    target.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
    target.saveAttributes()
}

private fun centerSquare(b: Bitmap): Bitmap {
    val side = minOf(b.width, b.height)
    return Bitmap.createBitmap(b, (b.width - side) / 2, (b.height - side) / 2, side, side)
}

/** Square photos are taken as 4:3 and cropped afterwards. */
private fun photoResolution(aspect: PhotoAspect, size: PhotoSize): ResolutionSelector {
    val wide = aspect == PhotoAspect.WIDE
    val strategy = size.longEdge?.let { edge ->
        ResolutionStrategy(Size(edge, if (wide) edge * 9 / 16 else edge * 3 / 4), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
    } ?: ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(if (wide) AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY else AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(strategy)
        .build()
}
