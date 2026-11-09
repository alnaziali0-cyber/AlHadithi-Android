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
        PrayerCompletionEntity::class,
        PrayerScheduleEntity::class
    ],
    version = 6,
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

        /** Corrects seeded book titles that previously implied complete editions while
         *  the bundled text contains only selected excerpts. Only exact original seed titles
         *  are changed; user-edited titles and all reading-progress rows remain untouched.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE library_books SET title = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "مختارات من نهج البلاغة — خطب وكتب وحكم أمير المؤمنين (ع)",
                        "نهج البلاغة — خطب وكتب وحكم أمير المؤمنين (ع)",
                        "جمع الشريف الرضي (ت ٤٠٦ هـ)",
                        "نسخة محققة على طبعة الدكتور صبحي الصالح والشيخ محمد عبده"
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET title = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "مختارات من الصحيفة السجادية (زبور آل محمد)",
                        "الصحيفة السجادية الكاملة (زبور آل محمد)",
                        "الإمام علي بن الحسين زين العابدين (عليه السلام)",
                        "مروية بأسانيد متواترة عن الإمام الباقر وأخيه زيد الشهيد (ع)"
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET summary = ? WHERE title = ? AND author = ? AND summary = ?",
                    arrayOf(
                        "مختارات من التراث الروحي والتربوي في الدعاء والمناجاة، تعرض نماذج من أصول المعرفة الإلهية وتهذيب النفس وحقوق الوالدين والناس.",
                        "مختارات من الصحيفة السجادية (زبور آل محمد)",
                        "الإمام علي بن الحسين زين العابدين (عليه السلام)",
                        "تراثٌ روحي وتربوي وعقائدي فريد صيغ في قالب الدعاء والمناجاة، يجمع أصول المعرفة الإلهية وتهذيب النفس وحقوق الوالدين والناس."
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET sourceAttribution = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "مرجع النص: الصحيفة السجادية المنسوبة إلى الإمام علي بن الحسين (ع)؛ يلزم تعيين طبعة التحقيق عند إضافة النص الكامل",
                        "مختارات من الصحيفة السجادية (زبور آل محمد)",
                        "الإمام علي بن الحسين زين العابدين (عليه السلام)",
                        "مروية بأسانيد متواترة عن الإمام الباقر وأخيه زيد الشهيد (ع)"
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET title = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "مقتطفات من اعتقادات الإمامية",
                        "اعتقادات الإمامية",
                        "الشيخ الصدوق محمد بن علي بن بابويه القمي (ت ٣٨١ هـ)",
                        "المطبعة الحيدرية في النجف الأشرف"
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET title = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "مختارات من رسالة الحقوق للإمام زين العابدين (ع)",
                        "رسالة الحقوق للإمام زين العابدين (ع)",
                        "الإمام علي بن الحسين السجاد (عليه السلام)",
                        "مروية في من لا يحضره الفقيه للصدوق وتحف العقول"
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET title = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "مدخل موجز إلى سيرة النبي الأكرم ﷺ والأئمة الاثني عشر (ع)",
                        "معالم سيرة النبي الأكرم ﷺ والأئمة الاثني عشر (ع)",
                        "الشيخ المفيد / الشيخ عباس القمي",
                        "الإرشاد في معرفة حجج الله على العباد — الشيخ المفيد · منتهى الآمال — الشيخ عباس القمي"
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET title = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "لمحات عن أعلام مدرسة أهل البيت عبر القرون",
                        "أعلام مدرسة أهل البيت عبر القرون",
                        "موسوعة تراجم العلماء",
                        "رياض العلماء · أعيان الشيعة للسيد محسن الأمين"
                    )
                )
                database.execSQL(
                    "UPDATE library_books SET sourceAttribution = ? WHERE title = ? AND author = ? AND sourceAttribution = ?",
                    arrayOf(
                        "مراجع للمقابلة: طبعة صبحي الصالح وطبعة محمد عبده؛ النص المعروض هنا مختارات وليس نسخة كاملة",
                        "مختارات من نهج البلاغة — خطب وكتب وحكم أمير المؤمنين (ع)",
                        "جمع الشريف الرضي (ت ٤٠٦ هـ)",
                        "نسخة محققة على طبعة الدكتور صبحي الصالح والشيخ محمد عبده"
                    )
                )
            }
        }

        /** Adds the normalized offline cache for one timetable per governorate/date. */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS prayer_schedule_cache (
                        governorateId TEXT NOT NULL, dateKey TEXT NOT NULL,
                        fajrMinutes INTEGER NOT NULL, sunriseMinutes INTEGER NOT NULL,
                        dhuhrMinutes INTEGER NOT NULL, asrMinutes INTEGER NOT NULL,
                        maghribMinutes INTEGER NOT NULL, ishaMinutes INTEGER NOT NULL,
                        midnightMinutes INTEGER NOT NULL, hijriYear INTEGER NOT NULL,
                        hijriMonth INTEGER NOT NULL, hijriDay INTEGER NOT NULL,
                        gregorianDateLabel TEXT NOT NULL, sourceType TEXT NOT NULL,
                        sourceLabel TEXT NOT NULL, sourceUrl TEXT NOT NULL,
                        sourceRecordId TEXT NOT NULL, sourceUpdatedAtMillis INTEGER NOT NULL,
                        cachedAtMillis INTEGER NOT NULL, timeZoneId TEXT NOT NULL,
                        PRIMARY KEY(governorateId, dateKey)
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_prayer_schedule_cache_dateKey ON prayer_schedule_cache(dateKey)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_prayer_schedule_cache_sourceType ON prayer_schedule_cache(sourceType)")
            }
        }

        /** Adds private file references for developer-managed wallpaper uploads. */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wallpapers ADD COLUMN localImagePath TEXT NOT NULL DEFAULT ''")
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
