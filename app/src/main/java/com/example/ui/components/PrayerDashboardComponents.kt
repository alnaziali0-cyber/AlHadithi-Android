package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DailyPrayerSchedule
import com.example.domain.GovernorateInfo
import com.example.domain.NextPrayerCountdown
import com.example.domain.PrayerKey
import com.example.ui.theme.LocalStatusColors

/**
 * The five daily prayers rendered as a horizontal row of equally sized tiles.
 *
 * Each tile keeps the prayer name and its time on separate lines with `maxLines = 1`, so the row
 * stays stable in RTL and on narrow screens: nothing overlaps and no time is ever clipped.
 * The current (upcoming) prayer is highlighted with the app's primary colour.
 */
@Composable
fun PrayerTimesStrip(
    schedule: DailyPrayerSchedule,
    countdown: NextPrayerCountdown,
    formatPrayerTime: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current
    val prayerItems = listOf(
        PrayerKey.FAJR to schedule.fajrMinutes,
        PrayerKey.DHUHR to schedule.dhuhrMinutes,
        PrayerKey.ASR to schedule.asrMinutes,
        PrayerKey.MAGHRIB to schedule.maghribMinutes,
        PrayerKey.ISHA to schedule.ishaMinutes
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        prayerItems.forEach { (prayer, minutes) ->
            val isNext = prayer.arabicName == countdown.prayerArabicName
            val containerColor = if (isNext) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            }
            val nameColor = if (isNext) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val timeColor = if (isNext) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag("prayer_tile_${prayer.key}"),
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                border = if (isNext) {
                    null
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 10.dp, horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = prayer.arabicName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = formatPrayerTime(minutes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = timeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    // Referenced so the semantic status palette stays linked to this component even when the
    // upcoming prayer is unavailable.
    @Suppress("UNUSED_EXPRESSION")
    statusColors
}

/**
 * A compact row that shows the upcoming prayer, the countdown and the source of the timetable.
 * It is used inside the prayer-times card so the information hierarchy stays: title, then the
 * countdown, then the technical details.
 */
@Composable
fun PrayerCountdownPanel(
    countdown: NextPrayerCountdown,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (countdown.remainingSeconds > 0L) {
                        "الصلاة القادمة · ${countdown.prayerArabicName}"
                    } else {
                        "الصلاة القادمة"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countdown.prayerFormattedTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (countdown.remainingSeconds > 0L) countdown.countdownHhMmSs else "غير متاح",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * A small, non-intrusive freshness chip that tells the user when the timetable was refreshed.
 * When the source is not the verified AlAdhan feed, the chip switches to the warning palette.
 */
@Composable
fun PrayerSourceChip(
    sourceLabel: String,
    updatedAtMillis: Long,
    isVerifiedSource: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current
    val tint = if (isVerifiedSource) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        statusColors.warning
    }
    val updatedText = if (updatedAtMillis > 0L) {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale("ar", "IQ")).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Baghdad")
        }
        " · تحديث ${formatter.format(java.util.Date(updatedAtMillis))}"
    } else {
        ""
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = if (isVerifiedSource) Icons.Outlined.Info else Icons.Outlined.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = sourceLabel + updatedText,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A warning banner shown when the calculated timetable does not match the reference
 * "Mu'min Bag" timings. It is deliberately calm: a soft warning container, not a red alarm.
 */
@Composable
fun PrayerTimingMismatchWarning(
    message: String,
    isWarning: Boolean = true,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current
    val accent = if (isWarning) statusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
    val container = if (isWarning) statusColors.warningContainer else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * One row of the prayer-follow-up list: a tappable status circle, the prayer name, its time and
 * a short state description. The whole row is the touch target (>= 48dp) and the circle carries
 * its own accessible label.
 */
@Composable
fun PrayerTrackingRow(
    prayer: PrayerKey,
    timeText: String,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current
    val stateLabel = if (isCompleted) "مكتملة" else "غير مكتملة"
    val stateColor = if (isCompleted) statusColors.success else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .testTag("prayer_tracking_${prayer.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isCompleted) {
                statusColors.successContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            border = BorderStroke(
                1.dp,
                if (isCompleted) {
                    statusColors.success.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ),
            modifier = Modifier
                .size(40.dp)
                .clearAndSetSemantics {
                    contentDescription = "${prayer.arabicName} $stateLabel"
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isCompleted) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (isCompleted) statusColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prayer.arabicName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = stateColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = timeText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The header of the prayer-times card: title, governorate, the Hijri and Gregorian dates and a
 * clock icon, mirroring the reference layout.
 */
@Composable
fun PrayerTimesCardHeader(
    governorate: GovernorateInfo,
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "مواقيت الصلاة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = governorate.arabicName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = schedule.hijriDate.formattedArabic(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "· ${schedule.gregorianDateLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "مواقيت الصلاة",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(10.dp).size(22.dp)
            )
        }
    }
}

/** Minimal helper so a row can expose a single accessible action without duplicating semantics. */
internal val SettingsRowIconSize: androidx.compose.ui.unit.Dp = 22.dp
