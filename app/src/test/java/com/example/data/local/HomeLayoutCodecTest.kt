package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Protects the fixed home hierarchy while keeping old stored preference values upgrade-safe. */
class HomeLayoutCodecTest {
    @Test
    fun `an empty value restores the fixed order`() {
        val (cards, hidden) = HomeLayoutCodec.decode(null)
        assertEquals(HomeCard.defaultOrder, cards)
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `legacy custom order and hidden cards do not break the required hierarchy`() {
        val legacy = "quick_access,-prayer_hero,daily_tracking,daily_hadith"
        val (cards, hidden) = HomeLayoutCodec.decode(legacy)
        assertEquals(HomeCard.defaultOrder, cards)
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `unknown or malformed legacy preferences restore the fixed order`() {
        listOf("not_a_card", "--,,,", "prayer_hero,unknown,daily_dua").forEach { raw ->
            val (cards, hidden) = HomeLayoutCodec.decode(raw)
            assertEquals(HomeCard.defaultOrder, cards)
            assertTrue(hidden.isEmpty())
        }
    }

    @Test
    fun `daily goals codec round trip`() {
        val goals = setOf(DailyGoal.TASBIH, DailyGoal.QURAN_READING)
        val encoded = DailyGoal.encode(goals)
        assertEquals(goals, DailyGoal.decode(encoded))
    }

    @Test
    fun `daily goals decode of an empty value is empty`() {
        assertTrue(DailyGoal.decode(null).isEmpty())
        assertTrue(DailyGoal.decode("").isEmpty())
    }
}
