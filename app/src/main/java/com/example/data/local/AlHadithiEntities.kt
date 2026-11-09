package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_ayahs",
    primaryKeys = ["surahNumber", "ayahNumberInSurah"]
)
data class CachedAyahEntity(
    val surahNumber: Int,
    val surahNameArabic: String,
    val ayahNumberInSurah: Int,
    val globalAyahNumber: Int,
    // Legacy Room column name retained for migration compatibility; stores the exact selected Arabic edition text.
    val uthmaniText: String,
    val juz: Int,
    val hizbQuarter: Int,
    val hasSajdah: Boolean = false,
    val tafsirSummary: String = "",
    val tafsirSource: String = ""
)

@Entity(
    tableName = "hadiths",
    indices = [Index("infallibleName"), Index("topicCategory")]
)
data class HadithEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val arabicText: String,
    val infallibleName: String,
    val topicCategory: String,
    val sourceBook: String,
    val authorName: String,
    val volumeAndPage: String,
    val gradingNote: String,
    val briefExplanation: String,
    val isFeaturedDaily: Boolean = false
)

@Entity(
    tableName = "duas_ziyarat",
    indices = [Index("entryType"), Index("category")]
)
data class DuaZiyaratEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryType: String, // "DUA" or "ZIYARAT"
    val category: String,  // e.g. "أدعية مشهورة", "أدعية الأيام", "أدعية الشهور", "زيارات الأئمة"
    val title: String,
    val vocalizedText: String,
    val attribution: String,
    val sourceBook: String,
    val recommendedTime: String,
    val audioStreamUrl: String = "",
    val targetRepeat: Int = 1,
    val weekdayIndex: Int = -1 // 0=Sat..6=Fri for daily duas, -1 otherwise
)

@Entity(
    tableName = "adhkar",
    indices = [Index("category")]
)
data class DhikrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // الصباح، المساء، تعقيبات الصلاة، قبل النوم، الاستيقاظ، الجمعة، السفر، رمضان، الاستغفار
    val title: String,
    val vocalizedText: String,
    val targetCount: Int,
    val virtueAndSource: String
)

@Entity(
    tableName = "library_books",
    indices = [Index("category"), Index("sectionType")]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionType: String, // "LIBRARY" or "ENCYCLOPEDIA" (Seerah, Karbala/Maqatil, Shrines)
    val category: String,    // العقائد، الفقه، الأخلاق، التفسير، الحديث، نهج البلاغة، الصحيفة السجادية، المقاتل، السيرة، العتبات...
    val title: String,
    val author: String,
    val eraLabel: String,
    val summary: String,
    val sourceAttribution: String,
    val chaptersDelimited: String, // Format: ChapterTitle::ChapterBody||ChapterTitle2::ChapterBody2
    val isDownloadedOffline: Boolean = true,
    val lastReadChapterIndex: Int = 0,
    val readingProgressPercent: Int = 0
)

@Entity(tableName = "curriculum_courses")
data class CurriculumCourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // العقيدة وأصول الدين، الفقه والعبادات، الأخلاق والسلوك، علوم القرآن والحديث
    val title: String,
    val levelLabel: String,
    val summary: String,
    val sourceReference: String,
    val lessonsJson: String, // Structured pipe-separated lessons: UnitTitle::LessonTitle::LessonBody::LessonSummary||...
    val quizJson: String     // Structured pipe-separated MCQs: Question::Opt0;;Opt1;;Opt2;;Opt3::CorrectIdx::Explanation||...
)

@Entity(
    tableName = "lesson_progress",
    primaryKeys = ["courseId", "lessonIndex"]
)
data class LessonProgressEntity(
    val courseId: Long,
    val lessonIndex: Int,
    val completedAtMillis: Long
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseTitle: String,
    val score: Int,
    val totalQuestions: Int,
    val attemptedAtMillis: Long
)

@Entity(
    tableName = "audio_tracks",
    indices = [Index("category")]
)
data class AudioTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // القرآن الكريم، اللطميات والقصائد، النعي الحسيني، الأدعية والمناجاة، الزيارات والأوراد، المجالس الحسينية...
    val title: String,
    val reciterOrRadood: String,
    val reciterBio: String,
    val streamUrl: String,
    val durationLabel: String,
    val offlineRawResName: String = ""
)

@Entity(tableName = "admin_announcements_local")
data class AdminAnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cloudId: String = "",
    val title: String,
    val body: String,
    val badgeLabel: String = "إعلان إداري",
    val actionUrl: String = "",
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "islamic_occasions",
    indices = [Index("hijriMonth", "hijriDay")]
)
data class IslamicOccasionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hijriMonth: Int, // 1..12
    val hijriDay: Int,   // 1..30
    val eventType: String, // "MOURNING", "JOY", "EID", "COMMEMORATION"
    val title: String,
    val infallibleOrTopic: String,
    val description: String,
    val alternateNarrationsNote: String = ""
)

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val drawableResName: String,
    val resolutionLabel: String,
    val description: String
)

@Entity(
    tableName = "favorites_bookmarks",
    indices = [Index(value = ["itemType", "itemRefKey"], unique = true)]
)
data class FavoriteBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String, // "AYAH", "HADITH", "DUA", "ZIYARAT", "BOOK", "AUDIO", "WALLPAPER"
    val itemRefKey: String,
    val title: String,
    val subtitle: String,
    val contentSnippet: String,
    val savedAtMillis: Long
)

@Entity(
    tableName = "reading_history",
    indices = [Index(value = ["itemType", "itemRefKey"], unique = true)]
)
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String,
    val itemRefKey: String,
    val title: String,
    val positionLabel: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "tasbih_sessions")
data class TasbihSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeTitle: String,
    val dhikrText: String,
    val completedCount: Int,
    val targetCount: Int,
    val completedAtMillis: Long
)

@Entity(tableName = "content_sources")
data class ContentSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceTitle: String,
    val authorAndDeathYear: String,
    val domainCategory: String,
    val editionNote: String
)


/**
 * Daily prayer completion for the active local profile. The profile key leaves room for a future
 * authenticated multi-device account without tying private progress to a developer account.
 */
@Entity(
    tableName = "prayer_daily_completions",
    primaryKeys = ["profileKey", "dayKey", "prayerKey"],
    indices = [Index("dayKey")]
)
data class PrayerCompletionEntity(
    val profileKey: String = LOCAL_PRAYER_PROFILE_KEY,
    val dayKey: String,
    val prayerKey: String,
    val isCompleted: Boolean,
    val updatedAtMillis: Long
)

const val LOCAL_PRAYER_PROFILE_KEY = "local-device-profile"
