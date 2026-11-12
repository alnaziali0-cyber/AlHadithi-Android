package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.DailyPrayerSchedule
import com.example.domain.GovernorateInfo
import com.example.domain.NextPrayerCountdown
import com.example.domain.PrayerAlertProfile
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerKey
import com.example.domain.QuietHours
import com.example.domain.TimeFormat
import com.example.media.AdhanAudioStorage
import com.example.media.AdhanPlaybackService
import com.example.media.CompassReading
import com.example.media.QiblaSensorController
import com.example.ui.components.EmeraldHeroCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.HeritageGold
import com.example.util.SystemActionsHelper
import com.example.util.PreciseDeviceLocationAccess
import com.example.util.rememberPreciseDeviceLocationAction
import com.example.media.PrayerNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PrayerTimesScreen(
    governorate: GovernorateInfo,
    schedule: DailyPrayerSchedule,
    countdown: NextPrayerCountdown,
    monthlySchedules: List<DailyPrayerSchedule>,
    notifyPrayers: Boolean,
    timeFormat: TimeFormat,
    prayerAlertProfile: PrayerAlertProfile,
    onBack: () -> Unit,
    onOpenGovernorateSheet: () -> Unit,
    onToggleNotifyPrayers: (Boolean) -> Unit,
    onChangeTimeFormat: (TimeFormat) -> Unit,
    onChangePrayerAlert: (PrayerKey, PrayerAlertSettings) -> Unit,
    onChangeQuietHours: (QuietHours) -> Unit,
    onRefreshPrayerSchedule: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    fun formatTime(minutesOfDay: Int): String = when (timeFormat) {
        TimeFormat.TWELVE_HOUR -> schedule.formatted12h(minutesOfDay)
        TimeFormat.TWENTY_FOUR_HOUR -> schedule.formatted24h(minutesOfDay)
    }
    var adhanStatus by remember { mutableStateOf<String?>(null) }
    var hasAdhanAudio by remember { mutableStateOf(AdhanPlaybackService.hasPlayableAudio(context)) }
    var exactAlarmAllowed by remember {
        mutableStateOf(PrayerNotificationScheduler.canScheduleExactAlarms(context))
    }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _: ActivityResult ->
        exactAlarmAllowed = PrayerNotificationScheduler.canScheduleExactAlarms(context)
        if (exactAlarmAllowed) onRefreshPrayerSchedule()
    }

    fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmAllowed) {
            onRefreshPrayerSchedule()
            return
        }
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        try {
            exactAlarmSettingsLauncher.launch(intent)
        } catch (_: Exception) {
            adhanStatus = "تعذر فتح إعداد التنبيهات الدقيقة؛ افتح إعدادات التطبيق يدوياً"
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleNotifyPrayers(granted)
        if (granted) requestExactAlarmAccess()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "مواقيت الصلاة — ${governorate.arabicName}",
            subtitle = "المنطقة الزمنية: Asia/Baghdad · بيانات المحافظة المحددة فقط",
            onBack = onBack,
            trailing = {
                TextButton(onClick = onOpenGovernorateSheet) {
                    Text("تغيير المحافظة")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("prayer_times_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Prayer and astronomical markers from one central AlAdhan API record.
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)) {
                    Text("مواقيت الصلاة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${governorate.arabicName} · ${schedule.gregorianDateLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    val rows = listOf(
                        Triple("الفجر", formatTime(schedule.fajrMinutes), Icons.Outlined.WbTwilight),
                        Triple("الشروق", formatTime(schedule.sunriseMinutes), Icons.Outlined.WbTwilight),
                        Triple("الظهر", formatTime(schedule.dhuhrMinutes), Icons.Outlined.LightMode),
                        Triple("العصر", formatTime(schedule.asrMinutes), Icons.Outlined.WbTwilight),
                        Triple("الغروب", formatTime(schedule.sunsetMinutes), Icons.Outlined.NightsStay),
                        Triple("المغرب", formatTime(schedule.maghribMinutes), Icons.Outlined.NightsStay),
                        Triple("العشاء", formatTime(schedule.ishaMinutes), Icons.Outlined.Bedtime),
                        Triple("منتصف الليل", formatTime(schedule.midnightMinutes), Icons.Outlined.DarkMode)
                    )
rows.forEachIndexed { idx, (name, timeStr, iconVec) ->
                        val isNext = name == countdown.prayerArabicName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isNext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(7.dp)
                                    )
                                }
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNext) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isNext) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (idx < rows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (countdown.remainingSeconds > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("المتبقي على ${countdown.prayerArabicName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(countdown.prayerFormattedTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(countdown.countdownHhMmSs, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(
                            countdown.countdownHhMmSs,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (schedule.sourceType in setOf("ALADHAN_API", "ALADHAN_API_CACHE")) {
                        Text(
                            "المصدر: ${schedule.sourceLabel} · آخر تحديث للسجل: ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale("ar", "IQ")).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Baghdad") }.format(java.util.Date(schedule.sourceUpdatedAtMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (schedule.sourceUrl.isNotBlank()) {
                            Text(
                                "رابط المصدر: ${schedule.sourceUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "لم تصل نتيجة AlAdhan لهذا التاريخ ولا توجد نسخة محفوظة. لم يُستخدم حساب بديل ولن تُعرض أوقات تقديرية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الأذان المرفق بالتطبيق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("يُستخدم تلقائياً عند تفعيل التنبيه الصوتي.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            runCatching { AdhanPlaybackService.play(context) }
                                .onSuccess { adhanStatus = "بدأ اختبار تشغيل الأذان المرفق." }
                                .onFailure { adhanStatus = "تعذر تشغيل الأذان؛ تحقق من إعدادات الصوت." }
                        }) { Icon(Icons.Outlined.PlayArrow, contentDescription = "اختبار الأذان") }
                    }
                    adhanStatus?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    Text("تم تضمين التسجيل الصوتي الذي زوّدتنا به داخل حزمة التطبيق. احرص على امتلاك حق استخدامه وتوزيعه.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Runtime notification permission + exact-alarm access
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تنبيهات دخول وقت الصلاة", fontWeight = FontWeight.Bold)
                            Text(
                                "إشعار محلي عند دخول وقت الفجر والظهر والعصر والمغرب والعشاء",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyPrayers,
                            onCheckedChange = { want ->
                                if (want && !SystemActionsHelper.hasNotificationPermission(context)) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    onToggleNotifyPrayers(want)
                                    if (want) requestExactAlarmAccess()
                                }
                            }
                        )
                    }
                    if (notifyPrayers) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (exactAlarmAllowed) "التنبيهات الدقيقة مفعّلة على هذا الجهاز."
                            else "إذن (المنبّهات والتذكيرات) غير ممنوح؛ قد يتأخر الإشعار حتى تسمح بالتنبيهات الدقيقة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (exactAlarmAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (schedule.sourceType in setOf("ALADHAN_API", "ALADHAN_API_CACHE")) {
                                "سيُجدول التنبيه من مواقيت AlAdhan المحفوظة لنفس المحافظة والتاريخ."
                            } else {
                                "تفضيل التنبيهات محفوظ، لكن لا توجد نتيجة API أو نسخة محلية لهذا التاريخ؛ لن تُنشأ أوقات تقديرية."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (schedule.sourceType in setOf("ALADHAN_API", "ALADHAN_API_CACHE")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        if (!exactAlarmAllowed) {
                            OutlinedButton(onClick = { requestExactAlarmAccess() }) {
                                Text("السماح بالتنبيهات الدقيقة")
                            }
                        }
                    }
                }
            }

            // Per-prayer reminder configuration
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ضبط تنبيه كل صلاة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يمكنك إيقاف تنبيه صلاة بعينها، أو تحديد وقت التنبيه قبل دخول الوقت، أو اختيار الصوت والاهتزاز.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrayerKey.entries.forEach { prayer ->
                        val settings = prayerAlertProfile.settingsFor(prayer)
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prayer.arabicName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = settings.enabled,
                                    onCheckedChange = { onChangePrayerAlert(prayer, settings.copy(enabled = it)) },
                                    modifier = Modifier.testTag("prayer_alert_switch_${prayer.key}")
                                )
                            }
                            if (settings.enabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "التنبيه قبل دخول الوقت بـ ${settings.leadMinutes} دقيقة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = {
                                            onChangePrayerAlert(
                                                prayer,
                                                settings.copy(leadMinutes = (settings.leadMinutes - 5).coerceAtLeast(0))
                                            )
                                        }) { Text("−") }
                                        TextButton(onClick = {
                                            onChangePrayerAlert(
                                                prayer,
                                                settings.copy(leadMinutes = (settings.leadMinutes + 5).coerceAtMost(120))
                                            )
                                        }) { Text("+") }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("صوت التنبيه", style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = settings.soundEnabled,
                                        onCheckedChange = {
                                            onChangePrayerAlert(prayer, settings.copy(soundEnabled = it))
                                        }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("اهتزاز التنبيه", style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = settings.vibrateEnabled,
                                        onCheckedChange = {
                                            onChangePrayerAlert(prayer, settings.copy(vibrateEnabled = it))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quiet hours (do not disturb) window
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("فترة منع الإزعاج", fontWeight = FontWeight.Bold)
                            Text(
                                "خلال هذه الفترة تصل إشعارات الصلاة بلا صوت وبلا اهتزاز.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prayerAlertProfile.quietHours.enabled,
                            onCheckedChange = {
                                onChangeQuietHours(prayerAlertProfile.quietHours.copy(enabled = it))
                            },
                            modifier = Modifier.testTag("quiet_hours_switch")
                        )
                    }
                    if (prayerAlertProfile.quietHours.enabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "من ${prayerAlertProfile.quietHours.formatted()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "اضبط البداية والنهاية بدقة 15 دقيقة.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TimeOfDayStepperRow(
                            label = "بداية الفترة",
                            value = prayerAlertProfile.quietHours.startMinutes
                        ) { newValue ->
                            onChangeQuietHours(
                                prayerAlertProfile.quietHours.copy(
                                    startMinutes = ((newValue / 15) * 15).coerceIn(0, 1425)
                                )
                            )
                        }
                        TimeOfDayStepperRow(
                            label = "نهاية الفترة",
                            value = prayerAlertProfile.quietHours.endMinutes
                        ) { newValue ->
                            onChangeQuietHours(
                                prayerAlertProfile.quietHours.copy(
                                    endMinutes = ((newValue / 15) * 15).coerceIn(0, 1425)
                                )
                            )
                        }
                    }
                }
            }

            // Time format preference
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "صيغة عرض الوقت",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TimeFormat.entries.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = timeFormat == format,
                                onClick = { onChangeTimeFormat(format) },
                                modifier = Modifier.testTag("time_format_${format.key}")
                            )
                            Text(format.arabicLabel)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "المنطقة الزمنية: Asia/Baghdad (UTC+3). تُستخدم إحداثيات المحافظة المحفوظة ومعاملات AlAdhan المحددة في إعداد المصدر.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Monthly Timetable
            if (monthlySchedules.isNotEmpty()) {
                item {
                    Text(
                        text = "جدول المواقيت لليوم واليوم التالي (${governorate.arabicName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                itemsIndexed(monthlySchedules) { idx, daySched ->
                    GlassCard(
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${idx + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("فجر ${daySched.formatted24h(daySched.fajrMinutes)}", style = MaterialTheme.typography.labelSmall)
                            Text("ظهر ${daySched.formatted24h(daySched.dhuhrMinutes)}", style = MaterialTheme.typography.labelSmall)
                            Text("مغرب ${daySched.formatted24h(daySched.maghribMinutes)}", style = MaterialTheme.typography.labelSmall)
                            Text("عشاء ${daySched.formatted24h(daySched.ishaMinutes)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stepper for a wall-clock time of day (minutes since midnight) used by the quiet-hours window.
 */
@Composable
private fun TimeOfDayStepperRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    val clamped = value.coerceIn(0, 1439)
    val formatted = String.format(
        java.util.Locale.US,
        "%02d:%02d",
        clamped / 60,
        clamped % 60
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onValueChange(((clamped - 15) + 1440) % 1440) }) { Text("−") }
            Text(
                text = formatted,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { onValueChange((clamped + 15) % 1440) }) { Text("+") }
        }
    }
}

@Composable
fun QiblaScreen(
    governorate: GovernorateInfo,
    onBack: () -> Unit,
    onDetectedCoordinates: (Double, Double) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val compassFlow = remember(context) { QiblaSensorController.observeCompass(context) }
    val compass by compassFlow.collectAsStateWithLifecycle(initialValue = CompassReading())
    var gpsStatus by remember { mutableStateOf<String?>(null) }

    val requestQiblaLocation = rememberPreciseDeviceLocationAction(
        onLocation = { fix ->
            onDetectedCoordinates(fix.latitude, fix.longitude)
            gpsStatus = "تم تحديث موقعك بدقة ±${fix.accuracyMeters.toInt()} متر"
        },
        onStatus = { status -> gpsStatus = status }
    )

    val qiblaBearing = governorate.qiblaBearingDegrees.toFloat()
    val relativeQiblaAngle = ((qiblaBearing - compass.azimuthDegrees + 360f) % 360f)
    // Without a working compass sensor the alignment cannot be trusted, so it is never claimed.
    val isAligned = compass.isSensorAvailable &&
        (abs(relativeQiblaAngle) <= 6f || abs(relativeQiblaAngle - 360f) <= 6f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val ringColor = MaterialTheme.colorScheme.outline

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "اتجاه القبلة الشريفة",
            subtitle = "محسوب بالدائرة العظمى من ${governorate.arabicName} نحو الكعبة المشرفة",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("qibla_screen_content"),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = CircleShape,
                    color = if (isAligned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isAligned) {
                            "أنت الآن باتجاه القبلة الصحيح ✓"
                        } else {
                            "زاوية القبلة من الشمال: ${governorate.qiblaBearingDegrees}° · المسافة للكعبة: ${governorate.distanceToKaabaKm} كم"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isAligned) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // The live compass is only drawn when the device really has a usable sensor.
            if (!compass.isSensorAvailable) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Text(
                            text = "مستشعر البوصلة غير متوفر على هذا الجهاز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لا يمكن قياس اتجاه هاتفك الآن، ولذلك لا يُرسم سهم القبلة تفادياً لإظهار اتجاه غير دقيق. استعن بتطبيق بوصلة آخر أو ببوصلة خارجية، أو استخدم الزاوية الظاهرة أعلاه مع بوصلة يدوية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Sensor Compass Canvas (uses DrawScope size.width/height dynamically).
            // It is only rendered when the device really exposes a usable compass sensor.
            if (compass.isSensorAvailable) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 14.dp.toPx()

                        drawCircle(
                            color = ringColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Rotate compass dial opposite to device heading
                        rotate(degrees = -compass.azimuthDegrees, pivot = center) {
                            for (deg in 0 until 360 step 15) {
                                rotate(degrees = deg.toFloat(), pivot = center) {
                                    val tickLen = if (deg % 90 == 0) 14.dp.toPx() else 7.dp.toPx()
                                    drawLine(
                                        color = if (deg == 0) primaryColor else ringColor,
                                        start = Offset(center.x, center.y - radius),
                                        end = Offset(center.x, center.y - radius + tickLen),
                                        strokeWidth = if (deg % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                                    )
                                }
                            }
                        }

                        // Rotate Golden Qibla Pointer toward exact Kaaba bearing
                        rotate(degrees = relativeQiblaAngle, pivot = center) {
                            drawLine(
                                color = HeritageGold,
                                start = center,
                                end = Offset(center.x, center.y - radius + 24.dp.toPx()),
                                strokeWidth = 5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            val arrowTip = Path().apply {
                                moveTo(center.x, center.y - radius + 8.dp.toPx())
                                lineTo(center.x - 10.dp.toPx(), center.y - radius + 28.dp.toPx())
                                lineTo(center.x + 10.dp.toPx(), center.y - radius + 28.dp.toPx())
                                close()
                            }
                            drawPath(arrowTip, color = HeritageGold)
                        }

                        drawCircle(color = primaryColor, radius = 8.dp.toPx(), center = center)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(110.dp))
                        Text(
                            text = "اتجاه الهاتف: ${compass.azimuthDegrees.roundToInt()}°",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (compass.needsCalibration) {
                            Text(
                                text = "حرّك الجهاز على شكل رقم ٨ لمعايرة البوصلة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            }

            item {
                OutlinedButton(
                    onClick = requestQiblaLocation
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when {
                            PreciseDeviceLocationAccess.hasPrecisePermission(context) -> "تحديث موقعي بدقة"
                            PreciseDeviceLocationAccess.hasShownPermissionPrompt(context) -> "إعدادات إذن الموقع"
                            else -> "استخدم موقعي بدقة (GPS)"
                        }
                    )
                }
                if (gpsStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(gpsStatus!!, style = MaterialTheme.typography.labelSmall, color = primaryColor)
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "إرشادات المعايرة للدقة عند الصلاة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• أبعد الهاتف عن المعادن والأغلفة المغناطيسية والسماعات.")
                    Text("• حرّك الهاتف في الهواء على شكل رقم (8) عدة مرات لمعايرة المستشعر المغناطيسي.")
                    Text("• عند عدم توفر مستشعر مغناطيسي، اعتمد زاوية القبلة المحسوبة لمحافظتك (${governorate.qiblaBearingDegrees}° من الشمال).")
                }
            }
        }
    }
}
