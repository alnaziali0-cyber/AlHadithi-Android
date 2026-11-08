package com.example.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * بطاقة آية في القارئ.
 *
 * التصميم يتبع الصورة المرجعية: خلفية إضاءة هادئة، بطاقة شبه بيضاء لكل آية
 * بحدود دقيقة وزوايا دائرية، ورقم الآية داخل دائرة صغيرة على جهة اليمين
 * (بداية السطر في الاتجاه من اليمين إلى اليسار).
 */
@Composable
fun QuranAyahCard(
    ayahNumber: Int,
    ayahText: String,
    textStyle: TextStyle,
    containerColor: Color,
    borderColor: Color,
    numberColor: Color,
    textColor: Color,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("quran_ayah_card_$ayahNumber")
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // رقم الآية داخل دائرة صغيرة على جهة البداية (اليمين في RTL).
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(numberColor.copy(alpha = if (isHighlighted) 0.32f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ayahNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = numberColor,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = ayahText,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * شريط المناسبة الإسلامية أعلى الصفحة الرئيسية.
 *
 * شريط أفقي صغير وأنيق يعرض المناسبة القادمة مع التاريخ الهجري، ولا يظهر
 * إلا عند وجود مناسبة موثّقة.
 */
@Composable
fun OccasionBar(
    title: String,
    subtitle: String,
    daysLabel: String,
    containerColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_occasion_bar")
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1
                )
                Text(
                    text = "$daysLabel · $subtitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}
