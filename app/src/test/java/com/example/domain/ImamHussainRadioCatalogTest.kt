package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImamHussainRadioCatalogTest {
    @Test
    fun officialPagesUseHttpsAndStationHost() {
        assertTrue(ImamHussainRadioCatalog.officialHomeUrl.startsWith("https://"))
        assertTrue(ImamHussainRadioCatalog.officialHomeUrl.contains("imamhussain-fm.com"))
        assertTrue(ImamHussainRadioCatalog.officialProgramsUrl.startsWith("https://"))
        assertTrue(ImamHussainRadioCatalog.officialProgramsUrl.contains("imamhussain-fm.com"))
    }

    @Test
    fun frequenciesAreUniqueByGovernorateAndInsideFmBand() {
        val rows = ImamHussainRadioCatalog.frequencies
        assertEquals(10, rows.size)
        assertEquals(rows.size, rows.map { it.governorateArabic }.distinct().size)
        assertEquals(
            setOf("بغداد", "كربلاء", "بابل", "النجف", "واسط", "الديوانية", "المثنى", "ذي قار", "ميسان", "البصرة"),
            rows.map { it.governorateArabic }.toSet()
        )
        assertTrue(rows.all { it.frequencyMhz in 87.5..108.0 })
    }

    @Test
    fun keyPublishedFrequenciesMatchOfficialStationListing() {
        val rows = ImamHussainRadioCatalog.frequencies.associate { it.governorateArabic to it.frequencyMhz }
        assertEquals(88.3, rows["كربلاء"]!!, 0.0)
        assertEquals(107.5, rows["بابل"]!!, 0.0)
        assertEquals(93.5, rows["بغداد"]!!, 0.0)
    }
}
