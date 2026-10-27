package app.tinycam

import app.tinycam.data.DailyUsage
import app.tinycam.data.Settings
import app.tinycam.data.capturesLeft
import app.tinycam.data.isTimeUp
import app.tinycam.data.maxVideoMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitsTest {
    @Test
    fun capturesLeft_isNull_whenLimitOff() {
        assertNull(Settings(dailyCaptures = 0).capturesLeft(DailyUsage("d", captures = 99)))
    }

    @Test
    fun capturesLeft_countsDown_andStopsAtZero() {
        val s = Settings(dailyCaptures = 20)
        assertEquals(15, s.capturesLeft(DailyUsage("d", captures = 5)))
        assertEquals(0, s.capturesLeft(DailyUsage("d", captures = 25)))
    }

    @Test
    fun timeUp_onlyWhenLimitReached() {
        val s = Settings(dailyMinutes = 15)
        assertFalse(s.isTimeUp(DailyUsage("d", usageMs = 14 * 60_000L)))
        assertTrue(s.isTimeUp(DailyUsage("d", usageMs = 15 * 60_000L)))
        assertFalse(Settings(dailyMinutes = 0).isTimeUp(DailyUsage("d", usageMs = Long.MAX_VALUE)))
    }

    @Test
    fun usage_resetsOnNewDay() {
        val old = DailyUsage("2026-10-26", captures = 7, usageMs = 1000)
        assertEquals(DailyUsage("2026-10-27"), old.on("2026-10-27"))
        assertEquals(old, old.on("2026-10-26"))
    }

    @Test
    fun maxVideo_zeroMeansUnlimited() {
        assertNull(Settings(maxVideoSec = 0).maxVideoMs())
        assertEquals(30_000L, Settings(maxVideoSec = 30).maxVideoMs())
    }

    @Test
    fun levels_unlockFeatures() {
        assertFalse(Settings(level = 1).videoUnlocked)
        assertTrue(Settings(level = 2).videoUnlocked)
        assertFalse(Settings(level = 2).galleryUnlocked)
        assertTrue(Settings(level = 3).galleryUnlocked)
    }
}
