package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AlHadithiDao
import com.example.data.local.PrayerScheduleEntity
import com.example.domain.DailyPrayerSchedule
import com.example.domain.HijriDateValue
import com.example.domain.IraqGovernorates
import com.example.domain.PrayerAndHijriCalculator
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerMinuteOffsets
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Strict validation for imported official timetable records. */
data class OfficialPrayerRecord(
    val documentId: String, val governorateId: String, val dateKey: String,
    val fajr: Int, val sunrise: Int, val dhuhr: Int, val asr: Int,
    val maghrib: Int, val isha: Int, val midnight: Int,
    val sourceUrl: String, val sourceRecordId: String,
    val sourceUpdatedAtMillis: Long, val verifiedAtMillis: Long,
    val timeZoneId: String, val sourceCode: String, val status: String
)

object PrayerTimesRecordValidator {
    private val datePattern = Regex("\\d{8}")
    private val allowedHosts = setOf("imamhussain.org", "www.imamhussain.org", "imamhussain.gov.iq", "www.imamhussain.gov.iq")

    fun parseTime(value: Any?): Int? {
        val raw = value as? String ?: return null
        if (!raw.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d"))) return null
        val parts = raw.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun validate(record: OfficialPrayerRecord, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (record.governorateId !in IraqGovernorates.all.map { it.id }) return false
        if (record.documentId != "${record.governorateId}_${record.dateKey}") return false
        if (!isRealDateKey(record.dateKey) || record.timeZoneId != "Asia/Baghdad") return false
        if (record.sourceCode != "imamhussain_official" || record.status != "published") return false
        if (record.sourceRecordId.isBlank() || record.sourceUpdatedAtMillis <= 0L || record.verifiedAtMillis <= 0L) return false
        // Do not accept future-dated or unverifiable provenance as a published update.
        val allowedClockSkewMillis = 5 * 60 * 1000L
        if (record.sourceUpdatedAtMillis > record.verifiedAtMillis ||
            record.verifiedAtMillis > nowMillis + allowedClockSkewMillis ||
            record.sourceUpdatedAtMillis > nowMillis + allowedClockSkewMillis
        ) return false
        val uri = runCatching { java.net.URI(record.sourceUrl) }.getOrNull() ?: return false
        val sourceHost = uri.host?.lowercase(Locale.ROOT) ?: return false
        if (uri.scheme != "https" || uri.userInfo != null || (uri.port != -1 && uri.port != 443) ||
            uri.rawFragment != null || sourceHost !in allowedHosts
        ) return false
        val values = listOf(record.fajr, record.sunrise, record.dhuhr, record.asr, record.maghrib, record.isha, record.midnight)
        if (values.any { it !in 0..1439 }) return false
        val orderedDay = record.fajr < record.sunrise && record.sunrise < record.dhuhr &&
            record.dhuhr < record.asr && record.asr < record.maghrib && record.maghrib < record.isha
        val midnightInNightWindow = record.midnight > record.maghrib || record.midnight < record.fajr
        return orderedDay && midnightInNightWindow
    }

    fun uniqueByDate(
        records: List<OfficialPrayerRecord>, duplicateDateKeys: Set<String> = emptySet()
    ): Map<String, OfficialPrayerRecord> {
        val grouped = records.groupBy { it.dateKey }
        val duplicateSourceIds = records.groupBy { it.sourceRecordId }
            .filter { (sourceId, items) -> sourceId.isNotBlank() && items.size > 1 }
            .keys
        return grouped.mapNotNull { (date, items) ->
            val only = items.singleOrNull() ?: return@mapNotNull null
            if (date in duplicateDateKeys || only.sourceRecordId in duplicateSourceIds || !validate(only)) null
            else date to only
        }.toMap()
    }

    private fun isRealDateKey(value: String): Boolean {
        if (!datePattern.matches(value)) return false
        val parser = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("Asia/Baghdad")
        }
        return runCatching { parser.parse(value) != null }.getOrDefault(false)
    }
}

/** One source of truth for UI, countdowns, offline storage and prayer alarms. */
class PrayerTimesService(context: Context, private val dao: AlHadithiDao) {

