package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.domain.IraqGovernorates
import com.example.media.ObserveDeviceShake
import com.example.ui.components.GlassAmbientBackground
import com.example.ui.components.GovernorateSelectorSheet
import com.example.ui.components.PersistentGlassMiniPlayer
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.AudioLibraryScreen
import com.example.ui.screens.CalendarOccasionsScreen
import com.example.ui.screens.CurriculaScreen
import com.example.ui.screens.DuasZiyaratAdhkarScreen
import com.example.ui.screens.FavoritesAndHistoryScreen
import com.example.ui.screens.HadithEncyclopediaScreen
import com.example.ui.screens.HiddenDeveloperPanelScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryAndBookReaderScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranIndexAndReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.screens.UnifiedSearchAndScholarScreen
import com.example.ui.screens.WallpapersScreen
import com.example.domain.PrayerCalcMethod
import com.example.ui.theme.AlHadithiTheme
import com.example.ui.theme.LocalIsDarkThemeActive
import com.example.ui.viewmodel.AlHadithiViewModel
import com.example.util.SystemActionsHelper

private data class PrimaryNavSpec(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AlHadithiAppRoot(
    vm: AlHadithiViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val schedule by vm.todaySchedule.collectAsStateWithLifecycle()
    val countdown by vm.nextPrayerCountdown.collectAsStateWithLifecycle()
    val monthlySchedules by vm.monthlySchedules.collectAsStateWithLifecycle()

    val hadiths by vm.hadiths.collectAsStateWithLifecycle()
    val duasAndZiyarat by vm.duasAndZiyarat.collectAsStateWithLifecycle()
    val adhkar by vm.adhkar.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val courses by vm.courses.collectAsStateWithLifecycle()
    val lessonProgress by vm.lessonProgress.collectAsStateWithLifecycle()
    val todayPrayerCompletions by vm.todayPrayerCompletions.collectAsStateWithLifecycle()
    val quizAttempts by vm.quizAttempts.collectAsStateWithLifecycle()
    val audioTracks by vm.audioTracks.collectAsStateWithLifecycle()
    val occasions by vm.occasions.collectAsStateWithLifecycle()
    val wallpapers by vm.wallpapers.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val readingHistory by vm.readingHistory.collectAsStateWithLifecycle()
    val tasbihSessions by vm.tasbihSessions.collectAsStateWithLifecycle()
    val contentSources by vm.contentSources.collectAsStateWithLifecycle()
    val dhikrCounters by vm.dhikrCounters.collectAsStateWithLifecycle()

    val activeAnnouncements by vm.activeAnnouncements.collectAsStateWithLifecycle()
    val allAnnouncements by vm.allAnnouncements.collectAsStateWithLifecycle()
    val developerAuthUser by vm.developerAuthUser.collectAsStateWithLifecycle()

    val activeSurahAyahs by vm.activeSurahAyahs.collectAsStateWithLifecycle()
    val isLoadingSurah by vm.isLoadingSurah.collectAsStateWithLifecycle()
    val surahError by vm.surahError.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val aiScholarState by vm.aiScholarState.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var openedSurahInReader by rememberSaveable { mutableStateOf<Int?>(null) }
    var manualGovSheetOpen by remember { mutableStateOf(false) }

    val openHiddenDeveloperPanel: () -> Unit = {
        if (currentDestination != AppDestination.ADMIN) {
            SystemActionsHelper.triggerHapticPulse(context, strongMilestone = true)
            Toast.makeText(context, "تم كشف لوحة المطور المخفية", Toast.LENGTH_SHORT).show()
            currentDestination = AppDestination.ADMIN
        }
    }

    // Physical Device Shake Trigger -> Opens Hidden Developer Panel!
    ObserveDeviceShake(
        enabled = currentDestination != AppDestination.ADMIN,
        onShakeDetected = openHiddenDeveloperPanel
    )

    val homeLabel = stringResource(R.string.nav_home)
    val quranLabel = stringResource(R.string.nav_quran)
    val duasLabel = stringResource(R.string.nav_duas)
    val moreLabel = stringResource(R.string.nav_more)

    val navItems = remember(homeLabel, quranLabel, duasLabel, moreLabel) {
        listOf(
            PrimaryNavSpec(AppDestination.HOME, homeLabel, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
            PrimaryNavSpec(AppDestination.QURAN, quranLabel, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_tab_quran"),
            PrimaryNavSpec(AppDestination.AUDIO_LIBRARY, "الصوتيات", Icons.Filled.Headphones, Icons.Outlined.Headphones, "nav_tab_audio"),
            PrimaryNavSpec(AppDestination.DUAS_ZIYARAT, duasLabel, Icons.Filled.VolunteerActivism, Icons.Outlined.VolunteerActivism, "nav_tab_duas"),
            PrimaryNavSpec(AppDestination.MORE_HUB, moreLabel, Icons.Filled.Apps, Icons.Outlined.Apps, "nav_tab_more")
        )
    }

    val currentGovernorate = IraqGovernorates.byId(settings.governorateId)

    // A short, human readable summary of how the displayed prayer times were produced.
    val prayerCalculationSummary = remember(settings.prayerCalcMethodCode, currentGovernorate.id) {
        val method = PrayerCalcMethod.fromCode(settings.prayerCalcMethodCode)
        "${method.arabicTitle} · توقيت بغداد (UTC+3)"
    }

    AlHadithiTheme(
        themeMode = settings.themeMode,
        darkTheme = settings.darkMode,
        accentChoice = settings.accentChoice,
        fontChoice = settings.fontChoice,
        readingFontChoice = settings.readingFontChoice,
        uiFontScale = settings.uiFontScale,
        readingFontScale = settings.readingFontScale,
        cardDensity = settings.cardDensity,
        lineSpacing = settings.lineSpacing,
        content = {
        val navGlassBg = if (settings.darkMode) {
            Color(0xFF102422).copy(alpha = 0.72f)
        } else {
            Color.White.copy(alpha = 0.70f)
        }
        val navGlassBorder = if (settings.darkMode) {
            Color.White.copy(alpha = 0.16f)
        } else {
            Color.White.copy(alpha = 0.85f)
        }

        GlassAmbientBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedScreen = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Column {
                            PersistentGlassMiniPlayer(
                                onSpeedChanged = { vm.setPlaybackSpeed(it) }
                            )
                            if (!isExpandedScreen) {
                                GlassBottomNavigationBar(
                                    items = navItems,
                                    currentDestination = currentDestination,
                                    containerColor = navGlassBg,
                                    borderColor = navGlassBorder,
                                    onSelect = { destination ->
                                        if (destination == AppDestination.QURAN && currentDestination == AppDestination.QURAN) {
                                            openedSurahInReader = null
                                        }
                                        currentDestination = destination
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isExpandedScreen) {
                            NavigationRail(
                                containerColor = navGlassBg,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(BorderStroke(1.dp, navGlassBorder), RoundedCornerShape(24.dp))
                            ) {
                                navItems.forEach { spec ->
                                    val isSelected = currentDestination == spec.destination
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (spec.destination == AppDestination.QURAN && currentDestination == AppDestination.QURAN) {
                                                openedSurahInReader = null
                                            }
                                            currentDestination = spec.destination
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) spec.selectedIcon else spec.unselectedIcon,
                                                contentDescription = spec.label
                                            )
                                        },
                                        label = { Text(spec.label) },
                                        modifier = Modifier.testTag(spec.testTag)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = if (isExpandedScreen) 860.dp else 640.dp)
                                    .fillMaxHeight()
                            ) {
                                when (currentDestination) {
                                    AppDestination.HOME -> HomeScreen(
                                        governorate = currentGovernorate,
                                        schedule = schedule,
                                        countdown = countdown,
                                        hijriOffsetDays = settings.hijriOffsetDays,
                                        announcements = activeAnnouncements,
                                        hadiths = hadiths,
                                        duas = duasAndZiyarat,
                                        occasions = occasions,
                                        lastSurahNumber = settings.lastSurahNumber,
                                        lastSurahName = settings.lastSurahName,
                                        lastAyahNumber = settings.lastAyahNumber,
                                        homeCards = settings.homeCards,
                                        hiddenHomeCards = settings.hiddenHomeCards,
                                        prayerCompletionRecords = todayPrayerCompletions,
                                        onSetPrayerCompletion = vm::setPrayerCompleted,
                                        dailyGoals = settings.dailyGoals,
                                        tasbihCount = settings.dailyTasbihCount,
                                        tasbihGoal = settings.dailyTasbihGoal,
                                        timeFormat = settings.timeFormat,
                                        prayerCalculationSummary = prayerCalculationSummary,
                                        onOpenGovernorateSheet = { manualGovSheetOpen = true },
                                        onNavigate = { currentDestination = it },
                                        onOpenSurah = { sNum ->
                                            vm.loadSurah(sNum, recordHistory = true)
                                            openedSurahInReader = sNum
                                            currentDestination = AppDestination.QURAN
                                        },
                                        onSaveFavorite = vm::toggleFavorite,
                                        onSecretDeveloperTrigger = openHiddenDeveloperPanel
                                    )

                                    AppDestination.QURAN -> QuranIndexAndReaderScreen(
                                        activeSurahNumber = openedSurahInReader,
                                        activeSurahAyahs = activeSurahAyahs,
                                        isLoadingSurah = isLoadingSurah,
                                        surahError = surahError,
                                        lastSurahNumber = settings.lastSurahNumber,
                                        lastSurahName = settings.lastSurahName,
                                        lastAyahNumber = settings.lastAyahNumber,
                                        selectedReciterId = settings.selectedReciterId,
                                        darkMode = settings.darkMode,
                                        readingFontChoice = settings.readingFontChoice,
                                        quranBackground = settings.quranBackground,
                                        autoScrollEnabled = settings.quranAutoScrollEnabled,
                                        autoScrollSpeed = settings.quranAutoScrollSpeed,
                                        onSelectSurahToRead = { sNum ->
                                            vm.loadSurah(sNum, recordHistory = true)
                                            openedSurahInReader = sNum
                                        },
                                        onCloseReaderToIndex = { openedSurahInReader = null },
                                        onPlaySurahAudio = vm::playSurahAudio,
                                        onSelectReciter = vm::setReciter,
                                        onChangeFontScale = vm::setReadingFontScale,
                                        onToggleDarkMode = vm::setDarkMode,
                                        onChangeReadingFont = vm::setReadingFontChoice,
                                        onChangeBackground = vm::setQuranBackground,
                                        onChangeAutoScroll = vm::setQuranAutoScroll,
                                        onSaveLastReadAyah = vm::saveLastReadAyah,
                                        onSaveFavorite = vm::toggleFavorite
                                    )

                                    AppDestination.LIBRARY -> LibraryAndBookReaderScreen(
                                        books = books,
                                        onUpdateProgress = vm::updateBookReadingProgress,
                                        onChangeFontScale = vm::setReadingFontScale,
                                        onSaveFavorite = vm::toggleFavorite
                                    )

                                    AppDestination.DUAS_ZIYARAT -> DuasZiyaratAdhkarScreen(
                                        duasAndZiyarat = duasAndZiyarat,
                                        adhkar = adhkar,
                                        hapticEnabled = settings.hapticTasbihEnabled,
                                        dhikrCounters = dhikrCounters,
                                        onChangeFontScale = vm::setReadingFontScale,
                                        onIncrementDhikr = vm::incrementDhikrCounter,
                                        onResetDhikr = vm::resetDhikrCounter,
                                        onSaveFavorite = vm::toggleFavorite
                                    )

                                    AppDestination.MORE_HUB -> MoreHubScreen(
                                        onNavigate = { currentDestination = it }
                                    )

                                    AppDestination.PRAYER_TIMES -> PrayerTimesScreen(
                                        governorate = currentGovernorate,
                                        schedule = schedule,
                                        countdown = countdown,
                                        monthlySchedules = monthlySchedules,
                                        calcMethodCode = settings.prayerCalcMethodCode,
                                        offsets = settings.prayerOffsets,
                                        notifyPrayers = settings.notifyPrayers,
                                        coordinatesLatitude = settings.customLatitude ?: currentGovernorate.latitude,
                                        coordinatesLongitude = settings.customLongitude ?: currentGovernorate.longitude,
                                        isUsingDeviceLocation = settings.customLatitude != null && settings.customLongitude != null,
                                        timeFormat = settings.timeFormat,
                                        prayerAlertProfile = settings.prayerAlertProfile,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onOpenGovernorateSheet = { manualGovSheetOpen = true },
                                        onSelectMethod = vm::setPrayerCalcMethod,
                                        onUpdateOffsets = vm::updatePrayerOffsets,
                                        onToggleNotifyPrayers = { vm.updateNotificationPrefs(notifyPrayers = it) },
                                        onChangeTimeFormat = vm::setTimeFormat,
                                        onChangePrayerAlert = vm::setPrayerAlert,
                                        onChangeQuietHours = vm::setQuietHours,
                                        onSetPrayerCoordinates = vm::setPrayerCoordinates,
                                        onClearPrayerCoordinates = vm::clearPrayerCoordinates,
                                        onRefreshPrayerSchedule = { vm.refreshPrayerTimes() }
                                    )

                                    AppDestination.TASBIH -> TasbihScreen(
                                        hapticEnabled = settings.hapticTasbihEnabled,
                                        dailyGoal = settings.dailyTasbihGoal,
                                        dailyCount = settings.dailyTasbihCount,
                                        sessions = tasbihSessions,
                                        savedPresetId = settings.tasbihPresetId,
                                        savedStageIndex = settings.tasbihStageIndex,
                                        savedStageCount = settings.tasbihStageCount,
                                        savedTotalCount = settings.tasbihTotalCount,
                                        savedCustomTarget = settings.tasbihCustomTarget,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onToggleHaptic = vm::setHapticTasbih,
                                        onChangeDailyGoal = vm::setDailyTasbihGoal,
                                        onTickDailyCount = vm::recordTasbihTick,
                                        onSaveSession = vm::saveTasbihSession,
                                        onSaveScreenState = vm::saveTasbihScreenState
                                    )

                                    AppDestination.QIBLA -> QiblaScreen(
                                        governorate = currentGovernorate,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onDetectedCoordinates = vm::selectNearestGovernorateFromCoordinates
                                    )

                                    AppDestination.AUDIO_LIBRARY -> AudioLibraryScreen(
                                        tracks = audioTracks,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onPlayTrack = vm::playLibraryAudioTrack,
                                        onSaveFavorite = vm::toggleFavorite
                                    )

                                    AppDestination.HADITH -> HadithEncyclopediaScreen(
                                        hadiths = hadiths,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onSaveFavorite = vm::toggleFavorite
                                    )

                                    AppDestination.CURRICULA -> CurriculaScreen(
                                        courses = courses,
                                        lessonProgress = lessonProgress,
                                        quizAttempts = quizAttempts,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onMarkLessonCompleted = vm::markLessonCompleted,
                                        onSaveQuizAttempt = vm::saveQuizAttempt,
                                        onOpenAiScholar = { currentDestination = AppDestination.SEARCH }
                                    )

                                    AppDestination.ETHICS -> CurriculaScreen(
                                        courses = courses,
                                        lessonProgress = lessonProgress,
                                        quizAttempts = quizAttempts,
                                        initialCategory = "الأخلاقيات",
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onMarkLessonCompleted = vm::markLessonCompleted,
                                        onSaveQuizAttempt = vm::saveQuizAttempt,
                                        onOpenAiScholar = { currentDestination = AppDestination.SEARCH }
                                    )

                                    AppDestination.CALENDAR_OCCASIONS -> CalendarOccasionsScreen(
                                        schedule = schedule,
                                        occasions = occasions,
                                        hijriOffsetDays = settings.hijriOffsetDays,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onChangeHijriOffset = vm::setHijriOffsetDays
                                    )

                                    AppDestination.WALLPAPERS -> WallpapersScreen(
                                        wallpapers = wallpapers,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onSaveFavorite = vm::toggleFavorite
                                    )

                                    AppDestination.SEARCH -> UnifiedSearchAndScholarScreen(
                                        searchResults = searchResults,
                                        aiScholarState = aiScholarState,
                                        hijriOffsetDays = settings.hijriOffsetDays,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onExecuteSearch = vm::executeUnifiedSearch,
                                        onAskAiScholar = vm::askDeepScholarAi,
                                        onOpenSurah = { sNum ->
                                            vm.loadSurah(sNum, recordHistory = true)
                                            openedSurahInReader = sNum
                                            currentDestination = AppDestination.QURAN
                                        }
                                    )

                                    AppDestination.FAVORITES -> FavoritesAndHistoryScreen(
                                        favorites = favorites,
                                        history = readingHistory,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onRemoveFavorite = { fav ->
                                            vm.toggleFavorite(
                                                fav.itemType,
                                                fav.itemRefKey,
                                                fav.title,
                                                fav.subtitle,
                                                fav.contentSnippet
                                            )
                                        }
                                    )

                                    AppDestination.SETTINGS -> SettingsScreen(
                                        settings = settings,
                                        sources = contentSources,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onOpenGovernorateSheet = { manualGovSheetOpen = true },
                                        onChangeThemeMode = vm::setThemeMode,
                                        onChangeAccent = vm::setAccentChoice,
                                        onChangeCardDensity = vm::setCardDensity,
                                        onChangeUiFont = vm::setFontChoice,
                                        onChangeUiFontScale = vm::setUiFontScale,
                                        onChangeReadingFont = vm::setReadingFontChoice,
                                        onChangeReadingFontScale = vm::setReadingFontScale,
                                        onChangeLineSpacing = vm::setLineSpacing,
                                        onChangeHomeLayout = vm::setHomeLayout,
                                        onChangeDailyGoals = vm::setDailyGoals,
                                        onChangeTimeFormat = vm::setTimeFormat,
                                        onChangeCalcMethod = vm::setPrayerCalcMethod,
                                        onChangePrayerAlert = vm::setPrayerAlert,
                                        onChangeQuietHours = vm::setQuietHours,
                                        onChangeQuranBackground = vm::setQuranBackground,
                                        onChangeAutoScroll = vm::setQuranAutoScroll,
                                        onChangeReciter = vm::setReciter,
                                        onChangePlaybackSpeed = vm::setPlaybackSpeed,
                                        onToggleHaptic = vm::setHapticTasbih,
                                        onChangeDailyTasbihGoal = vm::setDailyTasbihGoal,
                                        onUpdateNotifications = vm::updateNotificationPrefs,
                                        onClearQuranCache = vm::clearCachedQuranTexts,
                                        onResetDefaults = vm::resetAllSettings,
                                        onDeleteUserData = vm::deleteAllUserData
                                    )

                                    AppDestination.ADMIN -> HiddenDeveloperPanelScreen(
                                        authorizedUser = developerAuthUser,
                                        announcements = allAnnouncements,
                                        hadiths = hadiths,
                                        duasAndZiyarat = duasAndZiyarat,
                                        adhkar = adhkar,
                                        books = books,
                                        audioTracks = audioTracks,
                                        occasions = occasions,
                                        wallpapers = wallpapers,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onAddAnnouncement = vm::developerAddAnnouncement,
                                        onToggleAnnouncementActive = vm::developerToggleAnnouncementActive,
                                        onDeleteAnnouncement = vm::developerDeleteAnnouncement,
                                        onAddHadith = vm::adminAddHadith,
                                        onDeleteHadith = vm::adminDeleteHadith,
                                        onAddDuaZiyarat = vm::adminAddDuaOrZiyarat,
                                        onDeleteDuaZiyarat = vm::adminDeleteDuaOrZiyarat,
                                        onAddDhikr = vm::adminAddDhikr,
                                        onDeleteDhikr = vm::adminDeleteDhikr,
                                        onAddBook = vm::adminAddBook,
                                        onDeleteBook = vm::adminDeleteBook,
                                        onAddAudio = vm::adminAddAudioTrack,
                                        onDeleteAudio = vm::adminDeleteAudioTrack,
                                        onAddOccasion = vm::adminAddOccasion,
                                        onDeleteOccasion = vm::adminDeleteOccasion,
                                        onAddWallpaper = vm::adminAddWallpaper,
                                        onDeleteWallpaper = vm::adminDeleteWallpaper
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Non-blocking first-launch Iraqi Governorate selector sheet
        if (!settings.hasSelectedGovernorateOnce || manualGovSheetOpen) {
            GovernorateSelectorSheet(
                currentGovernorateId = settings.governorateId,
                hapticEnabled = settings.hapticTasbihEnabled,
                notifyPrayersEnabled = settings.notifyPrayers,
                onSelectGovernorate = { govId ->
                    vm.selectGovernorate(govId)
                    manualGovSheetOpen = false
                },
                onDetectedCoordinates = vm::selectNearestGovernorateFromCoordinates,
                onToggleHaptic = vm::setHapticTasbih,
                onToggleNotifyPrayers = { vm.updateNotificationPrefs(notifyPrayers = it) },
                onDismiss = {
                    vm.dismissGovernoratePrompt()
                    manualGovSheetOpen = false
                }
            )
        }
        }
    )
}


/** Lightweight frosted capsule navigation. It intentionally avoids real-time blur so older
 * phones keep stable frame times; translucency, layered fill and a fine rim provide the glass look. */
@Composable
private fun GlassBottomNavigationBar(
    items: List<PrimaryNavSpec>,
    currentDestination: AppDestination,
    containerColor: Color,
    borderColor: Color,
    onSelect: (AppDestination) -> Unit
) {
    Surface(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(32.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Black.copy(alpha = 0.025f))
                    )
                )
                .padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { spec ->
                val selected = currentDestination == spec.destination
                val selectedFill = if (selected) {
                    if (LocalIsDarkThemeActive.current) Color.White.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else Color.Transparent
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(selectedFill)
                        .clickable { onSelect(spec.destination) }
                        .testTag(spec.testTag)
                        .padding(horizontal = 2.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                        contentDescription = spec.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(23.dp)
                    )
                    Text(
                        text = spec.label,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold
                        else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                }
            }
        }
    }
}
