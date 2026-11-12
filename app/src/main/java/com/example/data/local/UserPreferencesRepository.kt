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
import com.example.domain.PrayerAlertProfile
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerKey
import com.example.domain.PrayerMinuteOffsets
import com.example.domain.QuietHours
import com.example.domain.TasbihProgress
import com.example.domain.TimeFormat
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.CardDensity
import com.example.ui.theme.LineSpacingOption
import com.example.ui.theme.QuranBackgroundStyle
import com.example.ui.theme.ReadingFontChoice
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.alHadithiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "alhadithi_user_prefs"
)

/**
 * Stable identifiers for dashboard content. The four primary sections now use a fixed product
 * order; legacy identifiers stay readable so older preference files do not break on upgrade.
 */
enum class HomeCard(val key: String, val arabicTitle: String) {
    OCCASIONS("occasions", "المناسبة الإسلامية"),
    PRAYER_HERO("prayer_hero", "مؤقت الصلاة"),
    PRAYER_TRACKING("prayer_tracking", "مواقيت الصلاة"),
    QUICK_ACCESS("quick_access", "الوصول السريع"),
    // Legacy keys remain readable for saved v1.0.9 preferences, but are no longer shown on Home.
    DAILY_TRACKING("daily_tracking", "متابعة اليوم"),
    DAILY_TIMES("daily_times", "مواقيت اليوم"),
    DAILY_VERSE("daily_verse", "آية اليوم"),
    DAILY_HADITH("daily_hadith", "حديث اليوم"),
    DAILY_DUA("daily_dua", "دعاء اليوم");

    companion object {
        fun fromKey(key: String): HomeCard? = entries.firstOrNull { it.key == key }

        val defaultOrder: List<HomeCard> = listOf(
            OCCASIONS,
            DAILY_TIMES,
            PRAYER_TRACKING,
            QUICK_ACCESS
        )
    }
}

/**
 * The personal goals the user can switch on inside the optional "daily follow-up" card.
 * The card stays hidden until at least one goal is selected, so the dashboard is never noisy.
 */
enum class DailyGoal(val key: String, val arabicTitle: String) {
    QURAN_READING("quran_reading", "قراءة القرآن"),
    TASBIH("tasbih", "التسبيح"),
    ADHKAR("adhkar", "أذكار الصباح والمساء"),
    DUAS("duas", "الأدعية والزيارات");

    companion object {
        fun fromKey(key: String): DailyGoal? = entries.firstOrNull { it.key == key }

        fun decode(raw: String?): Set<DailyGoal> {
            if (raw.isNullOrBlank()) return emptySet()
            return raw.split(',').mapNotNull { fromKey(it.trim()) }.toSet()
        }

        fun encode(goals: Set<DailyGoal>): String =
            entries.filter { it in goals }.joinToString(",") { it.key }
    }
}

/**
 * Backward-compatible adapter for v1.0.9 home-layout preferences. The requested home hierarchy
 * is now fixed, so legacy order/visibility values are read without affecting the new dashboard.
 * The old preference key is intentionally left in DataStore until a future migration cleanup.
 */
object HomeLayoutCodec {
    fun encode(cards: List<HomeCard>, hidden: Set<HomeCard>): String =
        cards.joinToString(",") { card -> if (card in hidden) "-${card.key}" else card.key }

    @Suppress("UNUSED_PARAMETER")
    fun decode(raw: String?): Pair<List<HomeCard>, Set<HomeCard>> =
        HomeCard.defaultOrder to emptySet()
}

/**
 * Resolves the effective theme mode.
 *
 * An explicitly stored mode always wins. Users who never chose one keep the behaviour of the
 * previous release, where a single boolean switch decided between light and dark.
 */
internal fun resolveThemeMode(storedMode: String?, legacyDarkMode: Boolean): ThemeMode {
    val parsed = storedMode?.let { key -> ThemeMode.entries.firstOrNull { it.key == key } }
    return parsed ?: if (legacyDarkMode) ThemeMode.DARK else ThemeMode.SYSTEM
}

