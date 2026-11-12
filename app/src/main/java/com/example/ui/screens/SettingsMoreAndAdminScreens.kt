package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.VolunteerActivism
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
import com.example.ui.components.GlassCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.SectionDivider
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.LocalIsDarkThemeActive
import com.example.ui.theme.PastelGreenSurface
import com.example.ui.theme.PastelBlueSurface
import com.example.ui.theme.PastelPurpleSurface
import com.example.ui.theme.PastelPeachSurface
import com.example.ui.theme.PastelSandSurface
import com.example.ui.theme.PastelSageSurface
import kotlin.math.roundToInt

private data class HubMenuCard(
    val title: String,
    val icon: ImageVector,
    val destination: AppDestination
)

private data class HubPastelTone(val background: Color, val foreground: Color)

private val hubPastelTones = listOf(
    HubPastelTone(PastelGreenSurface, Color(0xFF34745D)),
    HubPastelTone(PastelPeachSurface, Color(0xFF9D634D)),
    HubPastelTone(PastelSandSurface, Color(0xFF7F713A)),
    HubPastelTone(PastelBlueSurface, Color(0xFF496B9A)),
    HubPastelTone(PastelPurpleSurface, Color(0xFF65558F)),
    HubPastelTone(PastelSageSurface, Color(0xFF55734E))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreHubScreen(
    onNavigate: (AppDestination) -> Unit
) {
    // The existing destinations are preserved; only the presentation changes to a compact RTL grid.
    val worshipItems = remember {
        listOf(
            HubMenuCard("القرآن الكريم", Icons.AutoMirrored.Outlined.MenuBook, AppDestination.QURAN),
            HubMenuCard("الأدعية والزيارات", Icons.Outlined.VolunteerActivism, AppDestination.DUAS_ZIYARAT),
            HubMenuCard("المسبحة", Icons.Outlined.Spa, AppDestination.TASBIH),
            HubMenuCard("مواقيت الصلاة", Icons.Outlined.Schedule, AppDestination.PRAYER_TIMES),
            HubMenuCard("اتجاه القبلة", Icons.Outlined.Explore, AppDestination.QIBLA)
        )
    }
    val serviceItems = remember {
        listOf(
            HubMenuCard("الصوتيات", Icons.Outlined.Headphones, AppDestination.AUDIO_LIBRARY),
            HubMenuCard("الحديث والروايات", Icons.Outlined.FormatQuote, AppDestination.HADITH),
            HubMenuCard("المكتبة والسيرة", Icons.Outlined.LocalLibrary, AppDestination.LIBRARY),
            HubMenuCard("الأخلاقيات", Icons.Outlined.Verified, AppDestination.ETHICS),
            HubMenuCard("المناهج والدروس", Icons.Outlined.School, AppDestination.CURRICULA),
            HubMenuCard("التقويم الهجري", Icons.Outlined.CalendarMonth, AppDestination.CALENDAR_OCCASIONS),
            HubMenuCard("البحث الشامل", Icons.Outlined.AutoAwesome, AppDestination.SEARCH),
            HubMenuCard("المفضلة", Icons.Outlined.Bookmarks, AppDestination.FAVORITES),
            HubMenuCard("الخلفيات", Icons.Outlined.Wallpaper, AppDestination.WALLPAPERS),
            HubMenuCard("الإعدادات", Icons.Outlined.Tune, AppDestination.SETTINGS)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "جميع أقسام خزائن الهدى",
            subtitle = "الموسوعة الإسلامية الشاملة؛ الأقسام المحلية متاحة دون اتصال"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("more_hub_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp)) {
                    Text("أقسام خزائن الهدى", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "بوابتك إلى القرآن والعبادات والمكتبة والخدمات الإسلامية في مكان واحد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                HubSectionGrid(title = "العبادات", items = worshipItems, onNavigate = onNavigate)
            }
            item { SectionDivider(modifier = Modifier.padding(vertical = 4.dp)) }
            item {
                HubSectionGrid(title = "الخدمات", items = serviceItems, onNavigate = onNavigate)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HubSectionGrid(
    title: String,
    items: List<HubMenuCard>,
    onNavigate: (AppDestination) -> Unit
) {
    val isDark = LocalIsDarkThemeActive.current
    val darkTones = listOf(
        HubPastelTone(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary),
        HubPastelTone(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        HubPastelTone(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary),
        HubPastelTone(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.tertiary)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items.forEachIndexed { index, card ->
                val tone = if (isDark) darkTones[index % darkTones.size]
                    else hubPastelTones[index % hubPastelTones.size]
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.235f)
                        .clickable { onNavigate(card.destination) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = tone.background,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = card.title,
                            tint = tone.foreground,
                            modifier = Modifier.padding(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        minLines = 2,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
