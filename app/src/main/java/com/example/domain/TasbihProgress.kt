package com.example.domain

/**
 * Pure helpers for the electronic tasbih counter so the daily rollover logic can be unit tested
 * without touching DataStore or the Android framework.
 */
object TasbihProgress {

    /**
     * Returns the counter value after a tick.
     *
     * The counter restarts whenever the day key changes, so a count saved yesterday never leaks
     * into today. The result is never negative so a reset can never produce a negative display.
     */
    fun nextCount(
        currentDayKey: String,
        currentCount: Int,
        targetDayKey: String,
        delta: Int
    ): Int {
        val base = if (currentDayKey == targetDayKey) currentCount else 0
        return (base + delta).coerceAtLeast(0)
    }

    /** True when the stored counter belongs to a previous day and must be treated as zero. */
    fun isStale(storedDayKey: String, targetDayKey: String): Boolean =
        storedDayKey.isNotBlank() && storedDayKey != targetDayKey
}
