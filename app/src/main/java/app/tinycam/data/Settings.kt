package app.tinycam.data

import android.content.Context
import androidx.core.content.edit

enum class Skin(val label: String) {
    CLASSIC("Klassisch"), RAINBOW("Regenbogen"), SPACE("Raumschiff"), DINO("Dino"), LEOPARD("Leopard")
}

enum class Side(val label: String) { LEFT("Links"), RIGHT("Rechts") }

enum class CaptureMode { PHOTO, VIDEO }

/** Everything parents can configure. A value of 0 for a limit means "off". */
data class Settings(
    val level: Int = 1,
    val skin: Skin = Skin.RAINBOW,
    val side: Side = Side.RIGHT,
    val cooldownSec: Int = 3,
    val dailyCaptures: Int = 0,
    val dailyMinutes: Int = 0,
    val maxVideoSec: Int = 30,
    val stripCount: Int = 5,
) {
    val videoUnlocked get() = level >= 2
    val galleryUnlocked get() = level >= 3
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tinycam", Context.MODE_PRIVATE)

    fun loadSettings(): Settings {
        val d = Settings()
        return Settings(
            level = prefs.getInt("level", d.level),
            skin = enumOr(prefs.getString("skin", null), d.skin),
            side = enumOr(prefs.getString("side", null), d.side),
            cooldownSec = prefs.getInt("cooldownSec", d.cooldownSec),
            dailyCaptures = prefs.getInt("dailyCaptures", d.dailyCaptures),
            dailyMinutes = prefs.getInt("dailyMinutes", d.dailyMinutes),
            maxVideoSec = prefs.getInt("maxVideoSec", d.maxVideoSec),
            stripCount = prefs.getInt("stripCount", d.stripCount),
        )
    }

    fun saveSettings(s: Settings) = prefs.edit {
        putInt("level", s.level)
        putString("skin", s.skin.name)
        putString("side", s.side.name)
        putInt("cooldownSec", s.cooldownSec)
        putInt("dailyCaptures", s.dailyCaptures)
        putInt("dailyMinutes", s.dailyMinutes)
        putInt("maxVideoSec", s.maxVideoSec)
        putInt("stripCount", s.stripCount)
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
