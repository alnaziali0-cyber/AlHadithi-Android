package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.IslamicOccasionEntity
import com.example.data.local.WallpaperEntity
import com.example.domain.DailyPrayerSchedule
import com.example.domain.HijriDateValue
import com.example.domain.PrayerAndHijriCalculator
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SubScreenTopBar
import com.example.util.SystemActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarOccasionsScreen(
    schedule: DailyPrayerSchedule,
    occasions: List<IslamicOccasionEntity>,
    hijriOffsetDays: Int,
    onBack: () -> Unit,
    onChangeHijriOffset: (Int) -> Unit
) {
    BackHandler { onBack() }
    var selectedMonth by remember(schedule.hijriDate.month) { mutableIntStateOf(schedule.hijriDate.month) }
    var typeFilter by remember { mutableStateOf("ALL") } // ALL, MOURNING, JOY

    val upcomingSorted = remember(occasions, hijriOffsetDays, typeFilter) {
        occasions.filter { occ ->
            when (typeFilter) {
                "MOURNING" -> occ.eventType == "MOURNING"
                "JOY" -> occ.eventType == "JOY" || occ.eventType == "EID"
                else -> true
            }
        }.map { occ ->
            val (daysLeft, gregLabel) = PrayerAndHijriCalculator.nextOccurrenceDaysAndGregorian(
                occ.hijriMonth,
                occ.hijriDay,
                hijriOffsetDays
            )
            Triple(occ, daysLeft, gregLabel)
        }.sortedBy { it.second }
    }

    val monthOccasions = remember(occasions, selectedMonth) {
        occasions.filter { it.hijriMonth == selectedMonth }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "التقويم الهجري ومناسبات أهل البيت (ع)",
            subtitle = "${schedule.gregorianDateLabel} — ${schedule.hijriDate.formattedArabic()}",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("calendar_occasions_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Manual Hijri Crescent Adjustment Card
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تصحيح ثبوت الهلال يدوياً (± يوم)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "لمطابقة إعلان مكتب المرجعية الدينية عند اختلاف بداية الشهر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onChangeHijriOffset((hijriOffsetDays - 1).coerceIn(-2, 2)) }) {
                                Text("−")
                            }
                            Text(
                                text = if (hijriOffsetDays > 0) "+$hijriOffsetDays" else "$hijriOffsetDays",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { onChangeHijriOffset((hijriOffsetDays + 1).coerceIn(-2, 2)) }) {
                                Text("+")
                            }
                        }
                    }
                }
            }

            // Month Selector + 30-day Hijri Grid
            item {
                GlassCard {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(12) { idx ->
                            val m = idx + 1
                            GlassFilterChip(
                                label = HijriDateValue.HIJRI_MONTH_NAMES[idx],
                                selected = selectedMonth == m,
                                onClick = { selectedMonth = m }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val occasionDaysSet = monthOccasions.map { it.hijriDay }.toSet()
                    FlowRow(
                        maxItemsInEachRow = 6,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..30).forEach { day ->
                            val isToday = selectedMonth == schedule.hijriDate.month && day == schedule.hijriDate.day
                            val hasEvent = day in occasionDaysSet
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isToday -> MaterialTheme.colorScheme.primary
                                    hasEvent -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isToday || hasEvent) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isToday -> MaterialTheme.colorScheme.onPrimary
                                            hasEvent -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Event Type Filter
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassFilterChip("الكل", typeFilter == "ALL", { typeFilter = "ALL" })
                    GlassFilterChip("أحزان وشهادات", typeFilter == "MOURNING", { typeFilter = "MOURNING" })
                    GlassFilterChip("أفراح وولادات وأعياد", typeFilter == "JOY", { typeFilter = "JOY" })
                }
            }

            item {
                Text(
                    text = "المناسبات القادمة مرتبة بالأيام المتبقية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(upcomingSorted, key = { it.first.id }) { (occ, daysLeft, gregDate) ->
                val monthName = HijriDateValue.HIJRI_MONTH_NAMES.getOrElse(occ.hijriMonth - 1) { "" }
                GlassCard(cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${occ.hijriDay} $monthName — $gregDate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (daysLeft == 0) "اليوم" else "بعد $daysLeft يوماً",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = occ.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = occ.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (occ.alternateNarrationsNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "تنبيه الروايات التاريخية: ${occ.alternateNarrationsNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperArtwork(wallpaper: WallpaperEntity, fallbackResId: Int, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = wallpaper.localImagePath) {
        value = if (wallpaper.localImagePath.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(wallpaper.localImagePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                BitmapFactory.decodeFile(
                    wallpaper.localImagePath,
                    BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 }
                )
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = wallpaper.title, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Image(painter = painterResource(id = fallbackResId), contentDescription = wallpaper.title, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

private fun saveWallpaperToGallery(context: android.content.Context, wallpaper: WallpaperEntity, fallbackResId: Int) {
    if (wallpaper.localImagePath.isNotBlank()) {
        SystemActionsHelper.saveImageFileToGallery(context, wallpaper.localImagePath, wallpaper.title)
    } else {
        SystemActionsHelper.saveDrawableWallpaperToGallery(context, fallbackResId, wallpaper.title)
    }
}

private fun resolveWallpaperResId(resName: String): Int = when (resName) {
    "bg_wallpaper_dawn_domes" -> R.drawable.bg_wallpaper_dawn_domes
    "bg_wallpaper_soft_stars" -> R.drawable.bg_wallpaper_soft_stars
    else -> R.drawable.bg_wallpaper_turquoise_gold
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpapersScreen(
    wallpapers: List<WallpaperEntity>,
    onBack: () -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("الكل") }
    var previewItem by remember { mutableStateOf<WallpaperEntity?>(null) }

    val categories = remember(wallpapers) {
        listOf("الكل") + wallpapers.map { it.category }.distinct()
    }
    val filtered = remember(wallpapers, selectedCategory) {
        if (selectedCategory == "الكل") wallpapers
        else wallpapers.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "الخلفيات الإسلامية والحسينية",
            subtitle = "خلفيات روحانية هادئة عالية الدقة — معاينة وحفظ ومشاركة",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("wallpapers_gallery_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        GlassFilterChip(
                            label = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { wp ->
                val resId = resolveWallpaperResId(wp.drawableResName)
                GlassCard(
                    cornerRadius = 22.dp,
                    onClick = { previewItem = wp }
                ) {
                    WallpaperArtwork(
                        wallpaper = wp,
                        fallbackResId = resId,
                        modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${wp.category} · ${wp.resolutionLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = wp.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = wp.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                saveWallpaperToGallery(context, wp, resId)
                            }) {
                                Icon(Icons.Outlined.Download, contentDescription = "حفظ في المعرض")
                            }
                            IconButton(onClick = {
                                onSaveFavorite("WALLPAPER", "wp_${wp.id}", wp.title, wp.category, wp.description)
                            }) {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "حفظ في المفضلة")
                            }
                        }
                    }
                }
            }
        }
    }

    if (previewItem != null) {
        val wp = previewItem!!
        val resId = resolveWallpaperResId(wp.drawableResName)
        ModalBottomSheet(
            onDismissRequest = { previewItem = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WallpaperArtwork(
                    wallpaper = wp,
                    fallbackResId = resId,
                    modifier = Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(22.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(wp.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(wp.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        saveWallpaperToGallery(context, wp, resId)
                    }) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنزيل للمعرض")
                    }
                    OutlinedButton(onClick = {
                        SystemActionsHelper.sharePlainText(context, wp.title, "${wp.title} — ${wp.description}")
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
