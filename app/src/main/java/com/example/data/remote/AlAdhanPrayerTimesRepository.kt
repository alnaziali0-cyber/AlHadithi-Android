package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.domain.DailyPrayerSchedule
import com.example.domain.GovernorateInfo
import com.example.domain.IraqGovernorates
import com.example.domain.PrayerAndHijriCalculator
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ALADHAN_CACHE_PREFS = "alhadithi_aladhan_prayer_cache_v1"
private const val ALADHAN_TIME_ZONE = "Asia/Baghdad"
private const val ALADHAN_SOURCE_URL = "https://aladhan.com/prayer-times-api"

/**
 * Parameters copied exactly from the supplied HTML integration.
 * Keep these values central: no screen, worker or alarm may invent its own settings.
 */
object AlAdhanPrayerTimesConfig {
    const val METHOD = 99
    const val METHOD_SETTINGS = "17.7,null,4"
    const val MIDNIGHT_MODE = 1
    const val TUNE = "0,0,0,0,0,1,0,0,0"
    const val TIME_ZONE = ALADHAN_TIME_ZONE
    const val ENDPOINT = "https://api.aladhan.com/v1/timings"
}

/** Parsed AlAdhan values, all represented as minutes after midnight in Asia/Baghdad. */
data class AlAdhanDayTimes(
    val dateKey: String,
    val fajr: Int,
    val sunrise: Int,
    val dhuhr: Int,
    val asr: Int,
    val sunset: Int,
    val maghrib: Int,
    val isha: Int,
    val midnight: Int,
    val fetchedAtMillis: Long,
    val freshlyFetched: Boolean = false
)

