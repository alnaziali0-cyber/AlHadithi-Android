package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerMinuteOffsets
import com.example.media.AdhanAudioStorage
import com.example.media.AdhanPlaybackService
import com.example.media.CompassReading
import com.example.media.QiblaSensorController
import com.example.ui.components.EmeraldHeroCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.HeritageGold
import com.example.util.SystemActionsHelper
import com.example.media.PrayerNotificationScheduler
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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
    calcMethodCode: Int,
    offsets: PrayerMinuteOffsets,
    notifyPrayers: Boolean,
    coordinatesLatitude: Double,
    coordinatesLongitude: Double,
    isUsingDeviceLocation: Boolean,
    onBack: () -> Unit,
    onOpenGovernorateSheet: () -> Unit,
    onSelectMethod: (Int) -> Unit,
    onUpdateOffsets: (PrayerMinuteOffsets) -> Unit,
    onToggleNotifyPrayers: (Boolean) -> Unit,
    onSetPrayerCoordinates: (Double, Double) -> Unit,
    onClearPrayerCoordinates: () -> Unit,
    onRefreshPrayerSchedule: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var adhanStatus by remember { mutableStateOf<String?>(null) }
    var isImportingAdhan by remember { mutableStateOf(false) }
    var hasAdhanAudio by remember { mutableStateOf(AdhanAudioStorage.hasAudio(context)) }
    val adhanAudioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null && !isImportingAdhan) {
            coroutineScope.launch {
                isImportingAdhan = true
                val result = withContext(Dispatchers.IO) {
                    AdhanAudioStorage.importAudio(context, selectedUri)
                }
                isImportingAdhan = false
                result.onSuccess {
                    hasAdhanAudio = true
                    adhanStatus = "تم حفظ ملف الأذان داخل التطبيق، وسيعمل دون الحاجة إلى الإنترنت."
                }.onFailure { error ->
                    adhanStatus = error.message ?: "تعذر استيراد ملف الأذان."
                }
            }
        }
    }
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
            locationStatus = "تعذر فتح إعداد إذن التنبيهات الدقيقة؛ افتح إعدادات التطبيق يدوياً"
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleNotifyPrayers(granted)
        if (granted) requestExactAlarmAccess()
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellation = CancellationTokenSource()
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .addOnSuccessListener { location ->
                    if (location == null) {
                        locationStatus = "لم يتمكن الهاتف من تحديد موقع حديث. تأكد من تشغيل GPS وحاول مجدداً."
                    } else {
                        onSetPrayerCoordinates(location.latitude, location.longitude)
                        val precise = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        locationStatus = if (precise) {
                            "تم اعتماد إحداثيات GPS الدقيقة لمواقيت الصلاة"
                        } else {
                            "تم اعتماد موقع تقريبي؛ اختر (دقيق) من نافذة أذونات أندرويد لتحسين المواقيت"
                        }
                    }
                }
                .addOnFailureListener {
                    locationStatus = "تعذر تحديد الموقع. تحقق من خدمة الموقع ثم أعد المحاولة."
                }
        } catch (_: SecurityException) {
            locationStatus = "لم يُمنح إذن الموقع؛ ستُستخدم إحداثيات المحافظة"
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allowed = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            SystemActionsHelper.hasLocationPermission(context)
        if (allowed) requestCurrentLocation()
        else locationStatus = "لم يتم منح إذن الموقع؛ يمكنك المتابعة باستخدام إحداثيات المحافظة"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "مواقيت الصلاة — ${governorate.arabicName}",
            subtitle = String.format(
                java.util.Locale.US,
                "%1$.5f° · %2$.5f° %3\$s",
                coordinatesLatitude,
                coordinatesLongitude,
                if (isUsingDeviceLocation) "(موقع GPS محفوظ)" else "(مركز المحافظة)"
            ),
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
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("الموقع المستخدم لحساب المواقيت", fontWeight = FontWeight.Bold)
                    Text(
                        if (isUsingDeviceLocation) "يُستخدم موقع GPS المحفوظ بدلاً من مركز المحافظة."
                        else "يُستخدم مركز محافظة ${governorate.arabicName}. تحديد GPS أدق للمناطق داخل المحافظة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            // Even when approximate access was granted earlier, request again so
                            // Android can offer the user the precise-location upgrade.
                            if (SystemActionsHelper.hasPreciseLocationPermission(context)) {
                                requestCurrentLocation()
                            } else {
                                locationPermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        }) {
                            Icon(Icons.Outlined.MyLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (SystemActionsHelper.hasPreciseLocationPermission(context)) "تحديث موقعي" else "السماح بالموقع الدقيق")
                        }
                        if (isUsingDeviceLocation) {
                            OutlinedButton(onClick = {
                                onClearPrayerCoordinates()
                                locationStatus = "تم الرجوع إلى إحداثيات المحافظة"
                            }) { Text("المحافظة") }
                        }
                    }
                    locationStatus?.let { status ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                EmeraldHeroCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "مواقيت اليوم — ${schedule.hijriDate.formattedArabic()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "متبقٍ لـ ${countdown.prayerArabicName}: ${countdown.countdownHhMmSs}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDF2D0)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "المصدر: ${schedule.sourceLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Detailed 7 Timings Card with Distinct Icons
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    val rows = listOf(
                        Triple("الفجر", schedule.formatted12h(schedule.fajrMinutes), Icons.Outlined.WbTwilight),
                        Triple("الشروق", schedule.formatted12h(schedule.sunriseMinutes), Icons.Outlined.WbSunny),
                        Triple("الظهر", schedule.formatted12h(schedule.dhuhrMinutes), Icons.Outlined.LightMode),
                        Triple("العصر", schedule.formatted12h(schedule.asrMinutes), Icons.Outlined.WbTwilight),
                        Triple("المغرب", schedule.formatted12h(schedule.maghribMinutes), Icons.Outlined.NightsStay),
                        Triple("العشاء", schedule.formatted12h(schedule.ishaMinutes), Icons.Outlined.Bedtime),
                        Triple("منتصف الليل الشرعي", schedule.formatted12h(schedule.midnightMinutes), Icons.Outlined.AutoAwesome)
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
                }
            }

            // User-selected, licensed adhan audio. The system picker grants access only to this file.
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "صوت الأذان عند دخول وقت الصلاة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (hasAdhanAudio) "يوجد تسجيل أذان محفوظ على الجهاز وسيعمل مع الإشعار." else "لم يتم اختيار تسجيل أذان بعد. يمكنك اختيار تسجيل تملك حق استخدامه، مثل تسجيل الأذان الخاص بالعتبة الحسينية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !isImportingAdhan,
                            onClick = { adhanAudioPicker.launch(arrayOf("audio/*")) }
                        ) {
                            Icon(Icons.Outlined.MusicNote, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isImportingAdhan) "جارٍ حفظ الملف…" else "اختيار ملف الأذان")
                        }
                        if (hasAdhanAudio) {
                            OutlinedButton(onClick = {
                                runCatching { AdhanPlaybackService.play(context) }
                                    .onSuccess { adhanStatus = "بدأ اختبار تشغيل تسجيل الأذان." }
                                    .onFailure { adhanStatus = "تعذر بدء التشغيل؛ تحقق من إعدادات الصوت." }
                            }) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                Text("تجربة")
                            }
                            TextButton(onClick = {
                                AdhanPlaybackService.stop(context)
                                AdhanAudioStorage.clear(context)
                                hasAdhanAudio = false
                                adhanStatus = "تم حذف التسجيل المحفوظ. سيعود تنبيه النظام الافتراضي."
                            }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                            }
                        }
                    }
                    adhanStatus?.let { status ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "لا يحتاج اختيار الملف إلى إذن عام للوصول إلى كل الصوتيات؛ يفتح أندرويد منتقي الملفات وتختار تسجيلاً واحداً، ثم يُحفظ داخل مساحة التطبيق الخاصة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Calculation Method Picker
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "طريقة الحساب الفلكي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PrayerCalcMethod.entries.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = calcMethodCode == method.apiCode,
                                onClick = { onSelectMethod(method.apiCode) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(method.arabicTitle, fontWeight = FontWeight.SemiBold)
                                Text(
                                    method.arabicSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Manual Minute Tuning
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ضبط يدوي للدقائق (الاحتياط المحلي)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "لمطابقة تقويم مسجدك أو الوقف المحلي بدقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onUpdateOffsets(PrayerMinuteOffsets()) }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصفير")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "مثال: إذا كان تقويم الجهة المحلية المعتمدة للفجر 04:59 والتطبيق يعرض 05:08، اضبط الفجر على −9 دقائق. استخدم تقويماً موثوقاً لمنطقتك ولا تغيّر الوقت بالتخمين.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OffsetStepperRow("الفجر", offsets.fajr) { onUpdateOffsets(offsets.copy(fajr = it)) }
                    OffsetStepperRow("الشروق", offsets.sunrise) { onUpdateOffsets(offsets.copy(sunrise = it)) }
                    OffsetStepperRow("الظهر", offsets.dhuhr) { onUpdateOffsets(offsets.copy(dhuhr = it)) }
                    OffsetStepperRow("العصر", offsets.asr) { onUpdateOffsets(offsets.copy(asr = it)) }
                    OffsetStepperRow("المغرب", offsets.maghrib) { onUpdateOffsets(offsets.copy(maghrib = it)) }
                    OffsetStepperRow("العشاء", offsets.isha) { onUpdateOffsets(offsets.copy(isha = it)) }
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
                        if (!exactAlarmAllowed) {
                            OutlinedButton(onClick = { requestExactAlarmAccess() }) {
                                Text("السماح بالتنبيهات الدقيقة")
                            }
                        }
                    }
                }
            }

            // Monthly Timetable
            if (monthlySchedules.isNotEmpty()) {
                item {
                    Text(
                        text = "جدول المواقيت الشهري (${governorate.arabicName})",
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

@Composable
private fun OffsetStepperRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onValueChange((value - 1).coerceIn(-30, 30)) }) { Text("−") }
            Text(
                text = if (value > 0) "+$value د" else "$value د",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { onValueChange((value + 1).coerceIn(-30, 30)) }) { Text("+") }
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

    @SuppressLint("MissingPermission")
    val locLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        onDetectedCoordinates(loc.latitude, loc.longitude)
                        gpsStatus = "تم تحديث موقعك عبر GPS بنجاح"
                    } else {
                        gpsStatus = "تعذر قراءة الموقع الآني — تُستخدم إحداثيات ${governorate.arabicName}"
                    }
                }
        } else {
            gpsStatus = "تم الاستمرار باستخدام إحداثيات محافظة ${governorate.arabicName}"
        }
    }

    val qiblaBearing = governorate.qiblaBearingDegrees.toFloat()
    val relativeQiblaAngle = ((qiblaBearing - compass.azimuthDegrees + 360f) % 360f)
    val isAligned = abs(relativeQiblaAngle) <= 6f || abs(relativeQiblaAngle - 360f) <= 6f
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
                        text = if (isAligned) "أنت الآن باتجاه القبلة الصحيح ✓"
                        else "زاوية القبلة من الشمال: ${governorate.qiblaBearingDegrees}° · المسافة للكعبة: ${governorate.distanceToKaabaKm} كم",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isAligned) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Live Sensor Compass Canvas (uses DrawScope size.width/height dynamically)
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
                            text = if (compass.isSensorAvailable) "اتجاه الهاتف: ${compass.azimuthDegrees.roundToInt()}°"
                            else "مستشعر البوصلة غير متوفر على هذا الجهاز",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        locLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استخدم موقعي الحالي (GPS)")
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
