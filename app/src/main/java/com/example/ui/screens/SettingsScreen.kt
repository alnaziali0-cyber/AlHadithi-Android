package com.example.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.BuildConfig
import com.example.data.local.ContentSourceEntity
import com.example.data.local.DailyGoal
import com.example.data.local.HomeCard
import com.example.data.local.UserSettingsState
import com.example.domain.IraqGovernorates
import com.example.domain.PrayerAlertProfile
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerCalcMethod
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
import com.example.ui.theme.QuranReadingLayout
import com.example.ui.theme.ReadingFontChoice
import com.example.ui.theme.ThemeMode
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
    onChangeHiddenHomeCards: (Set<HomeCard>) -> Unit,
    onChangeDailyGoals: (Set<DailyGoal>) -> Unit,
    onChangeTimeFormat: (TimeFormat) -> Unit,
    onChangePrayerAlert: (PrayerKey, PrayerAlertSettings) -> Unit,
    onChangeQuietHours: (QuietHours) -> Unit,
    onChangeQuranBackground: (QuranBackgroundStyle) -> Unit,
    onChangeReadingLayout: (QuranReadingLayout) -> Unit,
    onChangeAutoScroll: (Boolean, Float) -> Unit,
    onChangeReciter: (String) -> Unit,
    onChangePlaybackSpeed: (Float) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onChangeDailyTasbihGoal: (Int) -> Unit,
    onUpdateNotifications: (Boolean?, Int?, Boolean?, Boolean?) -> Unit,
    onChangeDailyContentNotifications: (Boolean) -> Unit,
    onChangeDailyContentType: (String) -> Unit,
    onChangeDailyContentPrayers: (Set<String>) -> Unit,
    onClearQuranCache: () -> Unit,
    onResetDefaults: () -> Unit,
    onDeleteUserData: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentGov = IraqGovernorates.byId(settings.governorateId)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSourcesDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
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
            SettingsSummaryCard(
                themeLabel = settings.themeMode.arabicLabel,
                accentLabel = settings.accentChoice.arabicLabel,
                fontLabel = settings.fontChoice.arabicLabel,
                uiScalePercent = (settings.uiFontScale * 100).roundToInt()
            )
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
            }
        }

        // ---------------- الخط ----------------
        item {
            SettingsSection(title = "الخط والنص") {
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

                Spacer(modifier = Modifier.height(12.dp))
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

                Spacer(modifier = Modifier.height(6.dp))
                Text("خط النصوص الطويلة (القرآن والأذكار)", style = MaterialTheme.typography.labelLarge)
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

                Spacer(modifier = Modifier.height(12.dp))
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

        // ---------------- تخصيص الصفحة الرئيسية ----------------
        item {
            SettingsSection(title = "الصفحة الرئيسية") {
                Text(
                    text = "أخفِ البطاقات التي لا تحتاجها؛ ترتيب البطاقات ثابت.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                HomeCard.defaultOrder.forEach { card ->
                    val hidden = card in settings.hiddenHomeCards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.arabicTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hidden) "مخفية" else "ظاهرة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !hidden,
                            onCheckedChange = { visible ->
                                val hiddenSet = settings.hiddenHomeCards.toMutableSet()
                                if (visible) hiddenSet.remove(card) else hiddenSet.add(card)
                                onChangeHiddenHomeCards(hiddenSet)
                            },
                            modifier = Modifier.testTag("home_card_visible_${card.key}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "أهداف «متابعة اليوم» التي تريد عرضها:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DailyGoal.entries.forEach { goal ->
                        GlassFilterChip(
                            label = goal.arabicTitle,
                            selected = goal in settings.dailyGoals,
                            onClick = {
                                val next = settings.dailyGoals.toMutableSet()
                                if (!next.add(goal)) next.remove(goal)
                                onChangeDailyGoals(next)
                            },
                            modifier = Modifier.testTag("daily_goal_${goal.key}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "لا تُعرض بطاقة «متابعة اليوم» إن لم تختر أي هدف.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "الحساب: ${PrayerCalcMethod.JAFARI.arabicTitle}",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PrayerCalcMethod.JAFARI.arabicSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Text("طريقة عرض الآيات", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranReadingLayout.entries.forEach { layout ->
                        GlassFilterChip(
                            label = layout.arabicLabel,
                            selected = settings.quranReadingLayout == layout,
                            onClick = { onChangeReadingLayout(layout) },
                            modifier = Modifier.testTag("settings_reading_layout_${layout.key}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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
            SettingsSection(title = "الإشعارات") {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("حديث اليوم ودعاء اليوم", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "يُرسل حديثاً أو دعاءً موثّقاً بعد كل صلاة. " +
                                "أُزيلت بطاقتا حديث اليوم ودعاء اليوم من الصفحة الرئيسية لهذا الغرض.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.dailyContentNotifications,
                        onCheckedChange = onChangeDailyContentNotifications,
                        modifier = Modifier.testTag("settings_daily_content_switch")
                    )
                }

                if (settings.dailyContentNotifications) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("نوع المحتوى:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "hadith" to "حديث",
                            "dua" to "دعاء",
                            "mixed" to "تناوب"
                        ).forEach { (key, label) ->
                            GlassFilterChip(
                                label = label,
                                selected = settings.dailyContentType == key,
                                onClick = { onChangeDailyContentType(key) },
                                modifier = Modifier.testTag("settings_content_type_$key")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("الصلوات التي يُرسل بعدها المحتوى:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "fajr" to "الفجر",
                            "dhuhr" to "الظهر",
                            "asr" to "العصر",
                            "maghrib" to "المغرب",
                            "isha" to "العشاء"
                        ).forEach { (key, label) ->
                            GlassFilterChip(
                                label = label,
                                selected = key in settings.dailyContentPrayers,
                                onClick = {
                                    val current = settings.dailyContentPrayers
                                    val next = if (key in current) current - key else current + key
                                    if (next.isNotEmpty()) onChangeDailyContentPrayers(next)
                                },
                                modifier = Modifier.testTag("settings_content_prayer_$key")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "فترة منع الإزعاج: ${
                        if (settings.prayerAlertProfile.quietHours.enabled) {
                            settings.prayerAlertProfile.quietHours.formatted()
                        } else {
                            "معطّلة"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                            text = "يُحفظ كل شيء على جهازك",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "التفضيلات والمفضلة وسجل القراءة محفوظة محلياً. لا يُطلب إذن الموقع إلا عند طلبك تحديد موقعك، ولا يُطلب إذن الإشعارات إلا عند تفعيل التنبيهات.",
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
                    onClick = onClearQuranCache,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مسح ذاكرة السور المؤقتة (لا يمسّ المفضلة)")
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
                    text = "يحذف المفضلة وسجل القراءة وجلسات التسبيح وتقدم الدروس والأخلاقيات ومتابعة الصلوات والإعدادات والتنزيلات المحفوظة. تبقى المواد الموسوعية المرفقة مع التطبيق.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------------- عن التطبيق ----------------
        item {
            SettingsSection(title = "حول التطبيق") {
                InfoRow(label = "الإصدار", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow(label = "المنطقة الزمنية", value = "Asia/Baghdad (UTC+3)")
                InfoRow(label = "اللغة", value = "العربية (اتجاه من اليمين إلى اليسار)")

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showSourcesDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مصادر المحتوى العلمي المعتمدة")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("استعادة الإعدادات الافتراضية")
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد حذف بيانات المستخدم") },
            text = {
                Text(
                    "سيتم حذف المفضلة وسجل القراءة وجلسات التسبيح وتقدم الدروس والأخلاقيات ومتابعة الصلوات وكل الإعدادات والتنزيلات المحفوظة على هذا الجهاز. لا يمكن التراجع عن هذه الخطوة."
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
                        text = "نص القرآن الكريم بالرسم العثماني مضمّن من خدمة Al Quran Cloud ويعمل دون إنترنت. معطيات تقسيم المصحف (الصفحات والأجزاء والأحزاب) مشتقة من بيانات مشروع تنزيل (tanzil.net) برخصة Creative Commons Attribution 3.0 (CC BY 3.0). ملفات الخطوط مرخّصة برخصة SIL Open Font License 1.1.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
}

/** A titled settings card. */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
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

/** Read-only overview of the settings that shape the whole app. */
@Composable
private fun SettingsSummaryCard(
    themeLabel: String,
    accentLabel: String,
    fontLabel: String,
    uiScalePercent: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "الحالة الحالية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "المظهر: $themeLabel · لون التمييز: $accentLabel",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "خط الواجهة: $fontLabel · حجم النص: $uiScalePercent%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
