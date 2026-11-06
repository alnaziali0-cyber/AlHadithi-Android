package com.example.domain

import java.util.Locale

data class SurahMeta(
    val number: Int,
    val arabicName: String,
    val revelationTypeArabic: String, // مكية / مدنية
    val ayahCount: Int,
    val juzStart: Int,
    val hizbStart: Int,
    val hasSajdah: Boolean = false,
    val isWajibSajdahShia: Boolean = false
)

/**
 * Filters the surah index by a free-text query. The search matches the Arabic name or the surah
 * number, and it is kept as a pure function so it can be unit tested.
 */
fun filterSurahs(
    surahs: List<SurahMeta>,
    query: String,
    onlySajdah: Boolean = false
): List<SurahMeta> {
    val base = if (onlySajdah) surahs.filter { it.hasSajdah } else surahs
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return base
    return base.filter {
        it.arabicName.contains(trimmed) || it.number.toString() == trimmed
    }
}

data class QuranReciter(
    val id: String,
    val arabicName: String,
    val surahServerFolder: String,
    val surahBitrate: Int = 128,
    val audioSourcePageUrl: String? = null,
    val audioUnit: String = "surah"
) {
    fun audioDownloadKey(surahNumber: Int): String =
        if (audioUnit == "juz") "juz_${id}_${SurahCatalog.byNumber(surahNumber).juzStart}"
        else "surah_${id}_${surahNumber}"

    fun audioDownloadUrl(surahNumber: Int): String? = when (audioUnit) {
        "juz" -> HamedShakernejadJuzCatalog.urlForJuz(SurahCatalog.byNumber(surahNumber).juzStart)
        "surah" -> buildSurahStreamUrl(surahNumber)
        else -> null
    }

    init {
        require(surahBitrate in setOf(32, 40, 48, 64, 128, 192)) {
            "Unsupported Quran audio bitrate: $surahBitrate"
        }
    }

    fun buildSurahStreamUrl(surahNumber: Int): String {
        require(surahNumber in 1..114) { "Surah number must be between 1 and 114" }
        return "https://cdn.islamic.network/quran/audio-surah/$surahBitrate/$surahServerFolder/$surahNumber.mp3"
    }

    fun buildMp3QuranFallbackUrl(surahNumber: Int): String? {
        if (surahNumber !in 1..114) return null
        val padded = String.format(Locale.US, "%03d", surahNumber)
        val serverPath = when (id) {
            "afasy" -> "https://server8.mp3quran.net/afs/$padded.mp3"
            "abdulbasit" -> "https://server7.mp3quran.net/basit/$padded.mp3"
            "husary" -> "https://server13.mp3quran.net/husr/$padded.mp3"
            "minshawi" -> "https://server10.mp3quran.net/minsh/$padded.mp3"
            // No fallback is provided for other reciters rather than playing another reciter
            // under the selected person's name.
            else -> return null
        }
        return serverPath
    }
}

