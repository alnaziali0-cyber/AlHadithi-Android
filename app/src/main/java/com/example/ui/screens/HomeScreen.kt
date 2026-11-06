package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AdminAnnouncementEntity
import com.example.data.local.CachedAyahEntity
import com.example.data.local.DuaZiyaratEntity
import com.example.data.local.HadithEntity
import com.example.data.local.IslamicOccasionEntity
import com.example.data.local.SeedDataPart1
import com.example.domain.DailyPrayerSchedule
import com.example.domain.GovernorateInfo
import com.example.domain.NextPrayerCountdown
import com.example.domain.PrayerAndHijriCalculator
import com.example.ui.components.AlHadithiBrandTopBar
import com.example.ui.components.EmeraldHeroCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SourceCitationBadge
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.JoyBannerBgDark
import com.example.ui.theme.JoyBannerBgLight
import com.example.ui.theme.JoyBannerTextDark
import com.example.ui.theme.JoyBannerTextLight
import com.example.ui.theme.LocalIsDarkThemeActive
import com.example.ui.theme.LocalReadingFontScale
import com.example.ui.theme.MourningBannerBgDark
import com.example.ui.theme.MourningBannerBgLight
import com.example.ui.theme.MourningBannerTextDark
import com.example.ui.theme.MourningBannerTextLight
import com.example.ui.theme.quranAndHeritageStyle
import com.example.util.SystemActionsHelper

private data class QuickNavTile(
    val title: String,
    val icon: ImageVector,
    val destination: AppDestination,
    val tag: String
)

