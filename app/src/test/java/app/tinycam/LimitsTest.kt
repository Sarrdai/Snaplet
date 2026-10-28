package app.tinycam

import app.tinycam.data.DailyUsage
import app.tinycam.data.Settings
import app.tinycam.data.Tokens
import app.tinycam.data.budgetTokens
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
    fun tokens_oneSymbolPerCapture_upToTen() {
        val s = Settings(dailyCaptures = 5)
        assertEquals(Tokens(3, 5), s.budgetTokens(DailyUsage("d", captures = 2)))
        assertNull(Settings(dailyCaptures = 0).budgetTokens(DailyUsage("d")))
    }

    @Test
    fun tokens_scaleToTen_forLargeLimits() {
        val s = Settings(dailyCaptures = 50)
        assertEquals(Tokens(10, 10), s.budgetTokens(DailyUsage("d", captures = 0)))
        assertEquals(Tokens(1, 10), s.budgetTokens(DailyUsage("d", captures = 49)))
        assertEquals(Tokens(0, 10), s.budgetTokens(DailyUsage("d", captures = 50)))
    }

    @Test
    fun features_areOffByDefault() {
        val s = Settings()
        assertFalse(s.video || s.gallery || s.selfie || s.zoom || s.effects || s.timer)
    }
}
