package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        PrayerDailyStateEntity::class,
        AkhlaqCategoryEntity::class,
        AkhlaqLessonEntity::class,
        AkhlaqQuizEntity::class,
        AkhlaqProgressEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AlHadithiDatabase : RoomDatabase() {

    abstract fun dao(): AlHadithiDao

    companion object {
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
                    // Every schema change ships an explicit migration so progress survives.
                    .addMigrations(AlHadithiMigrations.MIGRATION_2_3, AlHadithiMigrations.MIGRATION_3_4)
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
        suspend fun ensureSeeded(context: Context, dao: AlHadithiDao) {
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
            // The Akhlaq section is seeded from the bundled verified content asset.
            AkhlaqSeedData.ensureSeeded(context, dao)
        }
    }
}
