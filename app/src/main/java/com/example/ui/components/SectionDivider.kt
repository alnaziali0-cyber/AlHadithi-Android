package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/** Shared soft segmented divider for major content groups. */
@Composable
fun SectionDivider(
    modifier: Modifier = Modifier,
    dashWidth: Dp = 18.dp,
    dashGap: Dp = 7.dp,
    thickness: Dp = 1.5.dp
) {
    val color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f)
    Canvas(modifier = modifier.fillMaxWidth().height(8.dp)) {
        val dashPx = dashWidth.toPx()
        val gapPx = dashGap.toPx()
        val stroke = thickness.toPx()
        val y = size.height / 2f
        var x = 0f
        while (x < size.width) {
            val end = (x + dashPx).coerceAtMost(size.width)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x, y),
                end = androidx.compose.ui.geometry.Offset(end, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            x = end + gapPx
        }
    }
}
