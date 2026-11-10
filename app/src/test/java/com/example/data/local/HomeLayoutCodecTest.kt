package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The home dashboard keeps a fixed card order and stores only which cards are hidden. These tests
 * protect the codec, so hidden cards survive an upgrade and layouts saved by older versions still read.
 */
class HomeLayoutCodecTest {

    @Test
    fun `an empty value hides no card`() {
        assertTrue(HomeLayoutCodec.decodeHidden(null).isEmpty())
        assertTrue(HomeLayoutCodec.decodeHidden("").isEmpty())
    }

    @Test
    fun `encode and decode round trip keeps the hidden cards`() {
        val hidden = setOf(HomeCard.DAILY_TRACKING, HomeCard.OCCASIONS)
        val encoded = HomeLayoutCodec.encodeHidden(hidden)
        assertEquals(hidden, HomeLayoutCodec.decodeHidden(encoded))
    }

    @Test
    fun `layouts saved by older versions still decode their hidden cards`() {
        val legacy = "quick_access,daily_tracking,-occasions,prayer_hero,-prayer_tracker"
        assertEquals(setOf(HomeCard.OCCASIONS), HomeLayoutCodec.decodeHidden(legacy))
    }

    @Test
    fun `an unknown token is ignored`() {
        assertEquals(setOf(HomeCard.PRAYER_HERO), HomeLayoutCodec.decodeHidden("-prayer_hero,-not_a_card"))
    }

    @Test
    fun `daily hadith and dua can never be shown on the home screen`() {
        // حديث اليوم ودعاء اليوم صارا إشعاراً بعد كل صلاة، لا بطاقتين في الرئيسية.
        assertTrue(HomeLayoutCodec.decodeHidden("-daily_hadith,-daily_dua").isEmpty())
    }

    @Test
    fun `the default order is fixed and excludes the notification-only cards`() {
        val order = HomeCard.defaultOrder
        assertEquals(HomeCard.OCCASIONS, order.first())
        assertEquals(HomeCard.QUICK_ACCESS, order.last())
        assertFalse(order.contains(HomeCard.DAILY_HADITH))
        assertFalse(order.contains(HomeCard.DAILY_DUA))
        assertEquals(HomeCard.entries.count { it.shownOnHome }, order.size)
    }

    @Test
    fun `a corrupted value still decodes without crashing`() {
        assertTrue(HomeLayoutCodec.decodeHidden("--,,,").isEmpty())
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
