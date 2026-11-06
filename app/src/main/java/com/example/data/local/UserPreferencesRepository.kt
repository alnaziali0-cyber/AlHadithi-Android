package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerMinuteOffsets
import com.example.ui.theme.AppFontChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.alHadithiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "alhadithi_user_prefs"
)

data class UserSettingsState(
    val hasSelectedGovernorateOnce: Boolean = false,
    val governorateId: String = "baghdad",
    val customLatitude: Double? = null,
    val customLongitude: Double? = null,
    val darkMode: Boolean = false,
    val fontChoice: AppFontChoice = AppFontChoice.NASKH,
    val readingFontScale: Float = 1.15f,
    val prayerCalcMethodCode: Int = PrayerCalcMethod.JAFARI.apiCode,
    val prayerOffsets: PrayerMinuteOffsets = PrayerMinuteOffsets(),
    val hijriOffsetDays: Int = 0,
    val notifyPrayers: Boolean = false,
    val notifyMinutesBefore: Int = 5,
    val notifyOccasions: Boolean = true,
    val notifyAdhkar: Boolean = true,
    val hapticTasbihEnabled: Boolean = true,
    val dailyTasbihGoal: Int = 100,
    val dailyTasbihCount: Int = 0,
    val dailyTasbihDayKey: String = "",
    val lastSurahNumber: Int = 1,
    val lastSurahName: String = "سورة الفاتحة",
    val lastAyahNumber: Int = 1,
    val selectedReciterId: String = "afasy",
    val playbackSpeed: Float = 1.0f,
    val adminPinSha256: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAS_SELECTED_GOV = booleanPreferencesKey("has_selected_gov")
        val GOVERNORATE_ID = stringPreferencesKey("governorate_id")
        val CUSTOM_LATITUDE = doublePreferencesKey("custom_prayer_latitude")
        val CUSTOM_LONGITUDE = doublePreferencesKey("custom_prayer_longitude")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FONT_CHOICE = stringPreferencesKey("font_choice")
        val READING_FONT_SCALE = floatPreferencesKey("reading_font_scale")
        val CALC_METHOD = intPreferencesKey("calc_method")
        val TUNE_FAJR = intPreferencesKey("tune_fajr")
        val TUNE_SUNRISE = intPreferencesKey("tune_sunrise")
        val TUNE_DHUHR = intPreferencesKey("tune_dhuhr")
        val TUNE_ASR = intPreferencesKey("tune_asr")
        val TUNE_MAGHRIB = intPreferencesKey("tune_maghrib")
        val TUNE_ISHA = intPreferencesKey("tune_isha")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val NOTIFY_PRAYERS = booleanPreferencesKey("notify_prayers")
        val NOTIFY_MIN_BEFORE = intPreferencesKey("notify_min_before")
        val NOTIFY_OCCASIONS = booleanPreferencesKey("notify_occasions")
        val NOTIFY_ADHKAR = booleanPreferencesKey("notify_adhkar")
        val HAPTIC_TASBIH = booleanPreferencesKey("haptic_tasbih")
        val DAILY_TASBIH_GOAL = intPreferencesKey("daily_tasbih_goal")
        val DAILY_TASBIH_COUNT = intPreferencesKey("daily_tasbih_count")
        val DAILY_TASBIH_DAY_KEY = stringPreferencesKey("daily_tasbih_day_key")
        val LAST_SURAH_NUM = intPreferencesKey("last_surah_num")
        val LAST_SURAH_NAME = stringPreferencesKey("last_surah_name")
        val LAST_AYAH_NUM = intPreferencesKey("last_ayah_num")
        val RECITER_ID = stringPreferencesKey("reciter_id")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
    }

    val settingsFlow: Flow<UserSettingsState> = context.alHadithiDataStore.data.map { prefs ->
        UserSettingsState(
            hasSelectedGovernorateOnce = prefs[Keys.HAS_SELECTED_GOV] ?: false,
            governorateId = prefs[Keys.GOVERNORATE_ID] ?: "baghdad",
            customLatitude = prefs[Keys.CUSTOM_LATITUDE],
            customLongitude = prefs[Keys.CUSTOM_LONGITUDE],
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            fontChoice = AppFontChoice.fromKey(prefs[Keys.FONT_CHOICE] ?: AppFontChoice.NASKH.key),
            readingFontScale = prefs[Keys.READING_FONT_SCALE] ?: 1.15f,
            prayerCalcMethodCode = prefs[Keys.CALC_METHOD] ?: PrayerCalcMethod.JAFARI.apiCode,
            prayerOffsets = PrayerMinuteOffsets(
                fajr = prefs[Keys.TUNE_FAJR] ?: 0,
                sunrise = prefs[Keys.TUNE_SUNRISE] ?: 0,
                dhuhr = prefs[Keys.TUNE_DHUHR] ?: 0,
                asr = prefs[Keys.TUNE_ASR] ?: 0,
                maghrib = prefs[Keys.TUNE_MAGHRIB] ?: 0,
                isha = prefs[Keys.TUNE_ISHA] ?: 0
            ),
            hijriOffsetDays = (prefs[Keys.HIJRI_OFFSET] ?: 0).coerceIn(-2, 2),
            notifyPrayers = prefs[Keys.NOTIFY_PRAYERS] ?: false,
            notifyMinutesBefore = prefs[Keys.NOTIFY_MIN_BEFORE] ?: 5,
            notifyOccasions = prefs[Keys.NOTIFY_OCCASIONS] ?: true,
            notifyAdhkar = prefs[Keys.NOTIFY_ADHKAR] ?: true,
            hapticTasbihEnabled = prefs[Keys.HAPTIC_TASBIH] ?: true,
            dailyTasbihGoal = prefs[Keys.DAILY_TASBIH_GOAL] ?: 100,
            dailyTasbihCount = prefs[Keys.DAILY_TASBIH_COUNT] ?: 0,
            dailyTasbihDayKey = prefs[Keys.DAILY_TASBIH_DAY_KEY] ?: "",
            lastSurahNumber = prefs[Keys.LAST_SURAH_NUM] ?: 1,
            lastSurahName = prefs[Keys.LAST_SURAH_NAME] ?: "سورة الفاتحة",
            lastAyahNumber = prefs[Keys.LAST_AYAH_NUM] ?: 1,
            selectedReciterId = prefs[Keys.RECITER_ID] ?: "afasy",
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            adminPinSha256 = prefs[Keys.ADMIN_PIN_HASH] ?: ""
        )
    }

    suspend fun selectGovernorate(govId: String) {
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.GOVERNORATE_ID] = govId
            prefs.remove(Keys.CUSTOM_LATITUDE)
            prefs.remove(Keys.CUSTOM_LONGITUDE)
            prefs[Keys.HAS_SELECTED_GOV] = true
        }
    }

    suspend fun dismissOnboardingPrompt() {
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.HAS_SELECTED_GOV] = true
        }
    }

    suspend fun setPrayerCoordinates(latitude: Double, longitude: Double) {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid latitude" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid longitude" }
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_LATITUDE] = latitude
            prefs[Keys.CUSTOM_LONGITUDE] = longitude
        }
    }

    suspend fun clearPrayerCoordinates() {
        context.alHadithiDataStore.edit { prefs ->
            prefs.remove(Keys.CUSTOM_LATITUDE)
            prefs.remove(Keys.CUSTOM_LONGITUDE)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.alHadithiDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setFontChoice(choice: AppFontChoice) {
        context.alHadithiDataStore.edit { it[Keys.FONT_CHOICE] = choice.key }
    }

    suspend fun setReadingFontScale(scale: Float) {
        context.alHadithiDataStore.edit { it[Keys.READING_FONT_SCALE] = scale.coerceIn(0.85f, 1.55f) }
    }

    suspend fun setPrayerCalcMethod(code: Int) {
        context.alHadithiDataStore.edit { it[Keys.CALC_METHOD] = code }
    }

    suspend fun updatePrayerOffsets(offsets: PrayerMinuteOffsets) {
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.TUNE_FAJR] = offsets.fajr.coerceIn(-30, 30)
            prefs[Keys.TUNE_SUNRISE] = offsets.sunrise.coerceIn(-30, 30)
            prefs[Keys.TUNE_DHUHR] = offsets.dhuhr.coerceIn(-30, 30)
            prefs[Keys.TUNE_ASR] = offsets.asr.coerceIn(-30, 30)
            prefs[Keys.TUNE_MAGHRIB] = offsets.maghrib.coerceIn(-30, 30)
            prefs[Keys.TUNE_ISHA] = offsets.isha.coerceIn(-30, 30)
        }
    }

    suspend fun setHijriOffsetDays(offset: Int) {
        context.alHadithiDataStore.edit { it[Keys.HIJRI_OFFSET] = offset.coerceIn(-2, 2) }
    }

    suspend fun updateNotificationPrefs(
        notifyPrayers: Boolean? = null,
        minutesBefore: Int? = null,
        notifyOccasions: Boolean? = null,
        notifyAdhkar: Boolean? = null
    ) {
        context.alHadithiDataStore.edit { prefs ->
            notifyPrayers?.let { prefs[Keys.NOTIFY_PRAYERS] = it }
            minutesBefore?.let { prefs[Keys.NOTIFY_MIN_BEFORE] = it }
            notifyOccasions?.let { prefs[Keys.NOTIFY_OCCASIONS] = it }
            notifyAdhkar?.let { prefs[Keys.NOTIFY_ADHKAR] = it }
        }
    }

    suspend fun setHapticTasbih(enabled: Boolean) {
        context.alHadithiDataStore.edit { it[Keys.HAPTIC_TASBIH] = enabled }
    }

    suspend fun setDailyTasbihGoal(goal: Int) {
        context.alHadithiDataStore.edit { it[Keys.DAILY_TASBIH_GOAL] = goal.coerceIn(33, 10000) }
    }

    suspend fun incrementDailyTasbih(dayKey: String, delta: Int = 1) {
        context.alHadithiDataStore.edit { prefs ->
            val currentDay = prefs[Keys.DAILY_TASBIH_DAY_KEY] ?: ""
            val base = if (currentDay == dayKey) (prefs[Keys.DAILY_TASBIH_COUNT] ?: 0) else 0
            prefs[Keys.DAILY_TASBIH_DAY_KEY] = dayKey
            prefs[Keys.DAILY_TASBIH_COUNT] = (base + delta).coerceAtLeast(0)
        }
    }

    suspend fun saveLastQuranPosition(surahNumber: Int, surahName: String, ayahNumber: Int) {
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.LAST_SURAH_NUM] = surahNumber
            prefs[Keys.LAST_SURAH_NAME] = surahName
            prefs[Keys.LAST_AYAH_NUM] = ayahNumber
        }
    }

    suspend fun setReciter(reciterId: String) {
        context.alHadithiDataStore.edit { it[Keys.RECITER_ID] = reciterId }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.alHadithiDataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.75f, 1.75f) }
    }

    suspend fun setOrVerifyAdminPin(pin: String, currentHash: String): Boolean {
        val hashed = sha256(pin.trim())
        if (currentHash.isBlank()) {
            context.alHadithiDataStore.edit { it[Keys.ADMIN_PIN_HASH] = hashed }
            return true
        }
        return hashed == currentHash
    }

    suspend fun resetSettingsToDefaults() {
        context.alHadithiDataStore.edit { prefs ->
            val savedPin = prefs[Keys.ADMIN_PIN_HASH]
            prefs.clear()
            if (!savedPin.isNullOrBlank()) {
                prefs[Keys.ADMIN_PIN_HASH] = savedPin
            }
        }
    }

    companion object {
        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
