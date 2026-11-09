package com.example.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

enum class PrayerCalcMethod(
    val apiCode: Int,
    val arabicTitle: String,
    val arabicSubtitle: String,
    val fajrAngle: Double,
    val maghribAngle: Double?, // null means sunset (0.833°), non-null means sun depression angle (e.g. 4.0° Jafari)
    val ishaAngle: Double,
    val ishaFixedMinutes: Int?,
    val jafariMidnight: Boolean
) {
    JAFARI(
        apiCode = 0,
        arabicTitle = "الجعفرية (المعتمدة للشيعة الإمامية)",
        arabicSubtitle = "الفجر 16° · المغرب 4° · العشاء 14° · منتصف الليل الشرعي بين الغروب والفجر",
        fajrAngle = 16.0,
        maghribAngle = 4.0,
        ishaAngle = 14.0,
        ishaFixedMinutes = null,
        jafariMidnight = true
    ),
    TEHRAN(
        apiCode = 7,
        arabicTitle = "معهد الجيوفيزياء — جامعة طهران",
        arabicSubtitle = "الفجر 17.7° · المغرب 4.5° · العشاء 14° · منتصف الليل الشرعي",
        fajrAngle = 17.7,
        maghribAngle = 4.5,
        ishaAngle = 14.0,
        ishaFixedMinutes = null,
        jafariMidnight = true
    ),
    MWL(
        apiCode = 3,
        arabicTitle = "رابطة العالم الإسلامي",
        arabicSubtitle = "الفجر 18° · العشاء 17°",
        fajrAngle = 18.0,
        maghribAngle = null,
        ishaAngle = 17.0,
        ishaFixedMinutes = null,
        jafariMidnight = false
    ),
    UMM_AL_QURA(
        apiCode = 4,
        arabicTitle = "أم القرى",
        arabicSubtitle = "الفجر 18.5° · العشاء 90 دقيقة بعد المغرب",
        fajrAngle = 18.5,
        maghribAngle = null,
        ishaAngle = 0.0,
        ishaFixedMinutes = 90,
        jafariMidnight = false
    );

    companion object {
        fun fromCode(code: Int): PrayerCalcMethod =
            entries.firstOrNull { it.apiCode == code } ?: JAFARI
    }
}

data class PrayerMinuteOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
}

data class DailyPrayerSchedule(
    val gregorianDateLabel: String,
    val hijriDate: HijriDateValue,
    val fajrMinutes: Int,
    val sunriseMinutes: Int,
    val dhuhrMinutes: Int,
    val asrMinutes: Int,
    val maghribMinutes: Int,
    val ishaMinutes: Int,
    val midnightMinutes: Int,
    val sourceLabel: String,
    val dateKey: String = "",
    val governorateId: String = "",
    val sourceType: String = "CALCULATED_FALLBACK",
    val sourceUrl: String = "",
    val sourceUpdatedAtMillis: Long = 0L,
    val sourceRecordId: String = ""
) {
    fun formatted12h(minutesOfDay: Int): String {
        if (minutesOfDay !in 0..1439) return "--:--"
        val normalized = minutesOfDay
        val hour24 = normalized / 60
        val min = normalized % 60
        val period = if (hour24 < 12) "ص" else "م"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, min, period)
    }

    fun formatted24h(minutesOfDay: Int): String {
        if (minutesOfDay !in 0..1439) return "--:--"
        val normalized = minutesOfDay
        val hour24 = normalized / 60
        val min = normalized % 60
        return String.format(Locale.US, "%02d:%02d", hour24, min)
    }
}

data class NextPrayerCountdown(
    val prayerArabicName: String,
    val prayerFormattedTime: String,
    val countdownHhMmSs: String,
    val remainingSeconds: Long
)

