package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.media.DailyContentStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the three features added in the re-engineering:
 * the daily prayer tracker, the Akhlaq (ethics) section and the daily
 * content notification plan.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewFeaturesTest {

    private fun database(): AlHadithiDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AlHadithiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    // ── صلواتي اليوم ────────────────────────────────────────────────────────
    @Test
    fun `prayer tracker keeps every prayer status per day and user`() = runBlocking {
        val db = database()
        val dao = db.dao()

        dao.upsertPrayerState(
            PrayerDailyStateEntity(ownerKey = "guest", prayerDate = "2026-11-06", prayer = "fajr", status = PRAYER_STATUS_DONE)
        )
        dao.upsertPrayerState(
            PrayerDailyStateEntity(ownerKey = "guest", prayerDate = "2026-11-06", prayer = "dhuhr", status = PRAYER_STATUS_LATE)
        )
        // مستخدم آخر على الجهاز لا تتداخل بياناته.
        dao.upsertPrayerState(
            PrayerDailyStateEntity(ownerKey = "uid_other", prayerDate = "2026-11-06", prayer = "fajr", status = PRAYER_STATUS_MISSED)
        )

        val guest = dao.getPrayerStatesForDate("guest", "2026-11-06")
        assertEquals(2, guest.size)
        assertEquals(PRAYER_STATUS_DONE, guest.first { it.prayer == "fajr" }.status)

        val other = dao.getPrayerStatesForDate("uid_other", "2026-11-06")
        assertEquals(1, other.size)
        assertEquals(PRAYER_STATUS_MISSED, other.first().status)

        // تحديث الحالة نفسها يستبدل القيمة (لا يكرر السجل).
        dao.upsertPrayerState(
            PrayerDailyStateEntity(ownerKey = "guest", prayerDate = "2026-11-06", prayer = "fajr", status = PRAYER_STATUS_PENDING)
        )
        assertEquals(2, dao.getPrayerStatesForDate("guest", "2026-11-06").size)
        assertEquals(
            PRAYER_STATUS_PENDING,
            dao.getPrayerStatesForDate("guest", "2026-11-06").first { it.prayer == "fajr" }.status
        )
        db.close()
    }

    @Test
    fun `prayer history counts only the performed prayers`() = runBlocking {
        val db = database()
        val dao = db.dao()
        listOf("fajr", "dhuhr", "asr").forEach { prayer ->
            dao.upsertPrayerState(
                PrayerDailyStateEntity(ownerKey = "guest", prayerDate = "2026-11-06", prayer = prayer, status = PRAYER_STATUS_DONE)
            )
        }
        dao.upsertPrayerState(
            PrayerDailyStateEntity(ownerKey = "guest", prayerDate = "2026-11-06", prayer = "maghrib", status = PRAYER_STATUS_PENDING)
        )
        val history = dao.observePrayerHistory("guest", "2026-01-01").first()
        assertEquals(1, history.size)
        assertEquals(3, history.first().done)
        assertEquals(4, history.first().total)
        db.close()
    }

    // ── قسم الأخلاقيات ──────────────────────────────────────────────────────
    @Test
    fun `akhlaq content asset seeds fifteen verified lessons`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = database()
        val dao = db.dao()

        AkhlaqSeedData.ensureSeeded(context, dao)

        assertEquals(15, dao.countAkhlaqCategories())
        assertEquals(15, dao.countAkhlaqLessons())
        assertTrue("The Akhlaq section must ship quizzes", dao.countAkhlaqQuizzes() >= 45)

        val lessons = dao.observeAkhlaqLessons().first()
        lessons.forEach { lesson ->
            val ayat = AkhlaqParsers.ayat(lesson.evidenceAyat)
            assertTrue("Every lesson must carry Quranic evidence", ayat.isNotEmpty())
            ayat.forEach { ayah ->
                assertTrue("Verse text must not be empty", ayah.text.isNotBlank())
                assertTrue("Verse reference must be present", ayah.reference.isNotBlank())
                assertTrue(ayah.surah in 1..114)
                assertTrue(ayah.ayah in 1..286)
            }
            // كل درس يحمل حديثاً موثّقاً مع تخريجه.
            val hadith = AkhlaqParsers.hadith(lesson.evidenceHadith)
            assertNotNull("Every lesson must carry a sourced hadith", hadith)
            assertTrue(hadith!!.takhrij.isNotBlank())
            assertTrue(hadith.text.isNotBlank())
            // المواقف تُحلّل من الرؤية التعليمية لا من总的来说.
            assertTrue(AkhlaqParsers.scenarios(lesson.scenarios).isNotEmpty())
            assertTrue(AkhlaqParsers.stringList(lesson.applySteps).isNotEmpty())
            assertTrue(AkhlaqParsers.stringList(lesson.mistakes).isNotEmpty())
        }
        db.close()
    }

    @Test
    fun `akhlaq seed is idempotent and never duplicates rows`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = database()
        val dao = db.dao()
        AkhlaqSeedData.ensureSeeded(context, dao)
        val first = dao.countAkhlaqLessons()
        AkhlaqSeedData.ensureSeeded(context, dao)
        assertEquals(first, dao.countAkhlaqLessons())
        db.close()
    }

    @Test
    fun `akhlaq asset contains no fabricated latin words in the arabic content`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val raw = context.assets.open("akhlaq_content.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val lessons = root.getJSONArray("lessons")
        assertEquals(15, lessons.length())
        for (i in 0 until lessons.length()) {
            val lesson = lessons.getJSONObject(i)
            val arabicFields = listOf("intro", "body", "summary")
            arabicFields.forEach { field ->
                val value = lesson.optString(field)
                val latinWords = Regex("[A-Za-z]{2,}").findAll(value).map { it.value }.toList()
                assertTrue("Unexpected latin text in '$field': $latinWords", latinWords.isEmpty())
            }
            // الآيات تُنسخ حرفياً من نص المصحف ولا يجوز أن تحتوي حروفاً لاتينية.
            val ayat = lesson.optJSONArray("ayat") ?: JSONArray()
            for (a in 0 until ayat.length()) {
                val verse = ayat.getJSONObject(a).optString("text")
                assertTrue(
                    "A Quranic verse must not contain latin letters",
                    Regex("[A-Za-z]").findAll(verse).none()
                )
            }
        }
    }

    // ── خطة محتوى اليوم (إشعار بعد كل صلاة) ───────────────────────────────
    @Test
    fun `daily content plan stores and returns the content of a prayer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val item = DailyContentStore.ContentItem(
            title = "حديث اليوم",
            body = "إنما الأعمال بالنيات",
            takhrij = "صحيح البخاري، كتاب الإيمان"
        )
        DailyContentStore.putPlan(
            context = context,
            items = mapOf("20261106|fajr" to item),
            contentType = "hadith",
            enabledPrayers = setOf("fajr", "isha"),
            enabled = true
        )
        val stored = DailyContentStore.planItem(context, "20261106", "fajr")
        assertNotNull(stored)
        assertEquals("حديث اليوم", stored!!.title)
        assertEquals("إنما الأعمال بالنيات", stored.body)
        assertTrue(stored.takhrij.contains("البخاري"))
        assertTrue(DailyContentStore.isEnabled(context))
        assertEquals(setOf("fajr", "isha"), DailyContentStore.enabledPrayers(context))
        assertEquals("hadith", DailyContentStore.contentType(context))
        // صلاة غير موجودة في الخطة لا تعيد أي محتوى.
        assertEquals(null, DailyContentStore.planItem(context, "20261106", "isha"))
        assertEquals(null, DailyContentStore.planItem(context, "20261107", "fajr"))
    }

    @Test
    fun `daily content plan respects the disabled state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DailyContentStore.putPlan(
            context = context,
            items = mapOf("20261106|dhuhr" to DailyContentStore.ContentItem("دعاء اليوم", "نص", "تخريج")),
            contentType = "dua",
            enabledPrayers = emptySet(),
            enabled = false
        )
        assertFalse(DailyContentStore.isEnabled(context))
        assertTrue(DailyContentStore.enabledPrayers(context).isEmpty())
        assertEquals("dua", DailyContentStore.contentType(context))
    }
}
