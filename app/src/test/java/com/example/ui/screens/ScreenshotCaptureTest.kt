package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AdminAnnouncementEntity
import com.example.data.local.IslamicOccasionEntity
import com.example.domain.DailyPrayerSchedule
import com.example.domain.HijriDateValue
import com.example.domain.NextPrayerCountdown
import com.example.domain.TimeFormat
import com.example.ui.theme.AlHadithiTheme
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * لقطات شاشة للتحقق البصري من الواجهات الجديدة:
 * لوحة الأقسام (بالبطاقات الباستيل) وبطاقة صلواتي اليوم.
 * تُحفظ الصور في build/screenshots لمعاينتها.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun save(name: String, tag: String) {
        // The ambient background animates forever, so the clock must not auto-advance.
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(1200)
        // Robolectric: draw the decor view straight into a bitmap (native graphics mode).
        val view = composeRule.activity.window.decorView
        val width = if (view.width > 0) view.width else 1080
        val height = if (view.height > 0) view.height else 2340
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        val out = File(dir, "$name.png")
        out.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue("Screenshot ${out.absolutePath} must exist", out.isFile && out.length() > 1000)
    }

    @Test
    fun captureMoreHub() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AlHadithiTheme(themeMode = ThemeMode.DARK, darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    MoreHubScreen(onNavigate = {})
                }
            }
        }
        save("more_hub", "more_hub_list")
    }

    @Test
    fun captureQuranReaderCards() {
        composeRule.mainClock.autoAdvance = false
        val ayahs = listOf(
            com.example.data.local.CachedAyahEntity(
                surahNumber = 1, surahNameArabic = "الفاتحة", ayahNumberInSurah = 1, globalAyahNumber = 1,
                uthmaniText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", juz = 1, hizbQuarter = 1
            ),
            com.example.data.local.CachedAyahEntity(
                surahNumber = 1, surahNameArabic = "الفاتحة", ayahNumberInSurah = 2, globalAyahNumber = 2,
                uthmaniText = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", juz = 1, hizbQuarter = 1
            ),
            com.example.data.local.CachedAyahEntity(
                surahNumber = 1, surahNameArabic = "الفاتحة", ayahNumberInSurah = 3, globalAyahNumber = 3,
                uthmaniText = "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", juz = 1, hizbQuarter = 1
            )
        )
        composeRule.setContent {
            AlHadithiTheme(
                themeMode = ThemeMode.LIGHT,
                darkTheme = false,
                readingFontChoice = com.example.ui.theme.ReadingFontChoice.UTHMANI,
                readingFontScale = 1.25f
            ) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    QuranIndexAndReaderScreen(
                        activeSurahNumber = 1,
                        activeSurahAyahs = ayahs,
                        isLoadingSurah = false,
                        surahError = null,
                        lastSurahNumber = 1,
                        lastSurahName = "الفاتحة",
                        lastAyahNumber = 1,
                        selectedReciterId = "afasy",
                        darkMode = false,
                        readingFontChoice = com.example.ui.theme.ReadingFontChoice.UTHMANI,
                        quranBackground = com.example.ui.theme.QuranBackgroundStyle.PAPER,
                        readingLayout = com.example.ui.theme.QuranReadingLayout.CARDS,
                        autoScrollEnabled = false,
                        autoScrollSpeed = 1f,
                        onSelectSurahToRead = {},
                        onCloseReaderToIndex = {},
                        onPlaySurahAudio = {},
                        onSelectReciter = {},
                        onChangeFontScale = {},
                        onToggleDarkMode = {},
                        onChangeReadingFont = {},
                        onChangeBackground = {},
                        onChangeReadingLayout = {},
                        onChangeAutoScroll = { _, _ -> },
                        onSaveLastReadAyah = { _, _ -> },
                        onSaveFavorite = { _, _, _, _, _ -> }
                    )
                }
            }
        }
        save("quran_reader_cards", "quran_surah_continuous_page")
    }

    @Test
    fun captureHomeWithTracker() {
        composeRule.mainClock.autoAdvance = false
        val schedule = DailyPrayerSchedule(
            gregorianDateLabel = "الأحد، 8 شباط 2026 م",
            hijriDate = HijriDateValue(1447, 8, 20),
            fajrMinutes = 5 * 60 + 12,
            sunriseMinutes = 6 * 60 + 40,
            dhuhrMinutes = 12 * 60 + 5,
            asrMinutes = 15 * 60 + 20,
            maghribMinutes = 18 * 60 + 10,
            ishaMinutes = 19 * 60 + 35,
            midnightMinutes = 0,
            sourceLabel = "حساب فلكي جعفري محلي"
        )
        val countdown = NextPrayerCountdown(
            prayerArabicName = "الظهر",
            prayerFormattedTime = "12:05 م",
            countdownHhMmSs = "02:15:00",
            remainingSeconds = 8100
        )
        val occasion = IslamicOccasionEntity(
            hijriMonth = 8, hijriDay = 20, eventType = "JOY",
            title = "ذكرى المولد النبوي الشريف",
            infallibleOrTopic = "المولد",
            description = "ذكرى مولد النبي محمد ﷺ"
        )
        composeRule.setContent {
            AlHadithiTheme(themeMode = ThemeMode.LIGHT, darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    HomeScreen(
                        governorate = com.example.domain.IraqGovernorates.byId("baghdad"),
                        schedule = schedule,
                        countdown = countdown,
                        hijriOffsetDays = 0,
                        announcements = emptyList(),
                        hadiths = emptyList(),
                        duas = emptyList(),
                        occasions = listOf(occasion),
                        lastSurahNumber = 1,
                        lastSurahName = "الفاتحة",
                        lastAyahNumber = 1,
                        homeCards = com.example.data.local.HomeCard.defaultOrder,
                        hiddenHomeCards = emptySet(),
                        dailyGoals = emptySet(),
                        tasbihCount = 12,
                        tasbihGoal = 33,
                        timeFormat = TimeFormat.TWENTY_FOUR_HOUR,
                        prayerCalculationSummary = "الجعفرية · توقيت بغداد (UTC+3)",
                        prayerStatesToday = listOf(
                            com.example.data.local.PrayerDailyStateEntity(
                                ownerKey = "guest", prayerDate = "2026-02-08", prayer = "fajr",
                                status = com.example.data.local.PRAYER_STATUS_DONE
                            )
                        ),
                        onOpenGovernorateSheet = {},
                        onNavigate = {},
                        onOpenSurah = {},
                        onSaveFavorite = { _, _, _, _, _ -> },
                        onTogglePrayerCompleted = {},
                        onSecretDeveloperTrigger = {}
                    )
                }
            }
        }
        save("home_with_tracker", "home_screen_list")
    }
}
