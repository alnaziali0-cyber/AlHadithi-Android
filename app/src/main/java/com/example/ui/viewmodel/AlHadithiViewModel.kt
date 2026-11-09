package com.example.ui.viewmodel

import android.content.Intent
import android.net.Uri

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ScholarlyThinkingAssistant
import com.example.data.local.AdminAnnouncementEntity
import com.example.data.local.AlHadithiDatabase
import com.example.data.local.AudioTrackEntity
import com.example.data.local.BookEntity
import com.example.data.local.CachedAyahEntity
import com.example.data.local.ContentSourceEntity
import com.example.data.local.CurriculumCourseEntity
import com.example.data.local.DailyGoal
import com.example.data.local.DhikrEntity
import com.example.data.local.DuaZiyaratEntity
import com.example.data.local.FavoriteBookmarkEntity
import com.example.data.local.HadithEntity
import com.example.data.local.HomeCard
import com.example.data.local.IslamicOccasionEntity
import com.example.data.local.LessonProgressEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.PrayerCompletionEntity
import com.example.data.local.LOCAL_PRAYER_PROFILE_KEY
import com.example.data.local.ReadingHistoryEntity
import com.example.data.local.TasbihSessionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettingsState
import com.example.data.local.WallpaperEntity
import com.example.data.remote.AdminAnnouncementFirestoreRepository
import com.example.data.remote.CloudAdminAnnouncement
import com.example.data.remote.DeveloperGoogleAuthHelper
import com.example.data.remote.DeveloperGoogleAuthHelper.authStateFlow
import com.example.data.remote.PrayerTimesService
import com.example.data.remote.QuranNetworkRepository
import com.example.domain.DailyPrayerSchedule
import com.example.domain.IraqGovernorates
import com.example.domain.NextPrayerCountdown
import com.example.domain.PrayerAlertProfile
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerAndHijriCalculator
import com.example.domain.PrayerDailyProgress
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerKey
import com.example.domain.PrayerMinuteOffsets
import com.example.domain.QuietHours
import com.example.domain.HamedShakernejadJuzCatalog
import com.example.domain.SurahCatalog
import com.example.domain.TimeFormat
import com.example.media.AudioPlayerController
import com.example.media.AudioQueueItem
import com.example.media.AdhanAudioStorage
import com.example.media.PrayerNotificationScheduler
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.CardDensity
import com.example.ui.theme.LineSpacingOption
import com.example.ui.theme.QuranBackgroundStyle
import com.example.ui.theme.ReadingFontChoice
import com.example.ui.theme.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class UnifiedSearchResults(
    val query: String = "",
    val isSearching: Boolean = false,
    val ayahs: List<CachedAyahEntity> = emptyList(),
    val hadiths: List<HadithEntity> = emptyList(),
    val duasAndZiyarat: List<DuaZiyaratEntity> = emptyList(),
    val adhkar: List<DhikrEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val audioTracks: List<AudioTrackEntity> = emptyList(),
    val occasions: List<IslamicOccasionEntity> = emptyList()
) {
    val totalHits: Int
        get() = ayahs.size + hadiths.size + duasAndZiyarat.size +
            adhkar.size + books.size + audioTracks.size + occasions.size
}

sealed class AiScholarState {
    data object Idle : AiScholarState()
    data object Thinking : AiScholarState()
    data class Success(val question: String, val answer: String) : AiScholarState()
    data class Error(val message: String) : AiScholarState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class AlHadithiViewModel(application: Application) : AndroidViewModel(application) {

    private val appCtx = application.applicationContext
    private val database = AlHadithiDatabase.getDatabase(appCtx)
    private val dao = database.dao()
    private val prefsRepo = UserPreferencesRepository(appCtx)
    private val cloudAnnouncementRepo by lazy { AdminAnnouncementFirestoreRepository(appCtx) }
    private var prayerRefreshJob: Job? = null

    val settings: StateFlow<UserSettingsState> = prefsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSettingsState()
    )

    private val initialBaghdad = IraqGovernorates.byId("baghdad")
    private val _todaySchedule = MutableStateFlow(
        PrayerTimesService.previewLocalSchedule(initialBaghdad)
    )
    val todaySchedule: StateFlow<DailyPrayerSchedule> = _todaySchedule.asStateFlow()