    companion object {
        /**
         * A clearly-labelled first-frame fallback while the canonical service loads Room/Firestore.
         * The screen never calls the calculation engine directly; all prayer-time entry points
         * remain behind this service boundary.
         */
        fun previewLocalSchedule(
            governorate: com.example.domain.GovernorateInfo,
            method: PrayerCalcMethod = PrayerCalcMethod.JAFARI,
            offsets: PrayerMinuteOffsets = PrayerMinuteOffsets(),
            hijriOffsetDays: Int = 0,
            nowMillis: Long = System.currentTimeMillis()
        ): DailyPrayerSchedule {
            val zone = TimeZone.getTimeZone("Asia/Baghdad")
            val date = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
            val generated = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
                latitude = governorate.latitude,
                longitude = governorate.longitude,
                cal = date,
                method = method,
                offsets = offsets,
                hijriOffsetDays = hijriOffsetDays,
                sourceLabel = "حساب محلي (${method.arabicTitle}) — ليس جدولاً رسمياً للعتبة الحسينية"
            )
            return generated.copy(
                dateKey = String.format(Locale.US, "%04d%02d%02d", date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH)),
                governorateId = governorate.id,
                sourceType = "CALCULATED_FALLBACK",
                sourceUpdatedAtMillis = 0L
            )
        }
    }

    private val appContext = context.applicationContext
    private val firestore by lazy {
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }

    suspend fun loadWindow(
        governorateId: String, latitude: Double, longitude: Double,
        method: PrayerCalcMethod = PrayerCalcMethod.JAFARI,
        offsets: PrayerMinuteOffsets = PrayerMinuteOffsets(),
        hijriOffsetDays: Int = 0, days: Int = 35, nowMillis: Long = System.currentTimeMillis()
    ): List<DailyPrayerSchedule> = withContext(Dispatchers.IO) {
        require(governorateId in IraqGovernorates.all.map { it.id }) { "محافظة غير معروفة" }
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
        require(days in 1..62)

        val zone = TimeZone.getTimeZone("Asia/Baghdad")
        val start = Calendar.getInstance(zone).apply { timeInMillis = nowMillis; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val dates = (0 until days).map { offset -> (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) } }
        val startKey = keyOf(dates.first())
        val endKey = keyOf(dates.last())
        var duplicateDateKeys = emptySet<String>()
        val official = try {
            val snapshot = firestore.collection("prayer_times_daily")
                .whereEqualTo("governorateId", governorateId)
                .whereEqualTo("sourceCode", "imamhussain_official")
                .whereEqualTo("status", "published")
                .whereEqualTo("timeZoneId", "Asia/Baghdad")
                .whereGreaterThanOrEqualTo("dateKey", startKey)
                .whereLessThanOrEqualTo("dateKey", endKey)
                .get().await()
            duplicateDateKeys = snapshot.documents.mapNotNull { it.getString("dateKey") }
                .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                parseOfficial(doc.id, data)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Log.w("PrayerTimesService", "Official timetable query unavailable; using validated local cache")
            emptyList()
        }
        val officialByDate = PrayerTimesRecordValidator.uniqueByDate(official, duplicateDateKeys)
        if (duplicateDateKeys.isNotEmpty()) {
            Log.w("PrayerTimesService", "Rejected duplicate official timetable dates: ${duplicateDateKeys.sorted().joinToString()}")
        }
        val cachedRows = dao.getPrayerSchedules(governorateId, startKey, endKey).associateBy { it.dateKey }
        val missingOfficialDates = dates.map(::keyOf).filter { dateKey ->
            dateKey !in officialByDate && cachedRows[dateKey]?.sourceType != "OFFICIAL"
        }
        if (missingOfficialDates.isNotEmpty()) {
            Log.w("PrayerTimesService", "No verified official timetable row for $governorateId: ${missingOfficialDates.joinToString()}; explicit local fallback will be shown")
        }
        val rowsToPersist = mutableListOf<PrayerScheduleEntity>()
        val result = dates.map { date ->
            val dateKey = keyOf(date)
            val remote = officialByDate[dateKey]
            val cachedOfficial = cachedRows[dateKey]?.takeIf { it.sourceType == "OFFICIAL" }
            when {
                remote != null -> remote.toSchedule(date, governorateId, hijriOffsetDays).also {
                    rowsToPersist += it.toEntity(System.currentTimeMillis())
                }
                cachedOfficial != null -> cachedOfficial.toSchedule()
                else -> {
                    // Recompute local fallback from the active coordinates/method/offsets;
                    // never silently reuse a prior fallback generated with older settings.
                    val generated = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
                        latitude = latitude, longitude = longitude, cal = date, method = method,
                        offsets = offsets, hijriOffsetDays = hijriOffsetDays,
                        sourceLabel = "حساب محلي (${method.arabicTitle}) — ليس جدولاً رسمياً للعتبة الحسينية"
                    ).copy(
                        dateKey = dateKey, governorateId = governorateId,
                        sourceType = "CALCULATED_FALLBACK", sourceUpdatedAtMillis = 0L
                    )
                    rowsToPersist += generated.toEntity(System.currentTimeMillis())
                    generated
                }
            }
        }
        if (rowsToPersist.isNotEmpty()) dao.upsertPrayerSchedules(rowsToPersist)
        // Bound on-device cache growth while retaining a short history for offline review.
        val retentionCutoff = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        dao.deleteOldPrayerSchedules(keyOf(retentionCutoff))
        result
    }

    private fun parseOfficial(id: String, data: Map<String, Any>): OfficialPrayerRecord? {
        val fajr = PrayerTimesRecordValidator.parseTime(data["fajr"]) ?: return null
        val sunrise = PrayerTimesRecordValidator.parseTime(data["sunrise"]) ?: return null
        val dhuhr = PrayerTimesRecordValidator.parseTime(data["dhuhr"]) ?: return null
        val asr = PrayerTimesRecordValidator.parseTime(data["asr"]) ?: return null
        val maghrib = PrayerTimesRecordValidator.parseTime(data["maghrib"]) ?: return null
        val isha = PrayerTimesRecordValidator.parseTime(data["isha"]) ?: return null
        val midnight = PrayerTimesRecordValidator.parseTime(data["midnight"]) ?: return null
        val sourceUrl = data["sourceUrl"] as? String ?: return null
        val updated = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: (data["sourceUpdatedAtMillis"] as? Number)?.toLong() ?: 0L
        val verified = (data["verifiedAt"] as? Timestamp)?.toDate()?.time ?: (data["verifiedAtMillis"] as? Number)?.toLong() ?: 0L
        return OfficialPrayerRecord(
            documentId = id, governorateId = data["governorateId"] as? String ?: return null,
            dateKey = data["dateKey"] as? String ?: return null,
            fajr = fajr, sunrise = sunrise, dhuhr = dhuhr,
            asr = asr, maghrib = maghrib, isha = isha, midnight = midnight,
            sourceUrl = sourceUrl, sourceRecordId = data["sourceRecordId"] as? String ?: "",
            sourceUpdatedAtMillis = updated, verifiedAtMillis = verified,
            timeZoneId = data["timeZoneId"] as? String ?: "",
            sourceCode = data["sourceCode"] as? String ?: "", status = data["status"] as? String ?: ""
        )
    }

    private fun OfficialPrayerRecord.toSchedule(date: Calendar, governorateId: String, hijriOffsetDays: Int) = DailyPrayerSchedule(
        gregorianDateLabel = PrayerAndHijriCalculator.formatGregorianArabic(date),
        hijriDate = PrayerAndHijriCalculator.gregorianToHijri(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH), hijriOffsetDays),
        fajrMinutes = fajr, sunriseMinutes = sunrise, dhuhrMinutes = dhuhr, asrMinutes = asr,
        maghribMinutes = maghrib, ishaMinutes = isha, midnightMinutes = midnight,
        sourceLabel = "جدول رسمي منشور من العتبة الحسينية", dateKey = dateKey, governorateId = governorateId,
        sourceType = "OFFICIAL", sourceUrl = sourceUrl, sourceUpdatedAtMillis = sourceUpdatedAtMillis,
        sourceRecordId = sourceRecordId
    )

    private fun PrayerScheduleEntity.toSchedule() = DailyPrayerSchedule(
        gregorianDateLabel, HijriDateValue(hijriYear, hijriMonth, hijriDay),
        fajrMinutes, sunriseMinutes, dhuhrMinutes, asrMinutes, maghribMinutes, ishaMinutes, midnightMinutes,
        sourceLabel, dateKey, governorateId, sourceType, sourceUrl, sourceUpdatedAtMillis, sourceRecordId
    )

    private fun DailyPrayerSchedule.toEntity(now: Long) = PrayerScheduleEntity(
        governorateId = governorateId, dateKey = dateKey, fajrMinutes = fajrMinutes,
        sunriseMinutes = sunriseMinutes, dhuhrMinutes = dhuhrMinutes, asrMinutes = asrMinutes,
        maghribMinutes = maghribMinutes, ishaMinutes = ishaMinutes, midnightMinutes = midnightMinutes,
        hijriYear = hijriDate.year, hijriMonth = hijriDate.month, hijriDay = hijriDate.day,
        gregorianDateLabel = gregorianDateLabel, sourceType = sourceType, sourceLabel = sourceLabel,
        sourceUrl = sourceUrl, sourceRecordId = sourceRecordId, sourceUpdatedAtMillis = sourceUpdatedAtMillis,
        cachedAtMillis = now, timeZoneId = "Asia/Baghdad"
    )

    private fun keyOf(calendar: Calendar): String = String.format(
        Locale.US, "%04d%02d%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
    )
}
