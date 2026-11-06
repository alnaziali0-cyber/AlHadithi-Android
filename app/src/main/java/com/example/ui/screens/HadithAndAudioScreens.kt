package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AudioTrackEntity
import com.example.data.local.HadithEntity
import com.example.media.AudioPlayerController
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SourceCitationBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalReadingFontScale
import com.example.ui.theme.quranAndHeritageStyle
import com.example.util.SystemActionsHelper

@Composable
fun HadithEncyclopediaScreen(
    hadiths: List<HadithEntity>,
    onBack: () -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val readingScale = LocalReadingFontScale.current
    var selectedInfallible by remember { mutableStateOf("الكل") }
    var selectedTopic by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    val infallibles = remember(hadiths) {
        listOf("الكل") + hadiths.map { it.infallibleName }.distinct()
    }
    val topics = remember(hadiths) {
        listOf("الكل") + hadiths.map { it.topicCategory }.distinct()
    }

    val filtered = remember(hadiths, selectedInfallible, selectedTopic, searchQuery) {
        hadiths.filter { h ->
            (selectedInfallible == "الكل" || h.infallibleName == selectedInfallible) &&
                (selectedTopic == "الكل" || h.topicCategory == selectedTopic) &&
                (searchQuery.isBlank() ||
                    h.arabicText.contains(searchQuery) ||
                    h.sourceBook.contains(searchQuery) ||
                    h.infallibleName.contains(searchQuery))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "موسوعة الحديث والروايات",
            subtitle = "عن النبي الأكرم ﷺ وأهل البيت المعصومين (ع) — تعمل بدون إنترنت",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("hadith_encyclopedia_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في متن الحديث أو المعصوم أو الكتاب…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column {
                    Text("حسب المعصوم:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(infallibles) { name ->
                            GlassFilterChip(
                                label = name,
                                selected = selectedInfallible == name,
                                onClick = { selectedInfallible = name }
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topics) { topic ->
                        GlassFilterChip(
                            label = topic,
                            selected = selectedTopic == topic,
                            onClick = { selectedTopic = topic }
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { h ->
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = h.topicCategory,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "« ${h.arabicText} »",
                        style = quranAndHeritageStyle(readingScale * 0.92f).copy(lineHeight = 40.sp)
                    )
                    if (h.briefExplanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "إضاءة: ${h.briefExplanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SourceCitationBadge(
                        attribution = h.infallibleName,
                        sourceBook = "${h.sourceBook} — ${h.authorName}",
                        volumeOrGrading = "${h.volumeAndPage} · ${h.gradingNote}"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            AudioPlayerController.speakArabicTextOffline(
                                title = "حديث — ${h.infallibleName}",
                                arabicText = h.arabicText
                            )
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "قراءة صوتية أوفلاين")
                        }
                        IconButton(onClick = {
                            onSaveFavorite(
                                "HADITH",
                                "hadith_${h.id}",
                                h.infallibleName,
                                "${h.sourceBook} (${h.volumeAndPage})",
                                h.arabicText
                            )
                        }) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "حفظ الحديث")
                        }
                        IconButton(onClick = {
                            SystemActionsHelper.copyToClipboard(
                                context,
                                h.infallibleName,
                                "${h.arabicText}\n— ${h.infallibleName}\nالمصدر: ${h.sourceBook} (${h.volumeAndPage})"
                            )
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ")
                        }
                        IconButton(onClick = {
                            SystemActionsHelper.sharePlainText(
                                context,
                                h.infallibleName,
                                "${h.arabicText}\n— ${h.infallibleName}\nالمصدر: ${h.sourceBook} (${h.volumeAndPage})"
                            )
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "مشاركة")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioLibraryScreen(
    tracks: List<AudioTrackEntity>,
    onBack: () -> Unit,
    onPlayTrack: (AudioTrackEntity, List<AudioTrackEntity>) -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val playerState by AudioPlayerController.state.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember(tracks) {
        val base = listOf(
            "الكل",
            "اللطميات والقصائد",
            "النعي الحسيني",
            "المجالس الحسينية",
            "الأدعية والمناجاة",
            "الزيارات والأوراد",
            "القرآن الكريم",
            "العتبات المقدسة"
        )
        (base + tracks.map { it.category }).distinct()
    }

    val filtered = remember(tracks, selectedCategory, searchQuery) {
        tracks.filter { t ->
            (selectedCategory == "الكل" || t.category == selectedCategory) &&
                (searchQuery.isBlank() || t.title.contains(searchQuery) || t.reciterOrRadood.contains(searchQuery))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "الأصوات الحسينية والمكتبة الصوتية",
            subtitle = "لطميات · نعي حسيني · أدعية وزيارات · تلاوات — بث وتنزيل للمقاطع المتاحة",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("audio_library_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن لطمية، نعي، باسم الكربلائي، أباذر، أو سورة…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        GlassFilterChip(
                            label = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            leadingIcon = if (cat.contains("لطميات") || cat.contains("نعي") || cat.contains("المجالس")) {
                                Icons.Outlined.Headphones
                            } else null
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "لا توجد مقاطع مطابقة لبحثك ضمن هذا التصنيف.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { track ->
                    val idKey = "audio_${track.id}"
                    val safeKey = idKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    val isOfflineReady = AudioPlayerController.isTrackAvailableOffline(
                        idKey = idKey,
                        offlineRawResName = track.offlineRawResName
                    )
                    val isDownloading = playerState.downloadingKeys.contains(safeKey)
                    val isCurrentlyPlaying = playerState.currentTrack?.idKey == idKey && playerState.isPlaying

                    GlassCard(
                        cornerRadius = 20.dp,
                        onClick = { onPlayTrack(track, filtered) },
                        modifier = Modifier.testTag("audio_track_card_${track.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = { onPlayTrack(track, filtered) },
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrentlyPlaying) Icons.Outlined.GraphicEq else Icons.Outlined.PlayArrow,
                                    contentDescription = "تشغيل ${track.title}"
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = track.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isOfflineReady) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.OfflinePin,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "بدون إنترنت",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${track.reciterOrRadood} · ${track.durationLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isDownloading) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val progress = playerState.downloadProgress[safeKey] ?: -1f
                                    if (progress >= 0f) {
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.2.dp
                                        )
                                    }
                                    IconButton(
                                        onClick = { AudioPlayerController.cancelDownload(idKey) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Close, contentDescription = "إلغاء التنزيل")
                                    }
                                }
                            } else if (safeKey in playerState.downloadedKeys) {
                                IconButton(onClick = {
                                    val deleted = AudioPlayerController.deleteDownloadedTrack(idKey)
                                    Toast.makeText(
                                        context,
                                        if (deleted) "تم حذف النسخة المحفوظة" else "تعذر حذف الملف",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "حذف النسخة المحفوظة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else if (!isOfflineReady && track.streamUrl.startsWith("https://")) {
                                IconButton(onClick = {
                                    AudioPlayerController.downloadTrackForOffline(idKey, track.streamUrl) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudDownload,
                                        contentDescription = "حفظ للاستماع بدون إنترنت",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(onClick = {
                                onSaveFavorite(
                                    "AUDIO",
                                    idKey,
                                    track.title,
                                    track.reciterOrRadood,
                                    track.streamUrl
                                )
                            }) {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "إضافة للمفضلة")
                            }
                        }
                    }
                }
            }
        }
    }
}
