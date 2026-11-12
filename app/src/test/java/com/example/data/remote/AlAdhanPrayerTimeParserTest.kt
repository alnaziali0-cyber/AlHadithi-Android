package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlAdhanPrayerTimeParserTest {
    @Test fun parsesApiTimesWithOptionalTimezoneSuffix() {
        assertEquals(304, AlAdhanPrayerTimeParser.parseMinutes("05:04 (AST)"))
        assertEquals(17 * 60 + 21, AlAdhanPrayerTimeParser.parseMinutes("17:21"))
    }

    @Test fun rejectsInvalidOrMissingTimes() {
        assertEquals(null, AlAdhanPrayerTimeParser.parseMinutes("25:10"))
        assertEquals(null, AlAdhanPrayerTimeParser.parseMinutes("5:04"))
        assertEquals(null, AlAdhanPrayerTimeParser.parseMinutes(null))
    }

    @Test fun acceptsOrderedDayAndJafariMidnight() {
        val record = AlAdhanDayTimes(
            dateKey = "20261112",
            fajr = 304,
            sunrise = 388,
            dhuhr = 706,
            asr = 906,
            sunset = 1024,
            maghrib = 1040,
            isha = 1110,
            midnight = 138,
            fetchedAtMillis = 1L
        )
        assertTrue(AlAdhanPrayerTimeParser.isValid(record))
    }

    @Test fun rejectsMalformedTimeOrder() {
        val record = AlAdhanDayTimes(
            dateKey = "20261112",
            fajr = 400,
            sunrise = 388,
            dhuhr = 706,
            asr = 906,
            sunset = 1024,
            maghrib = 1040,
            isha = 1110,
            midnight = 138,
            fetchedAtMillis = 1L
        )
        assertFalse(AlAdhanPrayerTimeParser.isValid(record))
    }

    @Test fun preservesSuppliedCalculationParameters() {
        assertEquals(99, AlAdhanPrayerTimesConfig.METHOD)
        assertEquals("17.7,null,4", AlAdhanPrayerTimesConfig.METHOD_SETTINGS)
        assertEquals(1, AlAdhanPrayerTimesConfig.MIDNIGHT_MODE)
        assertEquals("0,0,0,0,0,1,0,0,0", AlAdhanPrayerTimesConfig.TUNE)
    }
}
