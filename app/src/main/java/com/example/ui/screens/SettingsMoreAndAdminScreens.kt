package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ContentSourceEntity
import com.example.data.local.UserSettingsState
import com.example.domain.IraqGovernorates
import com.example.domain.PrayerCalcMethod
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SubScreenTopBar
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.LocalIsDarkThemeActive
import kotlin.math.roundToInt

private data class HubMenuCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: AppDestination
)

@Composable
fun MoreHubScreen(
    onNavigate: (AppDestination) -> Unit
) {
    // Admin/Developer panel is strictly HIDDEN here — revealed only when shaking the phone!
    val items = remember {
        listOf(
            HubMenuCard("مواقيت الصلاة والتقويم الشهري", "الحساب الجعفري، منتصف الليل الشرعي، وضبط الدقائق", Icons.Outlined.Schedule, AppDestination.PRAYER_TIMES),
            HubMenuCard("المسبحة الإلكترونية المطورة", "تسبيح الزهراء (ع) ٣٤/٣٣/٣٣، الأهداف اليومية، والاهتزاز", Icons.Outlined.Spa, AppDestination.TASBIH),
            HubMenuCard("اتجاه القبلة الشريفة", "بوصلة حية بمستشعر الجهاز مع المسافة للكعبة المشرفة", Icons.Outlined.Explore, AppDestination.QIBLA),
            HubMenuCard("المكتبة الصوتية الحسينية والقرآنية", "لطميات ونعي وأدعية وتلاوات؛ وبعض المواد تحتاج اتصالاً", Icons.Outlined.Headphones, AppDestination.AUDIO_LIBRARY),
            HubMenuCard("موسوعة الحديث والروايات", "أحاديث النبي ﷺ والأئمة الاثني عشر (ع) مع المصادر", Icons.Outlined.FormatQuote, AppDestination.HADITH),
            HubMenuCard("المناهج والعلوم الإسلامية", "دروس العقيدة والفقه الجعفري واختبارات تفاعلية", Icons.Outlined.School, AppDestination.CURRICULA),
            HubMenuCard("التقويم الهجري والمناسبات", "مواليد وشهادات أهل البيت (ع) مع تصحيح الهلال", Icons.Outlined.CalendarMonth, AppDestination.CALENDAR_OCCASIONS),
            HubMenuCard("الخلفيات الإسلامية والحسينية", "معرض خلفيات هادئة عالية الدقة للتنزيل والمشاركة", Icons.Outlined.Wallpaper, AppDestination.WALLPAPERS),
            HubMenuCard("البحث الشامل والباحث الذكي", "بحث في النصوص المحلية والمخزنة + باحث علمي يتطلب اتصالاً", Icons.Outlined.AutoAwesome, AppDestination.SEARCH),
            HubMenuCard("المفضلة وسجل القراءة", "محفوظاتك المحلية على جهازك دون الحاجة لحساب", Icons.Outlined.Bookmarks, AppDestination.FAVORITES),
            HubMenuCard("الإعدادات والمصادر", "المحافظة، الخطوط، الوضع الليلي، والتنبيهات", Icons.Outlined.Tune, AppDestination.SETTINGS)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "جميع أقسام الحديثي",
            subtitle = "الموسوعة الإسلامية الشيعية؛ تتوفر الأقسام المحلية دون اتصال، وبعض الخدمات تحتاج الإنترنت"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("more_hub_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { card ->
                GlassCard(
                    cornerRadius = 20.dp,
                    onClick = { onNavigate(card.destination) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = card.icon,
                                contentDescription = card.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(11.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = card.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettingsState,
    sources: List<ContentSourceEntity>,
    onBack: () -> Unit,
    onOpenGovernorateSheet: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectFont: (AppFontChoice) -> Unit,
    onChangeFontScale: (Float) -> Unit,
    onChangeHijriOffset: (Int) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onUpdateNotifications: (Boolean?, Int?, Boolean?, Boolean?) -> Unit,
    onClearQuranCache: () -> Unit,
    onResetDefaults: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val isDark = LocalIsDarkThemeActive.current
    val currentGov = IraqGovernorates.byId(settings.governorateId)
    val currentMethod = PrayerCalcMethod.fromCode(settings.prayerCalcMethodCode)
    var showAdFreeDialog by remember { mutableStateOf(false) }
    var showSourcesDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "الإعدادات والتفضيلات",
            subtitle = "تُحفظ جميع تفضيلاتك محلياً وتعمل بدون إنترنت",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 0. Offline-Ready Status Card
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OfflinePin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("العمل المحلي والاتصال", fontWeight = FontWeight.Bold)
                            Text(
                                "المواقيت والقبلة والمسبحة والمواد المحفوظة تعمل محلياً. نص القرآن يُحمّل عند فتح السورة لأول مرة ويُخزّن محلياً؛ يلزم الإنترنت لتحميل السور غير المخزنة. لا تتضمن هذه النسخة مجموعة بيانات كاملة وموثقة للقرآن دون اتصال.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 1. Governorate & Prayer Calc
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المحافظة العراقية المعتمدة", fontWeight = FontWeight.Bold)
                            Text(
                                "${currentGov.arabicName} · ${currentMethod.arabicTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(onClick = onOpenGovernorateSheet) {
                            Text("تغيير")
                        }
                    }
                }
            }

            // 2. Appearance, Dark Mode, Fonts & Scale
            item {
                GlassCard {
                    Text("المظهر الزجاجي والخطوط العربية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الوضع الليلي الزجاجي الهادئ")
                        Switch(
                            checked = settings.darkMode,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("settings_dark_mode_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("نوع خط الواجهة:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFontChoice.entries.forEach { choice ->
                            GlassFilterChip(
                                label = choice.arabicLabel,
                                selected = settings.fontChoice == choice,
                                onClick = { onSelectFont(choice) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "حجم خط القراءة للقرآن والأدعية: ${(settings.readingFontScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = settings.readingFontScale,
                        onValueChange = onChangeFontScale,
                        valueRange = 0.85f..1.55f
                    )
                }
            }

            // 3. Hijri Offset & Notifications & Haptics
            item {
                GlassCard {
                    Text("التقويم والتنبيهات والاهتزاز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تصحيح التاريخ الهجري (± يوم)")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onChangeHijriOffset(settings.hijriOffsetDays - 1) }) { Text("−") }
                            Text("${settings.hijriOffsetDays}", fontWeight = FontWeight.Bold)
                            TextButton(onClick = { onChangeHijriOffset(settings.hijriOffsetDays + 1) }) { Text("+") }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تذكير مناسبات أهل البيت (ع)")
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
                        Text("تذكير أذكار الصباح والمساء")
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
                        Text("اهتزاز المسبحة والأذكار")
                        Switch(checked = settings.hapticTasbihEnabled, onCheckedChange = onToggleHaptic)
                    }
                }
            }

            // 4. Transparent Ad-Free Status
            item {
                GlassCard(
                    onClick = { showAdFreeDialog = true },
                    modifier = Modifier.testTag("settings_ad_free_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("خلوّ التطبيق من الإعلانات التجارية ✓", fontWeight = FontWeight.Bold)
                            Text(
                                "اضغط لعرض شفافية التطبيق وخصوصية المستخدم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Data & Verified Sources (No visible Admin button — shake device to reveal!)
            item {
                GlassCard {
                    Text("البيانات والمصادر العلمية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onClearQuranCache()
                            Toast.makeText(context, "تم تحديث الذاكرة المؤقتة مع الاحتفاظ بمفضلتك والمصحف المدمج", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تحديث الذاكرة المؤقتة (لا يمسح المفضلة)")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showSourcesDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حول التطبيق ومصادره العلمية المعتمدة")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onResetDefaults,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("استعادة الإعدادات الافتراضية")
                    }
                }
            }
        }
    }

    if (showAdFreeDialog) {
        AlertDialog(
            onDismissRequest = { showAdFreeDialog = false },
            containerColor = if (isDark) Color(0xFF122825).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f),
            title = { Text(stringResource(R.string.ad_free_title)) },
            text = { Text(stringResource(R.string.ad_free_message)) },
            confirmButton = {
                Button(onClick = { showAdFreeDialog = false }) { Text("حسناً") }
            }
        )
    }

    if (showSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showSourcesDialog = false },
            containerColor = if (isDark) Color(0xFF122825).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f),
            title = { Text("مصادر تطبيق الحديثي المعتمدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sources.forEach { src ->
                        Text("• ${src.sourceTitle}: ${src.authorAndDeathYear}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "خصوصية المستخدم: تُحفظ التفضيلات محلياً، ولا يشترط إنشاء حساب لاستخدام الأقسام الأساسية.",
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
}
