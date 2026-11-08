package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CachedAyahEntity
import com.example.domain.SurahCatalog
import com.example.domain.SurahMeta
import com.example.domain.filterSurahs
import com.example.media.AudioPlayerController
import com.example.ui.components.GlassCard
import com.example.ui.components.QuranAyahCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SourceCitationBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalIsDarkThemeActive
import com.example.ui.theme.LocalLineSpacing
import com.example.ui.theme.LocalReadingFontScale
import com.example.ui.theme.QuranBackgroundStyle
import com.example.ui.theme.QuranReadingLayout
import com.example.ui.theme.ReadingFontChoice
import com.example.ui.theme.quranAndHeritageStyle
import com.example.ui.theme.readerBackdropFor
import com.example.util.SystemActionsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranIndexAndReaderScreen(
    activeSurahNumber: Int?,
    activeSurahAyahs: List<CachedAyahEntity>,
    isLoadingSurah: Boolean,
    surahError: String?,
    lastSurahNumber: Int,
    lastSurahName: String,
    lastAyahNumber: Int,
    selectedReciterId: String,
    darkMode: Boolean,
    readingFontChoice: ReadingFontChoice,
    quranBackground: QuranBackgroundStyle,
    readingLayout: QuranReadingLayout,
    autoScrollEnabled: Boolean,
    autoScrollSpeed: Float,
    onSelectSurahToRead: (Int) -> Unit,
    onCloseReaderToIndex: () -> Unit,
    onPlaySurahAudio: (Int) -> Unit,
    onSelectReciter: (String) -> Unit,
    onChangeFontScale: (Float) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangeReadingFont: (ReadingFontChoice) -> Unit,
    onChangeBackground: (QuranBackgroundStyle) -> Unit,
    onChangeReadingLayout: (QuranReadingLayout) -> Unit,
    onChangeAutoScroll: (Boolean, Float) -> Unit,
    onSaveLastReadAyah: (Int, Int) -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    if (activeSurahNumber != null) {
        SurahReaderView(
            surahNumber = activeSurahNumber,
            reciterId = selectedReciterId,
            ayahs = activeSurahAyahs,
            isLoading = isLoadingSurah,
            errorMsg = surahError,
            darkMode = darkMode,
            readingFontChoice = readingFontChoice,
            quranBackground = quranBackground,
            readingLayout = readingLayout,
            autoScrollEnabled = autoScrollEnabled,
            autoScrollSpeed = autoScrollSpeed,
            onBack = onCloseReaderToIndex,
            onPlayAudio = { onPlaySurahAudio(activeSurahNumber) },
            onRetry = { onSelectSurahToRead(activeSurahNumber) },
            onChangeFontScale = onChangeFontScale,
            onToggleDarkMode = onToggleDarkMode,
            onChangeReadingFont = onChangeReadingFont,
            onChangeBackground = onChangeBackground,
            onChangeReadingLayout = onChangeReadingLayout,
            onChangeAutoScroll = onChangeAutoScroll,
            onSaveLastReadAyah = onSaveLastReadAyah,
            onSaveFavorite = onSaveFavorite
        )
        return
    }

    var filterQuery by remember { mutableStateOf("") }
    var tabIndex by remember { mutableIntStateOf(0) } // 0=السور, 1=الأجزاء, 2=الأحزاب, 3=العزائم والسجدات

    val filteredSurahs = remember(filterQuery, tabIndex) {
        filterSurahs(
            surahs = if (tabIndex == 3) {
                SurahCatalog.allSurahs.filter { it.hasSajdah }
            } else {
                SurahCatalog.allSurahs
            },
            query = filterQuery,
            onlySajdah = false
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "القرآن الكريم والتفسير",
            subtitle = "١١٤ سورة · الرسم العثماني · التلاوة والتفسير"
        )

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = "طريق الرواية المذكور في كتب القراءات: حفص عن عاصم، عن أبي عبد الرحمن السلمي، عن أمير المؤمنين علي بن أبي طالب (ع).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "نص السورة يُجلب بالرسم العثماني من خدمة Al Quran Cloud عند فتحها لأول مرة، ثم يُحفظ على جهازك وتبقى متاحة دون إنترنت. السور التي لم تُفتح بعد تحتاج إلى اتصال. النص معروض كما ورد دون تعديل أو إعادة صياغة.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("quran_index_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Last Reading Card
            item {
                GlassCard(
                    cornerRadius = 18.dp,
                    onClick = { onSelectSurahToRead(lastSurahNumber) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "آخر موضع قراءة محفوظ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$lastSurahName — الآية $lastAyahNumber",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(onClick = { onSelectSurahToRead(lastSurahNumber) }) {
                            Text("متابعة")
                        }
                    }
                }
            }

            // Reciter Selector Strip
            item {
                Column {
                    Text(
                        text = "القارئ المعتمد للتلاوة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SurahCatalog.reciters) { reciter ->
                            GlassFilterChip(
                                label = reciter.arabicName,
                                selected = reciter.id == selectedReciterId,
                                onClick = { onSelectReciter(reciter.id) }
                            )
                        }
                    }
                    when (selectedReciterId) {
                        "hamed-shakernejad" -> Text(
                            text = "تتوفر تلاوات هذا القارئ بصيغة ٣٠ جزءاً؛ تشغيل سورة يفتح الجزء الذي تبدأ فيه السورة. التنزيل متاح من صفحة القراءة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        "shahriar-parhizgar", "karim-mansouri" -> Text(
                            text = "المصدر الحالي صفحة القارئ الخارجية؛ اضغط تشغيل لفتحها. لا توجد روابط MP3 مباشرة موثقة ضمن الفهرس حالياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Surah Field
            item {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("ابحث باسم السورة أو رقمها (١ - ١١٤)…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_surah_search_input")
                )
            }

            // View Mode Tabs
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf("السور (١١٤)", "الأجزاء (٣٠)", "الأحزاب (٦٠)", "آيات السجدة والعزائم")
                    items(tabs.size) { idx ->
                        GlassFilterChip(
                            label = tabs[idx],
                            selected = tabIndex == idx,
                            onClick = { tabIndex = idx }
                        )
                    }
                }
            }

            when (tabIndex) {
                1 -> {
                    items((1..30).toList()) { juzNum ->
                        val firstSurahInJuz = SurahCatalog.allSurahs.firstOrNull { it.juzStart == juzNum }
                            ?: SurahCatalog.allSurahs.lastOrNull { it.juzStart <= juzNum }
                            ?: SurahCatalog.allSurahs.first()
                        GlassCard(
                            cornerRadius = 18.dp,
                            onClick = { onSelectSurahToRead(firstSurahInJuz.number) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الجزء $juzNum",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "يبدأ عند ${firstSurahInJuz.arabicName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                2 -> {
                    items((1..60).toList()) { hizbNum ->
                        val surahForHizb = SurahCatalog.allSurahs.lastOrNull { it.hizbStart <= hizbNum }
                            ?: SurahCatalog.allSurahs.first()
                        GlassCard(
                            cornerRadius = 18.dp,
                            onClick = { onSelectSurahToRead(surahForHizb.number) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الحزب $hizbNum (الجزء ${(hizbNum + 1) / 2})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = surahForHizb.arabicName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                else -> {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        SurahRowCard(
                            surah = surah,
                            onReadClick = { onSelectSurahToRead(surah.number) },
                            onPlayClick = { onPlaySurahAudio(surah.number) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahRowCard(
    surah: SurahMeta,
    onReadClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onReadClick,
        modifier = Modifier.testTag("surah_card_${surah.number}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = surah.arabicName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (surah.isWajibSajdahShia) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "سجدة واجبة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${surah.revelationTypeArabic} · ${surah.ayahCount} آية · الجزء ${surah.juzStart}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = "استماع إلى ${surah.arabicName}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SurahReaderView(
    surahNumber: Int,
    reciterId: String,
    ayahs: List<CachedAyahEntity>,
    isLoading: Boolean,
    errorMsg: String?,
    darkMode: Boolean,
    readingFontChoice: ReadingFontChoice,
    quranBackground: QuranBackgroundStyle,
    readingLayout: QuranReadingLayout,
    autoScrollEnabled: Boolean,
    autoScrollSpeed: Float,
    onBack: () -> Unit,
    onPlayAudio: () -> Unit,
    onRetry: () -> Unit,
    onChangeFontScale: (Float) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangeReadingFont: (ReadingFontChoice) -> Unit,
    onChangeBackground: (QuranBackgroundStyle) -> Unit,
    onChangeReadingLayout: (QuranReadingLayout) -> Unit,
    onChangeAutoScroll: (Boolean, Float) -> Unit,
    onSaveLastReadAyah: (Int, Int) -> Unit,
    onSaveFavorite: (String, String, String, String, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerState by AudioPlayerController.state.collectAsState()
    val meta = SurahCatalog.byNumber(surahNumber)
    val reciter = remember(reciterId) { SurahCatalog.reciterById(reciterId) }
    val downloadKey = reciter.audioDownloadKey(surahNumber)
        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val downloadUrl = reciter.audioDownloadUrl(surahNumber)
    val isSurahDownloading = downloadKey in playerState.downloadingKeys
    val isSurahDownloaded = downloadKey in playerState.downloadedKeys
    val downloadProgress = playerState.downloadProgress[downloadKey] ?: -1f
    val fontScale = LocalReadingFontScale.current
    val lineSpacing = LocalLineSpacing.current
    val scrollState = rememberScrollState()
    var quranTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var quranTextTopPx by remember { mutableIntStateOf(0) }
    val readerTopPaddingPx = with(LocalDensity.current) { 14.dp.roundToPx() }
    val isDarkReader = LocalIsDarkThemeActive.current
    val backdrop = remember(quranBackground, isDarkReader) { readerBackdropFor(quranBackground, isDarkReader) }
    val accentColor = MaterialTheme.colorScheme.primary
    val sortedAyahs = remember(ayahs) { ayahs.sortedBy { it.ayahNumberInSurah } }
    val quranContent = remember(sortedAyahs, accentColor) {
        buildQuranContinuousText(
            ayahs = sortedAyahs.map { it.ayahNumberInSurah to it.uthmaniText },
            markerColor = accentColor
        )
    }

    var selectedAyahForTafsir by remember { mutableStateOf<CachedAyahEntity?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }
    var showReaderSettings by remember { mutableStateOf(false) }
    var localAutoScroll by remember { mutableStateOf(autoScrollEnabled) }

    // The auto scroll pauses as soon as the reader drags the text by hand and resumes afterwards.
    val isUserDragging by scrollState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(localAutoScroll, autoScrollSpeed, isUserDragging, sortedAyahs.size) {
        if (!localAutoScroll || isUserDragging) return@LaunchedEffect
        val stepPx = (2.2f * autoScrollSpeed.coerceIn(0.25f, 3f)).toInt().coerceAtLeast(1)
        while (scrollState.value < scrollState.maxValue) {
            delay(40L)
            scrollState.scrollBy(stepPx.toFloat())
        }
    }

    LaunchedEffect(autoScrollEnabled) {
        localAutoScroll = autoScrollEnabled
    }

    LaunchedEffect(surahNumber) {
        scrollState.scrollTo(0)
        quranTextLayout = null
        quranTextTopPx = 0
    }

    LaunchedEffect(surahNumber, ayahs.size) {
        if (ayahs.isNotEmpty()) {
            onSaveLastReadAyah(surahNumber, 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = meta.arabicName,
            subtitle = "${meta.revelationTypeArabic} · ${meta.ayahCount} آية · الجزء ${meta.juzStart}",
            onBack = onBack,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // التبديل بين عرض البطاقات (كما في الصورة المرجعية) والنص المتصل.
                    IconButton(
                        onClick = {
                            onChangeReadingLayout(
                                if (readingLayout == QuranReadingLayout.CARDS) {
                                    QuranReadingLayout.CONTINUOUS
                                } else {
                                    QuranReadingLayout.CARDS
                                }
                            )
                        },
                        modifier = Modifier.testTag("quran_layout_toggle")
                    ) {
                        Icon(
                            imageVector = if (readingLayout == QuranReadingLayout.CARDS) {
                                Icons.Outlined.ViewAgenda
                            } else {
                                Icons.Outlined.Notes
                            },
                            contentDescription = if (readingLayout == QuranReadingLayout.CARDS) {
                                "عرض النص متصلاً"
                            } else {
                                "عرض الآيات في بطاقات"
                            }
                        )
                    }
                    IconButton(onClick = { onChangeFontScale(fontScale - 0.10f) }) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = "تصغير الخط")
                    }
                    IconButton(onClick = { onChangeFontScale(fontScale + 0.10f) }) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = "تكبير الخط")
                    }
                    IconButton(onClick = { onToggleDarkMode(!darkMode) }) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = "الوضع الليلي")
                    }
                    IconButton(
                        onClick = {
                            localAutoScroll = !localAutoScroll
                            onChangeAutoScroll(localAutoScroll, autoScrollSpeed)
                        },
                        modifier = Modifier.testTag("quran_auto_scroll_toggle")
                    ) {
                        Icon(
                            imageVector = if (localAutoScroll) {
                                Icons.Outlined.PauseCircle
                            } else {
                                Icons.Outlined.PlayCircle
                            },
                            contentDescription = if (localAutoScroll) {
                                "إيقاف التمرير التلقائي"
                            } else {
                                "تشغيل التمرير التلقائي"
                            },
                            tint = if (localAutoScroll) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = { showReaderSettings = true },
                        modifier = Modifier.testTag("quran_reader_settings_button")
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "إعدادات القراءة")
                    }
                    IconButton(onClick = onPlayAudio) {
                        Icon(
                            Icons.Outlined.PlayCircle,
                            contentDescription = "تشغيل التلاوة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    when {
                        isSurahDownloading -> {
                            if (downloadProgress >= 0f) {
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            IconButton(onClick = { AudioPlayerController.cancelDownload(downloadKey) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "إلغاء تنزيل السورة")
                            }
                        }
                        isSurahDownloaded -> {
                            IconButton(onClick = {
                                val deleted = AudioPlayerController.deleteDownloadedTrack(downloadKey)
                                Toast.makeText(
                                    context,
                                    if (deleted) "تم حذف تنزيل السورة" else "تعذر حذف تنزيل السورة",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "حذف تنزيل السورة",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            if (downloadUrl != null) {
                                IconButton(onClick = {
                                    AudioPlayerController.downloadTrackForOffline(
                                        idKey = downloadKey,
                                        streamUrl = downloadUrl
                                    ) { _, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        Icons.Outlined.CloudDownload,
                                        contentDescription = if (reciter.audioUnit == "juz") "تنزيل الجزء للاستماع دون إنترنت" else "تنزيل السورة للاستماع دون إنترنت",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "اضغط على أي آية لعرض التفسير الموثق أو الحفظ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { showJumpDialog = true }) {
                Text("انتقال لآية…")
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("جارٍ تحميل النص القرآني بالرسم العثماني…")
                    }
                }
            }

            errorMsg != null && ayahs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.padding(24.dp)) {
                        Text(errorMsg, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("إعادة المحاولة") }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backdrop.background)
                        .verticalScroll(scrollState)
                        .testTag("quran_surah_continuous_page")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        // Basmala is kept as a heading, never inside a decorative card.
                        if (surahNumber != 1 && surahNumber != 9) {
                            Text(
                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                style = quranAndHeritageStyle(
                                    fontScale,
                                    fontChoice = readingFontChoice,
                                    lineSpacing = lineSpacing
                                ).copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
                            )
                        }

                        if (sortedAyahs.isEmpty()) {
                            Text(
                                text = "لم يصل نص السورة بعد. تحقق من اتصال الإنترنت ثم أعد المحاولة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = backdrop.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onRetry) { Text("إعادة تحميل السورة") }
                        } else if (readingLayout == QuranReadingLayout.CARDS) {
                            // عرض البطاقات: يطابق الصورة المرجعية — بطاقة لكل آية.
                            val cardContainer = if (isDarkReader) {
                                Color(0xFF16211F).copy(alpha = 0.72f)
                            } else {
                                Color.White.copy(alpha = 0.94f)
                            }
                            val cardBorder = if (isDarkReader) {
                                Color.White.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                sortedAyahs.forEach { ayah ->
                                    QuranAyahCard(
                                        ayahNumber = ayah.ayahNumberInSurah,
                                        ayahText = ayah.uthmaniText,
                                        textStyle = quranAndHeritageStyle(
                                            fontScale,
                                            fontChoice = readingFontChoice,
                                            lineSpacing = lineSpacing
                                        ).copy(color = backdrop.onBackground),
                                        containerColor = cardContainer,
                                        borderColor = cardBorder,
                                        numberColor = backdrop.markerColor,
                                        textColor = backdrop.onBackground,
                                        onClick = {
                                            selectedAyahForTafsir = ayah
                                            onSaveLastReadAyah(surahNumber, ayah.ayahNumberInSurah)
                                        }
                                    )
                                }
                            }
                        } else {
                            ClickableText(
                                text = quranContent.annotatedText,
                                style = quranAndHeritageStyle(
                                    fontScale,
                                    fontChoice = readingFontChoice,
                                    lineSpacing = lineSpacing
                                ).copy(
                                    color = backdrop.onBackground,
                                    textAlign = TextAlign.Justify
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quran_continuous_text")
                                    .onGloballyPositioned { coordinates ->
                                        quranTextTopPx = coordinates.positionInParent().y.toInt()
                                    },
                                onTextLayout = { quranTextLayout = it },
                                onClick = { offset ->
                                    val ayahNumber = quranContent.annotatedText
                                        .getStringAnnotations(
                                            tag = QURAN_AYAH_ANNOTATION_TAG,
                                            start = offset,
                                            end = offset
                                        )
                                        .firstOrNull()
                                        ?.item
                                        ?.toIntOrNull()
                                    val selected = sortedAyahs.firstOrNull {
                                        it.ayahNumberInSurah == ayahNumber
                                    }
                                    if (selected != null) {
                                        selectedAyahForTafsir = selected
                                        onSaveLastReadAyah(surahNumber, selected.ayahNumberInSurah)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReaderSettings) {
        ModalBottomSheet(
            onDismissRequest = { showReaderSettings = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("quran_reader_settings_sheet")
            ) {
                Text(
                    text = "إعدادات القراءة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تُحفظ هذه الخيارات محلياً وتُطبَّق فوراً على القارئ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("خط النصوص القرآنية:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingFontChoice.entries.forEach { choice ->
                        GlassFilterChip(
                            label = choice.arabicLabel,
                            selected = readingFontChoice == choice,
                            onClick = { onChangeReadingFont(choice) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("خلفية الصفحة:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuranBackgroundStyle.entries.forEach { style ->
                        GlassFilterChip(
                            label = style.arabicLabel,
                            selected = quranBackground == style,
                            onClick = { onChangeBackground(style) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التمرير التلقائي أثناء التلاوة", style = MaterialTheme.typography.labelLarge)
                    androidx.compose.material3.Switch(
                        checked = localAutoScroll,
                        onCheckedChange = { checked ->
                            localAutoScroll = checked
                            onChangeAutoScroll(checked, autoScrollSpeed)
                        },
                        modifier = Modifier.testTag("quran_auto_scroll_switch")
                    )
                }
                if (localAutoScroll) {
                    Text(
                        text = "سرعة التمرير: ${"%.1f".format(java.util.Locale.US, autoScrollSpeed)}×",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = autoScrollSpeed,
                        onValueChange = { onChangeAutoScroll(localAutoScroll, it) },
                        valueRange = 0.25f..3.0f,
                        modifier = Modifier.testTag("quran_auto_scroll_speed")
                    )
                    Text(
                        text = "يتوقف التمرير تلقائياً عند تحريك النص يدوياً، ويُستأنف بعد رفع الإصبع.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "مصدر النص: خدمة Al Quran Cloud بالرسم العثماني، ويُحفظ محلياً بعد فتح السورة. النص لا يُعاد صياغته داخل التطبيق.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("الانتقال إلى رقم الآية") },
            text = {
                OutlinedTextField(
                    value = jumpInput,
                    onValueChange = { jumpInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("رقم الآية (١ - ${meta.ayahCount})") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = jumpInput.toIntOrNull()?.coerceIn(1, meta.ayahCount) ?: 1
                    showJumpDialog = false
                    onSaveLastReadAyah(surahNumber, target)
                    val textOffset = quranContent.ayahStartOffsets[target]
                    val layout = quranTextLayout
                    if (textOffset != null && layout != null) {
                        val line = layout.getLineForOffset(textOffset)
                        val targetScrollPx = (
                            readerTopPaddingPx + quranTextTopPx + layout.getLineTop(line)
                        ).toInt()
                        scope.launch {
                            scrollState.animateScrollTo(targetScrollPx.coerceAtLeast(0))
                        }
                    }
                }) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (selectedAyahForTafsir != null) {
        val a = selectedAyahForTafsir!!
        ModalBottomSheet(
            onDismissRequest = { selectedAyahForTafsir = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "${a.surahNameArabic} · الآية ${a.ayahNumberInSurah}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "﴿ ${a.uthmaniText} ﴾",
                    style = quranAndHeritageStyle(
                        fontScale * 0.95f,
                        fontChoice = readingFontChoice,
                        lineSpacing = lineSpacing
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "بيان التفسير والمعاني:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = a.tafsirSummary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                SourceCitationBadge(
                    attribution = "التفسير الإمامي المعتمد",
                    sourceBook = a.tafsirSource.ifBlank { "الميزان للطباطبائي · مجمع البيان للطبرسي" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        onSaveFavorite(
                            "AYAH",
                            "ayah_${a.surahNumber}_${a.ayahNumberInSurah}",
                            "${meta.arabicName} — الآية ${a.ayahNumberInSurah}",
                            "القرآن الكريم",
                            a.uthmaniText
                        )
                        Toast.makeText(context, "تم حفظ الآية", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ")
                    }
                    TextButton(onClick = {
                        SystemActionsHelper.copyToClipboard(
                            context,
                            meta.arabicName,
                            "﴿ ${a.uthmaniText} ﴾ [${meta.arabicName}: ${a.ayahNumberInSurah}]"
                        )
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ")
                    }
                    TextButton(onClick = {
                        SystemActionsHelper.sharePlainText(
                            context,
                            meta.arabicName,
                            "﴿ ${a.uthmaniText} ﴾\n[${meta.arabicName}: ${a.ayahNumberInSurah}]"
                        )
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
