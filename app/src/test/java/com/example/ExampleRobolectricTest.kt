package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AdminAnnouncementEntity
import com.example.data.local.AlHadithiDatabase
import com.example.data.local.SeedDataPart1
import com.example.data.remote.AUTHORIZED_DEVELOPER_EMAIL
import com.example.data.remote.QuranNetworkRepository
import com.example.media.AudioPlayerController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun appName_andDeveloperEmailRestriction_matchAlHadithiSpec() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("الحديثي", appName)
        assertEquals("alnaziali0@gmail.com", AUTHORIZED_DEVELOPER_EMAIL)
    }

    @Test
    fun bundledQuran_isAcceptedOnlyWhenTheFullValidatedCorpusExists() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val map = QuranNetworkRepository.ensureBundledQuranLoaded(context)
        assertEquals("The bundled Tanzil Simple corpus must contain all 114 surahs", 114, map.size)
        assertEquals("The bundled corpus must contain 6,236 ayahs", 6_236, map.values.sumOf { it.size })
        assertEquals("The first ayah must match the supplied Tanzil Simple edition", 
            "بِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ", map.getValue(1).first().uthmaniText)
        assertTrue("Sajdah metadata must be applied to Al-A'raf 206",
            map.getValue(7).first { it.ayahNumberInSurah == 206 }.hasSajdah)
        assertTrue("Tawbah starts without a basmala in this source edition",
            !map.getValue(9).first().uthmaniText.startsWith("بِسْمِ"))
        assertTrue("Tanzil quarter marker must remain in its original ayah text",
            map.getValue(2).first { it.ayahNumberInSurah == 26 }.uthmaniText.startsWith("۞"))
    }

    @Test
    fun roomSeedDatabase_populatesHusseiniOfflineAudio_Encyclopedia_AndAnnouncements() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AlHadithiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.dao()
            // Simulate a prior interrupted setup: the hadith table already has data,
            // while all other content tables are still empty.
            dao.insertHadiths(SeedDataPart1.authenticHadiths())
            AlHadithiDatabase.ensureSeeded(dao)

            assertTrue(dao.countCachedAyahs() > 0)
            assertTrue(dao.countHadiths() >= 10)
            assertTrue(dao.observeAllDuasAndZiyarat().first().isNotEmpty())
            assertTrue(dao.observeAllAdhkar().first().isNotEmpty())
            assertTrue(dao.observeAllBooks().first().isNotEmpty())
            assertTrue(dao.observeAllCourses().first().isNotEmpty())
            assertTrue(dao.observeAllWallpapers().first().size >= 3)

            // Resource names alone do not make an audio track available offline.
            val audioTracks = dao.observeAllAudioTracks().first()
            assertTrue(audioTracks.size >= 15)
            val tracksWithBundledNames = audioTracks.filter { it.offlineRawResName.isNotBlank() }
            // A bundled offline recording must really exist inside the APK and hold a valid MP3.
            // The empty placeholder files were removed from the resources, so no track may claim
            // a bundled offline recording any more.
            tracksWithBundledNames.forEach { track ->
                val resId = context.resources.getIdentifier(track.offlineRawResName, "raw", context.packageName)
                assertTrue("Declared raw resource '${track.offlineRawResName}' must exist", resId != 0)
                val firstRead = context.resources.openRawResource(resId).use { input ->
                    input.read(ByteArray(4_096))
                }
                assertTrue("A bundled offline recording must not be an empty file", firstRead > 0)
                assertTrue(
                    "A bundled offline recording must be reported as available offline",
                    AudioPlayerController.isTrackAvailableOffline("audio_${track.id}", track.offlineRawResName)
                )
            }

            // Seeding is safe to rerun: a second launch must not duplicate the starter catalogue.
            val beforeReseed = listOf(
                dao.countCachedAyahs(), dao.countHadiths(), dao.countDuasZiyarat(), dao.countAdhkar(),
                dao.countBooks(), dao.countCourses(), dao.countAudioTracks(), dao.countOccasions(),
                dao.countWallpapers(), dao.countContentSources()
            )
            AlHadithiDatabase.ensureSeeded(dao)
            val afterReseed = listOf(
                dao.countCachedAyahs(), dao.countHadiths(), dao.countDuasZiyarat(), dao.countAdhkar(),
                dao.countBooks(), dao.countCourses(), dao.countAudioTracks(), dao.countOccasions(),
                dao.countWallpapers(), dao.countContentSources()
            )
            assertEquals("Rerunning seed initialization must not duplicate content", beforeReseed, afterReseed)

            // Verify local announcements CRUD
            val adId = dao.insertAnnouncement(
                AdminAnnouncementEntity(
                    title = "مجلس عزاء مركزي",
                    body = "يقام المجلس في الصحن الحسيني الشريف الساعة الثامنة مساءً",
                    badgeLabel = "إعلان حسيني"
                )
            )
            assertEquals(1, dao.observeActiveAnnouncements().first().size)
            dao.deleteAnnouncementById(adId)
            assertEquals(0, dao.observeActiveAnnouncements().first().size)

            val occasions = dao.observeAllOccasions().first()
            assertTrue(occasions.isNotEmpty())
            val hasFirstNarration = occasions.any { it.hijriMonth == 5 && it.hijriDay == 13 }
            val hasSecondNarration = occasions.any { it.hijriMonth == 6 && it.hijriDay == 3 }
            assertTrue("Should include 13 Jumada I narration", hasFirstNarration)
            assertTrue("Should include 3 Jumada II narration", hasSecondNarration)
        } finally {
            db.close()
        }
    }
}
