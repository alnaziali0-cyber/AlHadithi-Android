package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.ContentSourceEntity
import com.example.data.local.DailyGoal
import com.example.data.local.SettingsResetScope
import com.example.data.local.UserSettingsState
import com.example.domain.IraqGovernorates
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerKey
import com.example.domain.QuietHours
import com.example.domain.SurahCatalog
import com.example.domain.SupportChannels
import com.example.domain.TimeFormat
import com.example.media.AdhanAudioStorage
import com.example.media.AdhanPlaybackService
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SettingsActionTile
import com.example.ui.components.SettingsRowDivider
import com.example.ui.components.SettingsSectionCard
import com.example.ui.components.SettingsSwitchTile
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.CardDensity
import com.example.ui.theme.LineSpacingOption
import com.example.ui.theme.LocalStatusColors
import com.example.ui.theme.QuranBackgroundStyle
import com.example.ui.theme.ReadingFontChoice
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.resolveReadingFontFamily
import com.example.ui.theme.resolveUiFontFamily
import com.example.util.NotificationTestHelper
import kotlin.math.roundToInt

/** The developer of the application. It is shown in the "Developers" settings row. */
const val APP_DEVELOPER_NAME = "علي اديب العنزي"

/**
 * The settings hub, rebuilt as a set of clearly titled sections.
 *
 * Every option is a button-like row that shows an icon, an Arabic title, a short description and
 * the current value of the setting. Binary options use a switch, everything else shows a chevron
 * that indicates a sub-page. Nothing that used to work has been removed: the previous behaviour
 * (dialogs, sliders, per-prayer alerts, the cache manager, the support forms, the reset scopes and
 * the delete confirmation) is preserved, only the presentation changed.
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
    onDeleteUserData: () -> Unit,
    onRefreshPrayerTimes: () -> Unit,
    onOpenDeveloperPanel: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val statusColors = LocalStatusColors.current
    val currentGov = IraqGovernorates.byId(settings.governorateId)

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var showSourcesDialog by rememberSaveable { mutableStateOf(false) }
    var showPermissionsDialog by rememberSaveable { mutableStateOf(false) }
    var showFaqDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showSupportDialog by rememberSaveable { mutableStateOf(false) }
    var showDeveloperDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationsDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncDialog by rememberSaveable { mutableStateOf(false) }
    var showSoundDialog by rememberSaveable { mutableStateOf(false) }
    var selectedSupportTopic by rememberSaveable { mutableStateOf(SupportTopic.TECHNICAL) }
    var supportTitle by rememberSaveable { mutableStateOf("") }
    var supportBody by rememberSaveable { mutableStateOf("") }
    var supportReference by rememberSaveable { mutableStateOf("") }
    var supportAttachment by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingResetScope by rememberSaveable { mutableStateOf<SettingsResetScope?>(null) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

    // The developer gate: seven taps on the version number reveal the access-code dialog, and the
    // code itself is what actually opens the panel.
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastVersionTapMillis by remember { mutableLongStateOf(0L) }
    var showDeveloperGate by rememberSaveable { mutableStateOf(false) }
    var developerAccessCode by rememberSaveable { mutableStateOf("") }
    var developerAccessError by remember { mutableStateOf<String?>(null) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------------- ترويسة الشاشة ----------------
        item {
            SettingsHeaderBar(
                title = "الإعدادات",
                subtitle = "كل خيار هنا مطبَّق فوراً ويُحفظ على جهازك",
                onBack = onBack
            )
        }

        // ---------------- هوية التطبيق ----------------
        item {
            AppIdentityCard(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE
            )
        }

        // ---------------- 1. إعدادات المظهر ----------------
        item {
            SettingsSectionCard(
                title = "إعدادات المظهر",
                icon = Icons.Outlined.Palette,
                description = "الوضع الليلي، لون الواجهة، الخط وحجم النص"
            ) {
                Column {
                    Text(
                        text = "وضع العرض",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
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

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "الوضع الداكن",
                        description = "أسود خفيف مريح للعين بدل الأسود التام",
                        icon = Icons.Outlined.DarkMode,
                        value = if (settings.themeMode == ThemeMode.DARK) "مُفعَّل" else "حسب النظام",
                        onClick = {
                            onChangeThemeMode(
                                if (settings.themeMode == ThemeMode.DARK) ThemeMode.SYSTEM else ThemeMode.DARK
                            )
                        },
                        testTag = "settings_dark_mode_row"
                    )

                    SettingsRowDivider()

                    Text(
                        text = "لون الواجهة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
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

                    SettingsRowDivider()

                    Text(
                        text = "حجم خط الواجهة: ${(settings.uiFontScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = settings.uiFontScale,
                        onValueChange = onChangeUiFontScale,
                        valueRange = 0.85f..1.45f,
                        modifier = Modifier.testTag("settings_ui_font_scale")
                    )
                    Text(
                        text = "معاينة مباشرة: خزائن الهدى · بسم الله الرحمن الرحيم",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = resolveUiFontFamily(settings.fontChoice)
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2
                    )

                    SettingsRowDivider()

                    Text(
                        text = "نوع خط القراءة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
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

                    SettingsRowDivider()

                    Text(
                        text = "كثافة البطاقات",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
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

                    SettingsRowDivider()

                    OutlinedButton(
                        onClick = { pendingResetScope = SettingsResetScope.APPEARANCE },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reset_appearance")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة الإعدادات الافتراضية")
                    }
                }
            }
        }

        // ---------------- 2. إعدادات القراءة ----------------
        item {
            SettingsSectionCard(
                title = "إعدادات القراءة",
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                description = "حجم خط القرآن والحديث والتباعد والتشكيل"
            ) {
                Column {
                    Text(
                        text = "حجم خط القرآن: ${(settings.readingFontScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = settings.readingFontScale,
                        onValueChange = onChangeReadingFontScale,
                        valueRange = 0.85f..1.55f,
                        modifier = Modifier.testTag("settings_reading_font_scale")
                    )

                    SettingsRowDivider()

                    Text(
                        text = "خط القراءة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        maxLines = 2
                    )

                    SettingsRowDivider()

                    Text(
                        text = "تباعد الأسطر",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
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

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "خلفية صفحة القراءة",
                        description = "ورقي · مصحف · بنّي · ليلي",
                        icon = Icons.Outlined.Description,
                        value = settings.quranBackground.arabicLabel,
                        onClick = {
                            // The available backdrops are cycled directly from the row so the
                            // setting stays reachable without hunting for a chip.
                            val next = QuranBackgroundStyle.entries[
                                (settings.quranBackground.ordinal + 1) % QuranBackgroundStyle.entries.size
                            ]
                            onChangeQuranBackground(next)
                        }
                    )

                    SettingsRowDivider()

                    SettingsSwitchTile(
                        title = "التمرير التلقائي",
                        description = "تنزيل الآيات ببطء أثناء التلاوة",
                        icon = Icons.Outlined.ArrowDownward,
                        checked = settings.quranAutoScrollEnabled,
                        onCheckedChange = { onChangeAutoScroll(it, settings.quranAutoScrollSpeed) },
                        testTag = "settings_auto_scroll_switch"
                    )

                    SettingsRowDivider()

                    OutlinedButton(
                        onClick = { pendingResetScope = SettingsResetScope.FONTS },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reset_fonts")
                    ) {
                        Icon(Icons.Outlined.TextFields, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة إعدادات القراءة الافتراضية")
                    }
                }
            }
        }

        // ---------------- 3. المتابعة اليومية في الرئيسية ----------------
        item {
            SettingsSectionCard(
                title = "المتابعة اليومية في الرئيسية",
                icon = Icons.Outlined.Home,
                description = "اختر مؤشرات المتابعة التي تريد إظهارها في الرئيسية"
            ) {
                Column {
                    Text(
                        text = "ترتيب الرئيسية ثابت للحفاظ على وضوحها. اختر فقط مؤشرات المتابعة التي تريد إظهارها؛ لا يؤدي تغييرها إلى حذف سجل إنجازاتك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DailyGoal.entries.forEach { goal ->
                        SettingsSwitchTile(
                            title = goal.arabicTitle,
                            description = "إظهار هذا المؤشر في بطاقة متابعة اليوم",
                            icon = Icons.Outlined.Spa,
                            checked = goal in settings.dailyGoals,
                            onCheckedChange = { enabled ->
                                onChangeDailyGoals(
                                    if (enabled) settings.dailyGoals + goal else settings.dailyGoals - goal
                                )
                            },
                            testTag = "settings_daily_goal_${goal.key}"
                        )
                        SettingsRowDivider()
                    }
                    Text(
                        text = if (settings.dailyGoals.isEmpty()) {
                            "لم يتم اختيار مؤشرات؛ بطاقة المتابعة مخفية حالياً."
                        } else {
                            "تم اختيار ${settings.dailyGoals.size} مؤشرات للمتابعة."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ---------------- 4. إعدادات مواقيت الصلاة ----------------
        item {
            SettingsSectionCard(
                title = "إعدادات مواقيت الصلاة",
                icon = Icons.Outlined.AccessTime,
                description = "المحافظة وطريقة الحساب والتنبيهات"
            ) {
                Column {
                    SettingsActionTile(
                        title = "المحافظة",
                        description = "المحافظة المستخدمة لحساب المواقيت",
                        icon = Icons.Outlined.LocationOn,
                        value = currentGov.arabicName,
                        onClick = onOpenGovernorateSheet,
                        testTag = "settings_governorate_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "طريقة الحساب",
                        description = "المذهب المعتمد في حساب المواقيت",
                        icon = Icons.Outlined.Calculate,
                        value = "الجعفرية (method=99)",
                        onClick = { onRefreshPrayerTimes() }
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "مذهب وقت العصر",
                        description = "طريقة تحديد وقت صلاة العصر",
                        icon = Icons.Outlined.Schedule,
                        value = "الجعفرية",
                        onClick = { onRefreshPrayerTimes() }
                    )

                    SettingsRowDivider()

                    Text(
                        text = "تعديل التاريخ الهجري",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "تعديل يدوي بمقدار يومين كحد أقصى، ويُطبَّق على صفحات التقويم والمناسبات والبحث.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
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

                    SettingsRowDivider()

                    Text(
                        text = "صيغة عرض الوقت",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeFormat.entries.forEach { format ->
                            GlassFilterChip(
                                label = format.arabicLabel,
                                selected = settings.timeFormat == format,
                                onClick = { onChangeTimeFormat(format) },
                                modifier = Modifier.testTag("settings_time_format_${format.key}")
                            )
                        }
                    }

                    SettingsRowDivider()

                    SettingsSwitchTile(
                        title = "تنبيهات الصلاة",
                        description = "إشعار محلي عند دخول وقت كل صلاة",
                        icon = Icons.Outlined.Notifications,
                        checked = settings.notifyPrayers,
                        onCheckedChange = { onUpdateNotifications(it, null, null, null) },
                        testTag = "settings_notify_prayers_switch"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "إعدادات الأذان",
                        description = "صوت الأذان المضمّن واختبار التشغيل",
                        icon = Icons.Outlined.RecordVoiceOver,
                        value = "مضمّن",
                        onClick = { showSoundDialog = true },
                        testTag = "settings_adhan_settings_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "الصلاة القادمة",
                        description = "الصلاة التالية وعدّاد الوقت المتبقي",
                        icon = Icons.Outlined.NotificationsActive,
                        value = settings.timeFormat.arabicLabel,
                        onClick = { onRefreshPrayerTimes() }
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "تحديث المواقيت يدوياً",
                        description = "جلب أحدث جدول من المصدر المركزي",
                        icon = Icons.Outlined.Refresh,
                        onClick = { onRefreshPrayerTimes() },
                        testTag = "settings_refresh_prayer_times"
                    )

                    SettingsRowDivider()

                    Text(
                        text = "مصدر المواقيت: خدمة AlAdhan بالإعدادات المخصصة المرفقة، مع حفظ نتيجة كل محافظة وتاريخ محلياً لاستخدامها دون إنترنت بعد أول جلب ناجح. لا يُستخدم حساب بديل عند غياب النتيجة أو النسخة المطابقة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------- 4. إعدادات الصوت ----------------
        item {
            SettingsSectionCard(
                title = "إعدادات الصوت",
                icon = Icons.Outlined.GraphicEq,
                description = "القارئ وصوت الأذان ومستوى الصوت"
            ) {
                Column {
                    SettingsActionTile(
                        title = "اختيار القارئ",
                        description = "القارئ الافتراضي لتلاوة القرآن",
                        icon = Icons.Outlined.RecordVoiceOver,
                        value = SurahCatalog.reciterById(settings.selectedReciterId).arabicName,
                        onClick = { showSoundDialog = true }
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "صوت الأذان",
                        description = "التسجيل المضمّن مع التطبيق",
                        icon = Icons.Outlined.MusicNote,
                        value = "مضمّن",
                        onClick = { showSoundDialog = true }
                    )

                    SettingsRowDivider()

                    SettingsSwitchTile(
                        title = "التشغيل التلقائي",
                        description = "تشغيل الأذان تلقائياً عند دخول الوقت",
                        icon = Icons.Outlined.PlayCircle,
                        checked = settings.notifyPrayers,
                        onCheckedChange = { onUpdateNotifications(it, null, null, null) },
                        testTag = "settings_auto_play_switch"
                    )

                    SettingsRowDivider()

                    Text(
                        text = "مستوى الصوت وسرعة التشغيل",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "سرعة التشغيل: ${"%.2f".format(java.util.Locale.US, settings.playbackSpeed)}×",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.playbackSpeed,
                        onValueChange = onChangePlaybackSpeed,
                        valueRange = 0.75f..1.75f,
                        modifier = Modifier.testTag("settings_playback_speed")
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "تنزيل الصوت أو حذفه",
                        description = "إدارة ملف الأذان المخصص على الجهاز",
                        icon = Icons.Outlined.Storage,
                        value = AdhanAudioStorage.currentAudioFile(context)
                            ?.let { formatBytes(it.length()) }
                            ?: "الصوت الافتراضي المضمّن",
                        onClick = { showSoundDialog = true }
                    )

                    SettingsRowDivider()

                    OutlinedButton(
                        onClick = {
                            runCatching { AdhanPlaybackService.play(context) }
                                .onSuccess { notificationMessage = "بدأ تشغيل تسجيل الأذان المضمّن." }
                                .onFailure { notificationMessage = "تعذر تشغيل الأذان؛ تحقق من إعدادات الصوت." }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_test_sound")
                    ) {
                        Icon(Icons.Outlined.Headphones, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اختبار الصوت")
                    }
                }
            }
        }

        // ---------------- 5. إعدادات الإشعارات ----------------
        item {
            SettingsSectionCard(
                title = "إعدادات الإشعارات",
                icon = Icons.Outlined.Notifications,
                description = "ما الذي يصلك على هاتفك ومتى"
            ) {
                Column {
                    SettingsSwitchTile(
                        title = "إشعارات مواقيت الصلاة",
                        description = "إشعار عند دخول وقت كل صلاة",
                        icon = Icons.Outlined.Notifications,
                        checked = settings.notifyPrayers,
                        onCheckedChange = { onUpdateNotifications(it, null, null, null) },
                        testTag = "settings_notify_prayers_switch_section"
                    )

                    SettingsRowDivider()

                    SettingsSwitchTile(
                        title = "إشعار الأذكار",
                        description = "تذكير بأذكار الصباح والمساء",
                        icon = Icons.Outlined.Spa,
                        checked = settings.notifyAdhkar,
                        onCheckedChange = { onUpdateNotifications(null, null, null, it) },
                        testTag = "settings_notify_adhkar_switch"
                    )

                    SettingsRowDivider()

                    SettingsSwitchTile(
                        title = "إشعار المحتوى اليومي",
                        description = "تذكير أذكار الصباح والمساء",
                        icon = Icons.Outlined.AutoStories,
                        checked = settings.notifyOccasions,
                        onCheckedChange = { onUpdateNotifications(null, null, it, null) },
                        testTag = "settings_notify_content_switch"
                    )

                    SettingsRowDivider()

                    Text(
                        text = "إشعارات الحديث اليومي والدعاء اليومي غير مفعّلة في هذا الإصدار؛ تحتاج خدمة محتوى وجدولة موثوقة من الخادم، لذلك لا نعرض مفاتيح تشغيل شكلية.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingsRowDivider()

                    SettingsSwitchTile(
                        title = "إشعارات التحديث",
                        description = "إشعار عند توفر نسخة جديدة من التطبيق",
                        icon = Icons.Outlined.SystemUpdate,
                        checked = settings.notifyOccasions,
                        onCheckedChange = { onUpdateNotifications(null, null, it, null) },
                        testTag = "settings_notify_updates_switch"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "التحكم بأوقات الإشعارات",
                        description = "فترة عدم الإزعاج وتوقيت كل تنبيه",
                        icon = Icons.Outlined.Schedule,
                        value = if (settings.prayerAlertProfile.quietHours.enabled) {
                            settings.prayerAlertProfile.quietHours.formatted()
                        } else {
                            "غير مفعّلة"
                        },
                        onClick = { showNotificationsDialog = true },
                        testTag = "settings_notification_times_row"
                    )

                    SettingsRowDivider()

                    Text(
                        text = "إعداد مستقل لكل صلاة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "يمكن إيقاف صلاة بعينها أو تغيير وقت التنبيه والصوت والاهتزاز.",
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
                            Text(
                                text = "التنبيه قبل الصلاة: ${alert.leadMinutes} دقيقة",
                                style = MaterialTheme.typography.labelMedium
                            )
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("الصوت", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = alert.soundEnabled,
                                        onCheckedChange = {
                                            onChangePrayerAlert(prayer, alert.copy(soundEnabled = it))
                                        },
                                        enabled = alert.enabled
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("الاهتزاز", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = alert.vibrateEnabled,
                                        onCheckedChange = {
                                            onChangePrayerAlert(prayer, alert.copy(vibrateEnabled = it))
                                        },
                                        enabled = alert.enabled
                                    )
                                }
                            }
                        }
                        if (index != PrayerKey.entries.lastIndex) {
                            SettingsRowDivider()
                        }
                    }

                    SettingsRowDivider()

                    val quietHours = settings.prayerAlertProfile.quietHours
                    SettingsSwitchTile(
                        title = "فترة عدم الإزعاج",
                        description = if (quietHours.enabled) quietHours.formatted() else "متوقفة",
                        icon = Icons.Outlined.Bedtime,
                        checked = quietHours.enabled,
                        onCheckedChange = { onChangeQuietHours(quietHours.copy(enabled = it)) },
                        testTag = "settings_quiet_hours_switch"
                    )
                    if (quietHours.enabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            onChangeQuietHours(
                                                quietHours.copy(startMinutes = hour * 60 + minute)
                                            )
                                        },
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
                                        { _, hour, minute ->
                                            onChangeQuietHours(
                                                quietHours.copy(endMinutes = hour * 60 + minute)
                                            )
                                        },
                                        quietHours.endMinutes / 60,
                                        quietHours.endMinutes % 60,
                                        true
                                    ).show()
                                }
                            ) { Text("إلى ${quietHours.formatted().substringAfter(" — ")}") }
                        }
                    }

                    SettingsRowDivider()

                    OutlinedButton(
                        onClick = {
                            val posted = NotificationTestHelper.postTestNotification(context)
                            notificationMessage = if (posted) {
                                "تم إرسال إشعار تجريبي. تحقق من شريط الإشعارات."
                            } else {
                                "لم يُمنح إذن الإشعارات بعد. فعّله من إعدادات النظام ثم أعد المحاولة."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_test_notification")
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اختبار الإشعار")
                    }
                }
            }
        }

        // ---------------- 6. إعدادات البيانات والمزامنة ----------------
        item {
            SettingsSectionCard(
                title = "إعدادات البيانات والمزامنة",
                icon = Icons.Outlined.CloudSync,
                description = "النسخ الاحتياطي والمزامنة والتنظيف"
            ) {
                Column {
                    SettingsActionTile(
                        title = "مزامنة البيانات",
                        description = "جلب أحدث المحتوى من الخادم",
                        icon = Icons.Outlined.CloudSync,
                        onClick = { showSyncDialog = true },
                        testTag = "settings_sync_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "النسخ الاحتياطي",
                        description = "حفظ نسخة من تفضيلاتك على الجهاز",
                        icon = Icons.Outlined.Backup,
                        onClick = { showSyncDialog = true },
                        testTag = "settings_backup_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "استعادة البيانات",
                        description = "استرجاع نسخة سابقة من التفضيلات",
                        icon = Icons.Outlined.Upload,
                        onClick = { showSyncDialog = true },
                        testTag = "settings_restore_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "مسح الكاش",
                        description = "حذف النصوص المنزّلة مع الإبقاء على الأساسيات",
                        icon = Icons.Outlined.DeleteOutline,
                        value = "$cachedAyahCount آية مخزنة",
                        onClick = { showClearCacheDialog = true },
                        testTag = "settings_clear_cache_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "مسح البيانات المحلية",
                        description = "حذف المفضلة والسجل والإعدادات والتنزيلات",
                        icon = Icons.Outlined.DeleteForever,
                        valueColor = statusColors.error,
                        onClick = { showDeleteDialog = true },
                        testTag = "settings_delete_user_data"
                    )

                    SettingsRowDivider()

                    Text(
                        text = "يحذف المفضلة وسجل القراءة وجلسات التسبيح وتقدم الدروس وسجل صلواتي اليوم والإعدادات والتنزيلات المحفوظة. تبقى المواد الموسعية المرفقة مع التطبيق.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------- 7. إعدادات اللغة ----------------
        item {
            SettingsSectionCard(
                title = "إعدادات اللغة",
                icon = Icons.Outlined.Language,
                description = "لغة واجهة التطبيق واتجاه العرض"
            ) {
                Column {
                    SettingsActionTile(
                        title = "العربية",
                        description = "اللغة الأساسية للتطبيق مع اتجاه من اليمين إلى اليسار",
                        icon = Icons.Outlined.Translate,
                        value = "مُفعَّلة",
                        onClick = { showLanguageDialog = true },
                        testTag = "settings_language_arabic"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "English",
                        description = "الإنجليزية غير متاحة كواجهة كاملة في هذه النسخة",
                        icon = Icons.Outlined.Translate,
                        value = "غير متاحة",
                        valueColor = statusColors.warning,
                        onClick = { showLanguageDialog = true },
                        testTag = "settings_language_english"
                    )
                }
            }
        }

        // ---------------- 8. المطورون ----------------
        item {
            SettingsSectionCard(
                title = "المطورون",
                icon = Icons.Outlined.AdminPanelSettings,
                description = "أدوات تشخيص خاصة بالمطوّر"
            ) {
                Column {
                    SettingsActionTile(
                        title = "لوحة المطور",
                        description = "الأدوات متاحة للمطوّر فقط — تتطلب رمز دخول",
                        icon = Icons.Outlined.AdminPanelSettings,
                        value = APP_DEVELOPER_NAME,
                        onClick = { showDeveloperDialog = true },
                        testTag = "settings_developer_panel_row"
                    )
                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "اسم المطوّر",
                        description = "الجهة التي طوّرت التطبيق وأشرفت عليه",
                        icon = Icons.Outlined.Info,
                        value = APP_DEVELOPER_NAME,
                        showChevron = false,
                        onClick = { showDeveloperDialog = true },
                        testTag = "settings_developer_name_row"
                    )
                }
            }
        }

        // ---------------- 9. الدعم والمساعدة ----------------
        item {
            SettingsSectionCard(
                title = "الدعم والمساعدة",
                icon = Icons.Outlined.Headphones,
                description = "التواصل معنا ومركز المساعدة"
            ) {
                Column {
                    SettingsActionTile(
                        title = "البريد الإلكتروني",
                        description = "فتح البريد الإلكتروني للدعم",
                        icon = Icons.Outlined.Email,
                        onClick = { openSupportEmail(context) },
                        testTag = "settings_support_email_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "Instagram",
                        description = "فتح حساب الدعم على Instagram",
                        icon = Icons.Outlined.CameraAlt,
                        onClick = {
                            openSupportProfile(
                                context = context,
                                url = SupportChannels.INSTAGRAM_URL,
                                expectedHost = "www.instagram.com",
                                expectedPath = "/alhadithi_iraq/"
                            )
                        },
                        testTag = "settings_support_instagram_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "TikTok",
                        description = "فتح حساب الدعم على TikTok",
                        icon = Icons.Outlined.MusicNote,
                        onClick = {
                            openSupportProfile(
                                context = context,
                                url = SupportChannels.TIKTOK_URL,
                                expectedHost = "www.tiktok.com",
                                expectedPath = "/@.al.hadithi"
                            )
                        },
                        testTag = "settings_support_tiktok_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "مركز المساعدة والأسئلة الشائعة",
                        description = "إرشادات استخدام الأقسام والإعدادات",
                        icon = Icons.Outlined.Lightbulb,
                        onClick = { showFaqDialog = true },
                        testTag = "settings_faq_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "شرح الأذونات المستخدمة",
                        description = "لماذا يطلب التطبيق كل إذن",
                        icon = Icons.Outlined.Security,
                        onClick = { showPermissionsDialog = true },
                        testTag = "settings_permissions_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "الإبلاغ عن مشكلة",
                        description = "صف الخطأ وحدد موضعه في التطبيق",
                        icon = Icons.Outlined.BugReport,
                        onClick = {
                            selectedSupportTopic = SupportTopic.BUG
                            supportTitle = "بلاغ عن مشكلة"
                            supportBody = ""
                            supportReference = ""
                            supportAttachment = null
                            showSupportDialog = true
                        },
                        testTag = "settings_bug_report_row"
                    )
                }
            }
        }

        // ---------------- 10. حول التطبيق ----------------
        item {
            SettingsSectionCard(
                title = "حول التطبيق",
                icon = Icons.Outlined.Info,
                description = "الهوية والإصدار والمصادر والتراخيص"
            ) {
                Column {
                    SettingsActionTile(
                        title = "اسم التطبيق",
                        description = "خزائن الهدى · KHAZAIN AL-HUDA",
                        icon = Icons.Outlined.Badge,
                        value = "خزائن الهدى",
                        showChevron = false,
                        onClick = { showSourcesDialog = true },
                        testTag = "settings_app_name_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "رقم الإصدار",
                        description = "النسخة المعروضة للمستخدم · اضغط 7 مرات لفتح بوابة المطور",
                        icon = Icons.Outlined.Description,
                        value = BuildConfig.VERSION_NAME,
                        showChevron = false,
                        onClick = {
                            val now = System.currentTimeMillis()
                            versionTapCount = if (now - lastVersionTapMillis > 1_500L) 1 else versionTapCount + 1
                            lastVersionTapMillis = now
                            if (versionTapCount >= DeveloperAccessController.REQUIRED_TAPS) {
                                versionTapCount = 0
                                showDeveloperGate = true
                            }
                        },
                        testTag = "settings_version_name_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "رقم البناء",
                        description = "معرّف البناء التقني",
                        icon = Icons.Outlined.Memory,
                        value = BuildConfig.VERSION_CODE.toString(),
                        showChevron = false,
                        onClick = { showSourcesDialog = true },
                        testTag = "settings_version_code_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "اسم المطوّر",
                        description = "الجهة التي طوّرت التطبيق وأشرفت عليه",
                        icon = Icons.Outlined.AdminPanelSettings,
                        value = APP_DEVELOPER_NAME,
                        showChevron = false,
                        onClick = { showDeveloperDialog = true },
                        testTag = "settings_about_developer_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "سياسة الخصوصية",
                        description = "ملخص ممارسات الخصوصية الحالية",
                        icon = Icons.Outlined.PrivacyTip,
                        onClick = { showPrivacyDialog = true },
                        testTag = "settings_privacy_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "التراخيص مفتوحة المصدر",
                        description = "مصادر المحتوى العلمي ورخص الخطوط والنص",
                        icon = Icons.Outlined.VerifiedUser,
                        onClick = { showSourcesDialog = true },
                        testTag = "settings_licenses_row"
                    )

                    SettingsRowDivider()

                    SettingsActionTile(
                        title = "روابط التواصل والدعم",
                        description = "البريد وInstagram وTikTok",
                        icon = Icons.AutoMirrored.Outlined.Send,
                        onClick = { openSupportEmail(context) },
                        testTag = "settings_support_links_row"
                    )

                    SettingsRowDivider()

                    OutlinedButton(
                        onClick = { pendingResetScope = SettingsResetScope.ALL },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reset_all")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة ضبط جميع التفضيلات")
                    }
                }
            }
        }

        notificationMessage?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = statusColors.successContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors.onSuccessContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // ---------------- الحوارات ----------------
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
                    notificationMessage = "تم تنظيف كاش القرآن مع الإبقاء على الأساسيات."
                }) { Text("تنظيف الكاش") }
            },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد حذف البيانات المحلية") },
            text = {
                Text("هل أنت متأكد؟ سيتم حذف البيانات المحلية ولا يمكن التراجع عن هذه العملية.")
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
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showSourcesDialog = false },
            title = { Text("مصادر تطبيق خزائن الهدى المعتمدة") },
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
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://tanzil.net/docs/Text_License"))
                                )
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
                    Text("افتح الإعدادات ثم إعدادات مواقيت الصلاة واضغط على المحافظة. يتحدث جدول الصلاة عبر الخدمة المركزية.", style = MaterialTheme.typography.bodySmall)
                    Text("كيف أفعّل الوضع الداكن؟", fontWeight = FontWeight.Bold)
                    Text("من إعدادات المظهر اختر داكن، أو اختر حسب النظام ليتبع إعداد Android.", style = MaterialTheme.typography.bodySmall)
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("لغة التطبيق") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("العربية", fontWeight = FontWeight.Bold)
                    Text("اللغة المترجمة حالياً والمطبَّقة بالكامل مع اتجاه من اليمين إلى اليسار.", style = MaterialTheme.typography.bodySmall)
                    Text("English", fontWeight = FontWeight.Bold, color = statusColors.warning)
                    Text("الإنجليزية غير مكتملة في هذه النسخة؛ لا نعرض لغة غير مترجمة كخيار فعّال.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { showLanguageDialog = false }) { Text("إغلاق") } }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("أوقات الإشعارات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تُضبط دقائق التنبيه لكل صلاة من قسم «إعدادات الإشعارات» أعلاه.", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "فترة عدم الإزعاج: ${settings.prayerAlertProfile.quietHours.formatted()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = { Button(onClick = { showNotificationsDialog = false }) { Text("إغلاق") } }
        )
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("البيانات والمزامنة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تُحفظ تفضيلاتك وسجل قراءتك محلياً على جهازك عبر DataStore وRoom.", style = MaterialTheme.typography.bodySmall)
                    Text("النسخ الاحتياطي واستعادة البيانات متاحان عبر تصدير الإعدادات من لوحة المطور.", style = MaterialTheme.typography.bodySmall)
                    Text("لمسح الكاش أو حذف البيانات المحلية استخدم القسم «إعدادات البيانات والمزامنة».", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { showSyncDialog = false }) { Text("إغلاق") } }
        )
    }

    if (showSoundDialog) {
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = { Text("إعدادات الصوت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("القارئ الافتراضي", fontWeight = FontWeight.Bold)
                    Text(
                        text = SurahCatalog.reciterById(settings.selectedReciterId).arabicName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SurahCatalog.reciters.take(8).forEach { reciter ->
                            GlassFilterChip(
                                label = reciter.arabicName,
                                selected = settings.selectedReciterId == reciter.id,
                                onClick = { onChangeReciter(reciter.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("صوت الأذان", fontWeight = FontWeight.Bold)
                    Text(
                        text = AdhanAudioStorage.currentAudioFile(context)
                            ?.let { "ملف مخصص: ${formatBytes(it.length())}" }
                            ?: "التسجيل الافتراضي المضمّن مع التطبيق",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "مستوى الصوت يتبع قناة المنبّه في Android؛ لا يغيّر التطبيق مستوى صوت الجهاز من تلقاء نفسه.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            runCatching { AdhanPlaybackService.play(context) }
                                .onSuccess { notificationMessage = "بدأ تشغيل تسجيل الأذان المضمّن." }
                                .onFailure { notificationMessage = "تعذر تشغيل الأذان؛ تحقق من إعدادات الصوت." }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("اختبار الصوت") }
                }
            },
            confirmButton = { Button(onClick = { showSoundDialog = false }) { Text("إغلاق") } }
        )
    }

    if (showDeveloperDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperDialog = false },
            title = { Text("لوحة المطور") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "هذه الأدوات مخصصة للمطوّر فقط.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColors.warning
                    )
                    Text(
                        text = "المطوّر: $APP_DEVELOPER_NAME",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "لفتح لوحة المطور الكاملة والتحقق من الصلاحيات، استخدم طريقة الدخول المخصّصة (الضغط المتكرر على رقم الإصدار أو رمز الدخول).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "سيفتح هذا الزر مسار الدخول المحمي للوحة المطور.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeveloperDialog = false
                        onOpenDeveloperPanel()
                    },
                    modifier = Modifier.testTag("settings_developer_panel_confirm")
                ) { Text("متابعة") }
            },
            dismissButton = {
                TextButton(onClick = { showDeveloperDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showDeveloperGate) {
        AlertDialog(
            onDismissRequest = {
                showDeveloperGate = false
                developerAccessCode = ""
                developerAccessError = null
            },
            title = { Text("بوابة المطور") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "هذه الأدوات مخصصة للمطوّر فقط. أدخل رمز الدخول للمتابعة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColors.warning
                    )
                    OutlinedTextField(
                        value = developerAccessCode,
                        onValueChange = {
                            developerAccessCode = it.take(12)
                            developerAccessError = null
                        },
                        label = { Text("رمز الدخول") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("developer_access_code_input")
                    )
                    developerAccessError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val unlocked = DeveloperAccessController.verifyAndUnlock(context, developerAccessCode)
                        if (unlocked) {
                            showDeveloperGate = false
                            developerAccessCode = ""
                            developerAccessError = null
                            onOpenDeveloperPanel()
                        } else {
                            developerAccessError = "رمز الدخول غير صحيح."
                        }
                    },
                    modifier = Modifier.testTag("developer_access_code_confirm")
                ) { Text("دخول") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeveloperGate = false
                    developerAccessCode = ""
                    developerAccessError = null
                }) { Text("إلغاء") }
            }
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
                        "سيُفتح مربع المشاركة في Android لتختار تطبيقاً لإرسال الرسالة. لا تُرفع البيانات تلقائياً إلى خادم خزائن الهدى في هذا الإصدار.",
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
                            appendLine(selectedSupportTopic.arabicTitle)
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
                                    clipData = ClipData.newUri(context.contentResolver, "مرفق بلاغ خزائن الهدى", uri)
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

/**
 * The settings screen header. It mirrors the reference design: a back arrow, a large title and a
 * quiet subtitle, all inside a rounded surface that keeps the title clear of the status bar.
 */
@Composable
private fun SettingsHeaderBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("settings_back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

/** The identity card at the top of the settings screen: the logo, the name and the version. */
@Composable
private fun AppIdentityCard(
    versionName: String,
    versionCode: Int
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF7F5EE),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Image(
                    painter = painterResource(R.drawable.img_alhadithi_logo),
                    contentDescription = "الشعار الرسمي لخزائن الهدى",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(58.dp)
                        .padding(4.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "خزائن الهدى",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "KHAZAIN AL-HUDA · $versionName ($versionCode)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "المطوّر: $APP_DEVELOPER_NAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
