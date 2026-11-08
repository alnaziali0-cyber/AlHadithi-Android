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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.BookEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SourceCitationBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalLineSpacing
import com.example.ui.theme.LocalReadingFontChoice
import com.example.ui.theme.LocalReadingFontScale
import com.example.ui.theme.heritageReadingStyle
import com.example.util.SystemActionsHelper

private data class ParsedChapter(val title: String, val body: String)

private fun parseBookChapters(delimited: String): List<ParsedChapter> {
    if (delimited.isBlank()) return listOf(ParsedChapter("المتن", "لا يوجد نص مفصل بعد."))
    return delimited.split("||").mapNotNull { chunk ->
        val idx = chunk.indexOf("::")
        if (idx == -1) ParsedChapter("فصل", chunk.trim())
        else ParsedChapter(chunk.substring(0, idx).trim(), chunk.substring(idx + 2).trim())
    }
}

@Composable
fun LibraryAndBookReaderScreen(
    books: List<BookEntity>,
    onUpdateProgress: (BookEntity, Int, Int) -> Unit,
    onChangeFontScale: (Float) -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    var openedBook by remember { mutableStateOf<BookEntity?>(null) }

    if (openedBook != null) {
        val currentBook = books.firstOrNull { it.id == openedBook!!.id } ?: openedBook!!
        BookReaderView(
            book = currentBook,
            onBack = { openedBook = null },
            onUpdateProgress = onUpdateProgress,
            onChangeFontScale = onChangeFontScale,
            onSaveFavorite = onSaveFavorite
        )
        return
    }

    var sectionFilter by remember { mutableStateOf("ALL") } // ALL, LIBRARY, ENCYCLOPEDIA
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchText by remember { mutableStateOf("") }

    val categories = remember(books, sectionFilter) {
        val scoped = if (sectionFilter == "ALL") books else books.filter { it.sectionType == sectionFilter }
        listOf("الكل") + scoped.map { it.category }.distinct()
    }

    val filteredBooks = remember(books, sectionFilter, selectedCategory, searchText) {
        books.filter { b ->
            val secMatch = sectionFilter == "ALL" || b.sectionType == sectionFilter
            val catMatch = selectedCategory == "الكل" || b.category == selectedCategory
            val qMatch = searchText.isBlank() ||
                b.title.contains(searchText, ignoreCase = true) ||
                b.author.contains(searchText, ignoreCase = true) ||
                b.summary.contains(searchText, ignoreCase = true)
            secMatch && catMatch && qMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "المكتبة الإسلامية وموسوعة السيرة",
            subtitle = "أمهات الكتب · نهج البلاغة · السيرة والمقاتل · تاريخ العتبات"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("library_books_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassFilterChip(
                        label = "الكل (${books.size})",
                        selected = sectionFilter == "ALL",
                        onClick = {
                            sectionFilter = "ALL"
                            selectedCategory = "الكل"
                        }
                    )
                    GlassFilterChip(
                        label = "المكتبة العلمية",
                        selected = sectionFilter == "LIBRARY",
                        onClick = {
                            sectionFilter = "LIBRARY"
                            selectedCategory = "الكل"
                        }
                    )
                    GlassFilterChip(
                        label = "السيرة والمقاتل والعتبات",
                        selected = sectionFilter == "ENCYCLOPEDIA",
                        onClick = {
                            sectionFilter = "ENCYCLOPEDIA"
                            selectedCategory = "الكل"
                        }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("ابحث في عناوين الكتب أو المؤلفين…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

            items(filteredBooks, key = { it.id }) { book ->
                GlassCard(
                    cornerRadius = 20.dp,
                    onClick = { openedBook = book },
                    modifier = Modifier.testTag("book_card_${book.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = book.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = if (book.isDownloadedOffline) "متاح دون إنترنت ✓" else book.eraLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${book.author} · ${book.eraLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = book.summary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (book.readingProgressPercent > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { book.readingProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "نسبة القراءة المحفوظة: ${book.readingProgressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { openedBook = book }) {
                            Text(if (book.readingProgressPercent > 0) "متابعة القراءة" else "قراءة الكتاب")
                        }
                        IconButton(onClick = {
                            onSaveFavorite(
                                "BOOK",
                                "book_${book.id}",
                                book.title,
                                book.author,
                                book.summary
                            )
                        }) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "حفظ الكتاب في المفضلة")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookReaderView(
    book: BookEntity,
    onBack: () -> Unit,
    onUpdateProgress: (BookEntity, Int, Int) -> Unit,
    onChangeFontScale: (Float) -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val fontScale = LocalReadingFontScale.current
    val chapters = remember(book.chaptersDelimited) { parseBookChapters(book.chaptersDelimited) }
    var currentIdx by remember(book.id) {
        mutableIntStateOf(book.lastReadChapterIndex.coerceIn(0, chapters.lastIndex))
    }
    var inBookQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentIdx) {
        onUpdateProgress(book, currentIdx, chapters.size)
    }

    val currentChapter = chapters[currentIdx]
    val matchingChapters = remember(inBookQuery, chapters) {
        if (inBookQuery.isBlank()) emptyList()
        else chapters.mapIndexedNotNull { idx, ch ->
            if (ch.title.contains(inBookQuery) || ch.body.contains(inBookQuery)) idx to ch else null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = book.title,
            subtitle = book.author,
            onBack = onBack,
            trailing = {
                Row {
                    IconButton(onClick = { onChangeFontScale(fontScale - 0.10f) }) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = "تصغير الخط")
                    }
                    IconButton(onClick = { onChangeFontScale(fontScale + 0.10f) }) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = "تكبير الخط")
                    }
                    IconButton(onClick = {
                        onSaveFavorite("BOOK", "book_${book.id}", book.title, book.author, currentChapter.body)
                    }) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "حفظ")
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
                OutlinedTextField(
                    value = inBookQuery,
                    onValueChange = { inBookQuery = it },
                    placeholder = { Text("بحث داخل فصول هذا الكتاب…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (matchingChapters.isNotEmpty()) {
                item {
                    GlassCard(cornerRadius = 16.dp) {
                        Text(
                            text = "نتائج البحث داخل الكتاب (${matchingChapters.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        matchingChapters.forEach { (idx, ch) ->
                            OutlinedButton(
                                onClick = {
                                    currentIdx = idx
                                    inBookQuery = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("انتقال إلى: ${ch.title}")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chapters.size) { idx ->
                        GlassFilterChip(
                            label = "${idx + 1}. ${chapters[idx].title}",
                            selected = currentIdx == idx,
                            onClick = { currentIdx = idx }
                        )
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = currentChapter.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentChapter.body,
                        style = heritageReadingStyle(
                            fontScale * 0.92f,
                            fontChoice = LocalReadingFontChoice.current,
                            lineSpacing = LocalLineSpacing.current
                        ).copy(lineHeight = 40.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SourceCitationBadge(
                        attribution = book.author,
                        sourceBook = book.sourceAttribution,
                        volumeOrGrading = "الفصل ${currentIdx + 1} من ${chapters.size}"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            SystemActionsHelper.copyToClipboard(
                                context,
                                currentChapter.title,
                                "${currentChapter.title}\n${currentChapter.body}\n— ${book.title}"
                            )
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ الفصل")
                        }
                        IconButton(onClick = {
                            SystemActionsHelper.sharePlainText(
                                context,
                                book.title,
                                "${currentChapter.title}\n\n${currentChapter.body}\n— ${book.title} (${book.author})"
                            )
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "مشاركة الفصل")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (currentIdx > 0) currentIdx-- },
                        enabled = currentIdx > 0
                    ) {
                        Text("الفصل السابق")
                    }
                    Button(
                        onClick = { if (currentIdx < chapters.lastIndex) currentIdx++ },
                        enabled = currentIdx < chapters.lastIndex
                    ) {
                        Text("الفصل التالي")
                    }
                }
            }
        }
    }
}