data class HijriDateValue(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..30
) {
    val monthArabicName: String
        get() = HIJRI_MONTH_NAMES.getOrElse(month - 1) { "محرم" }

    fun formattedArabic(): String = "$day $monthArabicName $year هـ"

    companion object {
        val HIJRI_MONTH_NAMES = listOf(
            "محرم",
            "صفر",
            "ربيع الأول",
            "ربيع الآخر",
            "جمادى الأولى",
            "جمادى الآخرة",
            "رجب",
            "شعبان",
            "رمضان",
            "شوال",
            "ذو القعدة",
            "ذو الحجة"
        )
    }
}

object PrayerAndHijriCalculator {

    private val IRAQI_GREGORIAN_MONTHS = listOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
        "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"
    )

    private val ARABIC_WEEKDAYS = mapOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الاثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )

    fun baghdadTimeZone(): TimeZone = TimeZone.getTimeZone("Asia/Baghdad")

    fun formatGregorianArabic(cal: Calendar): String {
        val weekday = ARABIC_WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = IRAQI_GREGORIAN_MONTHS.getOrElse(cal.get(Calendar.MONTH)) { "" }
        val year = cal.get(Calendar.YEAR)
        return "$weekday، $day $monthName $year م"
    }

    fun currentWeekdayIndex(cal: Calendar = Calendar.getInstance(baghdadTimeZone())): Int {
        // Returns 0 = Saturday .. 6 = Friday for Daily Duas
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    /**
     * Converts Gregorian date (with optional manual Hijri day adjustment -2..+2) to Hijri using
     * astronomical Julian Day conversion calibrated to Umm al-Qura / Iraqi lunar sighting.
     */
    fun gregorianToHijri(
        year: Int,
        month1Based: Int,
        dayOfMonth: Int,
        hijriOffsetDays: Int = 0
    ): HijriDateValue {
        val cal = Calendar.getInstance(baghdadTimeZone()).apply {
            set(year, month1Based - 1, dayOfMonth, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, hijriOffsetDays)
        }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val jd = gregorianToJulianDay(y, m, d)
        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hMonth = ((24 * l) / 709).coerceIn(1, 12)
        val hDay = (l - (709 * hMonth) / 24).coerceIn(1, 30)
        val hYear = 30 * n + j - 30
        return HijriDateValue(hYear, hMonth, hDay)
    }

    /**
     * Dynamically calculates the corresponding Gregorian date for a given Hijri (year, month, day),
     * honoring the user's manual Hijri offset (no hardcoded Gregorian dates for Islamic occasions).
     */
    fun hijriToGregorianCalendar(
        hYear: Int,
        hMonth: Int,
        hDay: Int,
        hijriOffsetDays: Int = 0
    ): Calendar {
        val jd = ((11 * hYear + 3) / 30) + 354 * hYear + 30 * hMonth -
            ((hMonth - 1) / 2) + hDay + 1948440 - 385
        val (gy, gm, gd) = julianDayToGregorian(jd)
        return Calendar.getInstance(baghdadTimeZone()).apply {
            set(gy, gm - 1, gd, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -hijriOffsetDays)
        }
    }

    /**
     * Computes days remaining until the next occurrence of a Hijri (month, day) occasion,
     * plus the exact corresponding Gregorian date label for that next occurrence.
     */
    fun nextOccurrenceDaysAndGregorian(
        occasionHijriMonth: Int,
        occasionHijriDay: Int,
        hijriOffsetDays: Int = 0,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Int, String> {
        val todayCal = Calendar.getInstance(baghdadTimeZone()).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayHijri = gregorianToHijri(
            todayCal.get(Calendar.YEAR),
            todayCal.get(Calendar.MONTH) + 1,
            todayCal.get(Calendar.DAY_OF_MONTH),
            hijriOffsetDays
        )

        var targetHijriYear = todayHijri.year
        var candidateGregorian = hijriToGregorianCalendar(
            targetHijriYear,
            occasionHijriMonth,
            occasionHijriDay,
            hijriOffsetDays
        )
        var diffDays = ((candidateGregorian.timeInMillis - todayCal.timeInMillis) / 86_400_000L).toInt()
        if (diffDays < 0) {
            targetHijriYear += 1
            candidateGregorian = hijriToGregorianCalendar(
                targetHijriYear,
                occasionHijriMonth,
                occasionHijriDay,
                hijriOffsetDays
            )
            diffDays = ((candidateGregorian.timeInMillis - todayCal.timeInMillis) / 86_400_000L).toInt()
                .coerceAtLeast(0)
        }
        val gDay = candidateGregorian.get(Calendar.DAY_OF_MONTH)
        val gMonth = IRAQI_GREGORIAN_MONTHS.getOrElse(candidateGregorian.get(Calendar.MONTH)) { "" }
        val gYear = candidateGregorian.get(Calendar.YEAR)
        return diffDays to "$gDay $gMonth $gYear م"
    }

    private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Int {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    private fun julianDayToGregorian(jd: Int): Triple<Int, Int, Int> {
        val l = jd + 68569
        val n = (4 * l) / 146097
        val l1 = l - (146097 * n + 3) / 4
        val i = (4000 * (l1 + 1)) / 1461001
        val l2 = l1 - (1461 * i) / 4 + 31
        val j = (80 * l2) / 2447
        val day = l2 - (2447 * j) / 80
        val l3 = j / 11
        val month = j + 2 - 12 * l3
        val year = 100 * (n - 49) + i + l3
        return Triple(year, month, day)
    }

    /**
     * Astronomical Jafari & Standard Prayer Times calculator for offline reliability (Iraq UTC+3).
     */
    fun calculateLocalPrayerSchedule(
        latitude: Double,
        longitude: Double,
        cal: Calendar = Calendar.getInstance(baghdadTimeZone()),
        method: PrayerCalcMethod = PrayerCalcMethod.JAFARI,
        offsets: PrayerMinuteOffsets = PrayerMinuteOffsets(),
        hijriOffsetDays: Int = 0,
        sourceLabel: String = "حساب فلكي جعفري محلي"
    ): DailyPrayerSchedule {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzHours = 3.0 // Iraq Standard Time (UTC+3, no DST since 2008)

        val jd = gregorianToJulianDay(year, month, day).toDouble() - 0.5
        val d = jd - 2451545.0 + 0.5

        val g = Math.toRadians((357.529 + 0.98560028 * d) % 360.0)
        val q = (280.459 + 0.98564736 * d) % 360.0
        val lDeg = (q + 1.915 * sin(g) + 0.020 * sin(2 * g) + 360.0) % 360.0
        val lRad = Math.toRadians(lDeg)
        val eRad = Math.toRadians(23.439 - 0.00000036 * d)

        val raHours = ((Math.toDegrees(atan2(cos(eRad) * sin(lRad), cos(lRad))) + 360.0) % 360.0) / 15.0
        val declRad = asin(sin(eRad) * sin(lRad))

        var eqTimeHours = q / 15.0 - raHours
        if (eqTimeHours > 12.0) eqTimeHours -= 24.0
        if (eqTimeHours < -12.0) eqTimeHours += 24.0

        val dhuhrHours = 12.0 + tzHours - (longitude / 15.0) - eqTimeHours

        fun sunAngleOffsetHours(angleDeg: Double): Double {
            val latRad = Math.toRadians(latitude)
            val num = -sin(Math.toRadians(angleDeg)) - sin(latRad) * sin(declRad)
            val den = cos(latRad) * cos(declRad)
            val ratio = (num / den).coerceIn(-0.9999, 0.9999)
            return Math.toDegrees(acos(ratio)) / 15.0
        }

        fun asrOffsetHours(shadowFactor: Double = 1.0): Double {
            val latRad = Math.toRadians(latitude)
            val targetAngle = -Math.toDegrees(atan(1.0 / (shadowFactor + tan(kotlin.math.abs(latRad - declRad)))))
            return sunAngleOffsetHours(targetAngle)
        }

        val fajrH = dhuhrHours - sunAngleOffsetHours(method.fajrAngle)
        val sunriseH = dhuhrHours - sunAngleOffsetHours(0.833)
        val sunsetH = dhuhrHours + sunAngleOffsetHours(0.833)
        val asrH = dhuhrHours + asrOffsetHours(1.0)
        val maghribH = if (method.maghribAngle != null) {
            dhuhrHours + sunAngleOffsetHours(method.maghribAngle)
        } else {
            sunsetH
        }
        val ishaH = if (method.ishaFixedMinutes != null) {
            maghribH + method.ishaFixedMinutes / 60.0
        } else {
            dhuhrHours + sunAngleOffsetHours(method.ishaAngle)
        }

        val midnightH = if (method.jafariMidnight) {
            // Shia Jafari Legal Midnight = midpoint between Sunset and Fajr
            val span = (fajrH + 24.0) - sunsetH
            (sunsetH + span / 2.0) % 24.0
        } else {
            val span = (sunriseH + 24.0) - sunsetH
            (sunsetH + span / 2.0) % 24.0
        }

        fun hoursToMin(h: Double, tune: Int): Int {
            val base = (h * 60.0).roundToInt() + tune
            return ((base % 1440) + 1440) % 1440
        }

        return DailyPrayerSchedule(
            gregorianDateLabel = formatGregorianArabic(cal),
            hijriDate = gregorianToHijri(year, month, day, hijriOffsetDays),
            fajrMinutes = hoursToMin(fajrH, offsets.fajr),
            sunriseMinutes = hoursToMin(sunriseH, offsets.sunrise),
            dhuhrMinutes = hoursToMin(dhuhrHours, offsets.dhuhr),
            asrMinutes = hoursToMin(asrH, offsets.asr),
            maghribMinutes = hoursToMin(maghribH, offsets.maghrib),
            ishaMinutes = hoursToMin(ishaH, offsets.isha),
            midnightMinutes = hoursToMin(midnightH, 0),
            sourceLabel = sourceLabel
        )
    }

    fun parseHhMmToMinutes(raw: String): Int? {
        val cleaned = raw.trim().take(5)
        val parts = cleaned.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return (h * 60 + m).coerceIn(0, 1439)
    }

    fun computeNextPrayerCountdown(
        schedule: DailyPrayerSchedule,
        nowMillis: Long = System.currentTimeMillis()
    ): NextPrayerCountdown {
        val cal = Calendar.getInstance(baghdadTimeZone()).apply {
            timeInMillis = nowMillis
        }
        val nowSecondsOfDay = cal.get(Calendar.HOUR_OF_DAY) * 3600 +
            cal.get(Calendar.MINUTE) * 60 +
            cal.get(Calendar.SECOND)

        val orderedPrayers = listOf(
            "الفجر" to schedule.fajrMinutes,
            "الظهر" to schedule.dhuhrMinutes,
            "العصر" to schedule.asrMinutes,
            "المغرب" to schedule.maghribMinutes,
            "العشاء" to schedule.ishaMinutes
        )

        val upcomingToday = orderedPrayers.firstOrNull { (_, min) -> min * 60 > nowSecondsOfDay }
        val (targetName, targetMin, diffSec) = if (upcomingToday != null) {
            Triple(
                upcomingToday.first,
                upcomingToday.second,
                (upcomingToday.second * 60 - nowSecondsOfDay).toLong()
            )
        } else {
            val fajrSecNextDay = schedule.fajrMinutes * 60 + 86_400
            Triple(
                "الفجر",
                schedule.fajrMinutes,
                (fajrSecNextDay - nowSecondsOfDay).toLong()
            )
        }

        val hrs = diffSec / 3600
        val mins = (diffSec % 3600) / 60
        val secs = diffSec % 60
        val formattedCountdown = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)

        return NextPrayerCountdown(
            prayerArabicName = targetName,
            prayerFormattedTime = schedule.formatted12h(targetMin),
            countdownHhMmSs = formattedCountdown,
            remainingSeconds = diffSec
        )
    }
}
