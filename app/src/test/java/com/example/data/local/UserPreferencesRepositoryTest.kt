package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerKey
import com.example.domain.QuietHours
import com.example.domain.TimeFormat
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.CardDensity
import com.example.ui.theme.LineSpacingOption
import com.example.ui.theme.QuranBackgroundStyle
import com.example.ui.theme.ReadingFontChoice
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ensures that every preference written by the settings screen is really persisted and read
 * back by the rest of the app, and that resetting the settings never leaks the admin PIN.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesRepositoryTest {

    private fun repository(): UserPreferencesRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return UserPreferencesRepository(context)
    }

    @Test
    fun appearancePreferences_arePersistedAndReadBack() = runBlocking {
        val repo = repository()

        repo.setThemeMode(ThemeMode.DARK)
        repo.setAccentChoice(AccentChoice.GOLD)
        repo.setCardDensity(CardDensity.COMPACT)
        repo.setLineSpacing(LineSpacingOption.GENEROUS)
        repo.setUiFontScale(1.25f)
        repo.setReadingFontChoice(ReadingFontChoice.TAJAWAL)
        repo.setReadingFontScale(1.35f)

        val state = repo.settingsFlow.first()
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(AccentChoice.GOLD, state.accentChoice)
        assertEquals(CardDensity.COMPACT, state.cardDensity)
        assertEquals(LineSpacingOption.GENEROUS, state.lineSpacing)
        assertEquals(1.25f, state.uiFontScale, 0.001f)
        assertEquals(ReadingFontChoice.TAJAWAL, state.readingFontChoice)
        assertEquals(1.35f, state.readingFontScale, 0.001f)
    }

    @Test
    fun `the scale preferences are clamped to the supported range`() = runBlocking {
        val repo = repository()

        repo.setUiFontScale(9.0f)
        repo.setReadingFontScale(0.1f)

        val state = repo.settingsFlow.first()
        assertTrue(state.uiFontScale <= 1.45f)
        assertTrue(state.readingFontScale >= 0.85f)
    }

    @Test
    fun `the theme mode falls back to the legacy switch when nothing was chosen`() {
        // A legacy dark flag without an explicit mode means "dark" for the old builds.
        assertEquals(ThemeMode.DARK, resolveThemeMode(null, legacyDarkMode = true))
        assertEquals(ThemeMode.SYSTEM, resolveThemeMode(null, legacyDarkMode = false))
        // An explicit mode always wins, even over the legacy switch.
        assertEquals(ThemeMode.LIGHT, resolveThemeMode("light", legacyDarkMode = true))
        assertEquals(ThemeMode.SYSTEM, resolveThemeMode("system", legacyDarkMode = true))
        // An unknown stored key is ignored and the legacy switch decides.
        assertEquals(ThemeMode.DARK, resolveThemeMode("not-a-mode", legacyDarkMode = true))
    }

    @Test
    fun `an explicit theme mode is stored and applied`() = runBlocking {
        val repo = repository()

        repo.setThemeMode(ThemeMode.LIGHT)
        var state = repo.settingsFlow.first()
        assertEquals(ThemeMode.LIGHT, state.themeMode)
        assertFalse("effective dark mode must follow the explicit light mode", state.effectiveDarkMode)

        repo.setThemeMode(ThemeMode.DARK)
        state = repo.settingsFlow.first()
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertTrue(state.effectiveDarkMode)
    }

    @Test
    fun `the quran reader preferences are persisted`() = runBlocking {
        val repo = repository()

        repo.setQuranBackground(QuranBackgroundStyle.SEPIA)
        repo.setQuranAutoScroll(true, 1.5f)

        val state = repo.settingsFlow.first()
        assertEquals(QuranBackgroundStyle.SEPIA, state.quranBackground)
        assertTrue(state.quranAutoScrollEnabled)
        assertEquals(1.5f, state.quranAutoScrollSpeed, 0.001f)
    }

    @Test
    fun `the per prayer reminders and the quiet window are persisted`() = runBlocking {
        val repo = repository()

        repo.setPrayerAlert(
            PrayerKey.ASR,
            PrayerAlertSettings(enabled = false, leadMinutes = 20, soundEnabled = false, vibrateEnabled = true)
        )
        repo.setQuietHours(QuietHours())
        repo.setQuietHours(QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 5 * 60))

        val state = repo.settingsFlow.first()
        val asr = state.prayerAlertProfile.settingsFor(PrayerKey.ASR)
        assertFalse(asr.enabled)
        assertEquals(20, asr.leadMinutes)
        assertFalse(asr.soundEnabled)
        assertTrue(asr.vibrateEnabled)

        // Prayers that were never configured keep their defaults.
        assertTrue(state.prayerAlertProfile.settingsFor(PrayerKey.FAJR).enabled)

        assertTrue(state.prayerAlertProfile.quietHours.enabled)
        assertEquals(22 * 60, state.prayerAlertProfile.quietHours.startMinutes)
        assertEquals(5 * 60, state.prayerAlertProfile.quietHours.endMinutes)
    }

    @Test
    fun `the home layout and the daily goals are persisted`() = runBlocking {
        val repo = repository()

        repo.setHiddenHomeCards(setOf(HomeCard.PRAYER_HERO))
        repo.setDailyGoals(setOf(DailyGoal.TASBIH))

        val state = repo.settingsFlow.first()
        assertTrue(HomeCard.PRAYER_HERO in state.hiddenHomeCards)
        assertEquals(setOf(DailyGoal.TASBIH), state.dailyGoals)
    }

    @Test
    fun `the time format is persisted`() = runBlocking {
        val repo = repository()
        repo.setTimeFormat(TimeFormat.TWENTY_FOUR_HOUR)
        assertEquals(TimeFormat.TWENTY_FOUR_HOUR, repo.settingsFlow.first().timeFormat)
    }

    @Test
    fun `the tasbih counter restarts on a new day and is stored locally`() = runBlocking {
        val repo = repository()

        repo.incrementDailyTasbih("day-a", 1)
        repo.incrementDailyTasbih("day-a", 1)
        assertEquals(2, repo.settingsFlow.first().dailyTasbihCount)

        repo.incrementDailyTasbih("day-b", 1)
        assertEquals(1, repo.settingsFlow.first().dailyTasbihCount)
        assertEquals("day-b", repo.settingsFlow.first().dailyTasbihDayKey)
    }

    @Test
    fun `resetting the settings restores the defaults and keeps the admin pin`() = runBlocking {
        val repo = repository()
        repo.setOrVerifyAdminPin("1234", currentHash = "")
        repo.setThemeMode(ThemeMode.DARK)
        repo.setHiddenHomeCards(setOf(HomeCard.PRAYER_HERO))

        repo.resetSettingsToDefaults()

        val state = repo.settingsFlow.first()
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertTrue(state.hiddenHomeCards.isEmpty())
        assertEquals(UserPreferencesRepository.sha256("1234"), state.adminPinSha256)
    }

    @Test
    fun `the font choice and the notification switches are persisted`() = runBlocking {
        val repo = repository()

        repo.setFontChoice(AppFontChoice.KUFI)
        repo.updateNotificationPrefs(notifyPrayers = true, notifyOccasions = false, notifyAdhkar = false)

        val state = repo.settingsFlow.first()
        assertEquals(AppFontChoice.KUFI, state.fontChoice)
        assertTrue(state.notifyPrayers)
        assertFalse(state.notifyOccasions)
        assertFalse(state.notifyAdhkar)
    }
}
