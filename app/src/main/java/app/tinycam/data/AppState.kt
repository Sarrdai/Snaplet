package app.tinycam.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Single source of truth for settings, daily usage and the media list. */
class AppState(context: Context) {
    private val store = SettingsStore(context)
    val media = MediaRepository(context)
    val thumbs = Thumbnails()
    private val exporter = MediaExporter(context)

    /** Exports outlive the camera screen, so a copy is not cut off when the child's time is up. */
    private val exportScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The chosen folder was removed or access was revoked; shown in the parent area. */
    var exportFailed by mutableStateOf(false)
        private set

    var settings by mutableStateOf(store.loadSettings())
        private set
    private var usageState by mutableStateOf(store.loadUsage())
    val usage: DailyUsage get() = usageState.on(today())

    val items = mutableStateListOf<MediaItem>().apply { addAll(media.list()) }

    fun updateSettings(s: Settings) {
        settings = s
        store.saveSettings(s)
    }

    fun addCapture(item: MediaItem) {
        items.add(0, item)
        setUsage(usage.copy(captures = usage.captures + 1))
    }

    /** Called when saving failed; the capture does not count against the daily limit. */
    fun dropCapture(item: MediaItem) {
        items.remove(item)
        media.delete(item)
        setUsage(usage.copy(captures = (usage.captures - 1).coerceAtLeast(0)))
    }

    fun addUsageTime(ms: Long) {
        if (ms > 0) setUsage(usage.copy(usageMs = usage.usageMs + ms))
    }

    /** Called once a recording is final (after filters were baked in). */
    fun exportIfWanted(item: MediaItem) {
        val folder = settings.exportFolder?.takeIf { settings.autoExport } ?: return
        exportScope.launch {
            if (!item.file.exists()) return@launch
            val ok = exporter.export(item, Uri.parse(folder))
            if (!ok) exportFailed = true
        }
    }

    /** Parents picked a folder in the system picker; turns automatic export on. */
    fun setExportFolder(tree: Uri) {
        exporter.adopt(tree)
        exportFailed = false
        updateSettings(settings.copy(autoExport = true, exportFolder = tree.toString()))
    }

    /** True if the export folder is set and still writable. */
    fun exportFolderUsable() = settings.exportFolder?.let { exporter.hasAccess(Uri.parse(it)) } == true

    fun resetToday() = setUsage(DailyUsage(today()))

    fun hasPin() = store.hasPin()
    fun setPin(pin: String) = store.setPin(pin)
    fun checkPin(pin: String) = store.checkPin(pin)

    fun delete(selection: List<MediaItem>) {
        selection.forEach(media::delete)
        items.removeAll(selection.toSet())
    }

    private fun setUsage(u: DailyUsage) {
        usageState = u
        store.saveUsage(u)
    }

    private fun today() = LocalDate.now().toString()
}
