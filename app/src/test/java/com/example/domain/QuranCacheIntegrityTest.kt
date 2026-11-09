package com.example.domain

import com.example.data.local.CachedAyahEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranCacheIntegrityTest {

    private fun ayah(
        surah: Int,
        name: String,
        numberInSurah: Int,
        globalNumber: Int,
        text: String = "نَصٌّ عَرَبِيٌّ"
    ) = CachedAyahEntity(
        surahNumber = surah,
        surahNameArabic = name,
        ayahNumberInSurah = numberInSurah,
        globalAyahNumber = globalNumber,
        uthmaniText = text,
        juz = 1,
        hizbQuarter = 1
    )

    @Test
    fun acceptsCompleteAndCorrectlyNumberedSurah() {
        val meta = SurahCatalog.byNumber(1)
        val rows = (1..meta.ayahCount).map { index ->
            ayah(1, meta.arabicName, index, index)
        }
        assertTrue(QuranCacheIntegrity.isCompleteSurah(1, rows))
    }

    @Test
    fun rejectsCorrectRowCountWhenAyahNumberIsDuplicatedOrMissing() {
        val meta = SurahCatalog.byNumber(1)
        val rows = (1..meta.ayahCount).map { index ->
            val number = if (index == 3) 2 else index
            ayah(1, meta.arabicName, number, index)
        }
        assertFalse(QuranCacheIntegrity.isCompleteSurah(1, rows))
    }

    @Test
    fun rejectsCacheWithWrongGlobalAyahNumber() {
        val meta = SurahCatalog.byNumber(2)
        val rows = (1..meta.ayahCount).map { index ->
            val correctGlobal = 7 + index
            ayah(2, meta.arabicName, index, if (index == 1) correctGlobal + 1 else correctGlobal)
        }
        assertFalse(QuranCacheIntegrity.isCompleteSurah(2, rows))
    }

    @Test
    fun rejectsRowsBelongingToDifferentSurahOrWithoutArabicText() {
        val meta = SurahCatalog.byNumber(1)
        val rows = (1..meta.ayahCount).map { index ->
            ayah(
                surah = if (index == 1) 2 else 1,
                name = meta.arabicName,
                numberInSurah = index,
                globalNumber = index,
                text = if (index == 2) "123" else "نَصٌّ عَرَبِيٌّ"
            )
        }
        assertFalse(QuranCacheIntegrity.isCompleteSurah(1, rows))
    }


    @Test
    fun recognizesCompleteSeededCoreSurahsAsCompleteCaches() {
        val seededBySurah = com.example.data.local.SeedDataPart1
            .essentialQuranAyahs()
            .groupBy { it.surahNumber }
        assertTrue(seededBySurah.isNotEmpty())
        seededBySurah.forEach { (surahNumber, rows) ->
            assertTrue(
                "Seeded surah $surahNumber should pass the same integrity checks as downloaded data",
                QuranCacheIntegrity.isCompleteSurah(surahNumber, rows)
            )
        }
    }


    @Test
    fun rejectsOutOfRangeJuzAndHizbQuarterValues() {
        val meta = SurahCatalog.byNumber(1)
        val valid = (1..meta.ayahCount).map { index ->
            ayah(1, meta.arabicName, index, index)
        }
        assertFalse(QuranCacheIntegrity.isCompleteSurah(1, valid.mapIndexed { index, row ->
            if (index == 0) row.copy(juz = 31) else row
        }))
        assertFalse(QuranCacheIntegrity.isCompleteSurah(1, valid.mapIndexed { index, row ->
            if (index == 0) row.copy(hizbQuarter = 241) else row
        }))
    }

    @Test
    fun rejectsPartialCacheAndInvalidSurahNumber() {
        val meta = SurahCatalog.byNumber(1)
        val partial = (1 until meta.ayahCount).map { index ->
            ayah(1, meta.arabicName, index, index)
        }
        assertFalse(QuranCacheIntegrity.isCompleteSurah(1, partial))
        assertFalse(QuranCacheIntegrity.isCompleteSurah(0, emptyList()))
        assertFalse(QuranCacheIntegrity.isCompleteSurah(115, emptyList()))
    }
}
