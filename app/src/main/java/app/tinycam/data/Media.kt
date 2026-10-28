package app.tinycam.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class MediaItem(val file: File, val isVideo: Boolean) {
    val id: String get() = file.name
}

/** Photos and videos live in app-private storage; parents export them via the share sheet. */
class MediaRepository(private val context: Context) {
    private val dir = File(context.filesDir, "media").apply { mkdirs() }

    fun list(): List<MediaItem> = dir.listFiles().orEmpty()
        .filter { it.extension == "jpg" || it.extension == "mp4" }
        .sortedByDescending { it.nameWithoutExtension.substringAfter('_').toLongOrNull() ?: it.lastModified() }
        .map { MediaItem(it, it.extension == "mp4") }

    fun newPhoto() = MediaItem(File(dir, "IMG_${System.currentTimeMillis()}.jpg"), isVideo = false)
    fun newVideo() = MediaItem(File(dir, "VID_${System.currentTimeMillis()}.mp4"), isVideo = true)

    fun delete(item: MediaItem) { item.file.delete() }
    fun deleteAll() { dir.listFiles()?.forEach { it.delete() } }

    fun shareIntent(items: List<MediaItem>): Intent {
        val uris = items.mapTo(ArrayList<Uri>()) {
            FileProvider.getUriForFile(context, "${context.packageName}.files", it.file)
        }
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = if (items.all { !it.isVideo }) "image/jpeg" else "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Fotos teilen")
    }
}

class Thumb(val image: ImageBitmap, val durationMs: Long? = null)

/** Small in-memory cache of downsampled thumbnails, decoded off the main thread. */
class Thumbnails(private val sizePx: Int = 360) {
    private val cache = LruCache<String, Thumb>(64)

    fun peek(id: String): Thumb? = cache.get(id)
    fun put(id: String, thumb: Thumb) { cache.put(id, thumb) }

    suspend fun load(item: MediaItem): Thumb? = cache.get(item.id) ?: withContext(Dispatchers.IO) {
        runCatching { if (item.isVideo) videoThumb(item.file, sizePx) else photoThumb(item.file) }
            .getOrNull()?.also { cache.put(item.id, it) }
    }

    private fun photoThumb(file: File) = decodeUpright(file, sizePx)?.let { Thumb(it.asImageBitmap()) }
}

suspend fun loadFullImage(file: File, maxPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching { decodeUpright(file, maxPx)?.asImageBitmap() }.getOrNull()
}

/** Decodes [file] downsampled to roughly [targetPx] and rotated upright according to its EXIF data. */
fun decodeUpright(file: File, targetPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
    val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null
    val rotation = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) return bmp
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
}

private fun videoThumb(file: File, sizePx: Int): Thumb? {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(file.path)
        val duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        val frame = if (Build.VERSION.SDK_INT >= 27) {
            r.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, sizePx, sizePx)
        } else {
            r.getFrameAtTime(0)
        } ?: return null
        return Thumb(frame.asImageBitmap(), duration)
    } finally {
        r.release()
    }
}
