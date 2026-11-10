package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * سطح زجاجي حقيقي (Liquid Glass).
 *
 * يتكوّن من ثلاث طبقات:
 *  ١) طبقة طمس فعلية (RenderEffect) لأجهزة أندرويد ١٢ فأحدث — قبلها
 *     بديل شبه شفاف يحافظ على الوضوح.
 *  ٢) تدرّج شفاف خفيف يمنح إحساس الزجاج العمقي.
 *  ٣) حدّ شعري دقيق يبرز الحافة.
 *
 * يُستخدم في المشغّل الصغير أثناء التشغيل، وفي شريط التنقل، ولوحة المطور.
 */
@Composable
fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    borderColor: Color? = null,
    cornerRadius: Dp = 24.dp,
    blurRadius: Float = 28f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val base = tint ?: Color.White.copy(alpha = 0.72f)
    val border = borderColor ?: Color.White.copy(alpha = 0.24f)

    Box(modifier = modifier.clip(shape)) {
        // ١) الطمس الفعلي للعنصرunderlying
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
        )
        // ٢) التدرّج الشفاف
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            base.copy(alpha = base.alpha * 0.95f),
                            base.copy(alpha = base.alpha * 0.68f),
                            base.copy(alpha = base.alpha * 0.90f)
                        )
                    )
                )
        )
        // ٣) الحدّ الشعري
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(BorderStroke(1.dp, border), shape)
        )
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) { content() }
    }
}

/** زجاج داكن افتراضي للشاشات ذات الخلفية الغامقة. */
@Composable
fun DarkGlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        shape = shape,
        modifier = modifier,
        tint = Color(0xFF102422).copy(alpha = 0.66f),
        borderColor = Color.White.copy(alpha = 0.16f),
        cornerRadius = cornerRadius,
        content = content
    )
}
