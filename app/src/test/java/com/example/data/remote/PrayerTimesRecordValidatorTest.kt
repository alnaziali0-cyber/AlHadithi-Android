package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimesRecordValidatorTest {
    private fun validRecord(now: Long = System.currentTimeMillis()) = OfficialPrayerRecord(
        documentId = "karbala_20261110",
        governorateId = "karbala",
        dateKey = "20261110",
        fajr = 5 * 60,
        sunrise = 6 * 60 + 20,
        dhuhr = 11 * 60 + 45,
        asr = 14 * 60 + 50,
        maghrib = 17 * 60 + 10,
        isha = 18 * 60 + 30,
        midnight = 23 * 60 + 50,
        sourceUrl = "https://imamhussain.org/prayer-times/",
        sourceRecordId = "karbala-record-20261110",
        sourceUpdatedAtMillis = now - 2_000,
        verifiedAtMillis = now - 1_000,
        timeZoneId = "Asia/Baghdad",
        sourceCode = "imamhussain_official",
        status = "published"
    )

    @Test fun `accepts an attributable chronologically valid official record`() {
        assertTrue(PrayerTimesRecordValidator.validate(validRecord()))
    }

    @Test fun `rejects unsupported governorates and noncanonical document ids`() {
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(governorateId = "unknown")))
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(documentId = "karbala_20261111")))
    }

    @Test fun `rejects future or inverted provenance timestamps`() {
        val now = System.currentTimeMillis()
        assertFalse(PrayerTimesRecordValidator.validate(validRecord(now).copy(verifiedAtMillis = now + 600_000), now))
        assertFalse(PrayerTimesRecordValidator.validate(validRecord(now).copy(sourceUpdatedAtMillis = now - 500, verifiedAtMillis = now - 1_000), now))
    }

    @Test fun `rejects non-HTTPS or unapproved source hosts`() {
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(sourceUrl = "http://imamhussain.org/times")))
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(sourceUrl = "https://example.com/times")))
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(sourceUrl = "https://imamhussain.org@evil.example/times")))
    }

    @Test fun `rejects invalid chronology timezone and unpublished data`() {
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(isha = 16 * 60)))
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(timeZoneId = "UTC")))
        assertFalse(PrayerTimesRecordValidator.validate(validRecord().copy(status = "draft")))
    }

    @Test fun `deduplicates repeated dates and repeated source record ids`() {
        val one = validRecord()
        assertTrue(PrayerTimesRecordValidator.uniqueByDate(listOf(one)).containsKey(one.dateKey))
        assertTrue(PrayerTimesRecordValidator.uniqueByDate(listOf(one, one)).isEmpty())
        val otherDate = one.copy(
            dateKey = "20261111",
            documentId = "karbala_20261111"
        )
        assertTrue(PrayerTimesRecordValidator.uniqueByDate(listOf(one, otherDate)).isEmpty())
        assertTrue(PrayerTimesRecordValidator.uniqueByDate(listOf(one), setOf(one.dateKey)).isEmpty())
    }

    @Test fun `parses only strict 24 hour clock values`() {
        assertEquals(5 * 60 + 7, PrayerTimesRecordValidator.parseTime("05:07"))
        assertEquals(23 * 60 + 59, PrayerTimesRecordValidator.parseTime("23:59"))
        assertNull(PrayerTimesRecordValidator.parseTime("24:00"))
        assertNull(PrayerTimesRecordValidator.parseTime("5:07"))
        assertNull(PrayerTimesRecordValidator.parseTime("12:60"))
    }

    @Test fun `all 18 Iraqi governorates are registered`() {
        assertEquals(18, com.example.domain.IraqGovernorates.all.size)
        assertEquals(18, com.example.domain.IraqGovernorates.all.map { it.id }.distinct().size)
        assertNotNull(com.example.domain.IraqGovernorates.byId("babil"))
    }
}