/** Pure parsing/validation helpers, separated so the API contract can be unit-tested. */
object AlAdhanPrayerTimeParser {
    fun parseMinutes(raw: String?): Int? {
        val time = raw?.trim()?.substringBefore(' ') ?: return null
        if (!time.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d"))) return null
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun isValid(day: AlAdhanDayTimes): Boolean {
        if (!day.dateKey.matches(Regex("\\d{8}")) || day.fetchedAtMillis <= 0L) return false
        val times = listOf(
            day.fajr, day.sunrise, day.dhuhr, day.asr,
            day.sunset, day.maghrib, day.isha, day.midnight
        )
        if (times.any { it !in 0..1439 }) return false
        if (!(day.fajr < day.sunrise && day.sunrise < day.dhuhr &&
                day.dhuhr < day.asr && day.asr < day.sunset &&
                day.sunset <= day.maghrib && day.maghrib < day.isha)
        ) return false
        return day.midnight > day.maghrib || day.midnight < day.fajr
    }
}

/**
 * Central online/cache repository used by home, prayer screen, countdown and alarm scheduling.
 * The selected Iraqi governorate's saved coordinates are used instead of IP geolocation, so a
 * VPN or an ISP's IP database cannot silently override the user's saved governorate.
 * First use requires internet. Exact-date API results are cached and reused offline thereafter.
 */
class AlAdhanPrayerTimesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cache = appContext.getSharedPreferences(ALADHAN_CACHE_PREFS, Context.MODE_PRIVATE)
    private val apiSettings = loadApiSettings()
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadWindow(
        governorateId: String,
        days: Int = 2,
        hijriOffsetDays: Int = 0,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyPrayerSchedule> = withContext(Dispatchers.IO) {
        val governorate = IraqGovernorates.byId(governorateId)
        require(governorate.id == governorateId) { "Unknown governorate id" }
        // The supplied page asks AlAdhan for one timestamp/date. Two dates are requested here
        // only to support the app's midnight countdown and next-day alarm scheduling.
        val safeDays = days.coerceIn(1, 2)
        val tz = TimeZone.getTimeZone(ALADHAN_TIME_ZONE)
        val baseDate = Calendar.getInstance(tz).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        buildList {
            repeat(safeDays) { offset ->
                val target = (baseDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                val dateKey = dateKey(target)
                val cached = readCache(governorate.id, dateKey)
                val isCacheFresh = cached != null && nowMillis >= cached.fetchedAtMillis && nowMillis - cached.fetchedAtMillis <= (apiSettings.cacheFreshHours * 60L * 60L * 1000L)
                val resolved = if (isCacheFresh) {
                    cached
                } else {
                    val downloaded = runCatching { fetchDay(governorate, target, nowMillis) }
                        .onFailure { Log.w(TAG, "AlAdhan fetch failed for ${governorate.id}/$dateKey", it) }
                        .getOrNull()
                    if (downloaded != null) {
                        writeCache(governorate.id, downloaded)
                        downloaded
                    } else {
                        // Only an exact-date record previously received from AlAdhan is reused.
                        cached
                    }
                }
                if (resolved != null && AlAdhanPrayerTimeParser.isValid(resolved)) {
                    add(resolved.toSchedule(governorate, hijriOffsetDays))
                }
            }
        }.sortedBy { it.dateKey }
    }

    private fun fetchDay(
        governorate: GovernorateInfo,
        targetDate: Calendar,
        nowMillis: Long
    ): AlAdhanDayTimes {
        val timestamp = targetDate.timeInMillis / 1000L
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("api.aladhan.com")
            .addPathSegment("v1")
            .addPathSegment("timings")
            .addPathSegment(timestamp.toString())
            .addQueryParameter("latitude", governorate.latitude.toString())
            .addQueryParameter("longitude", governorate.longitude.toString())
            .addQueryParameter("method", apiSettings.method.toString())
            .addQueryParameter("methodSettings", apiSettings.methodSettings)
            .addQueryParameter("midnightMode", apiSettings.midnightMode.toString())
            .addQueryParameter("tune", apiSettings.tune)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "AlAdhan HTTP ${response.code}" }
            val body = response.body?.string() ?: error("AlAdhan returned an empty response")
            val root = JSONObject(body)
            check(root.optInt("code") == 200) { "AlAdhan API response code is not 200" }
            val data = root.optJSONObject("data") ?: error("AlAdhan response has no data")
            val meta = data.optJSONObject("meta") ?: error("AlAdhan response has no metadata")
            check(meta.optString("timezone") == ALADHAN_TIME_ZONE) {
                "AlAdhan timezone mismatch: ${meta.optString("timezone")}"
            }
            val dateInfo = data.optJSONObject("date") ?: error("AlAdhan response has no date")
            val gregorian = dateInfo.optJSONObject("gregorian") ?: error("AlAdhan Gregorian date missing")
            val returnedDate = gregorian.optString("date")
            val expectedDate = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
                timeZone = TimeZone.getTimeZone(ALADHAN_TIME_ZONE)
            }.format(targetDate.time)
            check(returnedDate == expectedDate) {
                "AlAdhan date mismatch: expected $expectedDate, received $returnedDate"
            }
            val timings = data.optJSONObject("timings") ?: error("AlAdhan timings missing")
            val record = AlAdhanDayTimes(
                dateKey = dateKey(targetDate),
                fajr = requireTime(timings, "Fajr"),
                sunrise = requireTime(timings, "Sunrise"),
                dhuhr = requireTime(timings, "Dhuhr"),
                asr = requireTime(timings, "Asr"),
                sunset = requireTime(timings, "Sunset"),
                maghrib = requireTime(timings, "Maghrib"),
                isha = requireTime(timings, "Isha"),
                midnight = requireTime(timings, "Midnight"),
                fetchedAtMillis = nowMillis,
                freshlyFetched = true
            )
            check(AlAdhanPrayerTimeParser.isValid(record)) { "AlAdhan timings failed validation" }
            return record
        }
    }

    private fun requireTime(timings: JSONObject, key: String): Int =
        AlAdhanPrayerTimeParser.parseMinutes(timings.optString(key).takeIf { it.isNotBlank() })
            ?: error("Missing or invalid AlAdhan time: $key")

    private fun readCache(governorateId: String, dateKey: String): AlAdhanDayTimes? {
        val raw = cache.getString(cacheKey(governorateId, dateKey), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            AlAdhanDayTimes(
                dateKey = json.getString("date"),
                fajr = json.getInt("fajr"),
                sunrise = json.getInt("sunrise"),
                dhuhr = json.getInt("dhuhr"),
                asr = json.getInt("asr"),
                sunset = json.getInt("sunset"),
                maghrib = json.getInt("maghrib"),
                isha = json.getInt("isha"),
                midnight = json.getInt("midnight"),
                fetchedAtMillis = json.getLong("fetchedAt"),
                freshlyFetched = false
            ).takeIf { it.dateKey == dateKey && AlAdhanPrayerTimeParser.isValid(it) }
        }.getOrNull()
    }

