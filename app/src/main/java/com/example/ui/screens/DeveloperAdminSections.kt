package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AkhlaqCategoryEntity
import com.example.data.local.AkhlaqLessonEntity
import com.example.data.remote.AuditEntry
import com.example.data.remote.NotificationLogEntry
import com.example.data.remote.RegisteredDevice
import com.example.domain.SurahCatalog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SectionHeaderRow

/** أقسام لوحة الإدارة العشرة. */
enum class AdminSection(val label: String, val icon: ImageVector) {
    DASHBOARD("لوحة القيادة", Icons.Outlined.Dashboard),
    USERS("المستخدمون", Icons.Outlined.Groups),
    CONTENT("إدارة المحتوى", Icons.Outlined.Source),
    ADS("إدارة الإعلانات", Icons.Outlined.Campaign),
    NOTIFICATIONS("إدارة الإشعارات", Icons.Outlined.Notifications),
    QURAN("القرآن والقراء", Icons.Outlined.RecordVoiceOver),
    AKHLAQ("الدروس الأخلاقية", Icons.Outlined.Policy),
    OCCASIONS("المناسبات", Icons.Outlined.ChevronLeft),
    SETTINGS("إدارة الإعدادات", Icons.Outlined.Settings),
    AUDIT("سجل العمليات", Icons.Outlined.History)
}

/**
 * شريط أقسام لوحة الإدارة.
 */
@Composable
fun AdminSectionTabs(
    selected: AdminSection,
    onSelect: (AdminSection) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().testTag("admin_section_tabs")
    ) {
        items(AdminSection.entries.toList(), key = { it.name }) { section ->
            GlassFilterChip(
                label = section.label,
                selected = selected == section,
                onClick = { onSelect(section) },
                modifier = Modifier.testTag("admin_tab_${section.name}")
            )
        }
    }
}

/** لوحة القيادة: أرقام فورية عن المحتوى والإعلانات والأجهزة. */
@Composable
fun AdminDashboardSection(
    stats: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        SectionHeaderRow(title = "لوحة القيادة")
        Spacer(modifier = Modifier.height(8.dp))
        stats.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** قسم المستخدمين: الأجهزة المسجّلة التي يمكنها استقبال الإشعارات. */
@Composable
fun AdminUsersSection(
    devices: List<RegisteredDevice>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeaderRow(title = "المستخدمون والأجهزة", actionLabel = "${devices.size} جهاز")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "تظهر هنا الأجهزة المسجّلة التي سمحت باستقبال الإشعارات. " +
                    "الحذف وإدارة الأدوار الكاملة تتم عبر حساب الخدمة (Admin SDK) على الخادم.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (devices.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "لا توجد أجهزة مسجّلة بعد. سجّل الجهاز من إعدادات الإشعارات في التطبيق.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            devices.take(30).forEach { device ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.platform.ifBlank { "android" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "المعرّف: ${device.userId.take(18)}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

/** قسم الإشعارات: تفضيلات الإرسال وسجل النتائج الفعلي. */
@Composable
fun AdminNotificationsSection(
    log: List<NotificationLogEntry>,
    channels: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeaderRow(title = "قنوات الإشعارات")
            Spacer(modifier = Modifier.height(6.dp))
            channels.forEach { channel ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = channel, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "مفعّلة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeaderRow(title = "سجل عمليات الإرسال", actionLabel = "${log.size} عملية")
            Spacer(modifier = Modifier.height(6.dp))
            if (log.isEmpty()) {
                Text(
                    text = "لم تُسجَّل أي عملية إرسال بعد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                log.take(25).forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title.ifBlank { entry.kind },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                            if (entry.providerMessage.isNotBlank()) {
                                Text(
                                    text = entry.providerMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = if (entry.status == "sent") "أُرسل" else entry.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.status == "sent") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }
    }
}

/** قسم القرآن والقراء: القرّاء المتاحون فعلياً وحالة كل رابط. */
@Composable
fun AdminQuranSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeaderRow(title = "مكتبة القرّاء", actionLabel = "${SurahCatalog.reciters.size} قارئاً")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "كل رابط في هذه القائمة مُتحقَّق منه فعلياً. القرّاء الذين تتوفر لديهم " +
                    "تلاوة آية مفردة فقط يُسمعون آية آية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SurahCatalog.reciters.forEach { reciter ->
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reciter.arabicName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                reciter.hasSurahAudio && reciter.hasAyahAudio -> "تلاوة سورة كاملة + آية مفردة"
                                reciter.hasSurahAudio -> "تلاوة سورة كاملة"
                                reciter.hasAyahAudio -> "تلاوة آية مفردة"
                                else -> "صفحة القارئ الخارجية"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (reciter.hasSurahAudio || reciter.hasAyahAudio) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                }
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeaderRow(title = "سور المصحف")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${SurahCatalog.allSurahs.size} سورة · " +
                    "${SurahCatalog.allSurahs.sumOf { it.ayahCount }} آية · " +
                    "${SurahCatalog.allSurahs.count { it.hasSajdah }} سورة سجدة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** قسم الدروس الأخلاقية: التصنيفات والدروس والاختبارات. */
@Composable
fun AdminAkhlaqSection(
    categories: List<AkhlaqCategoryEntity>,
    lessons: List<AkhlaqLessonEntity>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeaderRow(
                title = "الدروس الأخلاقية",
                actionLabel = "${categories.size} تصنيف · ${lessons.size} درس"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "المحتوى محمّل من أصل موثّق داخل التطبيق: الآيات من نص المصحف " +
                    "المتحقَّق منه، والأحاديث مع تخريجها. يمكن إضافة الدروس وتعديلها من لوحة " +
                    "المحتوى أو من خادم الإدارة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        categories.forEach { category ->
            val count = lessons.count { it.categoryId == category.id }
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$count درس",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** قسم سجل العمليات. */
@Composable
fun AdminAuditSection(
    entries: List<AuditEntry>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeaderRow(title = "سجل العمليات", actionLabel = "${entries.size} عملية")
            Spacer(modifier = Modifier.height(6.dp))
            if (entries.isEmpty()) {
                Text(
                    text = "لا توجد عمليات مسجّلة بعد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.take(40).forEach { entry ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = "${entry.action}${if (entry.entity.isNotBlank()) " · ${entry.entity}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = buildString {
                                if (entry.actorEmail.isNotBlank()) append(entry.actorEmail)
                                if (entry.meta.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(entry.meta)
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}
