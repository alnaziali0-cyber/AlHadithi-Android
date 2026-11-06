package com.example.domain

import java.util.Locale

/**
 * The five daily prayers that can carry their own reminder settings.
 * [key] is stable and used for persistence keys and alarm URIs.
 */
enum class PrayerKey(val key: String, val arabicName: String) {
    FAJR("fajr", "الفجر"),
    DHUHR("dhuhr", "الظهر"),
    ASR("asr", "العصر"),
    MAGHRIB("maghrib", "المغرب"),
    ISHA("isha", "العشاء");

    companion object {
        fun fromKey(key: String?): PrayerKey? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Per-prayer reminder configuration.
 *
 * The values are persisted as a compact string (`enabled|leadMinutes|sound|vibrate`) so that the
 * DataStore preferences file keeps one key per prayer. [decode] is tolerant: a corrupted or
 * truncated value falls back to safe defaults instead of throwing.
 */
data class PrayerAlertSettings(
    /**
     * Individual prayer reminders are on by default. The overall "prayer notifications" master
     * switch stays off until the user grants the permission, so nothing is ever assumed.
     */
    val enabled: Boolean = true,
    val leadMinutes: Int = 5,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true
) {
    /** Effective time of the reminder, in minutes of day, wrapping around midnight. */
    fun reminderMinutesOfDay(prayerMinutesOfDay: Int): Int {
        val shifted = prayerMinutesOfDay - leadMinutes.coerceIn(0, 120)
        return ((shifted % 1440) + 1440) % 1440
    }

    fun encode(): String =
        "${if (enabled) 1 else 0}|${leadMinutes.coerceIn(0, 120)}|" +
            "${if (soundEnabled) 1 else 0}|${if (vibrateEnabled) 1 else 0}"

    companion object {
        val DEFAULT = PrayerAlertSettings()

        fun decode(raw: String?): PrayerAlertSettings {
            if (raw.isNullOrBlank()) return DEFAULT
            val parts = raw.split('|')
            if (parts.size < 4) return DEFAULT
            val enabled = parts[0].trim() == "1"
            val lead = parts[1].trim().toIntOrNull() ?: return DEFAULT
            val sound = parts[2].trim() == "1"
            val vibrate = parts[3].trim() == "1"
            return PrayerAlertSettings(
                enabled = enabled,
                leadMinutes = lead.coerceIn(0, 120),
                soundEnabled = sound,
                vibrateEnabled = vibrate
            )
        }
    }
}

/**
 * A daily window during which prayer reminders stay silent (no sound, no vibration).
 * The window may wrap past midnight, e.g. 22:00 → 05:00.
 */
data class QuietHours(
    val enabled: Boolean = false,
    val startMinutes: Int = 0,
    val endMinutes: Int = 0
) {
    val wrapsMidnight: Boolean
        get() = enabled && startMinutes != endMinutes && endMinutes < startMinutes

    fun isActiveAt(minutesOfDay: Int): Boolean {
        if (!enabled || startMinutes == endMinutes) return false
        val minute = ((minutesOfDay % 1440) + 1440) % 1440
        return if (wrapsMidnight) {
            minute >= startMinutes || minute < endMinutes
        } else {
            minute >= startMinutes && minute < endMinutes
        }
    }

    fun formatted(): String {
        fun fmt(value: Int): String =
            String.format(Locale.US, "%02d:%02d", (value / 60) % 24, value % 60)
        return "${fmt(startMinutes)} — ${fmt(endMinutes)}"
    }

    fun encode(): String =
        "${if (enabled) 1 else 0}|${startMinutes.coerceIn(0, 1439)}|${endMinutes.coerceIn(0, 1439)}"

    companion object {
        val DEFAULT = QuietHours()

        fun decode(raw: String?): QuietHours {
            if (raw.isNullOrBlank()) return DEFAULT
            val parts = raw.split('|')
            if (parts.size < 3) return DEFAULT
            val enabled = parts[0].trim() == "1"
            val start = parts[1].trim().toIntOrNull() ?: return DEFAULT
            val end = parts[2].trim().toIntOrNull() ?: return DEFAULT
            return QuietHours(
                enabled = enabled,
                startMinutes = start.coerceIn(0, 1439),
                endMinutes = end.coerceIn(0, 1439)
            )
        }
    }
}

/**
 * The full reminder configuration for one day: the per-prayer settings plus the quiet window.
 */
data class PrayerAlertProfile(
    val alerts: Map<PrayerKey, PrayerAlertSettings> = PrayerKey.entries.associateWith {
        PrayerAlertSettings.DEFAULT
    },
    val quietHours: QuietHours = QuietHours.DEFAULT
) {
    val anyEnabled: Boolean
        get() = alerts.values.any { it.enabled }

    fun settingsFor(prayer: PrayerKey): PrayerAlertSettings =
        alerts[prayer] ?: PrayerAlertSettings.DEFAULT

    /**
     * Resolves whether a reminder should be audible/vibrating at the given moment, taking the
     * quiet window into account. Disabled prayers never alert.
     */
    fun shouldSoundAt(prayer: PrayerKey, minutesOfDay: Int): Boolean {
        val settings = settingsFor(prayer)
        if (!settings.enabled) return false
        if (quietHours.isActiveAt(minutesOfDay)) return false
        return settings.soundEnabled || settings.vibrateEnabled
    }
}
