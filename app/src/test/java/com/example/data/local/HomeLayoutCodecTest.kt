package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The home dashboard layout is persisted as a compact string; these tests protect the codec so a
 * user's card order and hidden cards survive an upgrade of the app.
 */
class HomeLayoutCodecTest {

    @Test
    fun `an empty value restores the default order`() {
        val (cards, hidden) = HomeLayoutCodec.decode(null)
        assertEquals(HomeCard.defaultOrder, cards)
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `encode and decode round trip keeps order and hidden state`() {
        val ordered = listOf(
            HomeCard.QUICK_ACCESS,
            HomeCard.DAILY_TRACKING,
            HomeCard.PRAYER_HERO
        )
        val hidden = setOf(HomeCard.DAILY_TRACKING)
        val encoded = HomeLayoutCodec.encode(ordered, hidden)

        val (cards, decodedHidden) = HomeLayoutCodec.decode(encoded)
        // The three chosen cards keep their order at the front of the layout.
        assertEquals(ordered, cards.take(ordered.size))
        assertEquals(hidden, decodedHidden)
        // The cards that were not part of the stored layout are appended, never lost.
        assertEquals(HomeCard.entries.size, cards.size)
    }

    @Test
    fun `cards that were added later still appear in the layout`() {
        val encoded = "prayer_hero,daily_verse"
        val (cards, hidden) = HomeLayoutCodec.decode(encoded)

        assertEquals(HomeCard.PRAYER_HERO, cards.first())
        assertTrue(cards.contains(HomeCard.QUICK_ACCESS))
        assertEquals(HomeCard.entries.size, cards.size)
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `an unknown token is ignored`() {
        val (cards, hidden) = HomeLayoutCodec.decode("prayer_hero,not_a_card,daily_dua")
        assertEquals(2, cards.count { it == HomeCard.PRAYER_HERO || it == HomeCard.DAILY_DUA })
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `a corrupted value still returns a complete layout`() {
        val (cards, _) = HomeLayoutCodec.decode("--,,,")
        assertEquals(HomeCard.entries.size, cards.size)
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
