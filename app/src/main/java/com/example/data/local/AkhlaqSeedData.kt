package com.example.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.PrayerKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Seeds the Akhlaq (الأخلاقيات) section from the bundled verified content asset.
 *
 * The asset ships the Quranic evidence verbatim (cross-checked against two
 * independent Quran APIs) and every hadith with its real takhrij, so no lesson
 * ever invents a source. Seeding is idempotent: it only writes when the tables
 * are empty, and a failed insert never removes user data.
 */
object AkhlaqSeedData {

    private const val ASSET_NAME = "akhlaq_content.json"

    suspend fun ensureSeeded(context: Context, dao: AlHadithiDao) {
        if (dao.countAkhlaqCategories() > 0) return
        val root = runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return

        val lessons = runCatching {
            JSONObject(root).getJSONArray("lessons")
        }.getOrNull() ?: return

        val categories = mutableListOf<AkhlaqCategoryEntity>()
        val lessonEntities = mutableListOf<AkhlaqLessonEntity>()
        val quizEntities = mutableListOf<AkhlaqQuizEntity>()

        for (i in 0 until lessons.length()) {
            val lesson = lessons.getJSONObject(i)
            val categoryTitle = lesson.optString("category")
            val categoryId = "akhlaq-cat-${lesson.optInt("order", i)}"
            if (categories.none { it.title == categoryTitle }) {
                categories += AkhlaqCategoryEntity(
                    id = categoryId,
                    title = categoryTitle,
                    icon = lesson.optString("icon", "book-open"),
                    sortOrder = lesson.optInt("order", i)
                )
            }

            val lessonId = lesson.optString("id", "akhlaq-lesson-$i")
            lessonEntities += AkhlaqLessonEntity(
                id = lessonId,
                categoryId = categoryId,
                title = lesson.optString("title"),
                intro = lesson.optString("intro"),
                body = lesson.optString("body"),
                evidenceAyat = lesson.optJSONArray("ayat")?.toString() ?: "[]",
                evidenceHadith = lesson.optJSONObject("hadith")?.toString(),
                examples = lesson.optJSONArray("examples")?.toString() ?: "[]",
                scenarios = lesson.optJSONArray("scenarios")?.toString() ?: "[]",
                applySteps = lesson.optJSONArray("apply")?.toString() ?: "[]",
                mistakes = lesson.optJSONArray("mistakes")?.toString() ?: "[]",
                summary = lesson.optString("summary"),
                sortOrder = lesson.optInt("order", i)
            )

            val quizzes = lesson.optJSONArray("quizzes") ?: continue
            for (q in 0 until quizzes.length()) {
                val quiz = quizzes.getJSONObject(q)
                quizEntities += AkhlaqQuizEntity(
                    id = "$lessonId-q$q",
                    lessonId = lessonId,
                    question = quiz.optString("q"),
                    options = quiz.optJSONArray("options")?.toString() ?: "[]",
                    answerIndex = quiz.optInt("answer", 0),
                    explanation = quiz.optString("explain"),
                    sortOrder = q
                )
            }
        }

        if (categories.isEmpty()) return
        dao.insertAkhlaqCategories(categories)
        dao.insertAkhlaqLessons(lessonEntities)
        if (quizEntities.isNotEmpty()) dao.insertAkhlaqQuizzes(quizEntities)
    }
}

/** Parses the JSON blobs stored in the Akhlaq tables into plain Kotlin values. */
object AkhlaqParsers {

    fun stringList(raw: String?): List<String> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    data class EvidenceAyah(
        val surah: Int,
        val ayah: Int,
        val surahName: String,
        val text: String,
        val reference: String
    )

    fun ayat(raw: String?): List<EvidenceAyah> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            EvidenceAyah(
                surah = obj.optInt("surah"),
                ayah = obj.optInt("ayah"),
                surahName = obj.optString("surahName"),
                text = obj.optString("text"),
                reference = obj.optString("reference")
            )
        }
    }.getOrDefault(emptyList())

    data class EvidenceHadith(
        val text: String,
        val source: String,
        val book: String,
        val number: String,
        val takhrij: String
    )

    fun hadith(raw: String?): EvidenceHadith? = runCatching {
        val obj = JSONObject(raw ?: return null)
        EvidenceHadith(
            text = obj.optString("text"),
            source = obj.optString("source"),
            book = obj.optString("book"),
            number = obj.optString("number"),
            takhrij = obj.optString("takhrij")
        )
    }.getOrNull()

    data class Scenario(
        val situation: String,
        val options: List<String>,
        val correctIndex: Int
    )

    fun scenarios(raw: String?): List<Scenario> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val options = obj.optJSONArray("options")?.let { opts ->
                (0 until opts.length()).map { opts.optString(it) }
            } ?: emptyList()
            Scenario(
                situation = obj.optString("situation"),
                options = options,
                correctIndex = obj.optInt("correct", 0)
            )
        }
    }.getOrDefault(emptyList())
}

/**
 * Room migrations. The database is never destroyed automatically — user progress
 * must survive every upgrade, so each schema change ships an explicit migration.
 */
object AlHadithiMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `prayer_daily_state` (
                    `ownerKey` TEXT NOT NULL,
                    `prayerDate` TEXT NOT NULL,
                    `prayer` TEXT NOT NULL,
                    `status` TEXT NOT NULL DEFAULT 'pending',
                    `note` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`ownerKey`, `prayerDate`, `prayer`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `akhlaq_categories` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `icon` TEXT NOT NULL DEFAULT 'book-open',
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `akhlaq_lessons` (
                    `id` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `intro` TEXT NOT NULL DEFAULT '',
                    `body` TEXT NOT NULL DEFAULT '',
                    `evidenceAyat` TEXT NOT NULL DEFAULT '[]',
                    `evidenceHadith` TEXT,
                    `examples` TEXT NOT NULL DEFAULT '[]',
                    `scenarios` TEXT NOT NULL DEFAULT '[]',
                    `applySteps` TEXT NOT NULL DEFAULT '[]',
                    `mistakes` TEXT NOT NULL DEFAULT '[]',
                    `summary` TEXT NOT NULL DEFAULT '',
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `akhlaq_quizzes` (
                    `id` TEXT NOT NULL,
                    `lessonId` TEXT NOT NULL,
                    `question` TEXT NOT NULL,
                    `options` TEXT NOT NULL DEFAULT '[]',
                    `answerIndex` INTEGER NOT NULL DEFAULT 0,
                    `explanation` TEXT NOT NULL DEFAULT '',
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `akhlaq_progress` (
                    `lessonId` TEXT NOT NULL,
                    `completedAt` INTEGER NOT NULL,
                    `score` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`lessonId`)
                )
                """.trimIndent()
            )
        }
    }
}
