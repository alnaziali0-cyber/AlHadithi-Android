package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The electronic tasbih counter must survive rotation, navigation and a day change without ever
 * losing the count or leaking yesterday's number into today.
 */
class TasbihProgressTest {

    @Test
    fun `a tick increases the counter of the same day`() {
        assertEquals(1, TasbihProgress.nextCount("2026-03-15", 0, "2026-03-15", 1))
        assertEquals(8, TasbihProgress.nextCount("2026-03-15", 7, "2026-03-15", 1))
    }

    @Test
    fun `the counter restarts when the day changes`() {
        assertEquals(1, TasbihProgress.nextCount("2026-03-14", 250, "2026-03-15", 1))
    }

    @Test
    fun `a negative delta never produces a negative counter`() {
        assertEquals(0, TasbihProgress.nextCount("2026-03-15", 0, "2026-03-15", -1))
        assertEquals(3, TasbihProgress.nextCount("2026-03-15", 4, "2026-03-15", -1))
    }

    @Test
    fun `a stale day is detected`() {
        assertTrue(TasbihProgress.isStale("2026-03-14", "2026-03-15"))
        assertFalse(TasbihProgress.isStale("2026-03-15", "2026-03-15"))
        assertFalse(TasbihProgress.isStale("", "2026-03-15"))
    }
}
