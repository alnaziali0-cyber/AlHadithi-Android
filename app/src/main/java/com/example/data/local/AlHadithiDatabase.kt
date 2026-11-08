package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedAyahEntity::class,
        HadithEntity::class,
        DuaZiyaratEntity::class,
        DhikrEntity::class,
        BookEntity::class,
        CurriculumCourseEntity::class,
        LessonProgressEntity::class,
        QuizAttemptEntity::class,
        AudioTrackEntity::class,
        IslamicOccasionEntity::class,
        WallpaperEntity::class,
        FavoriteBookmarkEntity::class,
        ReadingHistoryEntity::class,
        TasbihSessionEntity::class,
        ContentSourceEntity::class,
        AdminAnnouncementEntity::class,
        PrayerCompletionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AlHadithiDatabase : RoomDatabase() {

    abstract fun dao(): AlHadithiDao

    companion object {
        /** Adds only the prayer-completion table; all existing v2 tables and rows remain intact. */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS prayer_daily_completions (
                        profileKey TEXT NOT NULL,
                        dayKey TEXT NOT NULL,
                        prayerKey TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(profileKey, dayKey, prayerKey)
                    )""".trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_prayer_daily_completions_dayKey " +
                        "ON prayer_daily_completions(dayKey)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AlHadithiDatabase? = null

        fun getDatabase(context: Context): AlHadithiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlHadithiDatabase::class.java,
                    "alhadithi_encyclopedia.db"
                )
                    // Never erase user data automatically when a schema migration is missing.
                    // Add an explicit Room Migration before increasing the database version.
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Seeds each content table independently. A failed or interrupted insert in one
         * category must not prevent the remaining categories from being populated on the
         * next launch. Existing user data is never replaced by this seed operation.
         */
        suspend fun ensureSeeded(dao: AlHadithiDao) {
            if (dao.countCachedAyahs() == 0) {
                dao.insertCachedAyahs(SeedDataPart1.essentialQuranAyahs())
            }
            if (dao.countHadiths() == 0) {
                dao.insertHadiths(SeedDataPart1.authenticHadiths())
            }
            if (dao.countDuasZiyarat() == 0) {
                dao.insertDuasZiyarat(SeedDataPart1.authenticDuasAndZiyarat())
            }
            if (dao.countAdhkar() == 0) {
                dao.insertAdhkar(SeedDataPart1.authenticAdhkar())
            }
            if (dao.countBooks() == 0) {
                dao.insertBooks(SeedDataPart2.authenticBooksAndEncyclopedia())
            }
            if (dao.countCourses() == 0) {
                dao.insertCourses(SeedDataPart2.structuredCurricula())
            }
            // Add the ethics curriculum to existing installations without replacing user data
            // or re-seeding the existing courses.
            val missingEthicsCourses = SeedDataPart2.ethicsCurricula().filter {
                dao.countCourseByTitle(it.title) == 0
            }
            if (missingEthicsCourses.isNotEmpty()) {
                dao.insertCourses(missingEthicsCourses)
            }
            if (dao.countAudioTracks() == 0) {
                dao.insertAudioTracks(SeedDataPart2.authenticAudioLibrary())
            }
            if (dao.countOccasions() == 0) {
                dao.insertOccasions(SeedDataPart2.authenticIslamicOccasions())
            }
            if (dao.countWallpapers() == 0) {
                dao.insertWallpapers(SeedDataPart2.bundledWallpapers())
            }
            if (dao.countContentSources() == 0) {
                dao.insertContentSources(SeedDataPart2.verifiedSources())
            }
        }
    }
}
