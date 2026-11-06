package com.example.domain

/**
 * How prayer and Quran times are rendered across the app.
 */
enum class TimeFormat(val key: String, val arabicLabel: String) {
    TWELVE_HOUR("12h", "12 ساعة (ص/م)"),
    TWENTY_FOUR_HOUR("24h", "24 ساعة");

    companion object {
        fun fromKey(key: String?): TimeFormat =
            entries.firstOrNull { it.key == key } ?: TWELVE_HOUR
    }
}