    private val _nextPrayerCountdown = MutableStateFlow(
        PrayerAndHijriCalculator.computeNextPrayerCountdown(_todaySchedule.value)
    )
    val nextPrayerCountdown: StateFlow<NextPrayerCountdown> = _nextPrayerCountdown.asStateFlow()

    private val _monthlySchedules = MutableStateFlow<List<DailyPrayerSchedule>>(emptyList())
    val monthlySchedules: StateFlow<List<DailyPrayerSchedule>> = _monthlySchedules.asStateFlow()

    private val prayerTrackingDayKey = MutableStateFlow(PrayerDailyProgress.dayKey())
    val todayPrayerCompletions: StateFlow<List<PrayerCompletionEntity>> = prayerTrackingDayKey
        .flatMapLatest { dayKey ->
            dao.observePrayerCompletionsForDay(LOCAL_PRAYER_PROFILE_KEY, dayKey)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Encyclopedia Streams (100% Local Offline Room)
    val hadiths: StateFlow<List<HadithEntity>> = dao.observeAllHadiths()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val duasAndZiyarat: StateFlow<List<DuaZiyaratEntity>> = dao.observeAllDuasAndZiyarat()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adhkar: StateFlow<List<DhikrEntity>> = dao.observeAllAdhkar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val books: StateFlow<List<BookEntity>> = dao.observeAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val courses: StateFlow<List<CurriculumCourseEntity>> = dao.observeAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val lessonProgress: StateFlow<List<LessonProgressEntity>> = dao.observeAllLessonProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val quizAttempts: StateFlow<List<QuizAttemptEntity>> = dao.observeQuizAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val audioTracks: StateFlow<List<AudioTrackEntity>> = dao.observeAllAudioTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val occasions: StateFlow<List<IslamicOccasionEntity>> = dao.observeAllOccasions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val wallpapers: StateFlow<List<WallpaperEntity>> = dao.observeAllWallpapers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val favorites: StateFlow<List<FavoriteBookmarkEntity>> = dao.observeAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val readingHistory: StateFlow<List<ReadingHistoryEntity>> = dao.observeReadingHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val tasbihSessions: StateFlow<List<TasbihSessionEntity>> = dao.observeTasbihSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val contentSources: StateFlow<List<ContentSourceEntity>> = dao.observeContentSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** The locally stored repeat counters of the adhkar, keyed by the dhikr id. */
    val dhikrCounters: StateFlow<Map<Long, Int>> = prefsRepo.dhikrCountersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    // Local Announcements (for HomeScreen & Offline Developer Panel)
    val activeAnnouncements: StateFlow<List<AdminAnnouncementEntity>> = dao.observeActiveAnnouncements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAnnouncements: StateFlow<List<AdminAnnouncementEntity>> = dao.observeAllAnnouncements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Authorized Developer Firebase User (strictly gated to the configured owner)
    val developerAuthUser: StateFlow<FirebaseUser?> = try {
        Firebase.auth.authStateFlow()
            .map { user -> if (DeveloperGoogleAuthHelper.isAuthorizedDeveloperUser(user)) user else null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)
    } catch (_: Exception) {
        MutableStateFlow(null)
    }

    // Active Surah Reader State
    private val _activeSurahNumber = MutableStateFlow(1)
    val activeSurahNumber: StateFlow<Int> = _activeSurahNumber.asStateFlow()

    private val _activeSurahAyahs = MutableStateFlow<List<CachedAyahEntity>>(emptyList())
    val activeSurahAyahs: StateFlow<List<CachedAyahEntity>> = _activeSurahAyahs.asStateFlow()

    private val _isLoadingSurah = MutableStateFlow(false)
    val isLoadingSurah: StateFlow<Boolean> = _isLoadingSurah.asStateFlow()

    private val _surahError = MutableStateFlow<String?>(null)
    val surahError: StateFlow<String?> = _surahError.asStateFlow()

    // Unified Search & AI Scholar State
    private val _searchResults = MutableStateFlow(UnifiedSearchResults())
    val searchResults: StateFlow<UnifiedSearchResults> = _searchResults.asStateFlow()

    private val _aiScholarState = MutableStateFlow<AiScholarState>(AiScholarState.Idle)
    val aiScholarState: StateFlow<AiScholarState> = _aiScholarState.asStateFlow()

    init {
        AudioPlayerController.init(appCtx)

        viewModelScope.launch {
            AlHadithiDatabase.ensureSeeded(dao)
            // Attempt to load a verified bundled Quran if the release includes one; otherwise surahs use the network/cache path.
            QuranNetworkRepository.ensureBundledQuranLoaded(appCtx)
            loadSurah(1)
        }

        // Sync Cloud Admin Announcements into local Room only when the configured owner is signed in
        viewModelScope.launch {
            developerAuthUser.flatMapLatest { devUser ->
                if (devUser == null) {
                    flowOf(emptyList())
                } else {
                    cloudAnnouncementRepo.observeAnnouncements(devUser.uid)
                }
            }.collectLatest { cloudItems ->
                syncCloudAnnouncementsToLocal(cloudItems)
            }
        }

        // Recalculate prayer times and reschedule alarms whenever relevant settings change.
        viewModelScope.launch {
            settings.map { s ->
                PrayerSettingsKey(
                    governorateId = s.governorateId,
                    methodCode = s.prayerCalcMethodCode,
                    offsets = s.prayerOffsets,
                    hijriOffsetDays = s.hijriOffsetDays,
                    latitude = s.customLatitude,
                    longitude = s.customLongitude,
                    notificationsEnabled = s.notifyPrayers,
                    alertProfile = s.prayerAlertProfile
                )
            }.distinctUntilChanged().collectLatest { config ->
                refreshPrayerTimes(
                    govId = config.governorateId,
                    method = PrayerCalcMethod.fromCode(config.methodCode),
                    offsets = config.offsets,
                    hijriOffsetDays = config.hijriOffsetDays,
                    latitudeOverride = config.latitude,
                    longitudeOverride = config.longitude
                )
            }
        }

        // Live 1-second countdown ticker for Next Prayer
        viewModelScope.launch {
            while (true) {
                _nextPrayerCountdown.value = PrayerAndHijriCalculator.computeNextPrayerCountdown(
                    _todaySchedule.value
                )
                val currentDayKey = PrayerDailyProgress.dayKey()
                if (prayerTrackingDayKey.value != currentDayKey) {
                    prayerTrackingDayKey.value = currentDayKey
                }
                delay(1000L)
            }
        }
    }

    private suspend fun syncCloudAnnouncementsToLocal(cloudItems: List<CloudAdminAnnouncement>) {
        if (cloudItems.isEmpty()) return
        for (c in cloudItems) {
            if (c.id.isBlank()) continue
            val existing = dao.findAnnouncementByCloudId(c.id)
            val createdMs = c.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            if (existing != null) {
                dao.insertAnnouncement(
                    existing.copy(
                        title = c.title,
                        body = c.body,
                        badgeLabel = c.badgeLabel,
                        actionUrl = c.actionUrl,
                        isActive = c.isActive
                    )
                )
            } else {
                dao.insertAnnouncement(
                    AdminAnnouncementEntity(
                        cloudId = c.id,
                        title = c.title,
                        body = c.body,
                        badgeLabel = c.badgeLabel,
                        actionUrl = c.actionUrl,
                        isActive = c.isActive,
                        createdAtMillis = createdMs
                    )
                )
            }
        }
    }

    private data class PrayerSettingsKey(
        val governorateId: String,
        val methodCode: Int,
        val offsets: PrayerMinuteOffsets,
        val hijriOffsetDays: Int,
        val latitude: Double?,
        val longitude: Double?,
        val notificationsEnabled: Boolean,
        val alertProfile: PrayerAlertProfile
    )

    fun refreshPrayerTimes(
        govId: String = settings.value.governorateId,
        method: PrayerCalcMethod = PrayerCalcMethod.fromCode(settings.value.prayerCalcMethodCode),
        offsets: PrayerMinuteOffsets = settings.value.prayerOffsets,
        hijriOffsetDays: Int = settings.value.hijriOffsetDays,
        latitudeOverride: Double? = settings.value.customLatitude,
        longitudeOverride: Double? = settings.value.customLongitude
    ) {
        val gov = IraqGovernorates.byId(govId)
        val latitude = latitudeOverride ?: gov.latitude
        val longitude = longitudeOverride ?: gov.longitude
        prayerRefreshJob?.cancel()
        prayerRefreshJob = viewModelScope.launch {
            try {
                val schedules = PrayerTimesService(appCtx, dao).loadWindow(
                    governorateId = gov.id, latitude = latitude, longitude = longitude,
                    method = method, offsets = offsets, hijriOffsetDays = hijriOffsetDays, days = 35
                )
                val todayKey = PrayerDailyProgress.dayKey().replace("-", "")
                val today = schedules.firstOrNull { it.dateKey == todayKey }
                    ?: schedules.firstOrNull()
                    ?: return@launch
                _todaySchedule.value = today
                _nextPrayerCountdown.value = PrayerAndHijriCalculator.computeNextPrayerCountdown(today)
                _monthlySchedules.value = schedules
                if (settings.value.notifyPrayers) {
                    PrayerNotificationScheduler.schedulePrayerAlarms(
                        context = appCtx, latitude = latitude, longitude = longitude, method = method,
                        offsets = offsets, areaName = gov.arabicName, enabled = true,
                        schedules = schedules, alertProfile = settings.value.prayerAlertProfile,
                        governorateId = gov.id
                    )
                } else {
                    PrayerNotificationScheduler.disable(appCtx)
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (error: Exception) {
                android.util.Log.w("AlHadithiViewModel", "Central prayer schedule refresh failed")
                if (settings.value.notifyPrayers) PrayerNotificationScheduler.restoreAndReschedule(appCtx)
            }
        }
    }

    /** Persists today's prayer checkmarks in Room using Iraq's civil date. */
    fun setPrayerCompleted(prayer: PrayerKey, completed: Boolean) {
        val dayKey = PrayerDailyProgress.dayKey()
        viewModelScope.launch {
            if (completed) {
                dao.upsertPrayerCompletion(
                    PrayerCompletionEntity(
                        profileKey = LOCAL_PRAYER_PROFILE_KEY,
                        dayKey = dayKey,
                        prayerKey = prayer.key,
                        isCompleted = true,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            } else {
                dao.deletePrayerCompletion(LOCAL_PRAYER_PROFILE_KEY, dayKey, prayer.key)
            }
        }
    }

    // --- Settings Actions ---
    fun selectGovernorate(govId: String) {
        viewModelScope.launch { prefsRepo.selectGovernorate(govId) }
    }

    fun selectNearestGovernorateFromCoordinates(lat: Double, lon: Double) {
        if (!lat.isFinite() || lat !in -90.0..90.0 || !lon.isFinite() || lon !in -180.0..180.0) return
        val nearest = IraqGovernorates.nearestTo(lat, lon)
        viewModelScope.launch {
            prefsRepo.selectGovernorate(nearest.id)
            prefsRepo.setPrayerCoordinates(lat, lon)
        }
    }

    fun setPrayerCoordinates(latitude: Double, longitude: Double) {
        if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0
        ) return
        viewModelScope.launch { prefsRepo.setPrayerCoordinates(latitude, longitude) }
    }

    fun clearPrayerCoordinates() {
        viewModelScope.launch { prefsRepo.clearPrayerCoordinates() }
    }

    fun dismissGovernoratePrompt() {
        viewModelScope.launch { prefsRepo.dismissOnboardingPrompt() }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkMode(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAccentChoice(choice: AccentChoice) {
        viewModelScope.launch { prefsRepo.setAccentChoice(choice) }
    }

    fun setReadingFontChoice(choice: ReadingFontChoice) {
        viewModelScope.launch { prefsRepo.setReadingFontChoice(choice) }
    }

    fun setUiFontScale(scale: Float) {
        viewModelScope.launch { prefsRepo.setUiFontScale(scale) }
    }

    fun setLineSpacing(option: LineSpacingOption) {
        viewModelScope.launch { prefsRepo.setLineSpacing(option) }
    }

    fun setCardDensity(density: CardDensity) {
        viewModelScope.launch { prefsRepo.setCardDensity(density) }
    }

    fun setQuranBackground(background: QuranBackgroundStyle) {
        viewModelScope.launch { prefsRepo.setQuranBackground(background) }
    }

    fun setQuranAutoScroll(enabled: Boolean, speed: Float = 1.0f) {
        viewModelScope.launch { prefsRepo.setQuranAutoScroll(enabled, speed) }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch { prefsRepo.setTimeFormat(format) }
    }

    fun setPrayerAlert(prayer: PrayerKey, settings: PrayerAlertSettings) {
        viewModelScope.launch { prefsRepo.setPrayerAlert(prayer, settings) }
    }

    fun setQuietHours(quietHours: QuietHours) {
        viewModelScope.launch { prefsRepo.setQuietHours(quietHours) }
    }

    fun setHomeLayout(cards: List<HomeCard>, hidden: Set<HomeCard>) {
        viewModelScope.launch { prefsRepo.setHomeLayout(cards, hidden) }
    }

    fun setDailyGoals(goals: Set<DailyGoal>) {
        viewModelScope.launch { prefsRepo.setDailyGoals(goals) }
    }

    fun setFontChoice(choice: AppFontChoice) {
        viewModelScope.launch { prefsRepo.setFontChoice(choice) }
    }

    fun setReadingFontScale(scale: Float) {
        viewModelScope.launch { prefsRepo.setReadingFontScale(scale) }
    }

    fun setPrayerCalcMethod(code: Int) {
        viewModelScope.launch { prefsRepo.setPrayerCalcMethod(code) }
    }

    fun updatePrayerOffsets(offsets: PrayerMinuteOffsets) {
        viewModelScope.launch { prefsRepo.updatePrayerOffsets(offsets) }
    }

    fun setHijriOffsetDays(offset: Int) {
        viewModelScope.launch { prefsRepo.setHijriOffsetDays(offset) }
    }

    fun updateNotificationPrefs(
        notifyPrayers: Boolean? = null,
        minutesBefore: Int? = null,
        notifyOccasions: Boolean? = null,
        notifyAdhkar: Boolean? = null
    ) {
        viewModelScope.launch {
            prefsRepo.updateNotificationPrefs(notifyPrayers, minutesBefore, notifyOccasions, notifyAdhkar)
        }
    }

    fun setHapticTasbih(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticTasbih(enabled) }
    }

    /** Increments the repeat counter of one dhikr; the progress is stored on the device. */
    fun incrementDhikrCounter(id: Long) {
        val todayKey = _todaySchedule.value.hijriDate.formattedArabic()
        viewModelScope.launch { prefsRepo.incrementDhikrCounter(id, todayKey) }
    }

    fun resetDhikrCounter(id: Long) {
        viewModelScope.launch { prefsRepo.resetDhikrCounter(id) }
    }

    fun setDailyTasbihGoal(goal: Int) {
        viewModelScope.launch { prefsRepo.setDailyTasbihGoal(goal) }
    }

    /** Stores the electronic tasbih counter so it survives a rotation or a navigation. */
    fun saveTasbihScreenState(
        presetId: String,
        stageIndex: Int,
        stageCount: Int,
        totalCount: Int,
        customTarget: Int
    ) {
        viewModelScope.launch {
            prefsRepo.saveTasbihScreenState(presetId, stageIndex, stageCount, totalCount, customTarget)
        }
    }

    fun recordTasbihTick() {
        val todayKey = _todaySchedule.value.hijriDate.formattedArabic()
        viewModelScope.launch { prefsRepo.incrementDailyTasbih(todayKey, 1) }
    }

    fun saveTasbihSession(modeTitle: String, dhikrText: String, completedCount: Int, targetCount: Int) {
        if (completedCount <= 0) return
        viewModelScope.launch {
            dao.insertTasbihSession(
                TasbihSessionEntity(
                    modeTitle = modeTitle,
                    dhikrText = dhikrText,
                    completedCount = completedCount,
                    targetCount = targetCount,
                    completedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearCachedQuranTexts() {
        viewModelScope.launch {
            dao.clearDownloadedQuranCacheKeepEssentials()
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch { prefsRepo.resetSettingsToDefaults() }
    }

    /**
     * Removes everything the user created on this device: favourites, reading history, tasbih
     * sessions, course progress, cached Quran text and every stored preference. The bundled
     * encyclopedia content stays intact because it is part of the application, not user data.
     */
    fun deleteAllUserData() {
        viewModelScope.launch {
            dao.deleteAllFavorites()
            dao.deleteAllReadingHistory()
            dao.deleteAllTasbihSessions()
            dao.deleteAllLessonProgress()
            dao.deleteAllQuizAttempts()
            dao.deleteAllPrayerCompletions()
            dao.deleteAllCachedAyahs()
            prefsRepo.resetSettingsToDefaults()
            PrayerNotificationScheduler.disable(appCtx)
            AdhanAudioStorage.clear(appCtx)
        }
    }

    // --- Quran & Offline-First Audio Actions ---
    fun loadSurah(surahNumber: Int, recordHistory: Boolean = false) {
        _activeSurahNumber.value = surahNumber
        _isLoadingSurah.value = true
        _surahError.value = null
        val meta = SurahCatalog.byNumber(surahNumber)
        viewModelScope.launch {
            val result = QuranNetworkRepository.loadOrFetchSurah(surahNumber, dao, appCtx)
            _isLoadingSurah.value = false
            result.onSuccess { ayahs ->
                _activeSurahAyahs.value = ayahs
                if (recordHistory) {
                    prefsRepo.saveLastQuranPosition(surahNumber, meta.arabicName, 1)
                    dao.upsertReadingHistory(
                        ReadingHistoryEntity(
                            itemType = "QURAN",
                            itemRefKey = "surah_$surahNumber",
                            title = meta.arabicName,
                            positionLabel = "${meta.revelationTypeArabic} · ${meta.ayahCount} آية",
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            }.onFailure { err ->
                _surahError.value = err.localizedMessage ?: "تعذر تحميل السورة"
            }
        }
    }

    fun saveLastReadAyah(surahNumber: Int, ayahNumber: Int) {
        val meta = SurahCatalog.byNumber(surahNumber)
        viewModelScope.launch {
            prefsRepo.saveLastQuranPosition(surahNumber, meta.arabicName, ayahNumber)
            dao.upsertReadingHistory(
                ReadingHistoryEntity(
                    itemType = "QURAN",
                    itemRefKey = "surah_$surahNumber",
                    title = meta.arabicName,
                    positionLabel = "الآية $ayahNumber",
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun setReciter(reciterId: String) {
        viewModelScope.launch { prefsRepo.setReciter(reciterId) }
    }

    fun setPlaybackSpeed(speed: Float) {
        AudioPlayerController.setSpeed(speed)
        viewModelScope.launch { prefsRepo.setPlaybackSpeed(speed) }
    }

    fun playSurahAudio(surahNumber: Int) {
        val reciter = SurahCatalog.reciterById(settings.value.selectedReciterId)
        when (reciter.audioUnit) {
            "external" -> {
                val source = reciter.audioSourcePageUrl ?: return
                runCatching {
                    appCtx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                return
            }
            "juz" -> {
                val surah = SurahCatalog.byNumber(surahNumber)
                val juz = surah.juzStart.coerceIn(1, 30)
                val url = HamedShakernejadJuzCatalog.urlForJuz(juz) ?: return
                AudioPlayerController.playSingle(
                    AudioQueueItem(
                        idKey = "juz_${reciter.id}_$juz",
                        title = "الجزء ${juz.toString().padStart(2, '0')} — يبدأ من ${surah.arabicName}",
                        performer = reciter.arabicName,
                        category = "تلاوة القرآن الكريم — تلاوة بالأجزاء",
                        primaryUrl = url,
                        offlineRawResName = null
                    ),
                    settings.value.playbackSpeed
                )
                return
            }
        }
        val queue = SurahCatalog.allSurahs.map { s ->
            AudioQueueItem(
                idKey = reciter.audioDownloadKey(s.number),
                title = s.arabicName,
                performer = reciter.arabicName,
                category = "تلاوة القرآن الكريم",
                primaryUrl = reciter.buildSurahStreamUrl(s.number),
                fallbackUrl = reciter.buildMp3QuranFallbackUrl(s.number),
                offlineRawResName = null
            )
        }
        val startIdx = (surahNumber - 1).coerceIn(0, queue.lastIndex)
        AudioPlayerController.playQueue(queue, startIdx, settings.value.playbackSpeed)
    }

    fun playLibraryAudioTrack(track: AudioTrackEntity, fullList: List<AudioTrackEntity>) {
        val listToUse = fullList.ifEmpty { listOf(track) }
        val queue = listToUse.map { t ->
            AudioQueueItem(
                idKey = "audio_${t.id}",
                title = t.title,
                performer = t.reciterOrRadood,
                category = t.category,
                primaryUrl = t.streamUrl,
                offlineRawResName = t.offlineRawResName.ifBlank { null }
            )
        }
        val idx = listToUse.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        AudioPlayerController.playQueue(queue, idx, settings.value.playbackSpeed)
        viewModelScope.launch {
            dao.upsertReadingHistory(
                ReadingHistoryEntity(
                    itemType = "AUDIO",
                    itemRefKey = "audio_${track.id}",
                    title = track.title,
                    positionLabel = track.reciterOrRadood,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Favorites / Bookmarks & Reading Progress ---
    fun toggleFavorite(
        itemType: String,
        itemRefKey: String,
        title: String,
        subtitle: String,
        snippet: String
    ) {
        viewModelScope.launch {
            val exists = dao.isFavoriteExists(itemType, itemRefKey)
            if (exists) {
                dao.deleteFavoriteByRef(itemType, itemRefKey)
            } else {
                dao.insertFavorite(
                    FavoriteBookmarkEntity(
                        itemType = itemType,
                        itemRefKey = itemRefKey,
                        title = title,
                        subtitle = subtitle,
                        contentSnippet = snippet.take(260),
                        savedAtMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateBookReadingProgress(book: BookEntity, chapterIdx: Int, totalChapters: Int) {
        val percent = (((chapterIdx + 1).toFloat() / totalChapters.coerceAtLeast(1)) * 100f).toInt()
            .coerceIn(0, 100)
        viewModelScope.launch {
            dao.updateBook(
                book.copy(
                    lastReadChapterIndex = chapterIdx,
                    readingProgressPercent = maxOf(book.readingProgressPercent, percent)
                )
            )
            dao.upsertReadingHistory(
                ReadingHistoryEntity(
                    itemType = "BOOK",
                    itemRefKey = "book_${book.id}",
                    title = book.title,
                    positionLabel = "الفصل ${chapterIdx + 1} من $totalChapters",
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun markLessonCompleted(courseId: Long, lessonIndex: Int) {
        viewModelScope.launch {
            dao.markLessonCompleted(
                LessonProgressEntity(
                    courseId = courseId,
                    lessonIndex = lessonIndex,
                    completedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveQuizAttempt(courseId: Long, courseTitle: String, score: Int, total: Int) {
        viewModelScope.launch {
            dao.insertQuizAttempt(
                QuizAttemptEntity(
                    courseId = courseId,
                    courseTitle = courseTitle,
                    score = score,
                    totalQuestions = total,
                    attemptedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Unified Offline Search & High-Thinking AI Scholar ---
    fun executeUnifiedSearch(rawQuery: String) {
        val q = rawQuery.trim()
        if (q.isEmpty()) {
            _searchResults.value = UnifiedSearchResults()
            return
        }
        _searchResults.value = _searchResults.value.copy(query = q, isSearching = true)
        viewModelScope.launch {
            val ayahs = QuranNetworkRepository.searchQuran(q, dao, appCtx)
            val h = dao.searchHadiths(q)
            val dz = dao.searchDuasZiyarat(q)
            val ad = dao.searchAdhkar(q)
            val bk = dao.searchBooks(q)
            val au = dao.searchAudioTracks(q)
            val oc = dao.searchOccasions(q)
            _searchResults.value = UnifiedSearchResults(
                query = q,
                isSearching = false,
                ayahs = ayahs,
                hadiths = h,
                duasAndZiyarat = dz,
                adhkar = ad,
                books = bk,
                audioTracks = au,
                occasions = oc
            )
        }
    }

    fun askDeepScholarAi(question: String) {
        val q = question.trim()
        if (q.isEmpty()) return
        _aiScholarState.value = AiScholarState.Thinking
        viewModelScope.launch {
            val localContext = buildString {
                hadiths.value.take(5).forEach {
                    appendLine("- حديث (${it.infallibleName}): ${it.arabicText} [${it.sourceBook}]")
                }
                books.value.take(3).forEach {
                    appendLine("- كتاب: ${it.title} (${it.author}) — ${it.summary}")
                }
            }
            val result = ScholarlyThinkingAssistant.askScholarlyQuestion(q, localContext)
            result.onSuccess { ans ->
                _aiScholarState.value = AiScholarState.Success(q, ans)
            }.onFailure { err ->
                _aiScholarState.value = AiScholarState.Error(
                    err.localizedMessage ?: "الباحث الذكي يتطلب اتصالاً بالإنترنت؛ المواد المحلية المحفوظة متاحة دون اتصال، بينما تحميل النصوص أو المواد غير المخزنة قد يحتاج إلى الإنترنت."
                )
            }
        }
    }

    fun resetAiScholar() {
        _aiScholarState.value = AiScholarState.Idle
    }

    // --- Hidden Developer Panel: Announcements / Ads Management (configured owner only) ---
    fun developerAddAnnouncement(
        title: String,
        body: String,
        badgeLabel: String,
        actionUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val devUser = developerAuthUser.value
        if (devUser == null) {
            onComplete(false, "غير مصرح: يجب تسجيل الدخول بحساب المطور المعتمد")
            return
        }
        viewModelScope.launch {
            val cleanTitle = title.trim()
            val cleanBody = body.trim()
            val cleanBadge = badgeLabel.trim().ifBlank { "إعلان إداري" }
            val cleanUrl = actionUrl.trim()

            // Try cloud write first (also works offline with Firestore persistence), and mirror locally
            val cloudResult = cloudAnnouncementRepo.createAnnouncement(
                title = cleanTitle,
                body = cleanBody,
                badgeLabel = cleanBadge,
                actionUrl = cleanUrl
            )
            val cloudId = cloudResult.getOrDefault("")
            if (cloudId.isBlank() || dao.findAnnouncementByCloudId(cloudId) == null) {
                dao.insertAnnouncement(
                    AdminAnnouncementEntity(
                        cloudId = cloudId,
                        title = cleanTitle,
                        body = cleanBody,
                        badgeLabel = cleanBadge,
                        actionUrl = cleanUrl,
                        isActive = true,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            }
            onComplete(true, "تم نشر الإعلان بنجاح ✓")
        }
    }

    fun developerToggleAnnouncementActive(item: AdminAnnouncementEntity, active: Boolean) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch {
            dao.updateAnnouncementActiveById(item.id, active)
            if (item.cloudId.isNotBlank()) {
                cloudAnnouncementRepo.updateAnnouncementActive(
                    CloudAdminAnnouncement(id = item.cloudId),
                    active
                )
            }
        }
    }

    fun developerDeleteAnnouncement(item: AdminAnnouncementEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch {
            dao.deleteAnnouncementById(item.id)
            if (item.cloudId.isNotBlank()) {
                cloudAnnouncementRepo.deleteAnnouncement(item.cloudId)
            }
        }
    }

    // --- Hidden Developer Panel: Encyclopedia CMS Actions (gated to the configured owner) ---
    fun adminAddHadith(item: HadithEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.insertSingleHadith(item) }
    }

    fun adminDeleteHadith(id: Long) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.deleteHadithById(id) }
    }

    fun adminAddDuaOrZiyarat(item: DuaZiyaratEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.insertSingleDuaZiyarat(item) }
    }

    fun adminDeleteDuaOrZiyarat(id: Long) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.deleteDuaZiyaratById(id) }
    }

    fun adminAddDhikr(item: DhikrEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.insertSingleDhikr(item) }
    }

    fun adminDeleteDhikr(id: Long) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.deleteDhikrById(id) }
    }

    fun adminAddBook(item: BookEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.insertSingleBook(item) }
    }

    fun adminDeleteBook(id: Long) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.deleteBookById(id) }
    }

    fun adminAddAudioTrack(item: AudioTrackEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.insertSingleAudioTrack(item) }
    }

    fun adminDeleteAudioTrack(id: Long) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.deleteAudioTrackById(id) }
    }

    fun adminAddOccasion(item: IslamicOccasionEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.insertSingleOccasion(item) }
    }

    fun adminDeleteOccasion(id: Long) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.deleteOccasionById(id) }
    }

    fun adminAddWallpaper(item: WallpaperEntity) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch { dao.insertSingleWallpaper(item) }
    }

    fun adminDeleteWallpaper(id: Long) {
        if (developerAuthUser.value == null) return
        viewModelScope.launch {
            val existing = dao.getWallpaperById(id)
            dao.deleteWallpaperById(id)
            existing?.localImagePath?.takeIf { it.isNotBlank() }?.let { path ->
                runCatching { java.io.File(path).delete() }
            }
        }
    }
}
