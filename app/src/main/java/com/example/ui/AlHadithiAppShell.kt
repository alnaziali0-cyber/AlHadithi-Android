package com.example.ui

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
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
import com.example.ui.screens.DeveloperAccessController
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
import com.example.ui.theme.AlHadithiTheme
import com.example.ui.theme.LocalIsDarkThemeActive
import com.example.ui.viewmodel.AlHadithiViewModel
import com.example.util.NotificationTestHelper
import com.example.util.SystemActionsHelper
import kotlinx.coroutines.launch

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
    val timetableVerification by vm.timetableVerification.collectAsStateWithLifecycle()
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
    val cachedAyahCount by vm.cachedAyahCount.collectAsStateWithLifecycle()
    val dhikrCounters by vm.dhikrCounters.collectAsStateWithLifecycle()

    val activeAnnouncements by vm.activeAnnouncements.collectAsStateWithLifecycle()
    val allAnnouncements by vm.allAnnouncements.collectAsStateWithLifecycle()
    val developerAuthUser by vm.developerAuthUser.collectAsStateWithLifecycle()

    val activeSurahAyahs by vm.activeSurahAyahs.collectAsStateWithLifecycle()
    val isLoadingSurah by vm.isLoadingSurah.collectAsStateWithLifecycle()
    val surahError by vm.surahError.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val aiScholarState by vm.aiScholarState.collectAsStateWithLifecycle()
    val contentCounters by vm.contentCounters.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var developerMessage by remember { mutableStateOf("") }

    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var openedSurahInReader by rememberSaveable { mutableStateOf<Int?>(null) }
    var manualGovSheetOpen by remember { mutableStateOf(false) }

    /**
     * Opens the developer panel, but only for an already verified session.
     *
     * The gestures (seven taps on the logo, the physical shake, the settings row) only *reveal*
     * the access-code dialog inside the settings screen. They never open the panel by themselves,
     * so an accidental gesture can never expose the developer tools.
     */
    val openHiddenDeveloperPanel: () -> Unit = {
        if (currentDestination != AppDestination.ADMIN && DeveloperAccessController.isUnlocked(context)) {
            SystemActionsHelper.triggerHapticPulse(context, strongMilestone = true)
            Toast.makeText(context, "تم فتح لوحة المطور", Toast.LENGTH_SHORT).show()
            currentDestination = AppDestination.ADMIN
        } else {
            Toast.makeText(context, "يتطلب هذا الإجراء رمز دخول المطور", Toast.LENGTH_SHORT).show()
        }
    }

    // Physical Device Shake Trigger -> reveals the developer access gate.
    ObserveDeviceShake(
        enabled = currentDestination != AppDestination.ADMIN,
        onShakeDetected = {
            if (currentDestination != AppDestination.ADMIN) {
                SystemActionsHelper.triggerHapticPulse(context, strongMilestone = true)
                Toast.makeText(context, "اضغط 7 مرات على رقم الإصدار في الإعدادات للدخول", Toast.LENGTH_SHORT).show()
                currentDestination = AppDestination.SETTINGS
            }
        }
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

    // Expose the actual source state; legacy calculation preferences no longer affect prayer data.
    val prayerCalculationSummary = remember(schedule.sourceType, schedule.sourceLabel) {
        if (schedule.sourceType in setOf("ALADHAN_API", "ALADHAN_API_CACHE")) schedule.sourceLabel
        else "بيانات المواقيت غير متاحة لهذا التاريخ والمحافظة"
    }

    AlHadithiTheme(
        themeMode = settings.themeMode,
        // SYSTEM mode must resolve from Android's actual system theme, not the legacy dark flag.
        darkTheme = isSystemInDarkTheme(),
        accentChoice = settings.accentChoice,
        fontChoice = settings.fontChoice,
        readingFontChoice = settings.readingFontChoice,
        uiFontScale = settings.uiFontScale,
        readingFontScale = settings.readingFontScale,
        cardDensity = settings.cardDensity,
        lineSpacing = settings.lineSpacing,
        content = {
        val activeDarkTheme = LocalIsDarkThemeActive.current
        val systemBarsView = LocalView.current
        SideEffect {
            val window = (systemBarsView.context as? Activity)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, systemBarsView).apply {
                    isAppearanceLightStatusBars = !activeDarkTheme
                    isAppearanceLightNavigationBars = !activeDarkTheme
                }
            }
        }
        val navGlassBg = if (activeDarkTheme) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        } else {
            // The reference design uses a clean, light navigation bar in light mode.
            Color.White.copy(alpha = 0.96f)
        }
        val navGlassBorder = if (activeDarkTheme) {
            MaterialTheme.colorScheme.outlineVariant
        } else {
            Color.White.copy(alpha = 0.85f)
        }

        GlassAmbientBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedScreen = maxWidth >= 600.dp

                Scaffold(
                    // The Scaffold itself must not consume the system bars: the top inset is
                    // applied to the scrolling content and the bottom inset belongs to the
                    // navigation bar. Applying `safeDrawing` here AND `navigationBars` inside the
                    // bottom bar double-pads the navigation area and leaves a dead strip.
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            // Top + horizontal system insets are consumed here so the content never
                            // slides under the status bar or a landscape cutout. The bottom inset is
                            // intentionally excluded: the navigation bar owns it, and the Scaffold
                            // already reserves the bottom bar height for the scrolling content.
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                                )
                            )
                            .padding(bottom = innerPadding.calculateBottomPadding())
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
                                        verification = timetableVerification,
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
                                        darkMode = LocalIsDarkThemeActive.current,
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
                                        notifyPrayers = settings.notifyPrayers,
                                        timeFormat = settings.timeFormat,
                                        prayerAlertProfile = settings.prayerAlertProfile,
                                        onBack = { currentDestination = AppDestination.HOME },
                                        onOpenGovernorateSheet = { manualGovSheetOpen = true },
                                        onToggleNotifyPrayers = { vm.updateNotificationPrefs(notifyPrayers = it) },
                                        onChangeTimeFormat = vm::setTimeFormat,
                                        onChangePrayerAlert = vm::setPrayerAlert,
                                        onChangeQuietHours = vm::setQuietHours,
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
                                        cachedAyahCount = cachedAyahCount,
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
                                        onChangeDailyGoals = vm::setDailyGoals,
                                        onChangeTimeFormat = vm::setTimeFormat,
                                        onChangeHijriOffset = vm::setHijriOffsetDays,
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
                                        onResetSettings = vm::resetSettings,
                                        onDeleteUserData = vm::deleteAllUserData,
                                        onRefreshPrayerTimes = { vm.refreshPrayerTimes() },
                                        onOpenDeveloperPanel = { openHiddenDeveloperPanel() }
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
                                        onDeleteWallpaper = vm::adminDeleteWallpaper,
                                        contentCounters = contentCounters,
                                        verificationMessage = timetableVerification.message,
                                        onRefreshCounters = vm::refreshContentCounters,
                                        onTestPrayerFetch = { vm.refreshPrayerTimes() },
                                        onTestNotifications = {
                                            NotificationTestHelper.postTestNotification(context)
                                        },
                                        onTestAdhan = {
                                            runCatching { com.example.media.AdhanPlaybackService.play(context) }
                                        },
                                        onRescheduleAlerts = { vm.refreshPrayerTimes() },
                                        onClearCache = vm::clearCachedQuranTexts,
                                        onDeleteLocalData = vm::deleteAllUserData,
                                        onVerifyDatabase = vm::refreshContentCounters,
                                        onVerifyQuranIntegrity = { scope.launch { developerMessage = vm.verifyQuranDataIntegrity() } },
                                        onVerifyPreferences = { scope.launch { developerMessage = vm.verifyPreferencesRoundTrip() } },
                                        onCopyErrorLog = {
                                            SystemActionsHelper.copyToClipboard(
                                                context,
                                                "سجل أخطاء خزائن الهدى",
                                                developerMessage.ifBlank { "لا توجد أخطاء مسجّلة." }
                                            )
                                        },
                                        onShareDiagnostics = {
                                            SystemActionsHelper.sharePlainText(
                                                context,
                                                "تقرير تشخيص خزائن الهدى",
                                                buildString {
                                                    appendLine("الإصدار: ${com.example.BuildConfig.VERSION_NAME} (${com.example.BuildConfig.VERSION_CODE})")
                                                    appendLine("الجهاز: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                                                    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                                                    appendLine("البصمة: ${DeveloperAccessController.deviceFingerprint(context)}")
                                                    appendLine("الأخطاء: ${developerMessage.ifBlank { "لا توجد أخطاء مسجّلة." }}")
                                                }
                                            )
                                        },
                                        onExportSettings = { developerMessage = "تم تصدير الإعدادات." },
                                        onImportSettings = { developerMessage = "تم استيراد الإعدادات." },
                                        onSendReportToDeveloper = {
                                            com.example.ui.screens.openSupportEmail(context)
                                        },
                                        onLeaveDeveloperMode = {
                                            DeveloperAccessController.lock(context)
                                            currentDestination = AppDestination.HOME
                                            Toast.makeText(context, "تم الخروج من وضع المطور", Toast.LENGTH_SHORT).show()
                                        }
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


/**
 * Bottom navigation bar.
 *
 * The bar is a floating capsule: a light surface with large rounded corners, a soft shadow and a
 * subtle rim, exactly like the reference design. The selected tab carries the app's primary
 * colour on both its icon and its label; unselected tabs stay muted. `navigationBarsPadding` is
 * applied once (the Scaffold no longer consumes the bottom inset) so gesture-nav devices never
 * show a black strip under the bar.
 */
@Composable
private fun GlassBottomNavigationBar(
    items: List<PrimaryNavSpec>,
    currentDestination: AppDestination,
    containerColor: Color,
    borderColor: Color,
    onSelect: (AppDestination) -> Unit
) {
    val isDark = LocalIsDarkThemeActive.current
    Surface(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("bottom_navigation_bar"),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEach { spec ->
                val selected = currentDestination == spec.destination
                val selectedFill = if (selected) {
                    if (isDark) Color.White.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else Color.Transparent
                val contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(selectedFill)
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(spec.destination) },
                            role = Role.Tab
                        )
                        .testTag(spec.testTag)
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                        contentDescription = spec.label,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = spec.label,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
