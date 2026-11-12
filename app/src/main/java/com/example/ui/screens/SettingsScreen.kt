package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.ContentSourceEntity
import com.example.data.local.DailyGoal
import com.example.data.local.UserSettingsState
import com.example.data.local.SettingsResetScope
import com.example.domain.IraqGovernorates
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerKey
import com.example.domain.QuietHours
import com.example.domain.SurahCatalog
import com.example.domain.TimeFormat
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.CardDensity
import com.example.ui.theme.LineSpacingOption
import com.example.ui.theme.QuranBackgroundStyle
import com.example.ui.theme.ReadingFontChoice
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.resolveUiFontFamily
import com.example.ui.theme.resolveReadingFontFamily
import com.example.media.AdhanPlaybackService
import com.example.domain.SupportChannels
import com.example.util.NotificationTestHelper
import kotlin.math.roundToInt

/**
 * The settings hub.
 *
 * Everything is grouped by topic, every change is applied immediately, and every option that
 * needs a runtime permission explains why it is needed before the system dialog appears.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettingsState,
    sources: List<ContentSourceEntity>,
    cachedAyahCount: Int,
    onBack: () -> Unit,
    onOpenGovernorateSheet: () -> Unit,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onChangeAccent: (AccentChoice) -> Unit,
    onChangeCardDensity: (CardDensity) -> Unit,
    onChangeUiFont: (AppFontChoice) -> Unit,
    onChangeUiFontScale: (Float) -> Unit,
    onChangeReadingFont: (ReadingFontChoice) -> Unit,
    onChangeReadingFontScale: (Float) -> Unit,
    onChangeLineSpacing: (LineSpacingOption) -> Unit,
    onChangeDailyGoals: (Set<DailyGoal>) -> Unit,
    onChangeTimeFormat: (TimeFormat) -> Unit,
    onChangeHijriOffset: (Int) -> Unit,
    onChangePrayerAlert: (PrayerKey, PrayerAlertSettings) -> Unit,
    onChangeQuietHours: (QuietHours) -> Unit,
    onChangeQuranBackground: (QuranBackgroundStyle) -> Unit,
    onChangeAutoScroll: (Boolean, Float) -> Unit,
    onChangeReciter: (String) -> Unit,
    onChangePlaybackSpeed: (Float) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onChangeDailyTasbihGoal: (Int) -> Unit,
    onUpdateNotifications: (Boolean?, Int?, Boolean?, Boolean?) -> Unit,
    onClearQuranCache: () -> Unit,
    onResetSettings: (SettingsResetScope) -> Unit,
    onDeleteUserData: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentGov = IraqGovernorates.byId(settings.governorateId)

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var showSourcesDialog by rememberSaveable { mutableStateOf(false) }
    var showPermissionsDialog by rememberSaveable { mutableStateOf(false) }
    var showFaqDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showSupportDialog by rememberSaveable { mutableStateOf(false) }
    var selectedSupportTopic by rememberSaveable { mutableStateOf(SupportTopic.TECHNICAL) }
    var supportTitle by rememberSaveable { mutableStateOf("") }
    var supportBody by rememberSaveable { mutableStateOf("") }
    var supportReference by rememberSaveable { mutableStateOf("") }
    var supportAttachment by rememberSaveable { mutableStateOf<String?>(null) }
    val supportAttachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        supportAttachment = uri?.toString()
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }
    var pendingResetScope by rememberSaveable { mutableStateOf<SettingsResetScope?>(null) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "الإعدادات",
                subtitle = "تُحفظ تفضيلاتك محلياً على جهازك وتُطبَّق فوراً",
                onBack = onBack
            )
        }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_alhadithi_logo),
                            contentDescription = "شعار الحديثي",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(66.dp).padding(5.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مركز التحكم",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "خصّص القراءة والمظهر والتنبيهات ومواقيت الصلاة من مكان واحد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "AL-HADITHI  ·  ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ---------------- المظهر ----------------
        item {
            SettingsSection(title = "المظهر") {
                Text("وضع العرض", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        GlassFilterChip(
                            label = mode.arabicLabel,
                            selected = settings.themeMode == mode,
                            onClick = { onChangeThemeMode(mode) },
                            modifier = Modifier.testTag("settings_theme_${mode.key}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("لون التمييز", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentChoice.entries.forEach { accent ->
                        GlassFilterChip(
                            label = accent.arabicLabel,
                            selected = settings.accentChoice == accent,
                            onClick = { onChangeAccent(accent) },
                            modifier = Modifier.testTag("settings_accent_${accent.key}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("كثافة البطاقات", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardDensity.entries.forEach { density ->
                        GlassFilterChip(
                            label = density.arabicLabel,
                            selected = settings.cardDensity == density,
                            onClick = { onChangeCardDensity(density) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                SettingsValueRow(
                    title = "أيقونة التطبيق",
                    value = "شعار الحديثي",
                    subtitle = "هذه هي الأيقونة المعتمدة فعلياً حالياً؛ لا توجد بدائل launcher إضافية مهيأة للاختيار."
                )
            }
        }

        // ---------------- الخط ----------------
        item {
            SettingsSection(title = "اللغة والخطوط وحجم النص") {
                SettingsValueRow(
                    title = "لغة التطبيق",
                    value = "العربية",
                    subtitle = "العربية هي اللغة المترجمة حالياً؛ لن نعرض لغات غير مكتملة كخيارات فعالة."
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )
                Text("خط الواجهة", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFontChoice.entries.forEach { choice ->
                        GlassFilterChip(
                            label = choice.arabicLabel,
                            selected = settings.fontChoice == choice,
                            onClick = { onChangeUiFont(choice) }
                        )
                    }
                }

                Text(
                    text = "معاينة خط الواجهة: الحديثي · بسم الله الرحمن الرحيم",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = resolveUiFontFamily(settings.fontChoice)
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "حجم نص الواجهة: ${(settings.uiFontScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.uiFontScale,
                    onValueChange = onChangeUiFontScale,
                    valueRange = 0.85f..1.45f,
                    modifier = Modifier.testTag("settings_ui_font_scale")
                )

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )
                Text("خط القرآن والأذكار والأدعية", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadingFontChoice.entries.forEach { choice ->
                        GlassFilterChip(
                            label = choice.arabicLabel,
                            selected = settings.readingFontChoice == choice,
                            onClick = { onChangeReadingFont(choice) }
                        )
                    }
                }

                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = resolveReadingFontFamily(settings.readingFontChoice)
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "حجم خط القراءة: ${(settings.readingFontScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.readingFontScale,
                    onValueChange = onChangeReadingFontScale,
                    valueRange = 0.85f..1.55f,
                    modifier = Modifier.testTag("settings_reading_font_scale")
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("تباعد الأسطر", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LineSpacingOption.entries.forEach { option ->
                        GlassFilterChip(
                            label = option.arabicLabel,
                            selected = settings.lineSpacing == option,
                            onClick = { onChangeLineSpacing(option) }
                        )
                    }
                }
            }
        }

        // ---------------- تخصيص المتابعة اليومية ----------------
        item {
            SettingsSection(title = "المتابعة اليومية في الرئيسية") {
                Text(
                    text = "ترتيب الرئيسية ثابت للحفاظ على وضوحها. اختر فقط مؤشرات المتابعة التي تريد إظهارها؛ لا يؤدي تغييرها إلى حذف سجل إنجازاتك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                DailyGoal.entries.forEach { goal ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(goal.arabicTitle, fontWeight = FontWeight.SemiBold)
                            Text(
                                "إظهار هذا المؤشر في بطاقة متابعة اليوم",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = goal in settings.dailyGoals,
                            onCheckedChange = { enabled ->
                                onChangeDailyGoals(
                                    if (enabled) settings.dailyGoals + goal else settings.dailyGoals - goal
                                )
                            },
                            modifier = Modifier.testTag("settings_daily_goal_${goal.key}")
                        )
                    }
                }
                Text(
                    text = if (settings.dailyGoals.isEmpty()) "لم يتم اختيار مؤشرات؛ بطاقة المتابعة مخفية حالياً." else "تم اختيار ${settings.dailyGoals.size} مؤشرات للمتابعة.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ---------------- مواقيت الصلاة ----------------
        item {
            SettingsSection(title = "مواقيت الصلاة") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المحافظة", fontWeight = FontWeight.Bold)
                        Text(
                            text = currentGov.arabicName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(onClick = onOpenGovernorateSheet) { Text("تغيير") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "مصدر المواقيت: خدمة AlAdhan بالإعدادات المخصصة المرفقة، مع حفظ نتيجة كل محافظة وتاريخ محلياً لاستخدامها دون إنترنت بعد أول جلب ناجح. لا يُستخدم حساب بديل عند غياب النتيجة أو النسخة المطابقة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("تصحيح التاريخ الهجري", style = MaterialTheme.typography.labelLarge)
                Text(
                    "التعديل يدوي بمقدار يومين كحد أقصى، ويُطبّق على صفحات التقويم والمناسبات والبحث.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChangeHijriOffset((settings.hijriOffsetDays - 1).coerceIn(-2, 2)) },
                        enabled = settings.hijriOffsetDays > -2,
                        modifier = Modifier.weight(1f)
                    ) { Text("تأخير يوم") }
                    Text(
                        text = when (settings.hijriOffsetDays) {
                            0 -> "دون تصحيح"
                            else -> if (settings.hijriOffsetDays > 0) "+${settings.hijriOffsetDays} يوم" else "${settings.hijriOffsetDays} يوم"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { onChangeHijriOffset((settings.hijriOffsetDays + 1).coerceIn(-2, 2)) },
                        enabled = settings.hijriOffsetDays < 2,
                        modifier = Modifier.weight(1f)
                    ) { Text("تقديم يوم") }
                }
                if (settings.hijriOffsetDays != 0) {
                    TextButton(
                        onClick = { onChangeHijriOffset(0) }
                    ) { Text("استعادة التصحيح الافتراضي") }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("صيغة عرض الوقت", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeFormat.entries.forEach { format ->
                        GlassFilterChip(
                            label = format.arabicLabel,
                            selected = settings.timeFormat == format,
                            onClick = { onChangeTimeFormat(format) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ضبط الدقائق لكل صلاة يُجرى من شاشة «مواقيت الصلاة»، وكذلك تنبيهات كل صلاة على حدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------------- القرآن ----------------
        item {
            SettingsSection(title = "القرآن الكريم والقراءة") {
                Text("خلفية صفحة القراءة", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranBackgroundStyle.entries.forEach { style ->
                        GlassFilterChip(
                            label = style.arabicLabel,
                            selected = settings.quranBackground == style,
                            onClick = { onChangeQuranBackground(style) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("التمرير التلقائي", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "تنزيل الآيات ببطء أثناء التلاوة، ويتوقف عند تحريك النص يدوياً.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.quranAutoScrollEnabled,
                        onCheckedChange = { onChangeAutoScroll(it, settings.quranAutoScrollSpeed) },
                        modifier = Modifier.testTag("settings_auto_scroll_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "القارئ الافتراضي: ${SurahCatalog.reciterById(settings.selectedReciterId).arabicName}",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurahCatalog.reciters.take(8).forEach { reciter ->
                        GlassFilterChip(
                            label = reciter.arabicName,
                            selected = settings.selectedReciterId == reciter.id,
                            onClick = { onChangeReciter(reciter.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "سرعة التشغيل: ${"%.2f".format(java.util.Locale.US, settings.playbackSpeed)}×",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.playbackSpeed,
                    onValueChange = onChangePlaybackSpeed,
                    valueRange = 0.75f..1.75f,
                    modifier = Modifier.testTag("settings_playback_speed")
                )
            }
        }

        // ---------------- الأذكار والتسبيح ----------------
        item {
            SettingsSection(title = "الأذكار والتسبيح") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("اهتزاز المسبحة والأذكار", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "عند التشغيل، إن كان اهتزاز الجهاز مفعّلاً في النظام.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.hapticTasbihEnabled,
                        onCheckedChange = onToggleHaptic,
                        modifier = Modifier.testTag("settings_haptic_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "هدف التسبيح اليومي: ${settings.dailyTasbihGoal}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.dailyTasbihGoal.toFloat(),
                    onValueChange = { onChangeDailyTasbihGoal(it.toInt()) },
                    valueRange = 33f..1000f,
                    steps = 28,
                    modifier = Modifier.testTag("settings_tasbih_goal")
                )
                Text(
                    text = "العدّاد اختياري تماماً، ويمكنك تصفيره أو تغييره من شاشة المسبحة.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------------- الإشعارات ----------------
        item {
            SettingsSection(title = "الإشعارات والأذان") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تنبيهات مواقيت الصلاة", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "إشعار محلي عند دخول وقت كل صلاة حسب إعداداتك.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.notifyPrayers,
                        onCheckedChange = { onUpdateNotifications(it, null, null, null) },
                        modifier = Modifier.testTag("settings_notify_prayers_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تذكير المناسبات", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = settings.notifyOccasions,
                        onCheckedChange = { onUpdateNotifications(null, null, it, null) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تذكير أذكار الصباح والمساء", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = settings.notifyAdhkar,
                        onCheckedChange = { onUpdateNotifications(null, null, null, it) }
                    )
                }
                Text(
                    "إشعارات الحديث اليومي والدعاء اليومي غير مفعّلة في هذا الإصدار؛ تحتاج خدمة محتوى وجدولة موثوقة من الخادم، لذلك لا نعرض مفاتيح تشغيل شكلية.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("إعداد مستقل لكل صلاة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "يمكن إيقاف صلاة بعينها أو تغيير وقت التنبيه والصوت والاهتزاز.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrayerKey.entries.forEachIndexed { index, prayer ->
                    val alert = settings.prayerAlertProfile.settingsFor(prayer)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(prayer.arabicName, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = alert.enabled,
                                onCheckedChange = { onChangePrayerAlert(prayer, alert.copy(enabled = it)) },
                                modifier = Modifier.testTag("settings_prayer_${prayer.key}_enabled")
                            )
                        }
                        Text("التنبيه قبل الصلاة: ${alert.leadMinutes} دقيقة", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = alert.leadMinutes.toFloat(),
                            onValueChange = {
                                onChangePrayerAlert(prayer, alert.copy(leadMinutes = it.roundToInt()))
                            },
                            valueRange = 0f..60f,
                            steps = 11,
                            enabled = alert.enabled,
                            modifier = Modifier.testTag("settings_prayer_${prayer.key}_lead")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("الصوت", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(
                                    checked = alert.soundEnabled,
                                    onCheckedChange = { onChangePrayerAlert(prayer, alert.copy(soundEnabled = it)) },
                                    enabled = alert.enabled
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("الاهتزاز", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(
                                    checked = alert.vibrateEnabled,
                                    onCheckedChange = { onChangePrayerAlert(prayer, alert.copy(vibrateEnabled = it)) },
                                    enabled = alert.enabled
                                )
                            }
                        }
                    }
                    if (index != PrayerKey.entries.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                    }
                }

                val quietHours = settings.prayerAlertProfile.quietHours
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("فترة عدم الإزعاج", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (quietHours.enabled) quietHours.formatted() else "متوقفة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = quietHours.enabled,
                        onCheckedChange = { onChangeQuietHours(quietHours.copy(enabled = it)) },
                        modifier = Modifier.testTag("settings_quiet_hours_switch")
                    )
                }
                if (quietHours.enabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> onChangeQuietHours(quietHours.copy(startMinutes = hour * 60 + minute)) },
                                    quietHours.startMinutes / 60,
                                    quietHours.startMinutes % 60,
                                    true
                                ).show()
                            }
                        ) { Text("من ${quietHours.formatted().substringBefore(" — ")}") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> onChangeQuietHours(quietHours.copy(endMinutes = hour * 60 + minute)) },
                                    quietHours.endMinutes / 60,
                                    quietHours.endMinutes % 60,
                                    true
                                ).show()
                            }
                        ) { Text("إلى ${quietHours.formatted().substringAfter(" — ")}") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("صوت الأذان المضمّن", fontWeight = FontWeight.SemiBold)
                        Text("التسجيل المرفق مع التطبيق هو الصوت الافتراضي. مستوى الصوت يتبع قناة المنبّه في Android؛ لا يغيّر التطبيق مستوى صوت الجهاز من تلقاء نفسه.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        runCatching { AdhanPlaybackService.play(context) }
                            .onSuccess { notificationMessage = "بدأ تشغيل تسجيل الأذان المضمّن." }
                            .onFailure { notificationMessage = "تعذر تشغيل الأذان؛ تحقق من إعدادات الصوت." }
                    }) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Text("اختبار")
                    }
                }
                OutlinedButton(
                    onClick = { runCatching { AdhanPlaybackService.stop(context) }; notificationMessage = "تم طلب إيقاف تشغيل الأذان." },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("إيقاف صوت الأذان") }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val posted = NotificationTestHelper.postTestNotification(context)
                        notificationMessage = if (posted) {
                            "تم إرسال إشعار تجريبي. تحقق من شريط الإشعارات."
                        } else {
                            "لم يُمنح إذن الإشعارات بعد. فعّله من إعدادات النظام ثم أعد المحاولة."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختبار الإشعار")
                }
                notificationMessage?.let { message ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ---------------- الخصوصية ----------------
        item {
            SettingsSection(title = "الخصوصية والبيانات") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "بياناتك وتفضيلاتك",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تُحفظ التفضيلات وسجل القراءة والمفضلة محلياً عبر DataStore وRoom. لا يطلب التطبيق الموقع إلا عند طلب تحديد موقعك، ولا يستخدم إذن الموقع في الخلفية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showPermissionsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("شرح الأذونات المستخدمة")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_delete_user_data")
                ) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف بيانات المستخدم", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "يحذف المفضلة وسجل القراءة وجلسات التسبيح وتقدم الدروس وسجل صلواتي اليوم والإعدادات والتنزيلات المحفوظة. تبقى المواد الموسوعية المرفقة مع التطبيق.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------------- إدارة التنزيلات والتخزين ----------------
        item {
            val importedAdhan = com.example.media.AdhanAudioStorage.currentAudioFile(context)
            SettingsSection(title = "التنزيلات والتخزين") {
                InfoRow(label = "آيات مخزنة في قاعدة البيانات", value = cachedAyahCount.toString())
                InfoRow(
                    label = "صوت الأذان المخصص",
                    value = importedAdhan?.let { formatBytes(it.length()) } ?: "الصوت الافتراضي المضمّن"
                )
                Text(
                    "لا توجد في هذه النسخة قائمة تنزيلات عامة لكل أنواع المحتوى. المتاح هنا هو كاش السور في Room وملف أذان مخصص اختاره المستخدم؛ لا نعرض ملفات افتراضية على أنها تنزيلات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showClearCacheDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("إدارة كاش القرآن المخزن") }
                Text(
                    "المسح يحتفظ بالسور الأساسية المحددة للتشغيل دون اتصال، ولا يحذف المفضلة أو سجل القراءة أو ملف الأذان.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------------- الدعم والمساعدة ----------------
        item {
            SettingsSection(title = "الدعم والمساعدة") {
                Text(
                    "تواصل مع فريق الحديثي عبر البريد أو حساباتنا الرسمية، أو استخدم نماذج البلاغات والاقتراحات أدناه.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SupportContactRow(
                    title = "البريد الإلكتروني",
                    subtitle = SupportChannels.SUPPORT_EMAIL,
                    icon = Icons.Outlined.Email,
                    onClick = { openSupportEmail(context) }
                )
                SupportContactRow(
                    title = "Instagram",
                    subtitle = SupportChannels.INSTAGRAM_HANDLE,
                    icon = Icons.Outlined.CameraAlt,
                    onClick = {
                        openSupportProfile(
                            context = context,
                            url = SupportChannels.INSTAGRAM_URL,
                            expectedHost = "www.instagram.com",
                            expectedPath = "/alhadithi_iraq/"
                        )
                    }
                )
                SupportContactRow(
                    title = "TikTok",
                    subtitle = SupportChannels.TIKTOK_HANDLE,
                    icon = Icons.Outlined.MusicNote,
                    onClick = {
                        openSupportProfile(
                            context = context,
                            url = SupportChannels.TIKTOK_URL,
                            expectedHost = "www.tiktok.com",
                            expectedPath = "/@.al.hadithi"
                        )
                    }
                )
                SettingsActionRow(
                    title = "مركز المساعدة والأسئلة الشائعة",
                    subtitle = "إرشادات استخدام الأقسام والإعدادات",
                    icon = Icons.Outlined.Info,
                    onClick = { showFaqDialog = true }
                )
                SettingsActionRow(
                    title = "التواصل مع الدعم الفني",
                    subtitle = "إعداد رسالة ومشاركتها من تطبيق تختاره",
                    icon = Icons.Outlined.Headphones,
                    onClick = {
                        selectedSupportTopic = SupportTopic.TECHNICAL
                        supportTitle = "طلب مساعدة فنية"
                        supportBody = ""
                        supportReference = ""
                        supportAttachment = null
                        showSupportDialog = true
                    }
                )
                SettingsActionRow(
                    title = "الإبلاغ عن مشكلة",
                    subtitle = "صف الخطأ وحدد موضعه في التطبيق",
                    icon = Icons.Outlined.BugReport,
                    onClick = {
                        selectedSupportTopic = SupportTopic.BUG
                        supportTitle = "بلاغ عن مشكلة"
                        supportBody = ""
                        supportReference = ""
                        supportAttachment = null
                        showSupportDialog = true
                    }
                )
                SettingsActionRow(
                    title = "اقتراح تطوير",
                    subtitle = "شارك فكرة لتحسين الحديثي",
                    icon = Icons.Outlined.Lightbulb,
                    onClick = {
                        selectedSupportTopic = SupportTopic.SUGGESTION
                        supportTitle = "اقتراح تطوير"
                        supportBody = ""
                        supportReference = ""
                        supportAttachment = null
                        showSupportDialog = true
                    }
                )
                SettingsActionRow(
                    title = "الإبلاغ عن خطأ في محتوى إسلامي",
                    subtitle = "أرفق موضع النص أو مرجعه ومصدر التصحيح",
                    icon = Icons.Outlined.MenuBook,
                    onClick = {
                        selectedSupportTopic = SupportTopic.RELIGIOUS_CONTENT
                        supportTitle = "تصحيح محتوى إسلامي"
                        supportBody = ""
                        supportReference = ""
                        supportAttachment = null
                        showSupportDialog = true
                    }
                )
                SettingsActionRow(
                    title = "سياسة الخصوصية",
                    subtitle = "ملخص ممارسات الخصوصية الحالية",
                    icon = Icons.Outlined.Security,
                    onClick = { showPrivacyDialog = true }
                )
                SettingsActionRow(
                    title = "شروط الاستخدام",
                    subtitle = "حالة وثيقة الشروط",
                    icon = Icons.Outlined.Info,
                    onClick = { showTermsDialog = true }
                )
            }
        }

        // ---------------- إعادة ضبط التفضيلات ----------------
        item {
            SettingsSection(title = "إعادة ضبط التفضيلات") {
                Text(
                    "إعادة الضبط لا تحذف المفضلة أو سجل القراءة أو العدّادات أو التنزيلات أو المحافظة المحفوظة. حذف بيانات المستخدم عملية مستقلة أدناه.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    SettingsResetScope.APPEARANCE to "إعادة ضبط المظهر",
                    SettingsResetScope.FONTS to "إعادة ضبط الخطوط وأحجام النص",
                    SettingsResetScope.NOTIFICATIONS to "إعادة ضبط الإشعارات والأذان",
                    SettingsResetScope.PRAYER to "إعادة ضبط تفضيلات الصلاة",
                    SettingsResetScope.ALL to "إعادة ضبط جميع التفضيلات"
                ).forEach { (scope, label) ->
                    OutlinedButton(
                        onClick = { pendingResetScope = scope },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(label) }
                }
            }
        }

        // ---------------- عن التطبيق ----------------
        item {
            SettingsSection(title = "حول التطبيق") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_alhadithi_logo),
                            contentDescription = "شعار تطبيق الحديثي",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(76.dp).padding(5.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الحديثي | AL-HADITHI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "مكتبة إسلامية للقراءة والعبادة والمعرفة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "الإصدار", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow(label = "المنطقة الزمنية", value = "Asia/Baghdad (UTC+3)")
                InfoRow(label = "اللغة", value = "العربية (اتجاه من اليمين إلى اليسار)")
                InfoRow(label = "حقوق المحتوى", value = "تخضع لرخصة كل مصدر منشور")

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showSourcesDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مصادر المحتوى العلمي المعتمدة")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "سياسة المحتوى: كل نص ديني أو أدبي معروض في التطبيق موثّق بمصدره داخل الشاشة، وتُعرض عبارة «المصدر غير متوفر في بيانات التطبيق» عند غياب مصدر موثق.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("إدارة كاش القرآن") },
            text = {
                Text("سيُحذف الكاش الشبكي للسور غير الأساسية فقط، مع الإبقاء على مجموعة السور الأساسية المضمّنة. لن تُحذف المفضلة أو سجل القراءة أو ملف الأذان أو أي تقدم للمستخدم.")
            },
            confirmButton = {
                Button(onClick = {
                    onClearQuranCache()
                    showClearCacheDialog = false
                }) { Text("تنظيف الكاش") }
            },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد حذف بيانات المستخدم") },
            text = {
                Text(
                    "سيتم حذف المفضلة وسجل القراءة وجلسات التسبيح وتقدم الدروس وكل الإعدادات والتنزيلات المحفوظة على هذا الجهاز. لا يمكن التراجع عن هذه الخطوة."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteUserData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showSourcesDialog = false },
            title = { Text("مصادر تطبيق الحديثي المعتمدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sources.forEach { src ->
                        Text(
                            text = "• ${src.sourceTitle}: ${src.authorAndDeathYear}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نص القرآن المضمّن: Tanzil Quran Text (Simple، الإصدار 1.1)، بترخيص Creative Commons Attribution 3.0 مع الاحتفاظ بإشعار المصدر. النسخة العثمانية من Al Quran Cloud مسار احتياطي عند تعذّر الأصل المحلي. ملفات الخطوط مرخّصة برخصة SIL Open Font License 1.1.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tanzil.net/")))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("فتح مصدر Tanzil الرسمي")
                    }
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tanzil.net/docs/Text_License")))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("قراءة شروط الترخيص CC BY 3.0")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSourcesDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = { Text("الأذونات المستخدمة ولماذا") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PermissionRow(
                        title = "الموقع (تقريبي ودقيق)",
                        body = "يُطلب فقط عند الضغط على «تحديث موقعي» في شاشة المواقيت، لتحسين دقة الأوقات حسب موقعك الفعلي. يمكن رفضه والاستمرار بمركز المحافظة."
                    )
                    PermissionRow(
                        title = "الإشعارات",
                        body = "تُطلب فقط عند تفعيل تنبيهات الصلاة أو اختبار الإشعار. بدونها يعمل التطبيق كاملاً بلا تنبيهات."
                    )
                    PermissionRow(
                        title = "التنبيهات الدقيقة",
                        body = "إعداد خاص في نظام أندرويد يمنح التطبيق جدولة دقيقة. بدونه قد يتأخر الإشعار قليلاً."
                    )
                    PermissionRow(
                        title = "الاهتزاز",
                        body = "يُستخدم في المسبحة والأذكار عند تفعيل الخيار من الإعدادات، ويحترم إعداد اهتزاز الجهاز."
                    )
                    PermissionRow(
                        title = "الإنترنت",
                        body = "مطلوب فقط لتحميل السور والتلاوات غير المحفوظة، ولبحث القرآن الشبكي."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPermissionsDialog = false }) { Text("فهمت") }
            }
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("مركز المساعدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("كيف أغيّر المحافظة؟", fontWeight = FontWeight.Bold)
                    Text("افتح الإعدادات ثم مواقيت الصلاة واضغط تغيير المحافظة. يتحدث جدول الصلاة عبر الخدمة المركزية.", style = MaterialTheme.typography.bodySmall)
                    Text("كيف أفعّل الوضع الداكن؟", fontWeight = FontWeight.Bold)
                    Text("من مجموعة المظهر اختر داكن، أو اختر حسب النظام ليتبع إعداد Android.", style = MaterialTheme.typography.bodySmall)
                    Text("هل يعمل القرآن دون إنترنت؟", fontWeight = FontWeight.Bold)
                    Text("النص المحلي المضمّن متاح دون اتصال، بينما بعض التلاوات والتفاسير والخدمات الشبكية تحتاج الإنترنت.", style = MaterialTheme.typography.bodySmall)
                    Text("لماذا يطلب الموقع؟", fontWeight = FontWeight.Bold)
                    Text("يُطلب فقط عندما تختار تحديد موقعك لتحسين حساب المواقيت والقبلة، ويمكنك الاستمرار باختيار المحافظة يدوياً.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { showFaqDialog = false }) { Text("إغلاق") } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("الخصوصية") },
            text = {
                Text(
                    "• إعدادات العرض والقراءة محفوظة محلياً في DataStore.\n" +
                    "• المفضلة وسجل القراءة والتقدم محفوظة في قاعدة Room على الجهاز.\n" +
                    "• إذن الموقع لا يُستخدم في الخلفية، ويُطلب عند اختيارك تحديد الموقع.\n" +
                    "• خدمات Firebase تستخدم للوظائف السحابية ولوحة المطور وفق صلاحيات المشروع.\n\n" +
                    "لا يوجد رابط سياسة خصوصية منشور ومهيأ في إعدادات هذا الإصدار؛ لذلك لا نعرض رابطاً غير موثّق."
                )
            },
            confirmButton = { Button(onClick = { showPrivacyDialog = false }) { Text("فهمت") } }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("شروط الاستخدام") },
            text = {
                Text("لم تُهيّأ وثيقة شروط استخدام منشورة أو رابط رسمي داخل إعدادات المشروع الحالي. تبقى حقوق مصادر القرآن والكتب والصوتيات خاضعة لتراخيص كل مصدر، ولا يجوز إعادة توزيع محتوى لا يسمح ترخيصه بذلك.")
            },
            confirmButton = { Button(onClick = { showTermsDialog = false }) { Text("إغلاق") } }
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(selectedSupportTopic.arabicTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = supportTitle,
                        onValueChange = { supportTitle = it.take(120) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عنوان الرسالة") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = supportBody,
                        onValueChange = { supportBody = it.take(2500) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("وصف المشكلة أو الاقتراح") },
                        minLines = 3,
                        maxLines = 6
                    )
                    if (selectedSupportTopic == SupportTopic.RELIGIOUS_CONTENT) {
                        OutlinedTextField(
                            value = supportReference,
                            onValueChange = { supportReference = it.take(500) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("موضع النص أو مصدر التصحيح") },
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { supportAttachmentPicker.launch(arrayOf("image/*")) }) {
                            Text("إرفاق صورة")
                        }
                        Text(
                            supportAttachment?.let { "تم اختيار صورة" } ?: "اختياري",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (supportAttachment != null) {
                            TextButton(onClick = { supportAttachment = null }) { Text("إزالة") }
                        }
                    }
                    Text(
                        "سيُفتح مربع المشاركة في Android لتختار تطبيقاً لإرسال الرسالة. لا تُرفع البيانات تلقائياً إلى خادم الحديثي في هذا الإصدار.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = supportTitle.isNotBlank() && supportBody.isNotBlank(),
                    onClick = {
                        val message = buildString {
                            appendLine("${selectedSupportTopic.arabicTitle}")
                            appendLine("العنوان: ${supportTitle.trim()}")
                            appendLine()
                            appendLine(supportBody.trim())
                            if (supportReference.isNotBlank()) {
                                appendLine()
                                appendLine("المصدر/الموضع: ${supportReference.trim()}")
                            }
                            appendLine()
                            appendLine("الإصدار: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                            appendLine("الجهاز: ${Build.MANUFACTURER} ${Build.MODEL}")
                            appendLine("Android API: ${Build.VERSION.SDK_INT}")
                        }
                        runCatching {
                            val attachmentUri = supportAttachment?.let { Uri.parse(it) }
                            val attachmentMime = attachmentUri?.let { uri ->
                                context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") }
                            }
                            require(attachmentUri == null || attachmentMime != null) {
                                "نوع الصورة المرفقة غير مدعوم. اختر صورة بصيغة معروفة."
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = attachmentMime ?: "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, supportTitle.trim())
                                putExtra(Intent.EXTRA_TEXT, message)
                                attachmentUri?.let { uri ->
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    clipData = ClipData.newUri(context.contentResolver, "مرفق بلاغ الحديثي", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الرسالة"))
                        }.onFailure {
                            Toast.makeText(context, "لا يوجد تطبيق مشاركة متاح على الجهاز.", Toast.LENGTH_LONG).show()
                        }
                        showSupportDialog = false
                    }
                ) { Text("متابعة إلى المشاركة") }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text("إلغاء") } }
        )
    }

    pendingResetScope?.let { scope ->
        AlertDialog(
            onDismissRequest = { pendingResetScope = null },
            title = { Text("تأكيد إعادة الضبط") },
            text = {
                Text(
                    when (scope) {
                        SettingsResetScope.APPEARANCE -> "ستعود ألوان ومظهر التطبيق إلى القيم الافتراضية."
                        SettingsResetScope.FONTS -> "ستعود خطوط الواجهة والقراءة وأحجامها إلى القيم الافتراضية."
                        SettingsResetScope.NOTIFICATIONS -> "ستُعاد تفضيلات التنبيهات لكل صلاة وفترة عدم الإزعاج، وسيتم إيقاف جدولة تنبيهات الصلاة."
                        SettingsResetScope.PRAYER -> "ستُعاد صيغة الوقت وتصحيح التاريخ الهجري. تبقى المحافظة وتفضيلات التنبيهات محفوظة."
                        SettingsResetScope.ALL -> "ستُعاد جميع التفضيلات إلى القيم الافتراضية. لن تُحذف المفضلة أو سجل القراءة أو العدّادات أو التنزيلات أو المحافظة أو حساب المطور."
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onResetSettings(scope)
                    pendingResetScope = null
                    notificationMessage = "تمت إعادة ضبط التفضيلات المحددة مع الحفاظ على بياناتك."
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { pendingResetScope = null }) { Text("إلغاء") } }
        )
    }
}

/** Expandable settings group. Each group starts closed to keep the hub short and scannable. */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val icon = when (title) {
        "المظهر" -> Icons.Outlined.Palette
        "اللغة والخطوط وحجم النص", "الخطوط وحجم النص" -> Icons.Outlined.TextFields
        "المتابعة اليومية في الرئيسية" -> Icons.Outlined.Home
        "التنزيلات والتخزين" -> Icons.Outlined.DeleteForever
        "مواقيت الصلاة" -> Icons.Outlined.AccessTime
        "القرآن الكريم والقراءة" -> Icons.Outlined.MenuBook
        "الأذكار والتسبيح" -> Icons.Outlined.SelfImprovement
        "الإشعارات", "الإشعارات والأذان" -> Icons.Outlined.Notifications
        "الخصوصية والبيانات" -> Icons.Outlined.Security
        "الدعم والمساعدة" -> Icons.Outlined.Headphones
        "إعادة ضبط التفضيلات" -> Icons.Outlined.DeleteForever
        else -> Icons.Outlined.Info
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 21.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(21.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (expanded) "اضغط لإغلاق المجموعة" else "اضغط لعرض الخيارات",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "طي المجموعة" else "فتح المجموعة",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
                content()
            }
        }
    }
}

private fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts("mailto", SupportChannels.SUPPORT_EMAIL, null)
        putExtra(Intent.EXTRA_SUBJECT, "التواصل مع دعم تطبيق الحديثي")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "لا يوجد تطبيق بريد إلكتروني مثبت على الجهاز", Toast.LENGTH_LONG).show()
    }
}

private fun openSupportProfile(
    context: Context,
    url: String,
    expectedHost: String,
    expectedPath: String
) {
    if (!SupportChannels.isSafeHttpsProfile(url, expectedHost, expectedPath)) {
        Toast.makeText(context, "رابط الدعم غير صالح", Toast.LENGTH_LONG).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "تعذر فتح رابط التواصل على هذا الجهاز", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun SupportContactRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(9.dp).size(21.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            imageVector = Icons.Outlined.Send,
            contentDescription = "فتح $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(17.dp)
        )
    }
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(17.dp))
    }
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun SettingsValueRow(title: String, value: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1_024L -> "$bytes بايت"
    bytes < 1_048_576L -> "${"%.1f".format(java.util.Locale.US, bytes / 1_024.0)} كيلوبايت"
    else -> "${"%.1f".format(java.util.Locale.US, bytes / 1_048_576.0)} ميغابايت"
}

private enum class SupportTopic(val arabicTitle: String) {
    TECHNICAL("التواصل مع الدعم الفني"),
    BUG("الإبلاغ عن مشكلة"),
    SUGGESTION("اقتراح تطوير"),
    RELIGIOUS_CONTENT("الإبلاغ عن خطأ في محتوى إسلامي")
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionRow(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
