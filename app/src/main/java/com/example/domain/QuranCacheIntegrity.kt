package com.example.domain

import com.example.data.local.CachedAyahEntity

/**
 * Validation rules for a locally cached surah. A cached list is never considered complete
 * merely because its row count happens to match the surah's ayah count.
 */
object QuranCacheIntegrity {

    fun isCompleteSurah(surahNumber: Int, rows: List<CachedAyahEntity>): Boolean {
        if (surahNumber !in 1..SurahCatalog.allSurahs.size) return false
        val meta = SurahCatalog.byNumber(surahNumber)
        if (rows.size != meta.ayahCount) return false

        val expectedGlobalStart = SurahCatalog.allSurahs
            .asSequence()
            .takeWhile { it.number < surahNumber }
            .sumOf { it.ayahCount } + 1

        val ordered = rows.sortedBy { it.ayahNumberInSurah }
        return ordered.indices.all { index ->
            val ayah = ordered[index]
            ayah.surahNumber == surahNumber &&
                ayah.surahNameArabic == meta.arabicName &&
                ayah.ayahNumberInSurah == index + 1 &&
                ayah.globalAyahNumber == expectedGlobalStart + index &&
                ayah.juz in 1..30 &&
                ayah.hizbQuarter in 1..240 &&
                ayah.uthmaniText.isNotBlank() &&
                ayah.uthmaniText.any { it in '\u0600'..'\u06FF' }
        }
    }
}

/**
 * A source edition may include the basmala inside ayah 1 or expect it as a separate heading.
 * Avoid rendering it twice while preserving source text verbatim.
 */
private val arabicDiacriticsForBasmala = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

fun shouldDisplaySeparateBasmala(surahNumber: Int, firstAyahText: String?): Boolean {
    if (surahNumber !in 1..114 || surahNumber == 1 || surahNumber == 9) return false
    val normalizedPrefix = firstAyahText
        ?.trimStart()
        ?.take(48)
        ?.replace(arabicDiacriticsForBasmala, "")
        ?.trimStart()
    return normalizedPrefix?.startsWith("بسم") != true
}
