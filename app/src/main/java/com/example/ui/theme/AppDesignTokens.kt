package com.example.ui.theme

/**
 * User-facing appearance options. Every enum is persisted by a stable [key] so that stored
 * preferences survive future renames of the Arabic labels.
 */

enum class ThemeMode(val key: String, val arabicLabel: String) {
    LIGHT("light", "فاتح"),
    DARK("dark", "داكن"),
    SYSTEM("system", "حسب النظام");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * Accent families. Each one keeps the calm backgrounds and the heritage gold tertiary so the
 * identity of the app is preserved while the primary colour follows the user's choice.
 */
enum class AccentChoice(val key: String, val arabicLabel: String) {
    EMERALD("emerald", "زمردي (الافتراضي)"),
    TURQUOISE("turquoise", "فيروزي"),
    GOLD("gold", "ذهبي تراثي"),
    NIGHT_BLUE("night_blue", "أزرق ليلي");

    companion object {
        fun fromKey(key: String?): AccentChoice =
            entries.firstOrNull { it.key == key } ?: EMERALD
    }
}

/** Font family used for long-form reading (Quran, adhkar, duas) — independent of the UI font. */
enum class ReadingFontChoice(val key: String, val arabicLabel: String) {
    MUHAMMADI("MUHAMMADI", "محمدي قرآني (افتراضي)"),
    ME_QURAN("ME_QURAN", "قرآني نسخ"),
    AMIRI_COLORED("AMIRI_COLORED", "أميري قرآني ملوّن"),
    AMIRI("AMIRI", "أميري تراثي"),
    NASKH("NASKH", "نسخ واضح"),
    TAJAWAL("TAJAWAL", "تجوّل");

    companion object {
        fun fromKey(key: String?): ReadingFontChoice =
            entries.firstOrNull { it.key == key } ?: MUHAMMADI
    }
}

/** Reader backdrop for the continuous Quran text. */
enum class QuranBackgroundStyle(val key: String, val arabicLabel: String) {
    PAPER("paper", "ورقي فاتح"),
    MUSHAF("mushaf", "ورق مصحف"),
    SEPIA("sepia", "بنّي هادئ"),
    NIGHT("night", "ليلي داكن");

    companion object {
        fun fromKey(key: String?): QuranBackgroundStyle =
            entries.firstOrNull { it.key == key } ?: PAPER
    }
}

/** Spacing density of the reusable cards across the app. */
enum class CardDensity(val key: String, val arabicLabel: String) {
    COMFORTABLE("comfortable", "مريح"),
    COMPACT("compact", "مُكثَّف");

    companion object {
        fun fromKey(key: String?): CardDensity =
            entries.firstOrNull { it.key == key } ?: COMFORTABLE
    }
}

/** Multiplier applied to the global line height of reading texts. */
enum class LineSpacingOption(val key: String, val arabicLabel: String, val multiplier: Float) {
    TIGHT("tight", "مضغوط", 1.0f),
    NORMAL("normal", "معتدل", 1.15f),
    RELAXED("relaxed", "مريح", 1.35f),
    GENEROUS("generous", "واسع", 1.6f);

    companion object {
        fun fromKey(key: String?): LineSpacingOption =
            entries.firstOrNull { it.key == key } ?: NORMAL
    }
}
