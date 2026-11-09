package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlHadithiDao {

    // --- Quran Offline Cache ---
    @Query("SELECT * FROM cached_ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumberInSurah ASC")
    fun observeCachedSurahAyahs(surahNumber: Int): Flow<List<CachedAyahEntity>>

    @Query("SELECT * FROM cached_ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumberInSurah ASC")
    suspend fun getCachedSurahAyahsOnce(surahNumber: Int): List<CachedAyahEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAyahs(ayahs: List<CachedAyahEntity>)

    @Query("SELECT COUNT(*) FROM cached_ayahs")
    suspend fun countCachedAyahs(): Int

    @Query("SELECT * FROM cached_ayahs WHERE uthmaniText LIKE '%' || :query || '%' OR surahNameArabic LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchCachedAyahs(query: String): List<CachedAyahEntity>

    @Query("DELETE FROM cached_ayahs WHERE surahNumber NOT IN (1, 36, 55, 56, 67, 97, 103, 108, 109, 112, 113, 114)")
    suspend fun clearDownloadedQuranCacheKeepEssentials()

    // --- Hadiths ---
    @Query("SELECT * FROM hadiths ORDER BY id ASC")
    fun observeAllHadiths(): Flow<List<HadithEntity>>

    @Query("SELECT COUNT(*) FROM hadiths")
    suspend fun countHadiths(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(items: List<HadithEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleHadith(item: HadithEntity): Long

    @Query("DELETE FROM hadiths WHERE id = :id")
    suspend fun deleteHadithById(id: Long)

    @Query("SELECT * FROM hadiths WHERE arabicText LIKE '%' || :query || '%' OR infallibleName LIKE '%' || :query || '%' OR sourceBook LIKE '%' || :query || '%' LIMIT 40")
    suspend fun searchHadiths(query: String): List<HadithEntity>

    // --- Duas & Ziyarat ---
    @Query("SELECT * FROM duas_ziyarat ORDER BY id ASC")
    fun observeAllDuasAndZiyarat(): Flow<List<DuaZiyaratEntity>>

    @Query("SELECT * FROM duas_ziyarat WHERE entryType = :type ORDER BY id ASC")
    fun observeByEntryType(type: String): Flow<List<DuaZiyaratEntity>>

    @Query("SELECT COUNT(*) FROM duas_ziyarat")
    suspend fun countDuasZiyarat(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuasZiyarat(items: List<DuaZiyaratEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleDuaZiyarat(item: DuaZiyaratEntity): Long

    @Query("DELETE FROM duas_ziyarat WHERE id = :id")
    suspend fun deleteDuaZiyaratById(id: Long)

    @Query("SELECT * FROM duas_ziyarat WHERE title LIKE '%' || :query || '%' OR vocalizedText LIKE '%' || :query || '%' LIMIT 40")
    suspend fun searchDuasZiyarat(query: String): List<DuaZiyaratEntity>

    // --- Adhkar ---
    @Query("SELECT * FROM adhkar ORDER BY id ASC")
    fun observeAllAdhkar(): Flow<List<DhikrEntity>>

    @Query("SELECT COUNT(*) FROM adhkar")
    suspend fun countAdhkar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdhkar(items: List<DhikrEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleDhikr(item: DhikrEntity): Long

    @Query("DELETE FROM adhkar WHERE id = :id")
    suspend fun deleteDhikrById(id: Long)

    @Query("SELECT * FROM adhkar WHERE title LIKE '%' || :query || '%' OR vocalizedText LIKE '%' || :query || '%' LIMIT 30")
    suspend fun searchAdhkar(query: String): List<DhikrEntity>

    // --- Library Books & Seerah/Maqatil Encyclopedia ---
    @Query("SELECT * FROM library_books ORDER BY id ASC")
    fun observeAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM library_books WHERE id = :bookId LIMIT 1")
    fun observeBookById(bookId: Long): Flow<BookEntity?>

    @Query("SELECT COUNT(*) FROM library_books")
    suspend fun countBooks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("DELETE FROM library_books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("SELECT * FROM library_books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR chaptersDelimited LIKE '%' || :query || '%' LIMIT 30")
    suspend fun searchBooks(query: String): List<BookEntity>

    // --- Curricula, Lessons & Quizzes ---
    @Query("SELECT * FROM curriculum_courses ORDER BY id ASC")
    fun observeAllCourses(): Flow<List<CurriculumCourseEntity>>

    @Query("SELECT COUNT(*) FROM curriculum_courses")
    suspend fun countCourses(): Int

    @Query("SELECT COUNT(*) FROM curriculum_courses WHERE title = :title")
    suspend fun countCourseByTitle(title: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CurriculumCourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleCourse(course: CurriculumCourseEntity): Long

    @Query("DELETE FROM curriculum_courses WHERE id = :id")
    suspend fun deleteCourseById(id: Long)

    @Query("SELECT * FROM lesson_progress")
    fun observeAllLessonProgress(): Flow<List<LessonProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markLessonCompleted(progress: LessonProgressEntity)

    @Query("SELECT * FROM quiz_attempts ORDER BY attemptedAtMillis DESC")
    fun observeQuizAttempts(): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity)

    // --- Audio Tracks ---
    @Query("SELECT * FROM audio_tracks ORDER BY id ASC")
    fun observeAllAudioTracks(): Flow<List<AudioTrackEntity>>

    @Query("SELECT COUNT(*) FROM audio_tracks")
    suspend fun countAudioTracks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioTracks(tracks: List<AudioTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleAudioTrack(track: AudioTrackEntity): Long

    @Query("DELETE FROM audio_tracks WHERE id = :id")
    suspend fun deleteAudioTrackById(id: Long)

    @Query("SELECT * FROM audio_tracks WHERE title LIKE '%' || :query || '%' OR reciterOrRadood LIKE '%' || :query || '%' LIMIT 30")
    suspend fun searchAudioTracks(query: String): List<AudioTrackEntity>

    // --- Islamic Occasions ---
    @Query("SELECT * FROM islamic_occasions ORDER BY hijriMonth ASC, hijriDay ASC")
    fun observeAllOccasions(): Flow<List<IslamicOccasionEntity>>

    @Query("SELECT COUNT(*) FROM islamic_occasions")
    suspend fun countOccasions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccasions(occasions: List<IslamicOccasionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleOccasion(occasion: IslamicOccasionEntity): Long

    @Query("DELETE FROM islamic_occasions WHERE id = :id")
    suspend fun deleteOccasionById(id: Long)

    @Query("SELECT * FROM islamic_occasions WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' LIMIT 30")
    suspend fun searchOccasions(query: String): List<IslamicOccasionEntity>

    // --- Wallpapers ---
    @Query("SELECT * FROM wallpapers ORDER BY id ASC")
    fun observeAllWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT COUNT(*) FROM wallpapers")
    suspend fun countWallpapers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleWallpaper(wallpaper: WallpaperEntity): Long

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    suspend fun getWallpaperById(id: Long): WallpaperEntity?

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteWallpaperById(id: Long)

    // --- Favorites & Bookmarks ---
    @Query("SELECT * FROM favorites_bookmarks ORDER BY savedAtMillis DESC")
    fun observeAllFavorites(): Flow<List<FavoriteBookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites_bookmarks WHERE itemType = :type AND itemRefKey = :refKey)")
    suspend fun isFavoriteExists(type: String, refKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteBookmarkEntity)

    @Query("DELETE FROM favorites_bookmarks WHERE itemType = :type AND itemRefKey = :refKey")
    suspend fun deleteFavoriteByRef(type: String, refKey: String)

    // --- Reading & Listening History ---
    @Query("SELECT * FROM reading_history ORDER BY updatedAtMillis DESC LIMIT 50")
    fun observeReadingHistory(): Flow<List<ReadingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingHistory(entry: ReadingHistoryEntity)

    // --- Centralized daily prayer schedule cache ---
    @Query("SELECT * FROM prayer_schedule_cache WHERE governorateId = :governorateId AND dateKey BETWEEN :startDateKey AND :endDateKey ORDER BY dateKey ASC")
    suspend fun getPrayerSchedules(governorateId: String, startDateKey: String, endDateKey: String): List<PrayerScheduleEntity>

    @Query("SELECT * FROM prayer_schedule_cache WHERE governorateId = :governorateId AND dateKey = :dateKey LIMIT 1")
    suspend fun getPrayerSchedule(governorateId: String, dateKey: String): PrayerScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerSchedules(records: List<PrayerScheduleEntity>)

    @Query("DELETE FROM prayer_schedule_cache WHERE dateKey < :beforeDateKey")
    suspend fun deleteOldPrayerSchedules(beforeDateKey: String)

    // --- Daily Prayer Completion ---
    @Query("SELECT * FROM prayer_daily_completions WHERE profileKey = :profileKey AND dayKey = :dayKey ORDER BY prayerKey ASC")
    fun observePrayerCompletionsForDay(profileKey: String, dayKey: String): Flow<List<PrayerCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerCompletion(record: PrayerCompletionEntity)

    @Query("DELETE FROM prayer_daily_completions WHERE profileKey = :profileKey AND dayKey = :dayKey AND prayerKey = :prayerKey")
    suspend fun deletePrayerCompletion(profileKey: String, dayKey: String, prayerKey: String)

    @Query("DELETE FROM prayer_daily_completions")
    suspend fun deleteAllPrayerCompletions()

    // --- Tasbih Sessions ---
    @Query("SELECT * FROM tasbih_sessions ORDER BY completedAtMillis DESC LIMIT 60")
    fun observeTasbihSessions(): Flow<List<TasbihSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbihSession(session: TasbihSessionEntity)

    // --- Content Sources ---
    @Query("SELECT * FROM content_sources ORDER BY id ASC")
    fun observeContentSources(): Flow<List<ContentSourceEntity>>

    @Query("SELECT COUNT(*) FROM content_sources")
    suspend fun countContentSources(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentSources(sources: List<ContentSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleContentSource(source: ContentSourceEntity): Long

    // --- Admin & Developer Announcements / Ads ---
    @Query("SELECT * FROM admin_announcements_local WHERE isActive = 1 ORDER BY createdAtMillis DESC")
    fun observeActiveAnnouncements(): Flow<List<AdminAnnouncementEntity>>

    @Query("SELECT * FROM admin_announcements_local ORDER BY createdAtMillis DESC")
    fun observeAllAnnouncements(): Flow<List<AdminAnnouncementEntity>>

    @Query("SELECT * FROM admin_announcements_local WHERE cloudId = :cloudId LIMIT 1")
    suspend fun findAnnouncementByCloudId(cloudId: String): AdminAnnouncementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AdminAnnouncementEntity): Long

    @Query("UPDATE admin_announcements_local SET isActive = :active WHERE id = :id")
    suspend fun updateAnnouncementActiveById(id: Long, active: Boolean)

    @Query("UPDATE admin_announcements_local SET isActive = :active WHERE cloudId = :cloudId")
    suspend fun updateAnnouncementActiveByCloudId(cloudId: String, active: Boolean)

    @Query("DELETE FROM admin_announcements_local WHERE id = :id")
    suspend fun deleteAnnouncementById(id: Long)

    @Query("DELETE FROM admin_announcements_local WHERE cloudId = :cloudId")
    suspend fun deleteAnnouncementByCloudId(cloudId: String)

    // --- User data removal (privacy) ---
    // Only deletes rows; the schema is untouched, so no Room migration is required.

    @Query("DELETE FROM favorites_bookmarks")
    suspend fun deleteAllFavorites()

    @Query("DELETE FROM reading_history")
    suspend fun deleteAllReadingHistory()

    @Query("DELETE FROM tasbih_sessions")
    suspend fun deleteAllTasbihSessions()

    @Query("DELETE FROM lesson_progress")
    suspend fun deleteAllLessonProgress()

    @Query("DELETE FROM quiz_attempts")
    suspend fun deleteAllQuizAttempts()

    @Query("DELETE FROM cached_ayahs")
    suspend fun deleteAllCachedAyahs()
}
