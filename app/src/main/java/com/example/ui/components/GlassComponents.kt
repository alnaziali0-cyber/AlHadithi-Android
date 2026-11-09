package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.LocalIsDarkThemeActive
import com.example.ui.theme.TurquoisePrimaryLight
import com.example.ui.theme.TurquoiseSecondaryLight

/**
 * Serene spiritual ambient canvas layered beneath the entire app so every translucent frosted
 * GlassCard, NavigationBar, TopBar, and Sheet exhibits genuine luminous depth without clutter.
 */
@Composable
fun GlassAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkThemeActive.current
    val baseBg = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val glowAlpha = if (isDark) 0.22f else 0.045f
            val goldAlpha = if (isDark) 0.12f else 0.035f

            // Top-right calm turquoise sanctuary glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TurquoiseSecondaryLight.copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.10f),
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = Offset(w * 0.88f, h * 0.10f)
            )

            // Mid-left soft heritage gold aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HeritageGold.copy(alpha = goldAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.12f, h * 0.46f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.12f, h * 0.46f)
            )

            // Bottom emerald reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        EmeraldDeep.copy(alpha = glowAlpha * 0.9f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.75f, h * 0.88f),
                    radius = w * 0.80f
                ),
                radius = w * 0.80f,
                center = Offset(w * 0.75f, h * 0.88f)
            )
        }

        content()
    }
}

/**
 * Translucent Frosted Glass Card with luminous specular rim border and zero heavy shadows.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    containerColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkThemeActive.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_press_scale"
    )

    val resolvedBgBrush = if (containerColor != null) {
        Brush.verticalGradient(
            listOf(
                containerColor.copy(alpha = (containerColor.alpha * 0.82f).coerceIn(0.45f, 0.92f)),
                containerColor.copy(alpha = (containerColor.alpha * 0.68f).coerceIn(0.40f, 0.88f))
            )
        )
    } else if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF142B28).copy(alpha = 0.64f),
                Color(0xFF0E201E).copy(alpha = 0.54f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                Color(0xFFF4FBFA).copy(alpha = 0.58f)
            )
        )
    }

    val resolvedBorderBrush = if (borderColor != null) {
        Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.45f)))
    } else if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                TurquoiseSecondaryLight.copy(alpha = 0.18f),
                HeritageGold.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.92f),
                TurquoisePrimaryLight.copy(alpha = 0.22f),
                HeritageGold.copy(alpha = 0.18f)
            )
        )
    }

    val shape = RoundedCornerShape(cornerRadius)
    val clickMod = if (onClick != null) {
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else {
        Modifier.clip(shape)
    }

    Box(
        modifier = modifier
            .then(clickMod)
            .background(resolvedBgBrush, shape)
            .border(BorderStroke(1.dp, resolvedBorderBrush), shape)
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}

/**
 * Translucent Emerald-Turquoise Glass Hero Card with luminous gold rim for Next Prayer & Featured Banners.
 */
@Composable
fun EmeraldHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkThemeActive.current
    val gradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0E3A36).copy(alpha = 0.82f),
                Color(0xFF0A2724).copy(alpha = 0.76f),
                Color(0xFF13453F).copy(alpha = 0.80f)
            )
        )
    } else {
        Brush.linearGradient(
            // Keep all gradient stops dark enough for white text in light theme.
            colors = listOf(
                EmeraldDeep.copy(alpha = 0.97f),
                Color(0xFF0B625A).copy(alpha = 0.96f),
                Color(0xFF0B5B55).copy(alpha = 0.96f)
            )
        )
    }
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient, shape)
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            HeritageGold.copy(alpha = 0.55f)
                        )
                    )
                ),
                shape
            )
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}

/**
 * Frosted Glass Top Brand Bar featuring the official Al-Hadithi logo, offline-ready badge,
 * governorate chip, and quick action icons.
 * Also supports a hidden 7-rapid-tap developer gesture on the logo for desktop mouse-only emulators
 * alongside the primary physical phone-shake trigger.
 */
@Composable
fun AlHadithiBrandTopBar(
    governorateName: String,
    onGovernorateClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSecretDeveloperTrigger: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkThemeActive.current
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    val glassSurfaceColor = if (isDark) {
        Color(0xFF122825).copy(alpha = 0.56f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val glassBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = glassSurfaceColor,
        border = BorderStroke(1.dp, glassBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapMs > 1500L) {
                                logoTapCount = 1
                            } else {
                                logoTapCount++
                            }
                            lastTapMs = now
                            if (logoTapCount >= 7) {
                                logoTapCount = 0
                                onSecretDeveloperTrigger?.invoke()
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_alhadithi_logo),
                        contentDescription = stringResource(R.string.cd_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(1.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            onClick = onGovernorateClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.testTag("home_governorate_chip")
                        ) {
                            Text(
                                text = governorateName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OfflinePin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "الموسوعة المتكاملة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onFavoritesClick,
                    modifier = Modifier.testTag("top_favorites_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_favorites),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Translucent Glass Sub-screen Header with back button, title, optional subtitle, and trailing actions.
 */
@Composable
fun SubScreenTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val isDark = LocalIsDarkThemeActive.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF122825).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.60f),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.82f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("subscreen_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailing != null) {
                trailing()
            }
        }
    }
}

/**
 * Translucent Frosted Filter Pill Chip in Glass UI style.
 */
@Composable
fun GlassFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val isDark = LocalIsDarkThemeActive.current
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
    } else if (isDark) {
        Color(0xFF152E2B).copy(alpha = 0.56f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        HeritageGold.copy(alpha = 0.55f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

/**
 * Source Attribution Badge for Hadiths, Duas, Ziyarat, and Tafsir (Religious Integrity Rule).
 */
@Composable
fun SourceCitationBadge(
    attribution: String,
    sourceBook: String,
    volumeOrGrading: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attribution,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                val fullSource = buildString {
                    append("المصدر: ")
                    append(sourceBook)
                    if (!volumeOrGrading.isNullOrBlank()) {
                        append(" · ")
                        append(volumeOrGrading)
                    }
                }
                Text(
                    text = fullSource,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