object SurahCatalog {
    val reciters: List<QuranReciter> = listOf(
        QuranReciter("afasy", "مشاري راشد العفاسي", "ar.alafasy", 128),
        QuranReciter("abdulbasit", "عبد الباسط عبد الصمد (مرتل)", "ar.abdulbasit", 192),
        QuranReciter("husary", "محمود خليل الحصري", "ar.husary", 128),
        QuranReciter("minshawi", "محمد صديق المنشاوي", "ar.minshawi", 128),
        // Additional editions documented by Al Quran Cloud's official CDN catalogue.
        QuranReciter("sudais", "عبد الرحمن السديس", "ar.sudais", 192),
        QuranReciter("shuraim", "سعود الشريم", "ar.shuraim", 128),
        QuranReciter("ajamy", "أحمد بن علي العجمي", "ar.ajamy", 128),
        QuranReciter("muhammadayoub", "محمد أيوب", "ar.muhammadayoub", 128),
        QuranReciter("hudhaify", "علي الحذيفي", "ar.hudhaify", 128),
        QuranReciter("muhammadjibreel", "محمد جبريل", "ar.muhammadjibreel", 128),
        QuranReciter("minshawimujawwad", "محمد صديق المنشاوي (المجود)", "ar.minshawimujawwad", 64),
        QuranReciter("abdulbasitmujawwad", "عبد الباسط عبد الصمد (المجود)", "ar.abdulbasitmujawwad", 192),
        QuranReciter("parhizgar", "محمود خليل الحصري (المعلّم)", "ar.parhizgar", 64),
        // Additional catalog sources: source-page-only recordings are never fabricated as direct MP3 URLs.
        QuranReciter("shahriar-parhizgar", "شهريار پرهيزگار", "", 128,
            "https://ar.assabile.com/shahriar-parhizgar/quran", "external"),
        QuranReciter("hamed-shakernejad", "حامد شاكرنژاد", "", 128,
            HamedShakernejadJuzCatalog.SOURCE_PAGE, "juz"),
        QuranReciter("karim-mansouri", "كريم منصوري", "", 128,
            "https://ar.assabile.com/karim-mansouri/quran", "external")
    )
    fun reciterById(id: String): QuranReciter =
        reciters.firstOrNull { it.id == id } ?: reciters.first()

