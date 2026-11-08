package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PRAYER_STATUS_DONE
import com.example.data.local.PRAYER_STATUS_LATE
import com.example.data.local.PRAYER_STATUS_ON_TIME
import com.example.data.local.PrayerDailyStateEntity

/** أوقات الصلوات الخمس المعروضة في بطاقة "صلواتي اليوم". */
data class TrackedPrayer(
    val key: String,
    val nameAr: String,
    val timeLabel: String
)

/**
 * بطاقة "صلواتي اليوم".
 *
 * تعرض الصلوات الخمس مع حالة كل صلاة، وتتيح تعليمها كـ"أُديت" بنقرة واحدة،
 * مع حفظ تلقائي على الجهاز. مؤشر دائري يوضح عدد الصلوات المكتملة من أصل خمس.
 */
@Composable
fun PrayerTrackerCard(
    prayers: List<TrackedPrayer>,
    states: List<PrayerDailyStateEntity>,
    dateLabel: String,
    onTogglePrayer: (String) -> Unit,
    onOpenPrayerTimes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateByPrayer = states.associateBy { it.prayer }
    val completed = prayers.count { p ->
        stateByPrayer[p.key]?.status in setOf(PRAYER_STATUS_DONE, PRAYER_STATUS_ON_TIME, PRAYER_STATUS_LATE)
    }
    val progress by animateFloatAsState(
        targetValue = if (prayers.isEmpty()) 0f else completed.toFloat() / prayers.size.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
        label = "prayerProgress"
    )

    GlassCard(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionHeaderRow(
            title = "صلواتي اليوم",
            actionLabel = "المواقيت",
            onActionClick = onOpenPrayerTimes
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CompletionRing(progress = progress, completed = completed, total = prayers.size)
        }
        Spacer(modifier = Modifier.height(8.dp))
        prayers.forEach { prayer ->
            val state = stateByPrayer[prayer.key]
            val isDone = state?.status in setOf(PRAYER_STATUS_DONE, PRAYER_STATUS_ON_TIME, PRAYER_STATUS_LATE)
            PrayerRow(
                prayer = prayer,
                isDone = isDone,
                onToggle = { onTogglePrayer(prayer.key) }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (completed == prayers.size && prayers.isNotEmpty()) {
                "ما شاء الله، أتممت صلوات اليوم كلها."
            } else {
                "اضغط على أي صلاة لتعليمها، ويُحفظ ذلك تلقائياً."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrayerRow(
    prayer: TrackedPrayer,
    isDone: Boolean,
    onToggle: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "prayerRowTint"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .testTag("prayer_tracker_${prayer.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = prayer.nameAr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = prayer.timeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** حلقة إنجاز صغيرة توضح عدد الصلوات المكتملة من أصل خمس. */
@Composable
private fun CompletionRing(
    progress: Float,
    completed: Int,
    total: Int
) {
    // The colours must be resolved before the Canvas block: a draw scope is not composable.
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = Color.White.copy(alpha = 0.25f)
    Box(
        modifier = Modifier.size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(46.dp)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Text(
            text = "$completed/$total",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
