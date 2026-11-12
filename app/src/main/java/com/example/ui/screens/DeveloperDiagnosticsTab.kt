package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.components.GlassCard
import com.example.ui.components.SettingsActionTile
import com.example.ui.components.SettingsRowDivider
import com.example.ui.components.SettingsSectionCard
import com.example.ui.theme.LocalStatusColors
import com.example.ui.viewmodel.AlHadithiViewModel
import com.example.util.SystemActionsHelper

/**
 * The developer-only diagnostics and tools tab.
 *
 * It is only reachable from the unlocked developer panel, it never appears in the normal user
 * interface, and every destructive action asks for a confirmation first.
 */
@Composable
fun DeveloperDiagnosticsTab(
    contentCounters: AlHadithiViewModel.ContentCounters,
    verificationMessage: String,
    onRefreshCounters: () -> Unit,
    onTestPrayerFetch: () -> Unit,
    onTestNotifications: () -> Unit,
    onTestAdhan: () -> Unit,
    onRescheduleAlerts: () -> Unit,
    onClearCache: () -> Unit,
    onDeleteLocalData: () -> Unit,
    onVerifyDatabase: () -> Unit,
    onVerifyQuranIntegrity: () -> Unit,
    onVerifyPreferences: () -> Unit,
    onCopyErrorLog: () -> Unit,
    onShareDiagnostics: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
    onSendReportToDeveloper: () -> Unit,
    onLeaveDeveloperMode: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val statusColors = LocalStatusColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var lastActionMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------------- تنبيه الأدوات للمطور فقط ----------------
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = statusColors.warningContainer,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("developer_only_notice")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = statusColors.warning,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "هذه الأدوات متاحة للمطوّر فقط. لا تشارك محتواها مع المستخدمين العاديين.",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColors.onWarningContainer
                )
            }
        }

        // ---------------- 1. تشخيص التطبيق ----------------
        SettingsSectionCard(
            title = "تشخيص التطبيق",
            icon = Icons.Outlined.AdminPanelSettings,
            description = "حالة كل خدمة داخل التطبيق"
        ) {
            Column {
                DiagnosticValueRow("رقم الإصدار", BuildConfig.VERSION_NAME)
                DiagnosticValueRow("رقم البناء", BuildConfig.VERSION_CODE.toString())
                DiagnosticValueRow("إصدار Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                DiagnosticValueRow("نوع الجهاز", "${Build.MANUFACTURER} ${Build.MODEL}")
                DiagnosticValueRow("حالة الإنترنت", internetStatus(context))
                DiagnosticValueRow("حالة Firebase", "متاح عبر حزمة Firebase · تحقق عند استخدام لوحة المطور")
                DiagnosticValueRow(
                    "حالة قاعدة البيانات",
                    "أحاديث: ${contentCounters.hadiths} · أدعية: ${contentCounters.duasAndZiyarat} · أذكار: ${contentCounters.adhkar}"
                )
                DiagnosticValueRow("حالة الإشعارات", notificationStatus(context))
                DiagnosticValueRow("حالة الموقع", "يعتمد على أذونات المستخدم والإحداثيات المحفوظة")
                DiagnosticValueRow("حالة المنبهات", "تُجدول تلقائياً بعد نجاح جلب المواقيت")
                DiagnosticValueRow("حالة أذونات الصوت", "تتبع إعدادات نظام الصوت")
                DiagnosticValueRow("حالة التخزين", "ذاكرة تخزين داخلية خاصة بالتطبيق")
                DiagnosticValueRow("تحقق المواقيت", verificationMessage)
            }
        }

        // ---------------- 2. إدارة البيانات ----------------
        SettingsSectionCard(
            title = "إدارة البيانات",
            icon = Icons.Outlined.Storage,
            description = "فحص وتصفية محتوى قاعدة البيانات"
        ) {
            Column {
                SettingsActionTile(
                    title = "عرض عدد الأحاديث والأدعية والزيارات",
                    description = "عدّ مباشر من قاعدة البيانات المحلية",
                    icon = Icons.Outlined.Memory,
                    value = "${contentCounters.hadiths + contentCounters.duasAndZiyarat}",
                    onClick = onRefreshCounters,
                    testTag = "developer_count_content"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "فحص قاعدة البيانات",
                    description = "إعادة قراءة العدّادات والتأكد من سلامتها",
                    icon = Icons.Outlined.Science,
                    onClick = {
                        onVerifyDatabase()
                        lastActionMessage = "تم فحص قاعدة البيانات وتحديث العدّادات."
                    },
                    testTag = "developer_verify_database"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "فحص اكتمال بيانات القرآن",
                    description = "عدد سور الحزمة المحلية والآيات المخزنة",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = {
                        onVerifyQuranIntegrity()
                        lastActionMessage = "تم فحص بيانات القرآن."
                    },
                    testTag = "developer_verify_quran"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "اختبار حفظ واسترجاع الإعدادات",
                    description = "كتابة قيمة مؤقتة وقراءتها ثم حذفها",
                    icon = Icons.Outlined.Refresh,
                    onClick = {
                        onVerifyPreferences()
                        lastActionMessage = "تم اختبار حفظ واسترجاع الإعدادات."
                    },
                    testTag = "developer_verify_preferences"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "إعادة تحميل البيانات",
                    description = "جلب أحدث المواقيت والمحتوى من المصدر المركزي",
                    icon = Icons.Outlined.Cached,
                    onClick = {
                        onRefreshCounters()
                        lastActionMessage = "تم طلب إعادة تحميل البيانات."
                    },
                    testTag = "developer_reload_data"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "مسح الكاش",
                    description = "حذف النصوص المنزّلة مع الإبقاء على الأساسيات",
                    icon = Icons.Outlined.DeleteForever,
                    onClick = { showClearConfirm = true },
                    testTag = "developer_clear_cache"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "حذف البيانات المحلية",
                    description = "حذف المفضلة والسجل والإعدادات والتنزيلات",
                    icon = Icons.Outlined.DeleteForever,
                    valueColor = statusColors.error,
                    onClick = { showDeleteConfirm = true },
                    testTag = "developer_delete_local_data"
                )
            }
        }

        // ---------------- 3. اختبار المواقيت ----------------
        SettingsSectionCard(
            title = "اختبار المواقيت",
            icon = Icons.Outlined.Refresh,
            description = "فحص جلب المواقيت والتنبيهات والأذان"
        ) {
            Column {
                SettingsActionTile(
                    title = "اختبار جلب المواقيت",
                    description = "طلب أحدث جدول من المصدر المركزي",
                    icon = Icons.Outlined.Refresh,
                    onClick = {
                        onTestPrayerFetch()
                        lastActionMessage = "تم إرسال طلب جلب المواقيت."
                    },
                    testTag = "developer_test_prayer_fetch"
                )
                SettingsRowDivider()
                DiagnosticValueRow("مصدر المواقيت", "AlAdhan API · method=99 · methodSettings=17.7,null,4")
                DiagnosticValueRow("طريقة الحساب", "الجعفرية (مخصص)")
                SettingsRowDivider()
                SettingsActionTile(
                    title = "اختبار الصلاة القادمة",
                    description = "إعادة حساب الصلاة التالية والعدّاد",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = {
                        onTestPrayerFetch()
                        lastActionMessage = "تمت إعادة حساب الصلاة القادمة."
                    },
                    testTag = "developer_test_next_prayer"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "اختبار الإشعارات",
                    description = "إرسال إشعار تجريبي إلى شريط الإشعارات",
                    icon = Icons.Outlined.Notifications,
                    onClick = {
                        onTestNotifications()
                        lastActionMessage = "تم إرسال إشعار تجريبي."
                    },
                    testTag = "developer_test_notifications"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "اختبار الأذان",
                    description = "تشغيل تسجيل الأذان المضمّن",
                    icon = Icons.Outlined.Notifications,
                    onClick = {
                        onTestAdhan()
                        lastActionMessage = "تم تشغيل الأذان."
                    },
                    testTag = "developer_test_adhan"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "إعادة جدولة التنبيهات",
                    description = "إعادة بناء جدول تنبيهات الصلاة من المواقيت الحالية",
                    icon = Icons.Outlined.Refresh,
                    onClick = {
                        onRescheduleAlerts()
                        lastActionMessage = "تمت إعادة جدولة التنبيهات."
                    },
                    testTag = "developer_reschedule_alerts"
                )
                SettingsRowDivider()
                DiagnosticValueRow("آخر الأخطاء", lastActionMessage ?: "لا توجد أخطاء مسجّلة في الجلسة الحالية")
            }
        }

        // ---------------- 4. اختبار الواجهة ----------------
        SettingsSectionCard(
            title = "اختبار الواجهة",
            icon = Icons.Outlined.Science,
            description = "فحوص العرض والخطوط والاتجاه"
        ) {
            Column {
                SettingsActionTile(
                    title = "تفعيل شبكة التصحيح",
                    description = "إظهار حدود العناصر لتخطيط دقيق",
                    icon = Icons.Outlined.GridOn,
                    onClick = {
                        lastActionMessage = "شبكة التصحيح: يمكن تفعيلها من خيارات المطوّر في نظام Android."
                    },
                    testTag = "developer_debug_grid"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "عرض حدود العناصر",
                    description = "مقاس كل عنصر ومقاس النص",
                    icon = Icons.Outlined.Straighten,
                    onClick = {
                        lastActionMessage = "حدود العناصر: تُفحص عبر Layout Inspector في Android Studio."
                    },
                    testTag = "developer_show_bounds"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "اختبار الوضع الداكن",
                    description = "تبديل المظهر إلى الوضع الداكن الفاتح",
                    icon = Icons.Outlined.DarkMode,
                    onClick = {
                        lastActionMessage = "الوضع الداكن: بدّله من إعدادات المظهر."
                    },
                    testTag = "developer_test_dark_mode"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "اختبار RTL",
                    description = "التطبيق مربوط دائماً بالاتجاه من اليمين إلى اليسار",
                    icon = Icons.Outlined.CompareArrows,
                    onClick = {
                        lastActionMessage = "RTL: الاتجاه مفروض داخل AlHadithiTheme."
                    },
                    testTag = "developer_test_rtl"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "اختبار أحجام الشاشات",
                    description = "التخطيط متجاوب ومختبر على شاشة صغيرة وكبيرة",
                    icon = Icons.Outlined.PhoneAndroid,
                    onClick = {
                        lastActionMessage = "اختبار الشاشات: يُنفَّذ عبر Android Studio Device Manager."
                    },
                    testTag = "developer_test_screens"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "التقاط Screenshot للاختبار",
                    description = "التقاط صورة الشاشة الحالية ومشاركتها",
                    icon = Icons.Outlined.Screenshot,
                    onClick = {
                        lastActionMessage = "التقاط الشاشة: استخدم SystemActionsHelper لمشاركة لقطة الشاشة."
                        onShareDiagnostics()
                    },
                    testTag = "developer_capture_screenshot"
                )
            }
        }

        // ---------------- 5. أدوات الدعم ----------------
        SettingsSectionCard(
            title = "أدوات الدعم",
            icon = Icons.Outlined.BugReport,
            description = "نسخ السجل ومشاركة التقرير وإدارة الإعدادات"
        ) {
            Column {
                SettingsActionTile(
                    title = "نسخ سجل الأخطاء",
                    description = "نسخ آخر رسائل التشخيص إلى الحافظة",
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        onCopyErrorLog()
                        lastActionMessage = "تم نسخ سجل الأخطاء."
                    },
                    testTag = "developer_copy_error_log"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "مشاركة تقرير تشخيص",
                    description = "إرسال ملخص الحالة إلى تطبيق المشاركة",
                    icon = Icons.Outlined.Share,
                    onClick = {
                        onShareDiagnostics()
                        lastActionMessage = "تم تجهيز تقرير التشخيص للمشاركة."
                    },
                    testTag = "developer_share_diagnostics"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "تصدير إعدادات التطبيق",
                    description = "حفظ نسخة من تفضيلاتك على الجهاز",
                    icon = Icons.Outlined.Upload,
                    onClick = {
                        onExportSettings()
                        lastActionMessage = "تم تصدير الإعدادات."
                    },
                    testTag = "developer_export_settings"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "استيراد إعدادات التطبيق",
                    description = "استرجاع نسخة سابقة من التفضيلات",
                    icon = Icons.Outlined.Download,
                    onClick = {
                        onImportSettings()
                        lastActionMessage = "تم استيراد الإعدادات."
                    },
                    testTag = "developer_import_settings"
                )
                SettingsRowDivider()
                SettingsActionTile(
                    title = "إرسال تقرير للمطور",
                    description = "إرسال التقرير إلى $APP_DEVELOPER_NAME",
                    icon = Icons.AutoMirrored.Outlined.Send,
                    onClick = {
                        onSendReportToDeveloper()
                        lastActionMessage = "تم إرسال التقرير للمطور."
                    },
                    testTag = "developer_send_report"
                )
            }
        }

        // ---------------- الخروج من وضع المطور ----------------
        OutlinedButton(
            onClick = onLeaveDeveloperMode,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("developer_leave_mode_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = statusColors.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("خروج من وضع المطور")
        }

        lastActionMessage?.let { message ->
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

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("تأكيد مسح الكاش") },
            text = { Text("هل أنت متأكد؟ سيتم حذف الكاش المحلي ولا يمكن التراجع عن هذه العملية.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearCache()
                        lastActionMessage = "تم مسح الكاش."
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("مسح") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد حذف البيانات المحلية") },
            text = { Text("هل أنت متأكد؟ سيتم حذف البيانات المحلية ولا يمكن التراجع عن هذه العملية.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteLocalData()
                        lastActionMessage = "تم حذف البيانات المحلية."
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("حذف نهائي") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}

/** A label/value row used by the diagnostics card. */
@Composable
private fun DiagnosticValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1.4f)
        )
    }
}

/** Reports whether the device currently has a validated internet connection. */
private fun internetStatus(context: Context): String {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return "غير متاح"
    val network = manager.activeNetwork ?: return "غير متصل"
    val capabilities = manager.getNetworkCapabilities(network) ?: return "غير متصل"
    return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        "متصل"
    } else {
        "غير متصل"
    }
}

/** Reports whether the notification permission is granted on Android 13+. */
private fun notificationStatus(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return "غير مطلوب على هذا الإصدار"
    val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    return if (granted) "مسموح" else "غير مسموح"
}
