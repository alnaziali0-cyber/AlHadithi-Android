package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(HomeCard.entries.count { it.shownOnHome }, cards.size)
    }

    @Test
    fun `cards that were added later still appear in the layout`() {
        val encoded = "prayer_hero,daily_verse"
        val (cards, hidden) = HomeLayoutCodec.decode(encoded)

        assertEquals(HomeCard.PRAYER_HERO, cards.first())
        assertTrue(cards.contains(HomeCard.QUICK_ACCESS))
        assertEquals(HomeCard.entries.count { it.shownOnHome }, cards.size)
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `an unknown token is ignored`() {
        val (cards, hidden) = HomeLayoutCodec.decode("prayer_hero,not_a_card,daily_dua")
        assertEquals(1, cards.count { it == HomeCard.PRAYER_HERO || it == HomeCard.DAILY_DUA })
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun `daily hadith and dua never return to the home screen`() {
        // حديث اليوم ودعاء اليوم صارا إشعاراً بعد كل صلاة، لا بطاقتين في الرئيسية.
        val (cards, _) = HomeLayoutCodec.decode("daily_hadith,daily_dua,prayer_hero")
        assertFalse(cards.contains(HomeCard.DAILY_HADITH))
        assertFalse(cards.contains(HomeCard.DAILY_DUA))
        assertEquals(HomeCard.PRAYER_HERO, cards.first())
    }

    @Test
    fun `the default order follows the mandated home layout`() {
        val order = HomeCard.defaultOrder
        // ١) المناسبة، ٢) الصلاة القادمة، ٣) صلواتي اليوم، ثم بقية الأقسام والوصول السريع.
        assertEquals(HomeCard.OCCASIONS, order[0])
        assertEquals(HomeCard.PRAYER_HERO, order[1])
        assertEquals(HomeCard.PRAYER_TRACKER, order[2])
        assertEquals(HomeCard.QUICK_ACCESS, order.last())
        assertFalse(order.contains(HomeCard.DAILY_HADITH))
        assertFalse(order.contains(HomeCard.DAILY_DUA))
    }

    @Test
    fun `a corrupted value still returns a complete layout`() {
        val (cards, _) = HomeLayoutCodec.decode("--,,,")
        assertEquals(HomeCard.entries.count { it.shownOnHome }, cards.size)
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
