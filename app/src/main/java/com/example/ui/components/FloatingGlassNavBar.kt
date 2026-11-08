package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalIsDarkThemeActive

/** عنصر واحد في شريط التنقل العائم. */
data class GlassNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * شريط تنقل سفلي عائم بتأثير Liquid Glass.
 *
 * • خلفية زجاجية شفافة مع حافة زجاجية رفيعة.
 * • طمس حقيقي (BlurEffect) على أندرويد ١٢ فأحدث، مع بديل شفاف أنيق قبل ذلك.
 * • مؤشر تحديد منزلق بين الأقسام بإيقاع زنبركي (spring) — لا مجرد تغيّر لون.
 * • أيقونات خطية (Outlined/Filled) مع تلاشٍ متقاطع عند التبديل.
 * • تأثير ضغط زنبركي على كل زر.
 * • يحترم Safe Area ولا يغطّي المحتوى لأن Scaffold يحسب الحشو أسفله.
 */
@Composable
fun FloatingGlassNavBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 30.dp,
    itemSpacing: androidx.compose.ui.unit.Dp = 4.dp
) {
    val isDark = LocalIsDarkThemeActive.current
    val glassColor = if (isDark) Color(0xFF102422).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.72f)
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f)
    val shape = RoundedCornerShape(cornerRadius)
    val density = LocalDensity.current

    var rowWidthPx by remember { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    // المؤشر المنزلق: عرض كل عنصر = (عرض الصف - الحشو) / عدد العناصر.
    val itemWidthPx = remember(rowWidthPx, items.size) {
        if (items.isEmpty() || rowWidthPx <= 0) 0
        else (rowWidthPx - with(density) { (12.dp + itemSpacing * (items.size - 1)).toPx().toInt() }) / items.size
    }
    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { (safeIndex * itemWidthPx + safeIndex * itemSpacing.toPx()).toDp() },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 320f),
        label = "navIndicatorOffset"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { itemWidthPx.toDp() },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 320f),
        label = "navIndicatorWidth"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape)
    ) {
        // طبقة الخلفية الزجاجية: طمس حقيقي حيثما يدعمه النظام.
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(28f, 28f, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            glassColor,
                            (if (isDark) Color(0xFF17302D).copy(alpha = 0.66f) else Color(0xFFF3FAF8).copy(alpha = 0.62f)),
                            glassColor
                        )
                    )
                )
                .border(BorderStroke(1.dp, glassBorder), shape)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 6.dp, vertical = 7.dp)
        ) {
            // المؤشر المنزلق خلف الأيقونات.
            if (itemWidthPx > 0) {
                Box(
                    modifier = Modifier
                        .zIndex(0f)
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(indicatorWidth)
                        .height(48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChangedCompat { rowWidthPx = it },
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    GlassNavItem(
                        item = item,
                        selected = index == safeIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier
                            .weight(1f)
                            .zIndex(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassNavItem(
    item: GlassNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "navItemScale"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "navIconTint"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .semantics { contentDescription = item.label }
            .testTag(item.testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.animation.Crossfade(
            targetState = selected,
            animationSpec = tween(durationMillis = 180),
            label = "navIconCrossfade"
        ) { isSelected ->
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

/** اختصار لتتبّع عرض العنصر. */
private fun Modifier.onSizeChangedCompat(onSize: (Int) -> Unit): Modifier =
    this.onSizeChanged { onSize(it.width) }
