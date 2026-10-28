package app.tinycam.data

import android.content.Context
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom

enum class Skin(val label: String) {
    CLASSIC("Klassisch"), RAINBOW("Regenbogen"), SPACE("Raumschiff"), DINO("Dino"), LEOPARD("Leopard")
}

/** Where the shutter sits. CENTER uses the round shutter at the bottom. */
enum class Side(val label: String) { LEFT("Links"), CENTER("Mitte"), RIGHT("Rechts") }

enum class CaptureMode { PHOTO, VIDEO }

/** How parents get into the parent area after holding the lock. */
enum class ParentLock(val label: String) { HOLD("Nur halten"), DEVICE("Geräte-Sperre"), PIN("Eigene PIN") }

enum class PhotoAspect(val label: String) { STANDARD("4:3"), WIDE("16:9"), SQUARE("1:1") }

/** [longEdge] caps the photo size in pixels; null means the camera's full resolution. */
enum class PhotoSize(val label: String, val longEdge: Int?) {
    SMALL("Klein (≈ 2 MP)", 1600), MEDIUM("Mittel (≈ 5 MP)", 2560), FULL("Voll", null)
}

enum class VideoQuality(val label: String) { SD("480p"), HD("720p"), FHD("1080p") }

/**
 * Everything parents can configure. Optional limits have an on/off flag next to their value,
 * so a value is remembered while its limit is switched off.
 */
data class Settings(
    val skin: Skin = Skin.RAINBOW,
    val side: Side = Side.RIGHT,
    val cooldownOn: Boolean = true,
    val cooldownSec: Int = 3,
    val captureLimitOn: Boolean = false,
    val dailyCaptures: Int = 20,
    val timeLimitOn: Boolean = false,
    val dailyMinutes: Int = 30,
    val videoLimitOn: Boolean = true,
    val maxVideoSec: Int = 30,
    val stripCount: Int = 5,
    val video: Boolean = false,
    val gallery: Boolean = false,
    val selfie: Boolean = false,
    val zoom: Boolean = false,
    val effects: Boolean = false,
    val timer: Boolean = false,
    val timerSec: Int = 3,
    val parentLock: ParentLock = ParentLock.HOLD,
    val autoPin: Boolean = false,
    val photoAspect: PhotoAspect = PhotoAspect.STANDARD,
    val photoSize: PhotoSize = PhotoSize.MEDIUM,
    val videoQuality: VideoQuality = VideoQuality.HD,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tinycam", Context.MODE_PRIVATE)

    fun loadSettings(): Settings {
        val d = Settings()
        // Older versions used learning levels (2 = video, 3 = gallery) and 0 for "limit off"
        val level = prefs.getInt("level", 1)
        fun limit(onKey: String, valueKey: String, defOn: Boolean, defValue: Int): Pair<Boolean, Int> {
            val value = prefs.getInt(valueKey, defValue)
            val on = prefs.getBoolean(onKey, if (prefs.contains(valueKey)) value > 0 else defOn)
            return on to (if (value > 0) value else defValue)
        }
        val cooldown = limit("cooldownOn", "cooldownSec", d.cooldownOn, d.cooldownSec)
        val captures = limit("captureLimitOn", "dailyCaptures", d.captureLimitOn, d.dailyCaptures)
        val minutes = limit("timeLimitOn", "dailyMinutes", d.timeLimitOn, d.dailyMinutes)
        val videoLen = limit("videoLimitOn", "maxVideoSec", d.videoLimitOn, d.maxVideoSec)
        return Settings(
            skin = enumOr(prefs.getString("skin", null), d.skin),
            side = enumOr(prefs.getString("side", null), d.side),
            cooldownOn = cooldown.first, cooldownSec = cooldown.second,
            captureLimitOn = captures.first, dailyCaptures = captures.second,
            timeLimitOn = minutes.first, dailyMinutes = minutes.second,
            videoLimitOn = videoLen.first, maxVideoSec = videoLen.second,
            stripCount = prefs.getInt("stripCount", d.stripCount),
            video = prefs.getBoolean("video", level >= 2),
            gallery = prefs.getBoolean("gallery", level >= 3),
            selfie = prefs.getBoolean("selfie", d.selfie),
            zoom = prefs.getBoolean("zoom", d.zoom),
            effects = prefs.getBoolean("effects", d.effects),
            timer = prefs.getBoolean("timer", d.timer),
            timerSec = prefs.getInt("timerSec", d.timerSec),
            parentLock = enumOr(prefs.getString("parentLock", null), d.parentLock),
            autoPin = prefs.getBoolean("autoPin", d.autoPin),
            photoAspect = enumOr(prefs.getString("photoAspect", null), d.photoAspect),
            photoSize = enumOr(prefs.getString("photoSize", null), d.photoSize),
            videoQuality = enumOr(prefs.getString("videoQuality", null), d.videoQuality),
        )
    }

    fun saveSettings(s: Settings) = prefs.edit {
        remove("level")
        putString("skin", s.skin.name)
        putString("side", s.side.name)
        putBoolean("cooldownOn", s.cooldownOn)
        putInt("cooldownSec", s.cooldownSec)
        putBoolean("captureLimitOn", s.captureLimitOn)
        putInt("dailyCaptures", s.dailyCaptures)
        putBoolean("timeLimitOn", s.timeLimitOn)
        putInt("dailyMinutes", s.dailyMinutes)
        putBoolean("videoLimitOn", s.videoLimitOn)
        putInt("maxVideoSec", s.maxVideoSec)
        putInt("stripCount", s.stripCount)
        putBoolean("video", s.video)
        putBoolean("gallery", s.gallery)
        putBoolean("selfie", s.selfie)
        putBoolean("zoom", s.zoom)
        putBoolean("effects", s.effects)
        putBoolean("timer", s.timer)
        putInt("timerSec", s.timerSec)
        putString("parentLock", s.parentLock.name)
        putBoolean("autoPin", s.autoPin)
        putString("photoAspect", s.photoAspect.name)
        putString("photoSize", s.photoSize.name)
        putString("videoQuality", s.videoQuality.name)
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

    // The parent PIN is only stored as a salted hash.
    fun hasPin() = prefs.contains("pinHash")

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        prefs.edit {
            putString("pinSalt", salt)
            putString("pinHash", hash(salt, pin))
        }
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString("pinSalt", null) ?: return false
        return MessageDigest.isEqual(hash(salt, pin).toByteArray(), (prefs.getString("pinHash", "") ?: "").toByteArray())
    }

    private fun hash(salt: String, pin: String) =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
