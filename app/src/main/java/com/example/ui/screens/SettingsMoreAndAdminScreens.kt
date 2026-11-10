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
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Place
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
import com.example.ui.components.DashedSectionDivider
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.HubSectionTitle
import com.example.ui.components.PastelIconTile
import com.example.ui.components.PastelPalette
import com.example.ui.components.PastelTileColors
import com.example.ui.components.SubScreenTopBar
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.AppFontChoice
import com.example.ui.theme.LocalIsDarkThemeActive
import kotlin.math.roundToInt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.VolunteerActivism

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
    // لوحة الأقسام: مجموعات بألوان باستيل هادئة مع فاصل متقطّع بين المجموعات
    // (كما في التصميم المرجعي).
    val worship = listOf(
        HubTile("القرآن الكريم", Icons.AutoMirrored.Outlined.MenuBook, PastelPalette.mint, AppDestination.QURAN),
        HubTile("الأدعية", Icons.Outlined.VolunteerActivism, PastelPalette.blush, AppDestination.DUAS_ZIYARAT),
        HubTile("الزيارات", Icons.Outlined.Mosque, PastelPalette.cream, AppDestination.DUAS_ZIYARAT),
        HubTile("الأذكار", Icons.Outlined.AutoAwesome, PastelPalette.lavender, AppDestination.DUAS_ZIYARAT),
        HubTile("المسبحة", Icons.Outlined.Spa, PastelPalette.periwinkle, AppDestination.TASBIH),
        HubTile("مواقيت الصلاة", Icons.Outlined.Schedule, PastelPalette.peach, AppDestination.PRAYER_TIMES),
        HubTile("اتجاه القبلة", Icons.Outlined.Explore, PastelPalette.sage, AppDestination.QIBLA)
    )
    val services = listOf(
        HubTile("الصوتيات", Icons.Outlined.Headphones, PastelPalette.mint, AppDestination.AUDIO_LIBRARY),
        HubTile("الأخلاقيات", Icons.Outlined.Handshake, PastelPalette.lavender, AppDestination.AKHLAQ),
        HubTile("المكتبة والسيرة", Icons.Outlined.LocalLibrary, PastelPalette.cream, AppDestination.LIBRARY),
        HubTile("الحديث والروايات", Icons.Outlined.FormatQuote, PastelPalette.blush, AppDestination.HADITH),
        HubTile("المناهج والدروس", Icons.Outlined.School, PastelPalette.sage, AppDestination.CURRICULA),
        HubTile("التقويم الهجري", Icons.Outlined.CalendarMonth, PastelPalette.sand, AppDestination.CALENDAR_OCCASIONS),
        HubTile("البحث الشامل", Icons.Outlined.AutoAwesome, PastelPalette.periwinkle, AppDestination.SEARCH),
        HubTile("المفضلة", Icons.Outlined.Bookmarks, PastelPalette.cream, AppDestination.FAVORITES),
        HubTile("الإعدادات", Icons.Outlined.Tune, PastelPalette.peach, AppDestination.SETTINGS)
    )
    val occasions = listOf(
        HubTile("إمساكية شهر رمضان", Icons.Outlined.CalendarMonth, PastelPalette.lavender, AppDestination.CALENDAR_OCCASIONS),
        HubTile("بوابة الحج", Icons.Outlined.Mosque, PastelPalette.mint, AppDestination.CALENDAR_OCCASIONS),
        HubTile("دليل زائر الأربعين", Icons.Outlined.Place, PastelPalette.periwinkle, AppDestination.LIBRARY),
        HubTile("الخلفيات الإسلامية", Icons.Outlined.Wallpaper, PastelPalette.sand, AppDestination.WALLPAPERS)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "جميع أقسام الحديثي",
            subtitle = "الموسوعة الإسلامية الشاملة؛ معظم الأقسام متاحة دون اتصال"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("more_hub_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HubSectionTitle(title = "العبادات")
                HubTileGrid(tiles = worship, onNavigate = onNavigate)
            }
            item { DashedSectionDivider() }
            item {
                HubSectionTitle(title = "الخدمات")
                HubTileGrid(tiles = services, onNavigate = onNavigate)
            }
            item { DashedSectionDivider() }
            item {
                HubSectionTitle(title = "المناسبات")
                HubTileGrid(tiles = occasions, onNavigate = onNavigate)
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/** عنصر قسم في لوحة الأقسام. */
private data class HubTile(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val colors: PastelTileColors,
    val destination: AppDestination
)

/** شبكة بطاقات الأقسام: أربعة أعمدة مع تدفّق تلقائي للأسطر التالية. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HubTileGrid(
    tiles: List<HubTile>,
    onNavigate: (AppDestination) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        tiles.forEach { tile ->
            PastelIconTile(
                label = tile.label,
                icon = tile.icon,
                colors = tile.colors,
                onClick = { onNavigate(tile.destination) }
            )
        }
    }
}

