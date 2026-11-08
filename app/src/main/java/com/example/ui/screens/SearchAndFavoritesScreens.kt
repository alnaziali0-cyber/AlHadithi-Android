package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FavoriteBookmarkEntity
import com.example.data.local.ReadingHistoryEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SubScreenTopBar
import com.example.ui.viewmodel.AiScholarState
import com.example.ui.viewmodel.UnifiedSearchResults

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedSearchAndScholarScreen(
    searchResults: UnifiedSearchResults,
    aiScholarState: AiScholarState,
    hijriOffsetDays: Int,
    onBack: () -> Unit,
    onExecuteSearch: (String) -> Unit,
    onAskAiScholar: (String) -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    BackHandler { onBack() }
    var searchInput by remember { mutableStateOf(searchResults.query) }
    var scholarQuestion by remember { mutableStateOf("") }

    val suggestedScholarPrompts = remember {
        listOf(
            "اشرح دلالة حديث الثقلين وعلاقته بالقرآن الكريم في المصادر الإمامية",
            "ما الفرق المنهجي بين أصول الدين وفروع الدين في مدرسة أهل البيت؟",
            "بيّن فلسفة الدعاء في الصحيفة السجادية ومقاصد رسالة الحقوق"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "البحث الشامل والباحث العلمي المتعمق",
            subtitle = "بحث فوري في الموسوعة + تفكير علمي عميق موثق بالمصادر",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("unified_search_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Unified Local + Quran Search Box
            item {
                GlassCard {
                    Text(
                        text = "البحث الشامل في نصوص التطبيق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unified_search_input")
                        )
                        Button(
                            onClick = { onExecuteSearch(searchInput) },
                            modifier = Modifier.testTag("unified_search_submit")
                        ) {
                            Text("بحث")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يبحث في القرآن الكريم، الحديث، الأدعية، الزيارات، الأذكار، الكتب، والمناسبات (تصحيح التاريخ: $hijriOffsetDays يوم).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (searchResults.isSearching) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("جارٍ البحث في جميع الأقسام…")
                    }
                }
            } else if (searchResults.query.isNotBlank()) {
                item {
                    Text(
                        text = "إجمالي النتائج لـ «${searchResults.query}»: ${searchResults.totalHits}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (searchResults.ayahs.isNotEmpty()) {
                    item { SectionHeaderRow("القرآن الكريم (${searchResults.ayahs.size})") }
                    items(searchResults.ayahs.take(8)) { a ->
                        GlassCard(
                            cornerRadius = 16.dp,
                            onClick = { onOpenSurah(a.surahNumber) }
                        ) {
                            Text("${a.surahNameArabic} · الآية ${a.ayahNumberInSurah}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("﴿ ${a.uthmaniText} ﴾")
                        }
                    }
                }

                if (searchResults.hadiths.isNotEmpty()) {
                    item { SectionHeaderRow("الحديث والروايات (${searchResults.hadiths.size})") }
                    items(searchResults.hadiths) { h ->
                        GlassCard(cornerRadius = 16.dp) {
                            Text(h.infallibleName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(h.arabicText)
                            Text("المصدر: ${h.sourceBook} (${h.volumeAndPage})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (searchResults.duasAndZiyarat.isNotEmpty()) {
                    item { SectionHeaderRow("الأدعية والزيارات (${searchResults.duasAndZiyarat.size})") }
                    items(searchResults.duasAndZiyarat) { dz ->
                        GlassCard(cornerRadius = 16.dp) {
                            Text(dz.title, fontWeight = FontWeight.Bold)
                            Text(dz.vocalizedText, maxLines = 3)
                            Text(dz.sourceBook, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (searchResults.books.isNotEmpty()) {
                    item { SectionHeaderRow("المكتبة والسيرة (${searchResults.books.size})") }
                    items(searchResults.books) { b ->
                        GlassCard(cornerRadius = 16.dp) {
                            Text(b.title, fontWeight = FontWeight.Bold)
                            Text("${b.author} — ${b.summary}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (searchResults.occasions.isNotEmpty()) {
                    item { SectionHeaderRow("المناسبات (${searchResults.occasions.size})") }
                    items(searchResults.occasions) { occ ->
                        GlassCard(cornerRadius = 16.dp) {
                            Text(occ.title, fontWeight = FontWeight.Bold)
                            Text(occ.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // 2. High-Thinking Scholarly Research Assistant (gemini-4-argon + ThinkingLevel.HIGH)
            item {
                GlassCard(
                    borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = stringResource(R.string.ai_scholar_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.ai_scholar_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedScholarPrompts.forEach { sample ->
                            GlassFilterChip(
                                label = sample.take(36) + "…",
                                selected = false,
                                onClick = {
                                    scholarQuestion = sample
                                    onAskAiScholar(sample)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = scholarQuestion,
                        onValueChange = { scholarQuestion = it },
                        placeholder = { Text("اطرح سؤالاً عقائدياً أو تاريخياً أو منهجياً للشرح والتحليل العميق…") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_scholar_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onAskAiScholar(scholarQuestion) },
                        enabled = aiScholarState !is AiScholarState.Thinking && scholarQuestion.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_scholar_ask_button")
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحليل عميق بالمصادر")
                    }

                    when (aiScholarState) {
                        is AiScholarState.Idle -> Unit
                        is AiScholarState.Thinking -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                Text(
                                    text = "يفكر الباحث العلمي بعمق (ThinkingLevel.HIGH) ويراجع المراجع المعتمدة…",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        is AiScholarState.Success -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "الإجابة التحليلية الموثقة:",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = aiScholarState.answer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        is AiScholarState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = aiScholarState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesAndHistoryScreen(
    favorites: List<FavoriteBookmarkEntity>,
    history: List<ReadingHistoryEntity>,
    onBack: () -> Unit,
    onRemoveFavorite: (FavoriteBookmarkEntity) -> Unit
) {
    BackHandler { onBack() }
    var selectedType by remember { mutableStateOf("ALL") }

    val filterTabs = listOf(
        "ALL" to "الكل (${favorites.size})",
        "AYAH" to "الآيات",
        "HADITH" to "الأحاديث",
        "DUA" to "الأدعية والزيارات",
        "BOOK" to "الكتب",
        "AUDIO" to "الصوتيات",
        "WALLPAPER" to "الخلفيات"
    )

    val filtered = remember(favorites, selectedType) {
        when (selectedType) {
            "ALL" -> favorites
            "DUA" -> favorites.filter { it.itemType == "DUA" || it.itemType == "ZIYARAT" }
            else -> favorites.filter { it.itemType == selectedType }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "المفضلة وسجل القراءة",
            subtitle = "محفوظة محلياً على جهازك بالكامل — دون الحاجة لإنشاء حساب",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("favorites_history_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterTabs) { (key, label) ->
                        GlassFilterChip(
                            label = label,
                            selected = selectedType == key,
                            onClick = { selectedType = key }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "لا توجد محفوظات في هذا القسم حالياً.",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط أيقونة الحفظ بجانب الآيات أو الأحاديث أو الأدعية أو الكتب لإضافتها إلى مفضلتك المحلية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { fav ->
                    GlassCard(cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fav.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(fav.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(fav.contentSnippet, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                            }
                            IconButton(onClick = { onRemoveFavorite(fav) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "إزالة من المفضلة")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeaderRow(title = "سجل القراءة والاستماع الأخير (${history.size})")
            }

            if (history.isEmpty()) {
                item {
                    GlassCard {
                        Text("لم تبدأ القراءة أو الاستماع بعد.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(history.take(15), key = { it.id }) { h ->
                    GlassCard(cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(h.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(
                                h.positionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
