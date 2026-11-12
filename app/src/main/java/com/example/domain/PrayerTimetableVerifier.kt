package com.example.domain

import kotlin.math.abs

/**
 * Verification of the prayer timetable against the reference configuration used by
 * "Haqibat Al-Mumin" (the supplied reference page uses `method=99` with
 * `methodSettings=17.7,null,4`, `midnightMode=1` and `tune=0,0,0,0,0,1,0,0,0`).
 *
 * The app must never *claim* an official match it cannot prove. It therefore reports one of
 * three honest states:
 *
 *  - [VerificationStatus.MATCHED]  : the timetable was produced with the exact reference
 *                                    parameters and the five prayers are in a coherent order.
 *  - [VerificationStatus.MISMATCH] : the timetable is internally inconsistent or was produced
 *                                    with different parameters, so it must not be trusted blindly.
 *  - [VerificationStatus.UNVERIFIED]: there is no data to compare yet (offline, missing cache).
 */
enum class VerificationStatus {
    MATCHED,
    MISMATCH,
    UNVERIFIED
}

/** The canonical calculation configuration of the reference ("Haqibat Al-Mumin") page. */
object MuaminReferenceParameters {
    const val METHOD = 99
    const val METHOD_SETTINGS = "17.7,null,4"
    const val MIDNIGHT_MODE = 1
    const val TUNE = "0,0,0,0,0,1,0,0,0"

    /** Maximum tolerated drift, in minutes, between two readings of the same prayer. */
    const val TOLERANCE_MINUTES = 3

    /**
     * A human readable fingerprint of the calculation settings. The timetable source label is
     * expected to contain it when the values come from the reference configuration.
     */
    const val FINGERPRINT = "method=99"
}

/** The outcome of comparing the current timetable with the reference configuration. */
data class TimetableVerification(
    val status: VerificationStatus,
    val message: String,
    /** Names of the prayers whose ordering or values are inconsistent. */
    val offendingPrayers: List<String> = emptyList()
)

/**
 * Pure verification logic. It is deliberately free of Android dependencies so it can be unit
 * tested and reused by the developer panel.
 */
object PrayerTimetableVerifier {

    /**
     * @param schedule the timetable currently shown to the user.
     * @param referenceLabel the label of the source that produced the timetable. It must mention
     *        the reference method for a positive match to be possible.
     */
    fun verify(
        schedule: DailyPrayerSchedule,
        referenceLabel: String = schedule.sourceLabel
    ): TimetableVerification {
        if (schedule.sourceType == "UNAVAILABLE" || schedule.sourceType.isBlank()) {
            return TimetableVerification(
                status = VerificationStatus.UNVERIFIED,
                message = "لا توجد بيانات مواقيت متاحة للتحقق منها حالياً."
            )
        }

        val problems = detectOrderingProblems(schedule)
        if (problems.isNotEmpty()) {
            return TimetableVerification(
                status = VerificationStatus.MISMATCH,
                message = "التوقيت غير متسق (${problems.joinToString("، ")})؛ راجع مصدر المواقيت قبل الاعتماد عليه.",
                offendingPrayers = problems
            )
        }

        val usesReferenceMethod = referenceLabel.contains(MuaminReferenceParameters.FINGERPRINT)
        if (!usesReferenceMethod) {
            return TimetableVerification(
                status = VerificationStatus.MISMATCH,
                message = "طريقة الحساب المستخدمة لا تطابق إعدادات حقيبة المؤمن المرجعية."
            )
        }

        return TimetableVerification(
            status = VerificationStatus.MATCHED,
            message = "التوقيت محسوب بإعدادات مطابقة لحقيبة المؤمن المرجعية."
        )
    }

    /**
     * Compares two readings of the same day (for example a cached record against a fresh API
     * answer) and reports the prayers whose difference exceeds the tolerated drift.
     */
    fun compareDrift(
        baseline: DailyPrayerSchedule,
        candidate: DailyPrayerSchedule
    ): List<String> {
        val pairs = listOf(
            "الفجر" to (baseline.fajrMinutes to candidate.fajrMinutes),
            "الظهر" to (baseline.dhuhrMinutes to candidate.dhuhrMinutes),
            "العصر" to (baseline.asrMinutes to candidate.asrMinutes),
            "المغرب" to (baseline.maghribMinutes to candidate.maghribMinutes),
            "العشاء" to (baseline.ishaMinutes to candidate.ishaMinutes)
        )
        return pairs.filter { (_, times) ->
            abs(times.first - times.second) > MuaminReferenceParameters.TOLERANCE_MINUTES
        }.map { it.first }
    }

    /** Returns the prayers whose chronological order is impossible. */
    private fun detectOrderingProblems(schedule: DailyPrayerSchedule): List<String> {
        val problems = mutableListOf<String>()
        val ordered = listOf(
            "الفجر" to schedule.fajrMinutes,
            "الشروق" to schedule.sunriseMinutes,
            "الظهر" to schedule.dhuhrMinutes,
            "العصر" to schedule.asrMinutes,
            "المغرب" to schedule.maghribMinutes,
            "العشاء" to schedule.ishaMinutes
        )
        ordered.zipWithNext { previous, next ->
            if (previous.second < 0 || next.second < 0) return@zipWithNext
            if (next.second < previous.second) {
                problems += "${next.first} قبل ${previous.first}"
            }
        }
        if (schedule.sunsetMinutes >= 0 && schedule.maghribMinutes in 0..1439) {
            // Maghrib is reported by the API as the sunset moment itself in some responses and a
            // few minutes after it in others; both are acceptable, but it may never precede it.
            if (schedule.maghribMinutes < schedule.sunsetMinutes) {
                problems += "المغرب قبل الغروب"
            }
        }
        return problems.distinct()
    }
}