private data class PrayerChipItem(
    val name: String,
    val time: String,
    val icon: ImageVector
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    governorate: GovernorateInfo,
    schedule: DailyPrayerSchedule,
    countdown: NextPrayerCountdown,
    hijriOffsetDays: Int,
    announcements: List<AdminAnnouncementEntity>,
    hadiths: List<HadithEntity>,
    duas: List<DuaZiyaratEntity>,
    occasions: List<IslamicOccasionEntity>,
    lastSurahNumber: Int,
    lastSurahName: String,
    lastAyahNumber: Int,
    onOpenGovernorateSheet: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit,
    onSecretDeveloperTrigger: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkThemeActive.current
    val readingScale = LocalReadingFontScale.current

    val dayOfYearSeed = remember(schedule.hijriDate) {
        (schedule.hijriDate.month * 31 + schedule.hijriDate.day).coerceAtLeast(1)
    }

    val dailyVerse: CachedAyahEntity = remember(dayOfYearSeed) {
        val pool = SeedDataPart1.essentialQuranAyahs().filter { it.uthmaniText.length > 22 }
        pool[dayOfYearSeed % pool.size]
    }

    val dailyHadith: HadithEntity? = remember(hadiths, dayOfYearSeed) {
        if (hadiths.isEmpty()) null else hadiths[dayOfYearSeed % hadiths.size]
    }

    val weekdayIdx = remember { PrayerAndHijriCalculator.currentWeekdayIndex() }
    val dailyDua: DuaZiyaratEntity? = remember(duas, weekdayIdx) {
        duas.firstOrNull { it.weekdayIndex == weekdayIdx } ?: duas.firstOrNull { it.entryType == "DUA" }
    }

    val sortedUpcomingOccasions = remember(occasions, hijriOffsetDays) {
        occasions.map { occ ->
            val (daysLeft, gregDate) = PrayerAndHijriCalculator.nextOccurrenceDaysAndGregorian(
                occ.hijriMonth,
                occ.hijriDay,
                hijriOffsetDays
            )
            Triple(occ, daysLeft, gregDate)
        }.sortedBy { it.second }
    }
    val nearestOccasion = sortedUpcomingOccasions.firstOrNull()

    // 12 distinct, expressive, harmonious icons (zero duplicates!)
    val quickTiles = remember {
        listOf(
            QuickNavTile("القرآن الكريم", Icons.AutoMirrored.Outlined.MenuBook, AppDestination.QURAN, "quick_quran"),
            QuickNavTile("الصوتيات الحسينية", Icons.Outlined.Headphones, AppDestination.AUDIO_LIBRARY, "quick_audio"),
            QuickNavTile("الأدعية والزيارات", Icons.Outlined.VolunteerActivism, AppDestination.DUAS_ZIYARAT, "quick_duas"),
            QuickNavTile("المسبحة المطورة", Icons.Outlined.Spa, AppDestination.TASBIH, "quick_tasbih"),
            QuickNavTile("المكتبة والسيرة", Icons.Outlined.LocalLibrary, AppDestination.LIBRARY, "quick_library"),
            QuickNavTile("الحديث والروايات", Icons.Outlined.FormatQuote, AppDestination.HADITH, "quick_hadith"),
            QuickNavTile("مواقيت الصلاة", Icons.Outlined.Schedule, AppDestination.PRAYER_TIMES, "quick_prayer"),
            QuickNavTile("اتجاه القبلة", Icons.Outlined.Explore, AppDestination.QIBLA, "quick_qibla"),
            QuickNavTile("المناهج والدروس", Icons.Outlined.School, AppDestination.CURRICULA, "quick_curricula"),
            QuickNavTile("التقويم الهجري", Icons.Outlined.CalendarMonth, AppDestination.CALENDAR_OCCASIONS, "quick_calendar"),
            QuickNavTile("الخلفيات", Icons.Outlined.Wallpaper, AppDestination.WALLPAPERS, "quick_wallpapers"),
            QuickNavTile("البحث الشامل", Icons.Outlined.AutoAwesome, AppDestination.SEARCH, "quick_ai_search")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Frosted Glass Brand Top Header + Governorate Pill + Secret 7-Tap Developer Trigger
        item {
            AlHadithiBrandTopBar(
                governorateName = governorate.arabicName,
                onGovernorateClick = onOpenGovernorateSheet,
                onSearchClick = { onNavigate(AppDestination.SEARCH) },
                onFavoritesClick = { onNavigate(AppDestination.FAVORITES) },
                onSettingsClick = { onNavigate(AppDestination.SETTINGS) },
                onSecretDeveloperTrigger = onSecretDeveloperTrigger
            )
        }

        // 1b. Active Developer Announcements / Glass Ads (if published by the authorized developer)
        if (announcements.isNotEmpty()) {
            items(announcements, key = { "ad_${it.id}" }) { ad ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("home_announcement_card_${ad.id}"),
                    cornerRadius = 20.dp,
                    borderColor = HeritageGold.copy(alpha = 0.50f),
                    onClick = if (ad.actionUrl.startsWith("http")) {
                        {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ad.actionUrl)))
                            } catch (_: Exception) {
                            }
                        }
                    } else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                            ) {
                                Text(
                                    text = ad.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (ad.actionUrl.startsWith("http")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "فتح الرابط",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ad.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ad.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Hijri & Gregorian Date Strip
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                onClick = { onNavigate(AppDestination.CALENDAR_OCCASIONS) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = schedule.hijriDate.formattedArabic(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = schedule.gregorianDateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.70f),
                        border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.30f))
                    ) {
                        Text(
                            text = "محافظة ${governorate.arabicName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 3. Occasion-Adaptive Translucent Banner (Mourning vs Joy/Eid vs Upcoming Reminder)
        if (nearestOccasion != null && nearestOccasion.second <= 45) {
            item {
                val (occ, daysLeft, gregDate) = nearestOccasion
                val isMourning = occ.eventType == "MOURNING"
                val bannerBg = when {
                    isMourning && isDark -> MourningBannerBgDark
                    isMourning && !isDark -> MourningBannerBgLight
                    !isMourning && isDark -> JoyBannerBgDark
                    else -> JoyBannerBgLight
                }
                val bannerText = when {
                    isMourning && isDark -> MourningBannerTextDark
                    isMourning && !isDark -> MourningBannerTextLight
                    !isMourning && isDark -> JoyBannerTextDark
                    else -> JoyBannerTextLight
                }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 20.dp,
                    containerColor = bannerBg,
                    borderColor = bannerText.copy(alpha = 0.28f),
                    onClick = { onNavigate(AppDestination.CALENDAR_OCCASIONS) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (daysLeft == 0) "مناسبة اليوم" else "مناسبة قادمة · بعد $daysLeft يوماً",
                            style = MaterialTheme.typography.labelLarge,
                            color = bannerText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = gregDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = bannerText.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = occ.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bannerText
                    )
                    Text(
                        text = occ.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = bannerText.copy(alpha = 0.9f),
                        maxLines = 2
                    )
                }
            }
        }

        // 4. Next Prayer Glass Hero Card + Live Countdown + Daily Prayer Times with Icons
        item {
            EmeraldHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("next_prayer_hero_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الصلاة القادمة · ${governorate.arabicName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${countdown.prayerArabicName} — ${countdown.prayerFormattedTime}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.60f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "المتبقي",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                            Text(
                                text = countdown.countdownHhMmSs,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDF2D0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                val prayerItems = listOf(
                    PrayerChipItem("الفجر", schedule.formatted12h(schedule.fajrMinutes), Icons.Outlined.WbTwilight),
                    PrayerChipItem("الشروق", schedule.formatted12h(schedule.sunriseMinutes), Icons.Outlined.WbSunny),
                    PrayerChipItem("الظهر", schedule.formatted12h(schedule.dhuhrMinutes), Icons.Outlined.WbSunny),
                    PrayerChipItem("العصر", schedule.formatted12h(schedule.asrMinutes), Icons.Outlined.WbTwilight),
                    PrayerChipItem("المغرب", schedule.formatted12h(schedule.maghribMinutes), Icons.Outlined.NightsStay),
                    PrayerChipItem("العشاء", schedule.formatted12h(schedule.ishaMinutes), Icons.Outlined.NightsStay)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    prayerItems.forEach { item ->
                        val isNext = item.name == countdown.prayerArabicName
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(
                                    if (isNext) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isNext) Color(0xFFFDF2D0) else Color.White.copy(alpha = 0.80f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNext) Color(0xFFFDF2D0) else Color.White.copy(alpha = 0.85f),
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.time.replace(" ص", "").replace(" م", ""),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "منتصف الليل الشرعي: ${schedule.formatted12h(schedule.midnightMinutes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFDF2D0)
                    )
                    TextButton(
                        onClick = { onNavigate(AppDestination.PRAYER_TIMES) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "التقويم والضبط ←",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 5. Continue Last Quran Reading Strip
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                onClick = { onOpenSurah(lastSurahNumber) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mosque,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(9.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "متابعة الورد القرآني",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$lastSurahName · الآية $lastAyahNumber",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "افتح المصحف ←",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 6. Quick Access Bento Glass Grid (4 columns Fluid FlowRow)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeaderRow(
                    title = "الوصول السريع إلى الأقسام",
                    actionLabel = "كل الأقسام",
                    onActionClick = { onNavigate(AppDestination.MORE_HUB) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickTiles.forEach { tile ->
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag(tile.tag),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
                            onClick = { onNavigate(tile.destination) }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(9.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Daily Quranic Verse Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SectionHeaderRow(
                    title = "آية قرآنية يومية",
                    actionLabel = "${dailyVerse.surahNameArabic} (${dailyVerse.ayahNumberInSurah})",
                    onActionClick = { onOpenSurah(dailyVerse.surahNumber) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "﴿ ${dailyVerse.uthmaniText} ﴾",
                    style = quranAndHeritageStyle(readingScale),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (dailyVerse.tafsirSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dailyVerse.tafsirSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        SystemActionsHelper.copyToClipboard(
                            context,
                            dailyVerse.surahNameArabic,
                            "﴿ ${dailyVerse.uthmaniText} ﴾ [${dailyVerse.surahNameArabic}: ${dailyVerse.ayahNumberInSurah}]"
                        )
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ الآية")
                    }
                    IconButton(onClick = {
                        SystemActionsHelper.sharePlainText(
                            context,
                            dailyVerse.surahNameArabic,
                            "﴿ ${dailyVerse.uthmaniText} ﴾\n[${dailyVerse.surahNameArabic}: ${dailyVerse.ayahNumberInSurah}]"
                        )
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "مشاركة الآية")
                    }
                    IconButton(onClick = {
                        onSaveFavorite(
                            "AYAH",
                            "ayah_${dailyVerse.surahNumber}_${dailyVerse.ayahNumberInSurah}",
                            "${dailyVerse.surahNameArabic} — الآية ${dailyVerse.ayahNumberInSurah}",
                            "القرآن الكريم بالرسم العثماني",
                            dailyVerse.uthmaniText
                        )
                    }) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "حفظ الآية")
                    }
                }
            }
        }

        // 8. Verified Daily Hadith Card
        if (dailyHadith != null) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SectionHeaderRow(
                        title = "حديث اليوم الموثق",
                        actionLabel = "موسوعة الحديث",
                        onActionClick = { onNavigate(AppDestination.HADITH) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "« ${dailyHadith.arabicText} »",
                        style = quranAndHeritageStyle(readingScale * 0.92f).copy(lineHeight = 38.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SourceCitationBadge(
                        attribution = dailyHadith.infallibleName,
                        sourceBook = "${dailyHadith.sourceBook} — ${dailyHadith.authorName}",
                        volumeOrGrading = "${dailyHadith.volumeAndPage} · ${dailyHadith.gradingNote}"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            SystemActionsHelper.copyToClipboard(
                                context,
                                dailyHadith.infallibleName,
                                "${dailyHadith.arabicText}\n— ${dailyHadith.infallibleName} (${dailyHadith.sourceBook})"
                            )
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ الحديث")
                        }
                        IconButton(onClick = {
                            SystemActionsHelper.sharePlainText(
                                context,
                                dailyHadith.infallibleName,
                                "${dailyHadith.arabicText}\n— ${dailyHadith.infallibleName}\nالمصدر: ${dailyHadith.sourceBook} (${dailyHadith.volumeAndPage})"
                            )
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "مشاركة الحديث")
                        }
                    }
                }
            }
        }

        // 9. Daily Dua Card
        if (dailyDua != null) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SectionHeaderRow(
                        title = dailyDua.title,
                        actionLabel = "كل الأدعية",
                        onActionClick = { onNavigate(AppDestination.DUAS_ZIYARAT) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dailyDua.vocalizedText,
                        style = quranAndHeritageStyle(readingScale * 0.90f).copy(lineHeight = 38.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SourceCitationBadge(
                        attribution = dailyDua.attribution,
                        sourceBook = dailyDua.sourceBook,
                        volumeOrGrading = dailyDua.recommendedTime
                    )
                }
            }
        }
    }
}
