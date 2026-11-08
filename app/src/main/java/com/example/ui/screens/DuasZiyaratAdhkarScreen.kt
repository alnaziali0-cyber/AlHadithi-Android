package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DhikrEntity
import com.example.data.local.DuaZiyaratEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SourceCitationBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalLineSpacing
import com.example.ui.theme.LocalReadingFontChoice
import com.example.ui.theme.LocalReadingFontScale
import com.example.ui.theme.heritageReadingStyle
import com.example.ui.theme.quranAndHeritageStyle
import com.example.util.SystemActionsHelper

@Composable
fun DuasZiyaratAdhkarScreen(
    duasAndZiyarat: List<DuaZiyaratEntity>,
    adhkar: List<DhikrEntity>,
    hapticEnabled: Boolean,
    dhikrCounters: Map<Long, Int>,
    onChangeFontScale: (Float) -> Unit,
    onIncrementDhikr: (Long) -> Unit,
    onResetDhikr: (Long) -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val fontScale = LocalReadingFontScale.current
    val readingFontChoice = LocalReadingFontChoice.current
    val lineSpacing = LocalLineSpacing.current
    var openedItem by remember { mutableStateOf<DuaZiyaratEntity?>(null) }

    if (openedItem != null) {
        val item = openedItem!!
        var repeatCounter by remember(item.id) { mutableIntStateOf(0) }
        BackHandler { openedItem = null }

        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = item.title,
                subtitle = item.recommendedTime,
                onBack = { openedItem = null },
                trailing = {
                    Row {
                        IconButton(onClick = {
                            com.example.media.AudioPlayerController.speakArabicTextOffline(
                                title = item.title,
                                arabicText = item.vocalizedText
                            )
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "قراءة صوتية بدون إنترنت"
                            )
                        }
                        IconButton(onClick = { onChangeFontScale(fontScale - 0.10f) }) {
                            Icon(Icons.Outlined.ZoomOut, contentDescription = "تصغير الخط")
                        }
                        IconButton(onClick = { onChangeFontScale(fontScale + 0.10f) }) {
                            Icon(Icons.Outlined.ZoomIn, contentDescription = "تكبير الخط")
                        }
                        IconButton(onClick = {
                            onSaveFavorite(
                                item.entryType,
                                "${item.entryType.lowercase()}_${item.id}",
                                item.title,
                                item.attribution,
                                item.vocalizedText
                            )
                        }) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "حفظ في المفضلة")
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(cornerRadius = 24.dp) {
                        Text(
                            text = item.vocalizedText,
                            style = quranAndHeritageStyle(fontScale),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SourceCitationBadge(
                            attribution = item.attribution,
                            sourceBook = item.sourceBook,
                            volumeOrGrading = "الوقت المستحب: ${item.recommendedTime}"
                        )
                    }
                }

                item {
                    GlassCard(cornerRadius = 20.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "عداد التكرار التفاعلي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$repeatCounter / ${item.targetRepeat}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { repeatCounter = 0 }) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "تصفير")
                                }
                                Button(onClick = {
                                    repeatCounter++
                                    if (hapticEnabled) {
                                        SystemActionsHelper.triggerHapticPulse(
                                            context,
                                            strongMilestone = repeatCounter == item.targetRepeat
                                        )
                                    }
                                }) {
                                    Icon(Icons.Outlined.TouchApp, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تسبيح / تكرار")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                SystemActionsHelper.copyToClipboard(
                                    context,
                                    item.title,
                                    "${item.title}\n\n${item.vocalizedText}\n— ${item.sourceBook}"
                                )
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ النص")
                            }
                            IconButton(onClick = {
                                SystemActionsHelper.sharePlainText(
                                    context,
                                    item.title,
                                    "${item.title}\n\n${item.vocalizedText}\n— ${item.attribution} (${item.sourceBook})"
                                )
                            }) {
                                Icon(Icons.Outlined.Share, contentDescription = "مشاركة")
                            }
                        }
                    }
                }
            }
        }
        return
    }

    var primaryTab by remember { mutableIntStateOf(0) } // 0=الأدعية, 1=الزيارات, 2=الأذكار اليومية
    var selectedSubCat by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "الأدعية والزيارات والأذكار",
            subtitle = "مأثورات أهل البيت (عليهم السلام) مضبوطة بالشكل مع مصادرها"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("duas_ziyarat_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassFilterChip(
                        label = "الأدعية",
                        selected = primaryTab == 0,
                        onClick = {
                            primaryTab = 0
                            selectedSubCat = "الكل"
                        }
                    )
                    GlassFilterChip(
                        label = "الزيارات المشرفة",
                        selected = primaryTab == 1,
                        onClick = {
                            primaryTab = 1
                            selectedSubCat = "الكل"
                        }
                    )
                    GlassFilterChip(
                        label = "أذكار اليوم والليلة",
                        selected = primaryTab == 2,
                        onClick = {
                            primaryTab = 2
                            selectedSubCat = "الكل"
                        }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في العنوان أو متن الدعاء والزيارة…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (primaryTab == 2) {
                val adhkarCats = listOf("الكل") + adhkar.map { it.category }.distinct()
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(adhkarCats) { cat ->
                            GlassFilterChip(
                                label = cat,
                                selected = selectedSubCat == cat,
                                onClick = { selectedSubCat = cat }
                            )
                        }
                    }
                }

                val filteredAdhkar = adhkar.filter { d ->
                    (selectedSubCat == "الكل" || d.category == selectedSubCat) &&
                        (searchQuery.isBlank() || d.title.contains(searchQuery) || d.vocalizedText.contains(searchQuery))
                }

                items(filteredAdhkar, key = { it.id }) { dhikr ->
                    val currentCount = dhikrCounters[dhikr.id] ?: 0
                    val isDone = currentCount >= dhikr.targetCount
                    GlassCard(cornerRadius = 20.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dhikr.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isDone) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isDone) "مكتمل ✓ ($currentCount/${dhikr.targetCount})"
                                    else "$currentCount من ${dhikr.targetCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDone) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dhikr.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dhikr.vocalizedText,
                            style = heritageReadingStyle(
                                fontScale * 0.92f,
                                fontChoice = readingFontChoice,
                                lineSpacing = lineSpacing
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dhikr.virtueAndSource.ifBlank {
                                "المصدر غير متوفر في بيانات التطبيق"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { onResetDhikr(dhikr.id) }) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "تصفير العدّاد")
                                }
                                IconButton(onClick = {
                                    onSaveFavorite(
                                        "ADHKAR",
                                        "dhikr_${dhikr.id}",
                                        dhikr.title,
                                        dhikr.category,
                                        dhikr.vocalizedText
                                    )
                                }) {
                                    Icon(
                                        Icons.Outlined.BookmarkBorder,
                                        contentDescription = "حفظ الذكر في المفضلة"
                                    )
                                }
                                IconButton(onClick = {
                                    SystemActionsHelper.copyToClipboard(
                                        context,
                                        dhikr.title,
                                        "${dhikr.title}\n\n${dhikr.vocalizedText}"
                                    )
                                }) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ الذكر")
                                }
                                IconButton(onClick = {
                                    SystemActionsHelper.sharePlainText(
                                        context,
                                        dhikr.title,
                                        "${dhikr.title}\n\n${dhikr.vocalizedText}"
                                    )
                                }) {
                                    Icon(Icons.Outlined.Share, contentDescription = "مشاركة الذكر")
                                }
                            }
                            Button(
                                onClick = {
                                    onIncrementDhikr(dhikr.id)
                                    if (hapticEnabled) {
                                        SystemActionsHelper.triggerHapticPulse(
                                            context,
                                            strongMilestone = currentCount + 1 == dhikr.targetCount
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("dhikr_increment_${dhikr.id}")
                            ) {
                                Text("تكرار الذكر (+١)")
                            }
                        }
                    }
                }
            } else {
                val targetType = if (primaryTab == 0) "DUA" else "ZIYARAT"
                val scoped = duasAndZiyarat.filter { it.entryType == targetType }
                val subCats = listOf("الكل") + scoped.map { it.category }.distinct()

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subCats) { cat ->
                            GlassFilterChip(
                                label = cat,
                                selected = selectedSubCat == cat,
                                onClick = { selectedSubCat = cat }
                            )
                        }
                    }
                }

                val filteredItems = scoped.filter { item ->
                    (selectedSubCat == "الكل" || item.category == selectedSubCat) &&
                        (searchQuery.isBlank() || item.title.contains(searchQuery) || item.vocalizedText.contains(searchQuery))
                }

                items(filteredItems, key = { it.id }) { item ->
                    GlassCard(
                        cornerRadius = 20.dp,
                        onClick = { openedItem = item },
                        modifier = Modifier.testTag("dua_ziyarat_card_${item.id}")
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.vocalizedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SourceCitationBadge(
                            attribution = item.attribution,
                            sourceBook = item.sourceBook,
                            volumeOrGrading = item.recommendedTime
                        )
                    }
                }
            }
        }
    }
}
