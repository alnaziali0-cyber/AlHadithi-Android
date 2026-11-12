package com.example.ui.screens

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.local.DailyGoal
import com.example.data.local.HomeCard
import com.example.domain.DailyPrayerSchedule
import com.example.domain.HijriDateValue
import com.example.domain.NextPrayerCountdown
import com.example.domain.TimeFormat
import com.example.ui.theme.AlHadithiTheme
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** UI checks for the Arabic dashboard hierarchy and the daily five-prayer tracker. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class HomeScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val schedule = DailyPrayerSchedule(
        gregorianDateLabel = "الأحد، 15 شباط 2026 م",
        hijriDate = HijriDateValue(1447, 8, 26),
        fajrMinutes = 5 * 60 + 12,
        sunriseMinutes = 6 * 60 + 40,
        dhuhrMinutes = 12 * 60 + 5,
        asrMinutes = 15 * 60 + 20,
        maghribMinutes = 18 * 60 + 10,
        ishaMinutes = 19 * 60 + 35,
        midnightMinutes = 0,
        sourceLabel = "حساب فلكي جعفري محلي"
    )

    private val countdown = NextPrayerCountdown(
        prayerArabicName = "الظهر",
        prayerFormattedTime = "12:05 م",
        countdownHhMmSs = "02:15:00",
        remainingSeconds = 8100
    )

    private fun setContent(
        homeCards: List<HomeCard> = HomeCard.defaultOrder,
        hiddenHomeCards: Set<HomeCard> = emptySet(),
        dailyGoals: Set<DailyGoal> = emptySet()
    ) {
        composeRule.setContent {
            AlHadithiTheme(themeMode = ThemeMode.LIGHT, darkTheme = false) {
                HomeScreen(
                    governorate = com.example.domain.IraqGovernorates.byId("baghdad"),
                    schedule = schedule,
                    countdown = countdown,
                    hijriOffsetDays = 0,
                    announcements = emptyList(),
                    hadiths = emptyList(),
                    duas = emptyList(),
                    occasions = emptyList(),
                    lastSurahNumber = 1,
                    lastSurahName = "سورة الفاتحة",
                    lastAyahNumber = 1,
                    homeCards = homeCards,
                    hiddenHomeCards = hiddenHomeCards,
                    prayerCompletionRecords = emptyList(),
                    onSetPrayerCompletion = { _, _ -> },
                    dailyGoals = dailyGoals,
                    tasbihCount = 5,
                    tasbihGoal = 100,
                    timeFormat = TimeFormat.TWENTY_FOUR_HOUR,
                    prayerCalculationSummary = "الجعفرية · توقيت بغداد (UTC+3)",
                    verification = com.example.domain.PrayerTimetableVerifier.verify(schedule),
                    onOpenGovernorateSheet = {},
                    onNavigate = {},
                    onOpenSurah = {},
                    onSaveFavorite = { _, _, _, _, _ -> },
                    onSecretDeveloperTrigger = {}
                )
            }
        }
    }

    @Test
    fun `the layout direction is right to left`() {
        var direction: LayoutDirection? = null
        composeRule.setContent {
            AlHadithiTheme(themeMode = ThemeMode.LIGHT, darkTheme = false) {
                direction = LocalLayoutDirection.current
            }
        }
        composeRule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun `the dashboard renders the list and the quick access grid`() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_screen_list").assertIsDisplayed()
        composeRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("quick_quran"))
        composeRule.onNodeWithTag("quick_quran").assertIsDisplayed()
    }

    @Test
    fun `the primary sections include next prayer daily tracker and quick access`() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("الصلاة القادمة · بغداد").assertExists()
        composeRule.onNodeWithText("متابعة الصلوات").assertExists()
        composeRule.onNodeWithText("0 / 5 منجزة").assertExists()
        composeRule.onNodeWithText("مواقيت الصلاة").assertExists()
        composeRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("quick_ethics"))
        composeRule.onNodeWithTag("quick_quran").assertExists()
        composeRule.onNodeWithTag("quick_ethics").assertExists()
    }

    @Test
    fun `daily hadith and dua are not displayed as home cards`() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("حديث اليوم").assertDoesNotExist()
        composeRule.onNodeWithText("دعاء اليوم").assertDoesNotExist()
    }

}
