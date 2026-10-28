package app.tinycam.data

import android.content.Context
import androidx.core.content.edit

enum class Skin(val label: String) {
    CLASSIC("Klassisch"), RAINBOW("Regenbogen"), SPACE("Raumschiff"), DINO("Dino"), LEOPARD("Leopard")
}

enum class Side(val label: String) { LEFT("Links"), RIGHT("Rechts") }

enum class CaptureMode { PHOTO, VIDEO }

/**
 * Everything parents can configure. A value of 0 for a limit means "off".
 * Each kid feature beyond plain photos is switched on separately.
 */
data class Settings(
    val skin: Skin = Skin.RAINBOW,
    val side: Side = Side.RIGHT,
    val cooldownSec: Int = 3,
    val dailyCaptures: Int = 0,
    val dailyMinutes: Int = 0,
    val maxVideoSec: Int = 30,
    val stripCount: Int = 5,
    val video: Boolean = false,
    val gallery: Boolean = false,
    val selfie: Boolean = false,
    val zoom: Boolean = false,
    val effects: Boolean = false,
    val timer: Boolean = false,
    val timerSec: Int = 3,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tinycam", Context.MODE_PRIVATE)

    fun loadSettings(): Settings {
        val d = Settings()
        // Older versions used learning levels: 2 unlocked video, 3 also the gallery
        val level = prefs.getInt("level", 1)
        return Settings(
            skin = enumOr(prefs.getString("skin", null), d.skin),
            side = enumOr(prefs.getString("side", null), d.side),
            cooldownSec = prefs.getInt("cooldownSec", d.cooldownSec),
            dailyCaptures = prefs.getInt("dailyCaptures", d.dailyCaptures),
            dailyMinutes = prefs.getInt("dailyMinutes", d.dailyMinutes),
            maxVideoSec = prefs.getInt("maxVideoSec", d.maxVideoSec),
            stripCount = prefs.getInt("stripCount", d.stripCount),
            video = prefs.getBoolean("video", level >= 2),
            gallery = prefs.getBoolean("gallery", level >= 3),
            selfie = prefs.getBoolean("selfie", d.selfie),
            zoom = prefs.getBoolean("zoom", d.zoom),
            effects = prefs.getBoolean("effects", d.effects),
            timer = prefs.getBoolean("timer", d.timer),
            timerSec = prefs.getInt("timerSec", d.timerSec),
        )
    }

    fun saveSettings(s: Settings) = prefs.edit {
        remove("level")
        putString("skin", s.skin.name)
        putString("side", s.side.name)
        putInt("cooldownSec", s.cooldownSec)
        putInt("dailyCaptures", s.dailyCaptures)
        putInt("dailyMinutes", s.dailyMinutes)
        putInt("maxVideoSec", s.maxVideoSec)
        putInt("stripCount", s.stripCount)
        putBoolean("video", s.video)
        putBoolean("gallery", s.gallery)
        putBoolean("selfie", s.selfie)
        putBoolean("zoom", s.zoom)
        putBoolean("effects", s.effects)
        putBoolean("timer", s.timer)
        putInt("timerSec", s.timerSec)
    }

    fun loadUsage() = DailyUsage(
        day = prefs.getString("usageDay", "") ?: "",
        captures = prefs.getInt("usageCaptures", 0),
        usageMs = prefs.getLong("usageMs", 0),
    )

    fun saveUsage(u: DailyUsage) = prefs.edit {
        putString("usageDay", u.day)
        putInt("usageCaptures", u.captures)
        putLong("usageMs", u.usageMs)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
