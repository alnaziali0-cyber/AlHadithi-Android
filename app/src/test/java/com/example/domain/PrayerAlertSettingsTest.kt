package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the per-prayer reminder configuration, its persistence codec and the quiet-hours
 * window that silences the reminders without removing the notification.
 */
class PrayerAlertSettingsTest {

    @Test
    fun `encode and decode round trip keeps every field`() {
        val original = PrayerAlertSettings(
            enabled = true,
            leadMinutes = 15,
            soundEnabled = false,
            vibrateEnabled = true
        )
        val decoded = PrayerAlertSettings.decode(original.encode())

        assertEquals(original, decoded)
    }

    @Test
    fun `decode of a blank value falls back to the safe default`() {
        val decoded = PrayerAlertSettings.decode(null)
        assertEquals(PrayerAlertSettings.DEFAULT, decoded)
        assertEquals(PrayerAlertSettings.DEFAULT, PrayerAlertSettings.decode(""))
    }

    @Test
    fun `decode of a corrupted value never throws`() {
        val decoded = PrayerAlertSettings.decode("1|not-a-number|1|0")
        assertEquals(PrayerAlertSettings.DEFAULT, decoded)
    }

    @Test
    fun `decode clamps an out of range lead time`() {
        val decoded = PrayerAlertSettings.decode("1|999|1|1")
        assertEquals(120, decoded.leadMinutes)
    }

    @Test
    fun `reminder time is the prayer time minus the lead time`() {
        val settings = PrayerAlertSettings(leadMinutes = 10)
        // Dhuhr at 12:30 with a ten minute lead.
        assertEquals(12 * 60 + 20, settings.reminderMinutesOfDay(12 * 60 + 30))
    }

    @Test
    fun `reminder time wraps around midnight`() {
        val settings = PrayerAlertSettings(leadMinutes = 30)
        // Fajr at 00:10 with a thirty minute lead belongs to the previous evening.
        assertEquals(23 * 60 + 40, settings.reminderMinutesOfDay(10))
    }

    @Test
    fun `quiet hours are inactive while the feature is disabled`() {
        val quiet = QuietHours(enabled = false, startMinutes = 22 * 60, endMinutes = 5 * 60)
        assertFalse(quiet.isActiveAt(23 * 60))
        assertFalse(quiet.wrapsMidnight)
    }

    @Test
    fun `quiet hours wrap past midnight`() {
        val quiet = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 5 * 60)
        assertTrue(quiet.wrapsMidnight)
        assertTrue(quiet.isActiveAt(23 * 60))
        assertTrue(quiet.isActiveAt(2 * 60))
        assertFalse(quiet.isActiveAt(12 * 60))
    }

    @Test
    fun `quiet hours inside a single day do not wrap`() {
        val quiet = QuietHours(enabled = true, startMinutes = 1 * 60, endMinutes = 4 * 60)
        assertFalse(quiet.wrapsMidnight)
        assertTrue(quiet.isActiveAt(2 * 60))
        assertFalse(quiet.isActiveAt(23 * 60))
    }

    @Test
    fun `quiet hours codec round trip`() {
        val original = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 5 * 60)
        assertEquals(original, QuietHours.decode(original.encode()))
    }

    @Test
    fun `a disabled prayer never sounds even inside the quiet window`() {
        val profile = PrayerAlertProfile(
            alerts = PrayerKey.entries.associateWith {
                PrayerAlertSettings(enabled = it != PrayerKey.ASR, leadMinutes = 5)
            },
            quietHours = QuietHours(enabled = false)
        )
        assertFalse(profile.shouldSoundAt(PrayerKey.ASR, 12 * 60))
        assertTrue(profile.shouldSoundAt(PrayerKey.DHUHR, 12 * 60))
    }

    @Test
    fun `the quiet window silences an enabled prayer`() {
        val profile = PrayerAlertProfile(
            alerts = PrayerKey.entries.associateWith { PrayerAlertSettings(enabled = true) },
            quietHours = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 5 * 60)
        )
        assertFalse(profile.shouldSoundAt(PrayerKey.ISHA, 23 * 60))
        assertTrue(profile.shouldSoundAt(PrayerKey.ISHA, 20 * 60))
    }

    @Test
    fun `a silent prayer still keeps the notification but does not sound`() {
        val profile = PrayerAlertProfile(
            alerts = PrayerKey.entries.associateWith {
                PrayerAlertSettings(enabled = true, soundEnabled = false, vibrateEnabled = false)
            }
        )
        assertFalse(profile.shouldSoundAt(PrayerKey.FAJR, 4 * 60))
    }

    @Test
    fun `every prayer key is unique and stable`() {
        val keys = PrayerKey.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(PrayerKey.FAJR, PrayerKey.fromKey("fajr"))
        assertEquals(null, PrayerKey.fromKey("unknown"))
    }
}
