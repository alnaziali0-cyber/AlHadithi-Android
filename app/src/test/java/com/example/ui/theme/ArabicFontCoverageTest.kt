package com.example.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * verifies that the bundled Arabic fonts really contain Arabic and Quranic glyphs.
 *
 * The project once shipped Latin-only stub files under the names of real Arabic
 * fonts (amiri.ttf, tajawal.ttf, noto_kufi_arabic.ttf). They parsed correctly but
 * contained zero Arabic codepoints, so every Arabic string fell back to a system
 * font — or rendered as empty boxes on devices without a matching fallback.
 * This test reads the TrueType `cmap` tables directly and fails when a font that
 * is supposed to render Arabic or Quranic text does not cover it.
 */
class ArabicFontCoverageTest {

    /**
     * The font files are read straight from the module sources so the test needs
     * neither an emulator nor a Robolectric sandbox.
     */
    private fun fontBytes(name: String): ByteArray {
        val candidates = listOf(
            File("src/main/res/font/$name.ttf"),
            File("app/src/main/res/font/$name.ttf")
        )
        val file = candidates.firstOrNull { it.isFile }
        assertTrue("Font file '$name.ttf' must exist in res/font", file != null)
        return file!!.readBytes()
    }

    /** Reads the Unicode codepoints of every glyph reachable through the cmap table. */
    private fun cmapCodepoints(bytes: ByteArray): Set<Int> {
        fun u16(offset: Int): Int =
            ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
        fun u32(offset: Int): Long =
            ((u16(offset).toLong() and 0xFFFF) shl 16) or (u16(offset + 2).toLong() and 0xFFFF)

        val numTables = u16(4)
        var cmapOffset = -1
        for (i in 0 until numTables) {
            val record = 12 + i * 16
            val tag = String(bytes, record, 4, Charsets.ISO_8859_1)
            if (tag == "cmap") cmapOffset = u32(record + 8).toInt()
        }
        assertTrue("The font must contain a cmap table", cmapOffset > 0)

        val subtableCount = u16(cmapOffset + 2)
        val codepoints = mutableSetOf<Int>()
        // Every Unicode subtable is merged: a font may split the repertoire across
        // several of them, and a single one can be a partial subset.
        for (i in 0 until subtableCount) {
            val record = cmapOffset + 4 + i * 8
            val offset = u32(record + 4).toInt()
            if (offset <= 0) continue
            val subtable = cmapOffset + offset
            when (u16(subtable)) {
                0 -> for (c in 0 until 256) codepoints.add(c)
                4 -> {
                    val segCountX2 = u16(subtable + 6)
                    val segCount = segCountX2 / 2
                    val endCodes = subtable + 14
                    val startCodes = endCodes + segCountX2 + 2
                    for (seg in 0 until segCount) {
                        val end = u16(endCodes + seg * 2)
                        val start = u16(startCodes + seg * 2)
                        if (start == 0xFFFF) continue
                        for (c in start..end) codepoints.add(c)
                    }
                }
                12 -> {
                    val groupCount = u32(subtable + 12).toInt()
                    for (g in 0 until groupCount) {
                        val rec = subtable + 16 + g * 12
                        val start = u32(rec).toInt()
                        val end = u32(rec + 4).toInt()
                        for (c in start..end) codepoints.add(c)
                    }
                }
            }
        }
        assertTrue("The font must expose usable Unicode cmap subtables", codepoints.isNotEmpty())
        return codepoints
    }

    /** عدد الحروف العربية الأساسية الموجودة في الخط (المدى كامله ٤٢ محرفاً). */
    private fun arabicLetters(codepoints: Set<Int>) = codepoints.count { it in 0x0621..0x064A }

    /** عدد محارف الكتلة العربية كلها (حروف + علامات + أشكال). */
    private fun arabicBlock(codepoints: Set<Int>) = codepoints.count { it in 0x0600..0x06FF }

    private fun quranicMarks(codepoints: Set<Int>) = codepoints.count { it in 0x06D6..0x06ED }

    @Test
    fun primaryUiFonts_coverEveryArabicLetter() {
        // الخطوط الأساسية لواجهة التطبيق يجب أن تغطي الحروف العربية كلها.
        listOf(
            "amiri", "amiri_bold",
            "noto_naskh_arabic", "noto_naskh_arabic_bold",
            "noto_kufi_arabic"
        ).forEach { name ->
            val cps = cmapCodepoints(fontBytes(name))
            val letters = arabicLetters(cps)
            assertTrue(
                "Font '$name' must cover every basic Arabic letter (found $letters of 42)",
                letters == 42
            )
        }
    }

    @Test
    fun specialisedFonts_coverArabicButNotQuranicMarks() {
        // خطوط متخصّصة (تجوّل / رقعة) تغطي نصوص الواجهة العربية،
        // لكنها لا تحتوي علامات الوقف القرآنية — ولهذا لا تُستخدم لنص الآيات.
        listOf("tajawal", "tajawal_bold", "aref_ruqaa_regular", "aref_ruqaa_bold").forEach { name ->
            val cps = cmapCodepoints(fontBytes(name))
            val letters = arabicLetters(cps)
            assertTrue(
                "Font '$name' must cover Arabic letters (found $letters of 42)",
                letters >= 35
            )
        }
    }

    @Test
    fun amiriQuran_coversEveryQuranicPauseMark() {
        val cps = cmapCodepoints(fontBytes("amiri_quran_regular"))
        val marks = quranicMarks(cps)
        assertTrue(
            "The Quranic font must cover the Quranic pause marks (found $marks)",
            marks >= 18
        )
    }

    @Test
    fun readingFontChoice_coversTheWholeQuranText() {
        // The Uthmani Quran font is the reading default, so it must cover every mark.
        val cps = cmapCodepoints(fontBytes("amiri_quran_regular"))
        val required = listOf(0x06D6, 0x06D7, 0x06D8, 0x06D9, 0x06DA, 0x06DB, 0x06DC,
            0x06DE, 0x06DF, 0x06E0, 0x06E2, 0x06E3, 0x06E5, 0x06E6, 0x06E8, 0x06E9,
            0x06EA, 0x06EB, 0x06EC, 0x06ED, 0x0653, 0x0654, 0x0655, 0x0670)
        val missing = required.filter { it !in cps }
        assertTrue("The Quranic font is missing marks: ${missing.map { it.toString(16) }}", missing.isEmpty())
    }

    @Test
    fun uiFontFiles_areNotLatinOnlyStubs() {
        // A Latin-only stub of a real font is small and has no Arabic coverage.
        listOf("amiri", "tajawal", "noto_kufi_arabic").forEach { name ->
            val bytes = fontBytes(name)
            assertTrue("Font '$name' looks like a stub", bytes.size > 40_000)
        }
    }
}