    private fun writeCache(governorateId: String, day: AlAdhanDayTimes) {
        val json = JSONObject().apply {
            put("date", day.dateKey)
            put("fajr", day.fajr)
            put("sunrise", day.sunrise)
            put("dhuhr", day.dhuhr)
            put("asr", day.asr)
            put("sunset", day.sunset)
            put("maghrib", day.maghrib)
            put("isha", day.isha)
            put("midnight", day.midnight)
            put("fetchedAt", day.fetchedAtMillis)
            put("source", "AlAdhan API")
        }
        cache.edit().putString(cacheKey(governorateId, day.dateKey), json.toString()).apply()
    }

    private fun AlAdhanDayTimes.toSchedule(
        governorate: GovernorateInfo,
        hijriOffsetDays: Int
    ): DailyPrayerSchedule {
        val date = parseDateKey(dateKey)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(ALADHAN_TIME_ZONE)).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return DailyPrayerSchedule(
            gregorianDateLabel = PrayerAndHijriCalculator.formatGregorianArabic(calendar),
            hijriDate = PrayerAndHijriCalculator.gregorianToHijri(
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH), hijriOffsetDays
            ),
            fajrMinutes = fajr,
            sunriseMinutes = sunrise,
            dhuhrMinutes = dhuhr,
            asrMinutes = asr,
            maghribMinutes = maghrib,
            ishaMinutes = isha,
            midnightMinutes = midnight,
            sourceLabel = "AlAdhan API · method=99 · غير مؤكد التطابق مع حقيبة المؤمن",
            dateKey = dateKey,
            governorateId = governorate.id,
            sourceType = if (freshlyFetched) "ALADHAN_API" else "ALADHAN_API_CACHE",
            sourceUrl = ALADHAN_SOURCE_URL,
            sourceUpdatedAtMillis = fetchedAtMillis,
            sourceRecordId = "aladhan:${governorate.id}:$dateKey",
            sunsetMinutes = sunset
        )
    }

    private fun dateKey(calendar: Calendar): String = String.format(
        Locale.US, "%04d%02d%02d",
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    private fun parseDateKey(value: String): Date {
        val parser = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone(ALADHAN_TIME_ZONE)
        }
        return requireNotNull(parser.parse(value))
    }

    private fun cacheKey(governorateId: String, dateKey: String) = "${governorateId}_$dateKey"

    private fun loadApiSettings(): RuntimeApiSettings {
        val root = appContext.assets.open("aladhan_prayer_times_config.json").bufferedReader(Charsets.UTF_8).use {
            JSONObject(it.readText())
        }
        require(root.optInt("schemaVersion") == 2)
        require(root.optString("mode") == "remote_api_with_exact_date_cache")
        require(root.optString("timeZone") == ALADHAN_TIME_ZONE)
        val source = root.getJSONObject("source")
        require(source.optString("endpoint") == "https://api.aladhan.com/v1/timings/{timestamp}")
        val params = root.getJSONObject("calculationParameters")
        val settings = RuntimeApiSettings(
            method = params.getInt("method"),
            methodSettings = params.getString("methodSettings"),
            midnightMode = params.getInt("midnightMode"),
            tune = params.getString("tune"),
            cacheFreshHours = root.getJSONObject("cachePolicy").getLong("refreshWhenOlderThanHours")
        )
        // Fail closed if the packaged config drifts from the values explicitly supplied by the user.
        require(settings.method == AlAdhanPrayerTimesConfig.METHOD)
        require(settings.methodSettings == AlAdhanPrayerTimesConfig.METHOD_SETTINGS)
        require(settings.midnightMode == AlAdhanPrayerTimesConfig.MIDNIGHT_MODE)
        require(settings.tune == AlAdhanPrayerTimesConfig.TUNE)
        require(settings.cacheFreshHours in 1L..24L)
        return settings
    }

    private data class RuntimeApiSettings(
        val method: Int,
        val methodSettings: String,
        val midnightMode: Int,
        val tune: String,
        val cacheFreshHours: Long
    )

    companion object {
        private const val TAG = "AlAdhanPrayerTimes"
    }
}
