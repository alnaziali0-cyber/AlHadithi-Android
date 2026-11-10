package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * لوحة ألوان هادئة لبطاقات الأقسام — ألوان باستيل متناسقة تناسب هوية
 * التطبيق الزجاجية الهادئة.
 */
data class PastelTileColors(
    val circle: Color,
    val icon: Color
)

object PastelPalette {
    val mint = PastelTileColors(Color(0xFFD3EADD), Color(0xFF2E7A5C))
    val cream = PastelTileColors(Color(0xFFF4EACB), Color(0xFF7A6A2E))
    val blush = PastelTileColors(Color(0xFFF8E1D8), Color(0xFFA05B44))
    val lavender = PastelTileColors(Color(0xFFE4DDF6), Color(0xFF5B4E93))
    val periwinkle = PastelTileColors(Color(0xFFDDE8F8), Color(0xFF3D5E96))
    val peach = PastelTileColors(Color(0xFFFBE5D4), Color(0xFF9A5F3C))
    val sage = PastelTileColors(Color(0xFFE2EBD6), Color(0xFF5C7346))
    val sand = PastelTileColors(Color(0xFFEDE3D2), Color(0xFF7C6444))
}

/**
 * بطاقة قسم دائرة بلون باستيل — أيقونة داخل دائرة ملوّنة خفيفة واسم القسم أسفلها.
 */
@Composable
fun PastelIconTile(
    label: String,
    icon: ImageVector,
    colors: PastelTileColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(78.dp)
            .testTag("hub_tile_$label")
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(colors.circle),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.icon,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = MaterialTheme.typography.labelMedium.lineHeight
        )
    }
}

/** فاصل متقطّع بين مجموعات الأقسام (كما في التصميم المرجعي). */
@Composable
fun DashedSectionDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 4.dp)
            .testTag("hub_dashed_divider")
    ) {
        val dash = 12.dp.toPx()
        val gap = 8.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = pathEffect
        )
    }
}

/** عنوان مجموعة الأقسام. */
@Composable
fun HubSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp)
            .testTag("hub_section_$title")
    )
}

/** صف من بطاقات الأقسام (أربع بطاقات في الصف). */
@Composable
fun HubTileRow(
    tiles: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tiles.forEach { tile ->
            Box(modifier = Modifier.width(78.dp)) { tile() }
        }
        repeat(4 - tiles.size) { Box(modifier = Modifier.width(78.dp)) }
    }
}
