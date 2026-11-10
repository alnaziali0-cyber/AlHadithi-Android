package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIsDarkThemeActive
import kotlin.math.roundToInt

/** عنصر واحد في شريط التنقل العائم. */
data class GlassNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * شريط تنقل سفلي عائم بمظهر الزجاج الشفاف على غرار iOS.
 *
 * • كبسولة شفافة بتدرّج خفيف، وحافة زجاجية ساطعة من الأعلى، وظل ناعم.
 * • مؤشر تحديد فاتح يتحرك بين الأقسام بإيقاع زنبركي.
 * • أيقونات مملوءة للقسم المحدد وخطية لبقية الأقسام. تُعرض بلا نصوص كما في التصميم المرجعي،
 *   ويبقى اسم كل قسم متاحاً لقارئات الشاشة.
 * • الشريط لا يغطي المحتوى لأن Scaffold يحسب الحشو أسفله.
 *
 * ملاحظة: الطمس الحقيقي لما خلف الشريط يتطلب طمس محتوى الشاشة نفسها. لذلك يعتمد المظهر
 * على الشفافية والحواف والإضاءة، وهو ما يعطي الشكل المطلوب دون كلفة أداء إضافية.
 */
@Composable
fun FloatingGlassNavBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 34.dp,
    itemSpacing: Dp = 4.dp
) {
    val isDark = LocalIsDarkThemeActive.current
    val density = LocalDensity.current
    val capsule = RoundedCornerShape(cornerRadius)
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    val glassFill = Brush.verticalGradient(
        if (isDark) {
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f))
        } else {
            listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.48f))
        }
    )
    val glassRim = Brush.verticalGradient(
        if (isDark) {
            listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.20f))
        } else {
            listOf(Color.White.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.08f))
        }
    )
    val pillFill = if (isDark) Color.White.copy(alpha = 0.20f) else Color(0x220E3B3F)
    val pillRim = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color(0x26000000)

    var rowWidthPx by remember { mutableIntStateOf(0) }
    val insetPx = with(density) { itemSpacing.toPx() }
    val itemWidthPx = if (items.isEmpty() || rowWidthPx <= 0) 0f else rowWidthPx / items.size.toFloat()
    val pillOffsetPx by animateFloatAsState(
        targetValue = safeIndex * itemWidthPx,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "navPillOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(68.dp)
            .shadow(elevation = 12.dp, shape = capsule, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(capsule)
            .background(glassFill)
            .border(BorderStroke(1.dp, glassRim), capsule)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        // مؤشر التحديد: يقع خلف الأيقونات ويتحرك إلى القسم المحدد.
        if (itemWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((pillOffsetPx + insetPx).roundToInt(), 0) }
                    .size(
                        width = with(density) { (itemWidthPx - 2 * insetPx).toDp() },
                        height = 54.dp
                    )
                    .clip(RoundedCornerShape(percent = 50))
                    .background(pillFill)
                    .border(BorderStroke(1.dp, pillRim), RoundedCornerShape(percent = 50))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { rowWidthPx = it.width },
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                GlassNavItem(
                    item = item,
                    selected = index == safeIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
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
    val isDark = LocalIsDarkThemeActive.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "navItemScale"
    )
    val selectedTint = if (isDark) Color.White else Color(0xFF0E3B3F)
    val idleTint = if (isDark) Color.White.copy(alpha = 0.72f) else Color(0xFF0E3B3F).copy(alpha = 0.62f)
    val tint by animateColorAsState(
        targetValue = if (selected) selectedTint else idleTint,
        animationSpec = tween(durationMillis = 220),
        label = "navIconTint"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .semantics { contentDescription = item.label }
            .testTag(item.testTag)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = selected,
            animationSpec = tween(durationMillis = 180),
            label = "navIconCrossfade"
        ) { isSelected ->
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
