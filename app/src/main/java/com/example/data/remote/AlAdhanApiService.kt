package com.example.data.remote

import com.example.domain.DailyPrayerSchedule
import com.example.domain.HijriDateValue
import com.example.domain.PrayerAndHijriCalculator
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerMinuteOffsets
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AlAdhanTimingsResponse(
    val code: Int = 200,
    val status: String = "",
    val data: AlAdhanDayData? = null
)

data class AlAdhanCalendarResponse(
    val code: Int = 200,
    val status: String = "",
    val data: List<AlAdhanDayData>? = null
)

data class AlAdhanDayData(
    val timings: AlAdhanTimingsMap? = null,
    val date: AlAdhanDateContainer? = null
)

data class AlAdhanTimingsMap(
    val Fajr: String? = null,
    val Sunrise: String? = null,
    val Dhuhr: String? = null,
    val Asr: String? = null,
    val Maghrib: String? = null,
    val Isha: String? = null,
    val Midnight: String? = null
)

data class AlAdhanDateContainer(
    val readable: String? = null,
    val hijri: AlAdhanHijriDto? = null,
    val gregorian: AlAdhanGregorianDto? = null
)

data class AlAdhanHijriDto(
    val day: String? = null,
    val year: String? = null,
    val month: AlAdhanMonthDto? = null
)

data class AlAdhanGregorianDto(
    val day: String? = null,
    val year: String? = null,
    val month: AlAdhanMonthDto? = null
)

data class AlAdhanMonthDto(
    val number: Int? = null,
    val ar: String? = null,
    val en: String? = null
)

interface AlAdhanApiService {
    @GET("timings/{date}")
    suspend fun getDailyTimings(
        @Path("date") ddMmYyyy: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 0,
        @Query("midnightMode") midnightMode: Int = 1,
        @Query("timezonestring") timeZone: String = "Asia/Baghdad",
        @Query("tune") tune: String = "0,0,0,0,0,0,0,0,0",
        @Query("adjustment") hijriAdjustment: Int = 0
    ): AlAdhanTimingsResponse

    @GET("calendar/{year}/{month}")
    suspend fun getMonthlyCalendar(
        @Path("year") gregorianYear: Int,
        @Path("month") gregorianMonth: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 0,
        @Query("midnightMode") midnightMode: Int = 1,
        @Query("timezonestring") timeZone: String = "Asia/Baghdad",
        @Query("tune") tune: String = "0,0,0,0,0,0,0,0,0",
        @Query("adjustment") hijriAdjustment: Int = 0
    ): AlAdhanCalendarResponse
}

/**
 * Former AlAdhan client. Prayer times are no longer read from the network (see
 * PrayerAndHijriCalculator), so these functions are unused. Kept until the owner approves removal.
 */
object PrayerNetworkClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val alAdhanApi: AlAdhanApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/v1/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlAdhanApiService::class.java)
    }

    suspend fun fetchDailyScheduleWithOfflineFallback(
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        offsets: PrayerMinuteOffsets,
        hijriOffsetDays: Int,
        cal: Calendar = GregorianCalendar(PrayerAndHijriCalculator.baghdadTimeZone())
    ): DailyPrayerSchedule = withContext(Dispatchers.IO) {
        val localFallback = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = latitude,
            longitude = longitude,
            cal = cal,
            method = method,
            offsets = offsets,
            hijriOffsetDays = hijriOffsetDays
        )
        try {
            val ddMmYyyy = String.format(
                Locale.US,
                "%02d-%02d-%04d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            )
            val response = alAdhanApi.getDailyTimings(
                ddMmYyyy = ddMmYyyy,
                latitude = latitude,
                longitude = longitude,
                method = method.apiCode,
                midnightMode = if (method.jafariMidnight) 1 else 0,
                tune = offsets.toAlAdhanTuneParam(),
                hijriAdjustment = hijriOffsetDays
            )
            val dayData = response.data ?: return@withContext localFallback
            val t = dayData.timings ?: return@withContext localFallback

            val fajr = t.Fajr?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) }
                ?: localFallback.fajrMinutes
            val sunrise = t.Sunrise?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) }
                ?: localFallback.sunriseMinutes
            val dhuhr = t.Dhuhr?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) }
                ?: localFallback.dhuhrMinutes
            val asr = t.Asr?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) }
                ?: localFallback.asrMinutes
            val maghrib = t.Maghrib?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) }
                ?: localFallback.maghribMinutes
            val isha = t.Isha?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) }
                ?: localFallback.ishaMinutes
            val midnight = t.Midnight?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) }
                ?: localFallback.midnightMinutes

            val hijriDto = dayData.date?.hijri
            val parsedHijri = if (hijriDto != null) {
                val hy = hijriDto.year?.toIntOrNull() ?: localFallback.hijriDate.year
                val hm = hijriDto.month?.number ?: localFallback.hijriDate.month
                val hd = hijriDto.day?.toIntOrNull() ?: localFallback.hijriDate.day
                HijriDateValue(hy, hm, hd)
            } else {
                localFallback.hijriDate
            }

            DailyPrayerSchedule(
                gregorianDateLabel = localFallback.gregorianDateLabel,
                hijriDate = parsedHijri,
                fajrMinutes = fajr,
                sunriseMinutes = sunrise,
                dhuhrMinutes = dhuhr,
                asrMinutes = asr,
                maghribMinutes = maghrib,
                ishaMinutes = isha,
                midnightMinutes = midnight,
                sourceLabel = "AlAdhan API (${method.arabicTitle})"
            )
        } catch (_: Exception) {
            localFallback
        }
    }

    suspend fun fetchMonthlySchedules(
        year: Int,
        month1Based: Int,
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        offsets: PrayerMinuteOffsets,
        hijriOffsetDays: Int
    ): List<DailyPrayerSchedule> = withContext(Dispatchers.IO) {
        try {
            val response = alAdhanApi.getMonthlyCalendar(
                gregorianYear = year,
                gregorianMonth = month1Based,
                latitude = latitude,
                longitude = longitude,
                method = method.apiCode,
                midnightMode = if (method.jafariMidnight) 1 else 0,
                tune = offsets.toAlAdhanTuneParam(),
                hijriAdjustment = hijriOffsetDays
            )
            val days = response.data
            if (!days.isNullOrEmpty()) {
                return@withContext days.mapIndexed { idx, d ->
                    val dayNum = idx + 1
                    val cal = GregorianCalendar(PrayerAndHijriCalculator.baghdadTimeZone()).apply {
                        set(year, month1Based - 1, dayNum, 12, 0, 0)
                    }
                    val fb = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
                        latitude, longitude, cal, method, offsets, hijriOffsetDays
                    )
                    val t = d.timings
                    fb.copy(
                        fajrMinutes = t?.Fajr?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) } ?: fb.fajrMinutes,
                        sunriseMinutes = t?.Sunrise?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) } ?: fb.sunriseMinutes,
                        dhuhrMinutes = t?.Dhuhr?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) } ?: fb.dhuhrMinutes,
                        asrMinutes = t?.Asr?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) } ?: fb.asrMinutes,
                        maghribMinutes = t?.Maghrib?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) } ?: fb.maghribMinutes,
                        ishaMinutes = t?.Isha?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) } ?: fb.ishaMinutes,
                        midnightMinutes = t?.Midnight?.let { PrayerAndHijriCalculator.parseHhMmToMinutes(it) } ?: fb.midnightMinutes,
                        sourceLabel = "AlAdhan API"
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to local astronomical calculation for the entire month
        }

        val monthCal = GregorianCalendar(PrayerAndHijriCalculator.baghdadTimeZone()).apply {
            set(year, month1Based - 1, 1, 12, 0, 0)
        }
        val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (1..maxDay).map { day ->
            val dayCal = GregorianCalendar(PrayerAndHijriCalculator.baghdadTimeZone()).apply {
                set(year, month1Based - 1, day, 12, 0, 0)
            }
            PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
                latitude, longitude, dayCal, method, offsets, hijriOffsetDays
            )
        }
    }
}
