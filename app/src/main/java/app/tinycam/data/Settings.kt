package app.tinycam.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import app.tinycam.R
import java.security.MessageDigest
import java.security.SecureRandom

enum class Skin(@StringRes val label: Int) {
    CLASSIC(R.string.skin_classic), RAINBOW(R.string.skin_rainbow), SPACE(R.string.skin_space), DINO(R.string.skin_dino),
    LEOPARD(R.string.skin_leopard), UNICORN(R.string.skin_unicorn), SHARK(R.string.skin_shark), ROBOT(R.string.skin_robot),
    FIRE_TRUCK(R.string.skin_fire_truck), MONSTER(R.string.skin_monster), ICE_CREAM(R.string.skin_ice_cream),
}

/** Where the shutter sits. CENTER uses the round shutter at the bottom. */
enum class Side(@StringRes val label: Int) { LEFT(R.string.side_left), CENTER(R.string.side_center), RIGHT(R.string.side_right) }

enum class CaptureMode { PHOTO, VIDEO }

/** How parents get into the parent area after holding the lock. */
enum class ParentLock(@StringRes val label: Int, @StringRes val info: Int) {
    HOLD(R.string.lock_hold, R.string.lock_info_hold),
    DEVICE(R.string.lock_device, R.string.lock_info_device),
    PIN(R.string.lock_pin, R.string.lock_info_pin),
}

enum class PhotoAspect(@StringRes val label: Int) { STANDARD(R.string.aspect_standard), WIDE(R.string.aspect_wide), SQUARE(R.string.aspect_square) }

/** [longEdge] caps the photo size in pixels; null means the camera's full resolution. */
enum class PhotoSize(@StringRes val label: Int, val longEdge: Int?) {
    SMALL(R.string.size_small, 1600), MEDIUM(R.string.size_medium, 2560), FULL(R.string.size_full, null)
}

enum class VideoQuality(@StringRes val label: Int) { SD(R.string.quality_sd), HD(R.string.quality_hd), FHD(R.string.quality_fhd) }

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
    /** The child may switch between [kidSkins] with a tool button. */
    val skinChoice: Boolean = false,
    val kidSkins: Set<Skin> = setOf(Skin.RAINBOW, Skin.SPACE, Skin.DINO, Skin.UNICORN, Skin.SHARK, Skin.FIRE_TRUCK),
    val parentLock: ParentLock = ParentLock.HOLD,
    val autoPin: Boolean = false,
    val photoAspect: PhotoAspect = PhotoAspect.STANDARD,
    val photoSize: PhotoSize = PhotoSize.MEDIUM,
    val videoQuality: VideoQuality = VideoQuality.HD,
    /** Copy every new recording into [exportFolder], a folder tree URI picked by the parents. */
    val autoExport: Boolean = false,
    val exportFolder: String? = null,
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
            skinChoice = prefs.getBoolean("skinChoice", d.skinChoice),
            kidSkins = prefs.getString("kidSkins", null)
                ?.split(',')?.mapNotNull { n -> Skin.entries.firstOrNull { it.name == n } }?.toSet()
                ?.ifEmpty { null } ?: d.kidSkins,
            parentLock = enumOr(prefs.getString("parentLock", null), d.parentLock),
            autoPin = prefs.getBoolean("autoPin", d.autoPin),
            photoAspect = enumOr(prefs.getString("photoAspect", null), d.photoAspect),
            photoSize = enumOr(prefs.getString("photoSize", null), d.photoSize),
            videoQuality = enumOr(prefs.getString("videoQuality", null), d.videoQuality),
            autoExport = prefs.getBoolean("autoExport", d.autoExport),
            exportFolder = prefs.getString("exportFolder", d.exportFolder),
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
        putBoolean("skinChoice", s.skinChoice)
        putString("kidSkins", s.kidSkins.joinToString(",") { it.name })
        putString("parentLock", s.parentLock.name)
        putBoolean("autoPin", s.autoPin)
        putString("photoAspect", s.photoAspect.name)
        putString("photoSize", s.photoSize.name)
        putString("videoQuality", s.videoQuality.name)
        putBoolean("autoExport", s.autoExport)
        if (s.exportFolder != null) putString("exportFolder", s.exportFolder) else remove("exportFolder")
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
