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

