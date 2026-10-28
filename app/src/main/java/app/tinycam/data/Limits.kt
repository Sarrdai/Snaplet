package app.tinycam.data

/** Captures and foreground time for one calendar day (ISO date). */
data class DailyUsage(val day: String = "", val captures: Int = 0, val usageMs: Long = 0) {
    /** Returns this usage if it belongs to [today], otherwise a fresh day. */
    fun on(today: String) = if (day == today) this else DailyUsage(today)
}

/** Remaining captures today, or null when the daily limit is off. */
fun Settings.capturesLeft(usage: DailyUsage): Int? =
    if (dailyCaptures <= 0) null else (dailyCaptures - usage.captures).coerceAtLeast(0)

fun Settings.isTimeUp(usage: DailyUsage): Boolean =
    dailyMinutes > 0 && usage.usageMs >= dailyMinutes * 60_000L

/** Video length limit in ms, or null when unlimited. */
fun Settings.maxVideoMs(): Long? = if (maxVideoSec <= 0) null else maxVideoSec * 1000L

/** Remaining captures as countable symbols: [filled] of [total] slots are still available. */
data class Tokens(val filled: Int, val total: Int)

/**
 * Up to 10 captures get one symbol each, so kids can count what is left without reading numbers.
 * Larger limits are shown as 10 symbols that empty proportionally.
 */
fun Settings.budgetTokens(usage: DailyUsage): Tokens? {
    val left = capturesLeft(usage) ?: return null
    if (dailyCaptures <= 10) return Tokens(left, dailyCaptures)
    return Tokens((left * 10 + dailyCaptures - 1) / dailyCaptures, 10)
}