    // Exact 114 Surahs of the Holy Mushaf (Wajib Sajdah in Shia Fiqh: Surahs 32, 41, 53, 96)
    val allSurahs: List<SurahMeta> = listOf(
        SurahMeta(1, "سورة الفاتحة", "مكية", 7, 1, 1),
        SurahMeta(2, "سورة البقرة", "مدنية", 286, 1, 1),
        SurahMeta(3, "سورة آل عمران", "مدنية", 200, 3, 5),
        SurahMeta(4, "سورة النساء", "مدنية", 176, 4, 8),
        SurahMeta(5, "سورة المائدة", "مدنية", 120, 6, 11),
        SurahMeta(6, "سورة الأنعام", "مكية", 165, 7, 13),
        SurahMeta(7, "سورة الأعراف", "مكية", 206, 8, 16, hasSajdah = true),
        SurahMeta(8, "سورة الأنفال", "مدنية", 75, 9, 18),
        SurahMeta(9, "سورة التوبة", "مدنية", 129, 10, 19),
        SurahMeta(10, "سورة يونس", "مكية", 109, 11, 21),
        SurahMeta(11, "سورة هود", "مكية", 123, 11, 22),
        SurahMeta(12, "سورة يوسف", "مكية", 111, 12, 24),
        SurahMeta(13, "سورة الرعد", "مدنية", 43, 13, 25, hasSajdah = true),
        SurahMeta(14, "سورة إبراهيم", "مكية", 52, 13, 26),
        SurahMeta(15, "سورة الحجر", "مكية", 99, 14, 27),
        SurahMeta(16, "سورة النحل", "مكية", 128, 14, 27, hasSajdah = true),
        SurahMeta(17, "سورة الإسراء", "مكية", 111, 15, 29, hasSajdah = true),
        SurahMeta(18, "سورة الكهف", "مكية", 110, 15, 30),
        SurahMeta(19, "سورة مريم", "مكية", 98, 16, 31, hasSajdah = true),
        SurahMeta(20, "سورة طه", "مكية", 135, 16, 32),
        SurahMeta(21, "سورة الأنبياء", "مكية", 112, 17, 33),
        SurahMeta(22, "سورة الحج", "مدنية", 78, 17, 34, hasSajdah = true),
        SurahMeta(23, "سورة المؤمنون", "مكية", 118, 18, 35),
        SurahMeta(24, "سورة النور", "مدنية", 64, 18, 36),
        SurahMeta(25, "سورة الفرقان", "مكية", 77, 18, 36, hasSajdah = true),
        SurahMeta(26, "سورة الشعراء", "مكية", 227, 19, 37),
        SurahMeta(27, "سورة النمل", "مكية", 93, 19, 38, hasSajdah = true),
        SurahMeta(28, "سورة القصص", "مكية", 88, 20, 39),
        SurahMeta(29, "سورة العنكبوت", "مكية", 69, 20, 40),
        SurahMeta(30, "سورة الروم", "مكية", 60, 21, 41),
        SurahMeta(31, "سورة لقمان", "مكية", 34, 21, 42),
        SurahMeta(32, "سورة السجدة", "مكية", 30, 21, 42, hasSajdah = true, isWajibSajdahShia = true),
        SurahMeta(33, "سورة الأحزاب", "مدنية", 73, 21, 42),
        SurahMeta(34, "سورة سبأ", "مكية", 54, 22, 43),
        SurahMeta(35, "سورة فاطر", "مكية", 45, 22, 44),
        SurahMeta(36, "سورة يس", "مكية", 83, 22, 45),
        SurahMeta(37, "سورة الصافات", "مكية", 182, 23, 45),
        SurahMeta(38, "سورة ص", "مكية", 88, 23, 46, hasSajdah = true),
        SurahMeta(39, "سورة الزمر", "مكية", 75, 23, 47),
        SurahMeta(40, "سورة غافر", "مكية", 85, 24, 48),
        SurahMeta(41, "سورة فصلت", "مكية", 54, 24, 48, hasSajdah = true, isWajibSajdahShia = true),
        SurahMeta(42, "سورة الشورى", "مكية", 53, 25, 49),
        SurahMeta(43, "سورة الزخرف", "مكية", 89, 25, 50),
        SurahMeta(44, "سورة الدخان", "مكية", 59, 25, 50),
        SurahMeta(45, "سورة الجاثية", "مكية", 37, 25, 50),
        SurahMeta(46, "سورة الأحقاف", "مكية", 35, 26, 51),
        SurahMeta(47, "سورة محمد", "مدنية", 38, 26, 51),
        SurahMeta(48, "سورة الفتح", "مدنية", 29, 26, 52),
        SurahMeta(49, "سورة الحجرات", "مدنية", 18, 26, 52),
        SurahMeta(50, "سورة ق", "مكية", 45, 26, 52),
        SurahMeta(51, "سورة الذاريات", "مكية", 60, 26, 53),
        SurahMeta(52, "سورة الطور", "مكية", 49, 27, 53),
        SurahMeta(53, "سورة النجم", "مكية", 62, 27, 53, hasSajdah = true, isWajibSajdahShia = true),
        SurahMeta(54, "سورة القمر", "مكية", 55, 27, 53),
        SurahMeta(55, "سورة الرحمن", "مدنية", 78, 27, 54),
        SurahMeta(56, "سورة الواقعة", "مكية", 96, 27, 54),
        SurahMeta(57, "سورة الحديد", "مدنية", 29, 27, 54),
        SurahMeta(58, "سورة المجادلة", "مدنية", 22, 28, 55),
        SurahMeta(59, "سورة الحشر", "مدنية", 24, 28, 55),
        SurahMeta(60, "سورة الممتحنة", "مدنية", 13, 28, 55),
        SurahMeta(61, "سورة الصف", "مدنية", 14, 28, 56),
        SurahMeta(62, "سورة الجمعة", "مدنية", 11, 28, 56),
        SurahMeta(63, "سورة المنافقون", "مدنية", 11, 28, 56),
        SurahMeta(64, "سورة التغابن", "مدنية", 18, 28, 56),
        SurahMeta(65, "سورة الطلاق", "مدنية", 12, 28, 56),
        SurahMeta(66, "سورة التحريم", "مدنية", 12, 28, 56),
        SurahMeta(67, "سورة الملك", "مكية", 30, 29, 57),
        SurahMeta(68, "سورة القلم", "مكية", 52, 29, 57),
        SurahMeta(69, "سورة الحاقة", "مكية", 52, 29, 57),
        SurahMeta(70, "سورة المعارج", "مكية", 44, 29, 57),
        SurahMeta(71, "سورة نوح", "مكية", 28, 29, 58),
        SurahMeta(72, "سورة الجن", "مكية", 28, 29, 58),
        SurahMeta(73, "سورة المزمل", "مكية", 20, 29, 58),
        SurahMeta(74, "سورة المدثر", "مكية", 56, 29, 58),
        SurahMeta(75, "سورة القيامة", "مكية", 40, 29, 58),
        SurahMeta(76, "سورة الإنسان", "مدنية", 31, 29, 58),
        SurahMeta(77, "سورة المرسلات", "مكية", 50, 29, 58),
        SurahMeta(78, "سورة النبأ", "مكية", 40, 30, 59),
        SurahMeta(79, "سورة النازعات", "مكية", 46, 30, 59),
        SurahMeta(80, "سورة عبس", "مكية", 42, 30, 59),
        SurahMeta(81, "سورة التكوير", "مكية", 29, 30, 59),
        SurahMeta(82, "سورة الانفطار", "مكية", 19, 30, 59),
        SurahMeta(83, "سورة المطففين", "مكية", 36, 30, 59),
        SurahMeta(84, "سورة الانشقاق", "مكية", 25, 30, 59, hasSajdah = true),
        SurahMeta(85, "سورة البروج", "مكية", 22, 30, 59),
        SurahMeta(86, "سورة الطارق", "مكية", 17, 30, 59),
        SurahMeta(87, "سورة الأعلى", "مكية", 19, 30, 60),
        SurahMeta(88, "سورة الغاشية", "مكية", 26, 30, 60),
        SurahMeta(89, "سورة الفجر", "مكية", 30, 30, 60),
        SurahMeta(90, "سورة البلد", "مكية", 20, 30, 60),
        SurahMeta(91, "سورة الشمس", "مكية", 15, 30, 60),
        SurahMeta(92, "سورة الليل", "مكية", 21, 30, 60),
        SurahMeta(93, "سورة الضحى", "مكية", 11, 30, 60),
        SurahMeta(94, "سورة الشرح", "مكية", 8, 30, 60),
        SurahMeta(95, "سورة التين", "مكية", 8, 30, 60),
        SurahMeta(96, "سورة العلق", "مكية", 19, 30, 60, hasSajdah = true, isWajibSajdahShia = true),
        SurahMeta(97, "سورة القدر", "مكية", 5, 30, 60),
        SurahMeta(98, "سورة البينة", "مدنية", 8, 30, 60),
        SurahMeta(99, "سورة الزلزلة", "مدنية", 8, 30, 60),
        SurahMeta(100, "سورة العاديات", "مكية", 11, 30, 60),
        SurahMeta(101, "سورة القارعة", "مكية", 11, 30, 60),
        SurahMeta(102, "سورة التكاثر", "مكية", 8, 30, 60),
        SurahMeta(103, "سورة العصر", "مكية", 3, 30, 60),
        SurahMeta(104, "سورة الهمزة", "مكية", 9, 30, 60),
        SurahMeta(105, "سورة الفيل", "مكية", 5, 30, 60),
        SurahMeta(106, "سورة قريش", "مكية", 4, 30, 60),
        SurahMeta(107, "سورة الماعون", "مكية", 7, 30, 60),
        SurahMeta(108, "سورة الكوثر", "مكية", 3, 30, 60),
        SurahMeta(109, "سورة الكافرون", "مكية", 6, 30, 60),
        SurahMeta(110, "سورة النصر", "مدنية", 3, 30, 60),
        SurahMeta(111, "سورة المسد", "مكية", 5, 30, 60),
        SurahMeta(112, "سورة الإخلاص", "مكية", 4, 30, 60),
        SurahMeta(113, "سورة الفلق", "مكية", 5, 30, 60),
        SurahMeta(114, "سورة الناس", "مكية", 6, 30, 60)
    )

    fun byNumber(num: Int): SurahMeta =
        allSurahs.firstOrNull { it.number == num } ?: allSurahs.first()
}
