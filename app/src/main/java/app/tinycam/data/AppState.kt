package app.tinycam.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/** Single source of truth for settings, daily usage and the media list. */
class AppState(context: Context) {
    private val store = SettingsStore(context)
    val media = MediaRepository(context)
    val thumbs = Thumbnails()

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

    fun resetToday() = setUsage(DailyUsage(today()))

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
