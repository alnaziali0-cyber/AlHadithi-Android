package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
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

/**
 * UI level checks for the daily dashboard: the Arabic right-to-left layout, the empty states and
 * the optional "daily follow-up" card that only appears when the user picks a goal.
 */
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
                    hiddenHomeCards = hiddenHomeCards,
                    dailyGoals = dailyGoals,
                    tasbihCount = 5,
                    tasbihGoal = 100,
                    timeFormat = TimeFormat.TWENTY_FOUR_HOUR,
                    prayerCalculationSummary = "الجعفرية · توقيت بغداد (UTC+3)",
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
    fun `the daily tracking card stays hidden until the user picks a goal`() {
        setContent(dailyGoals = emptySet())
        composeRule.waitForIdle()

        composeRule.onNodeWithText("متابعة اليوم").assertDoesNotExist()
    }

    @Test
    fun `the daily tracking card appears once a goal is selected`() {
        setContent(dailyGoals = setOf(DailyGoal.TASBIH))
        composeRule.waitForIdle()

        // The card may sit below the composed viewport of the lazy list, so scroll to it first.
        composeRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasText("متابعة اليوم"))
        composeRule.onNodeWithText("متابعة اليوم").assertExists()
        composeRule.onNodeWithText("التسبيح").assertExists()
    }

    @Test
    fun `a hidden card is not rendered`() {
        setContent(hiddenHomeCards = setOf(HomeCard.QUICK_ACCESS))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_quran").assertDoesNotExist()
    }
}