/** Reset scopes clear preferences only; reading history, counters, favourites and downloads are preserved. */
enum class SettingsResetScope {
    APPEARANCE,
    FONTS,
    NOTIFICATIONS,
    PRAYER,
    ALL
}

data class UserSettingsState(
    val hasSelectedGovernorateOnce: Boolean = false,
    val governorateId: String = "baghdad",
    val customLatitude: Double? = null,
    val customLongitude: Double? = null,
    /** Legacy boolean mirror of [themeMode]; kept so older screens keep working. */
    val darkMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentChoice: AccentChoice = AccentChoice.TURQUOISE,
    val fontChoice: AppFontChoice = AppFontChoice.NASKH,
    val readingFontChoice: ReadingFontChoice = ReadingFontChoice.MUHAMMADI,
    val readingFontScale: Float = 1.15f,
    val uiFontScale: Float = 1.0f,
    val lineSpacing: LineSpacingOption = LineSpacingOption.NORMAL,
    val cardDensity: CardDensity = CardDensity.COMFORTABLE,
    val quranBackground: QuranBackgroundStyle = QuranBackgroundStyle.PAPER,
    val quranAutoScrollEnabled: Boolean = false,
    val quranAutoScrollSpeed: Float = 1.0f,
    val prayerCalcMethodCode: Int = PrayerCalcMethod.JAFARI.apiCode,
    val prayerOffsets: PrayerMinuteOffsets = PrayerMinuteOffsets(),
    val hijriOffsetDays: Int = 0,
    val timeFormat: TimeFormat = TimeFormat.TWELVE_HOUR,
    val notifyPrayers: Boolean = false,
    val notifyMinutesBefore: Int = 5,
    val prayerAlertProfile: PrayerAlertProfile = PrayerAlertProfile(),
    val notifyOccasions: Boolean = true,
    val notifyAdhkar: Boolean = true,
    val hapticTasbihEnabled: Boolean = false,
    val dailyTasbihGoal: Int = 100,
    val dailyTasbihCount: Int = 0,
    val dailyTasbihDayKey: String = "",
    val lastSurahNumber: Int = 1,
    val lastSurahName: String = "سورة الفاتحة",
    val lastAyahNumber: Int = 1,
    val selectedReciterId: String = "afasy",
    val playbackSpeed: Float = 1.0f,
    val adminPinSha256: String = "",
    val homeCards: List<HomeCard> = HomeCard.defaultOrder,
    val hiddenHomeCards: Set<HomeCard> = emptySet(),
    val dailyGoals: Set<DailyGoal> = emptySet(),
    val tasbihPresetId: String = "zahra",
    val tasbihStageIndex: Int = 0,
    val tasbihStageCount: Int = 0,
    val tasbihTotalCount: Int = 0,
    val tasbihCustomTarget: Int = 70
) {
    val effectiveDarkMode: Boolean
        get() = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> darkMode
        }
}

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
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_CHOICE = stringPreferencesKey("accent_choice")
        val READING_FONT_CHOICE = stringPreferencesKey("reading_font_choice")
        val UI_FONT_SCALE = floatPreferencesKey("ui_font_scale")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val CARD_DENSITY = stringPreferencesKey("card_density")
        val QURAN_BACKGROUND = stringPreferencesKey("quran_background")
        val QURAN_AUTO_SCROLL = booleanPreferencesKey("quran_auto_scroll")
        val QURAN_AUTO_SCROLL_SPEED = floatPreferencesKey("quran_auto_scroll_speed")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val QUIET_HOURS = stringPreferencesKey("quiet_hours")
        val HOME_LAYOUT = stringPreferencesKey("home_layout")
        val DAILY_GOALS = stringPreferencesKey("daily_goals")
        val DHIKR_COUNT_DAY_KEY = stringPreferencesKey("dhikr_count_day_key")
        val TASBIH_PRESET_ID = stringPreferencesKey("tasbih_preset_id")
        val TASBIH_STAGE_INDEX = intPreferencesKey("tasbih_stage_index")
        val TASBIH_STAGE_COUNT = intPreferencesKey("tasbih_stage_count")
        val TASBIH_TOTAL_COUNT = intPreferencesKey("tasbih_total_count")
        val TASBIH_CUSTOM_TARGET = intPreferencesKey("tasbih_custom_target")
        const val DHIKR_COUNT_PREFIX = "dhikr_count_"
        fun dhikrCountKey(id: Long) = intPreferencesKey(DHIKR_COUNT_PREFIX + id)
        fun prayerAlertKey(prayer: PrayerKey) = stringPreferencesKey("prayer_alert_${prayer.key}")
    }

    /**
     * The repeat counters of the adhkar, stored locally so the progress survives a rotation,
     * a navigation to another screen and a restart of the app.
     */
    val dhikrCountersFlow: Flow<Map<Long, Int>> = context.alHadithiDataStore.data.map { prefs ->
        prefs.asMap().mapNotNull { (key, value) ->
            if (!key.name.startsWith(Keys.DHIKR_COUNT_PREFIX)) return@mapNotNull null
            val id = key.name.removePrefix(Keys.DHIKR_COUNT_PREFIX).toLongOrNull()
                ?: return@mapNotNull null
            val count = value as? Int ?: return@mapNotNull null
            id to count
        }.toMap()
    }

    /**
     * Increments the repeat counter of one dhikr and returns the new value. Every counter resets
     * when the day changes so the progress always matches the current day.
     */
    suspend fun incrementDhikrCounter(id: Long, dayKey: String): Int {
        var result = 0
        context.alHadithiDataStore.edit { prefs ->
            val storedDay = prefs[Keys.DHIKR_COUNT_DAY_KEY] ?: ""
            if (storedDay != dayKey) {
                val staleKeys = prefs.asMap().keys
                    .filter { it.name.startsWith(Keys.DHIKR_COUNT_PREFIX) }
                    .toList()
                staleKeys.forEach { prefs.remove(it) }
                prefs[Keys.DHIKR_COUNT_DAY_KEY] = dayKey
            }
            val key = Keys.dhikrCountKey(id)
            val next = (prefs[key] ?: 0) + 1
            prefs[key] = next
            result = next
        }
        return result
    }

    suspend fun resetDhikrCounter(id: Long) {
        context.alHadithiDataStore.edit { prefs ->
            prefs.remove(Keys.dhikrCountKey(id))
        }
    }

    suspend fun resetAllDhikrCounters() {
        context.alHadithiDataStore.edit { prefs ->
            val staleKeys = prefs.asMap().keys
                .filter { it.name.startsWith(Keys.DHIKR_COUNT_PREFIX) }
                .toList()
            staleKeys.forEach { prefs.remove(it) }
        }
    }

    val settingsFlow: Flow<UserSettingsState> = context.alHadithiDataStore.data.map { prefs ->
        val legacyDarkMode = prefs[Keys.DARK_MODE] ?: false
        // A stored theme mode always wins; the legacy boolean is only the fallback for users who
        // never picked an explicit mode.
        val themeMode = resolveThemeMode(prefs[Keys.THEME_MODE], legacyDarkMode)
        val notifyMinutesBefore = prefs[Keys.NOTIFY_MIN_BEFORE] ?: 5
        val (homeCards, hiddenHomeCards) = HomeLayoutCodec.decode(prefs[Keys.HOME_LAYOUT])
        val prayerAlerts = PrayerKey.entries.associateWith { prayer ->
            prefs[Keys.prayerAlertKey(prayer)]?.let { PrayerAlertSettings.decode(it) }
                ?: PrayerAlertSettings(leadMinutes = notifyMinutesBefore)
        }
        UserSettingsState(
            hasSelectedGovernorateOnce = prefs[Keys.HAS_SELECTED_GOV] ?: false,
            governorateId = prefs[Keys.GOVERNORATE_ID] ?: "baghdad",
            customLatitude = prefs[Keys.CUSTOM_LATITUDE],
            customLongitude = prefs[Keys.CUSTOM_LONGITUDE],
            darkMode = legacyDarkMode,
            themeMode = themeMode,
            accentChoice = AccentChoice.fromKey(prefs[Keys.ACCENT_CHOICE]),
            fontChoice = AppFontChoice.fromKey(prefs[Keys.FONT_CHOICE] ?: AppFontChoice.NASKH.key),
            readingFontChoice = ReadingFontChoice.fromKey(prefs[Keys.READING_FONT_CHOICE]),
            readingFontScale = prefs[Keys.READING_FONT_SCALE] ?: 1.15f,
            uiFontScale = prefs[Keys.UI_FONT_SCALE] ?: 1.0f,
            lineSpacing = LineSpacingOption.fromKey(prefs[Keys.LINE_SPACING]),
            cardDensity = CardDensity.fromKey(prefs[Keys.CARD_DENSITY]),
            quranBackground = QuranBackgroundStyle.fromKey(prefs[Keys.QURAN_BACKGROUND]),
            quranAutoScrollEnabled = prefs[Keys.QURAN_AUTO_SCROLL] ?: false,
            quranAutoScrollSpeed = prefs[Keys.QURAN_AUTO_SCROLL_SPEED] ?: 1.0f,
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
            timeFormat = TimeFormat.fromKey(prefs[Keys.TIME_FORMAT]),
            notifyPrayers = prefs[Keys.NOTIFY_PRAYERS] ?: false,
            notifyMinutesBefore = notifyMinutesBefore,
            prayerAlertProfile = PrayerAlertProfile(
                alerts = prayerAlerts,
                quietHours = QuietHours.decode(prefs[Keys.QUIET_HOURS])
            ),
            notifyOccasions = prefs[Keys.NOTIFY_OCCASIONS] ?: true,
            notifyAdhkar = prefs[Keys.NOTIFY_ADHKAR] ?: true,
            hapticTasbihEnabled = prefs[Keys.HAPTIC_TASBIH] ?: false,
            dailyTasbihGoal = prefs[Keys.DAILY_TASBIH_GOAL] ?: 100,
            dailyTasbihCount = prefs[Keys.DAILY_TASBIH_COUNT] ?: 0,
            dailyTasbihDayKey = prefs[Keys.DAILY_TASBIH_DAY_KEY] ?: "",
            lastSurahNumber = prefs[Keys.LAST_SURAH_NUM] ?: 1,
            lastSurahName = prefs[Keys.LAST_SURAH_NAME] ?: "سورة الفاتحة",
            lastAyahNumber = prefs[Keys.LAST_AYAH_NUM] ?: 1,
            selectedReciterId = prefs[Keys.RECITER_ID] ?: "afasy",
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            adminPinSha256 = prefs[Keys.ADMIN_PIN_HASH] ?: "",
            homeCards = homeCards,
            hiddenHomeCards = hiddenHomeCards,
            dailyGoals = DailyGoal.decode(prefs[Keys.DAILY_GOALS]),
            tasbihPresetId = prefs[Keys.TASBIH_PRESET_ID] ?: "zahra",
            tasbihStageIndex = (prefs[Keys.TASBIH_STAGE_INDEX] ?: 0).coerceIn(0, 2),
            tasbihStageCount = (prefs[Keys.TASBIH_STAGE_COUNT] ?: 0).coerceAtLeast(0),
            tasbihTotalCount = (prefs[Keys.TASBIH_TOTAL_COUNT] ?: 0).coerceAtLeast(0),
            tasbihCustomTarget = (prefs[Keys.TASBIH_CUSTOM_TARGET] ?: 70).coerceIn(1, 10000)
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

    suspend fun setThemeMode(mode: ThemeMode) {
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.key
            // Keep the legacy flag in sync so any remaining reader of it stays consistent.
            prefs[Keys.DARK_MODE] = mode == ThemeMode.DARK
        }
    }

    suspend fun setAccentChoice(choice: AccentChoice) {
        context.alHadithiDataStore.edit { it[Keys.ACCENT_CHOICE] = choice.key }
    }

    suspend fun setReadingFontChoice(choice: ReadingFontChoice) {
        context.alHadithiDataStore.edit { it[Keys.READING_FONT_CHOICE] = choice.key }
    }

    suspend fun setUiFontScale(scale: Float) {
        context.alHadithiDataStore.edit { it[Keys.UI_FONT_SCALE] = scale.coerceIn(0.85f, 1.45f) }
    }

    suspend fun setLineSpacing(option: LineSpacingOption) {
        context.alHadithiDataStore.edit { it[Keys.LINE_SPACING] = option.key }
    }

    suspend fun setCardDensity(density: CardDensity) {
        context.alHadithiDataStore.edit { it[Keys.CARD_DENSITY] = density.key }
    }

    suspend fun setQuranBackground(background: QuranBackgroundStyle) {
        context.alHadithiDataStore.edit { it[Keys.QURAN_BACKGROUND] = background.key }
    }

    suspend fun setQuranAutoScroll(enabled: Boolean, speed: Float = 1.0f) {
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.QURAN_AUTO_SCROLL] = enabled
            prefs[Keys.QURAN_AUTO_SCROLL_SPEED] = speed.coerceIn(0.25f, 3.0f)
        }
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        context.alHadithiDataStore.edit { it[Keys.TIME_FORMAT] = format.key }
    }

    suspend fun setPrayerAlert(prayer: PrayerKey, settings: PrayerAlertSettings) {
        context.alHadithiDataStore.edit { it[Keys.prayerAlertKey(prayer)] = settings.encode() }
    }

    suspend fun setQuietHours(quietHours: QuietHours) {
        context.alHadithiDataStore.edit { it[Keys.QUIET_HOURS] = quietHours.encode() }
    }

    suspend fun setHomeLayout(cards: List<HomeCard>, hidden: Set<HomeCard>) {
        context.alHadithiDataStore.edit { it[Keys.HOME_LAYOUT] = HomeLayoutCodec.encode(cards, hidden) }
    }

    suspend fun setDailyGoals(goals: Set<DailyGoal>) {
        context.alHadithiDataStore.edit { it[Keys.DAILY_GOALS] = DailyGoal.encode(goals) }
    }

    /**
     * Stores the state of the electronic tasbih so the count is never lost when the screen is
     * recreated, the device is rotated or the user navigates to another section.
     */
    suspend fun saveTasbihScreenState(
        presetId: String,
        stageIndex: Int,
        stageCount: Int,
        totalCount: Int,
        customTarget: Int
    ) {
        context.alHadithiDataStore.edit { prefs ->
            prefs[Keys.TASBIH_PRESET_ID] = presetId
            prefs[Keys.TASBIH_STAGE_INDEX] = stageIndex.coerceIn(0, 2)
            prefs[Keys.TASBIH_STAGE_COUNT] = stageCount.coerceAtLeast(0)
            prefs[Keys.TASBIH_TOTAL_COUNT] = totalCount.coerceAtLeast(0)
            prefs[Keys.TASBIH_CUSTOM_TARGET] = customTarget.coerceIn(1, 10000)
        }
    }

    suspend fun setFontChoice(choice: AppFontChoice) {
        context.alHadithiDataStore.edit { it[Keys.FONT_CHOICE] = choice.key }
    }

    suspend fun setReadingFontScale(scale: Float) {
        context.alHadithiDataStore.edit { it[Keys.READING_FONT_SCALE] = scale.coerceIn(0.85f, 1.55f) }
    }

    // Legacy prayer-method/tuning DataStore keys remain read-only for upgrade compatibility.
    // The bundled Iraq timetable is the sole runtime source for all prayer-time consumers.

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
            val currentCount = prefs[Keys.DAILY_TASBIH_COUNT] ?: 0
            prefs[Keys.DAILY_TASBIH_DAY_KEY] = dayKey
            prefs[Keys.DAILY_TASBIH_COUNT] =
                TasbihProgress.nextCount(currentDay, currentCount, dayKey, delta)
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

    /**
     * Reset only the selected preference family. User-created data is intentionally outside this
     * operation: last reading position, dhikr counters, daily tasbih progress, saved location,
     * onboarding completion, and the developer PIN are preserved.
     */
    /** Explicit destructive wipe used only by the separately confirmed Delete My Data action. */
    suspend fun clearAllUserPreferencesPreservingDeveloperPin() {
        context.alHadithiDataStore.edit { prefs ->
            val savedPin = prefs[Keys.ADMIN_PIN_HASH]
            prefs.clear()
            if (!savedPin.isNullOrBlank()) prefs[Keys.ADMIN_PIN_HASH] = savedPin
        }
    }

    suspend fun resetSettingsToDefaults(scope: SettingsResetScope = SettingsResetScope.ALL) {
        context.alHadithiDataStore.edit { prefs ->
            fun removeAppearance() {
                prefs.remove(Keys.THEME_MODE)
                prefs.remove(Keys.DARK_MODE)
                prefs.remove(Keys.ACCENT_CHOICE)
                prefs.remove(Keys.CARD_DENSITY)
                prefs.remove(Keys.QURAN_BACKGROUND)
            }
            fun removeFonts() {
                prefs.remove(Keys.FONT_CHOICE)
                prefs.remove(Keys.READING_FONT_CHOICE)
                prefs.remove(Keys.UI_FONT_SCALE)
                prefs.remove(Keys.READING_FONT_SCALE)
                prefs.remove(Keys.LINE_SPACING)
            }
            fun removeNotifications() {
                prefs.remove(Keys.NOTIFY_PRAYERS)
                prefs.remove(Keys.NOTIFY_MIN_BEFORE)
                prefs.remove(Keys.NOTIFY_OCCASIONS)
                prefs.remove(Keys.NOTIFY_ADHKAR)
                prefs.remove(Keys.QUIET_HOURS)
                PrayerKey.entries.forEach { prayer -> prefs.remove(Keys.prayerAlertKey(prayer)) }
            }
            fun removePrayerPreferences() {
                prefs.remove(Keys.CALC_METHOD)
                prefs.remove(Keys.TUNE_FAJR)
                prefs.remove(Keys.TUNE_SUNRISE)
                prefs.remove(Keys.TUNE_DHUHR)
                prefs.remove(Keys.TUNE_ASR)
                prefs.remove(Keys.TUNE_MAGHRIB)
                prefs.remove(Keys.TUNE_ISHA)
                prefs.remove(Keys.HIJRI_OFFSET)
                prefs.remove(Keys.TIME_FORMAT)
            }

            when (scope) {
                SettingsResetScope.APPEARANCE -> removeAppearance()
                SettingsResetScope.FONTS -> removeFonts()
                SettingsResetScope.NOTIFICATIONS -> removeNotifications()
                SettingsResetScope.PRAYER -> removePrayerPreferences()
                SettingsResetScope.ALL -> {
                    removeAppearance()
                    removeFonts()
                    removeNotifications()
                    removePrayerPreferences()
                    prefs.remove(Keys.QURAN_AUTO_SCROLL)
                    prefs.remove(Keys.QURAN_AUTO_SCROLL_SPEED)
                    prefs.remove(Keys.HOME_LAYOUT)
                    prefs.remove(Keys.DAILY_GOALS)
                    prefs.remove(Keys.HAPTIC_TASBIH)
                    prefs.remove(Keys.DAILY_TASBIH_GOAL)
                    prefs.remove(Keys.RECITER_ID)
                    prefs.remove(Keys.PLAYBACK_SPEED)
                    // Governorate and exact coordinates are retained to avoid changing location
                    // unexpectedly; user progress and private developer credential hash are kept.
                }
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
