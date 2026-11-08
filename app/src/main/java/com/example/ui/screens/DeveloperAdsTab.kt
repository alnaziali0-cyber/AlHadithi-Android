package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.remote.AdEntity
import com.example.data.remote.AdFirestoreRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SectionHeaderRow
import kotlinx.coroutines.launch

/**
 * قسم "إعلانات التطبيق" في لوحة المطور.
 *
 * يتيح إنشاء إعلان بصورة من الهاتف، ومعاينتها، ونشره أو جدولته أو إيقافه
 * أو حذفه، وطلب إرسال إشعار حقيقي للمستخدمين المستهدفين.
 */
@Composable
fun DeveloperAdsTab(
    ads: List<AdEntity>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var shortDesc by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var ctaLabel by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf("") }
    var sendPush by remember { mutableStateOf(false) }
    var scheduled by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("all") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageUri = uri
                uploadedImageUrl = ""
                statusMessage = "تم اختيار الصورة — ستُرفع عند النشر"
            }
        }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard {
            SectionHeaderRow(title = "إعلانات التطبيق", actionLabel = "تحديث", onActionClick = onRefresh)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "أنشئ إعلاناً بصورة من الهاتف، ثم انشره ليظهر داخل التطبيق لكل المستخدمين.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الإعلان") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("ad_title")
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("نص الإعلان") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("ad_body")
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = shortDesc,
                onValueChange = { shortDesc = it },
                label = { Text("وصف مختصر (يظهر في الإشعار)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("رابط (اختياري)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ctaLabel,
                    onValueChange = { ctaLabel = it },
                    label = { Text("زر الإجراء") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            // اختيار صورة من الهاتف + معاينة قبل النشر
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val preview = uploadedImageUrl.ifBlank { imageUri?.toString().orEmpty() }
                    if (preview.isNotBlank()) {
                        AsyncImage(
                            model = preview,
                            contentDescription = "معاينة صورة الإعلان",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "اختر صورة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صورة الإعلان",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "اختر صورة من الهاتف (JPG/PNG)، تُضغط وتُحسَّن قبل الرفع.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "الفئة المستهدفة",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("all" to "الجميع", "users" to "المستخدمون", "guests" to "الزوار")) { (key, label) ->
                    GlassFilterChip(
                        label = label,
                        selected = target == key,
                        onClick = { target = key }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "جدولة الإعلان", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (scheduled) "يبدأ النشر عند التفعيل" else "يُنشر فوراً",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = scheduled, onCheckedChange = { scheduled = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "إرسال إشعار للمستخدمين", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "يتطلب تشغيل خدمة الإشعارات (FCM) على الخادم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = sendPush, onCheckedChange = { sendPush = it })
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    if (title.isBlank() || body.isBlank()) {
                        statusMessage = "العنوان ونص الإعلان مطلوبان"
                        return@OutlinedButton
                    }
                    busy = true
                    scope.launch {
                        val draft = AdEntity(
                            title = title,
                            text = body,
                            shortDesc = shortDesc,
                            link = link,
                            ctaLabel = ctaLabel,
                            status = if (scheduled) "scheduled" else "draft",
                            target = target,
                            sendPush = sendPush
                        )
                        val created = AdFirestoreRepository.createAd(context, draft)
                        val id = created.getOrNull()
                        if (id == null) {
                            statusMessage = "تعذّر إنشاء الإعلان: ${created.exceptionOrNull()?.message.orEmpty()}"
                            busy = false
                            return@launch
                        }
                        var imageUrl = uploadedImageUrl
                        val localUri = imageUri
                        if (localUri != null) {
                            val upload = AdFirestoreRepository.uploadAdImage(context, id, localUri)
                            imageUrl = upload.getOrNull().orEmpty()
                            if (imageUrl.isBlank()) {
                                statusMessage = "تم إنشاء الإعلان لكن تعذّر رفع الصورة"
                            }
                        }
                        val stored = draft.copy(
                            id = id,
                            imagePath = imageUrl,
                            status = if (scheduled) "scheduled" else "active"
                        )
                        val publish = AdFirestoreRepository.updateAd(context, stored)
                        if (!scheduled) {
                            AdFirestoreRepository.publishAd(context, stored.copy(status = "active"))
                        }
                        statusMessage = if (publish.isSuccess) {
                            if (scheduled) "تمت جدولة الإعلان بنجاح" else "تم نشر الإعلان بنجاح"
                        } else {
                            "تعذّر حفظ بيانات الإعلان"
                        }
                        title = ""; body = ""; shortDesc = ""; link = ""; ctaLabel = ""
                        imageUri = null; uploadedImageUrl = ""; sendPush = false
                        onRefresh()
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag("ad_publish")
            ) {
                Icon(imageVector = Icons.Outlined.Campaign, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (busy) "جارٍ النشر…" else "نشر الإعلان")
            }
            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "الإعلانات السابقة (${ads.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        ads.forEach { ad ->
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (ad.imagePath.isNotBlank()) {
                        AsyncImage(
                            model = ad.imagePath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ad.title.ifBlank { "(بدون عنوان)" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ad.text.take(90),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الحالة: ${statusLabel(ad.status)}" +
                                if (ad.sendPush) " · طلب إشعار" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            scope.launch {
                                if (ad.status == "active") {
                                    AdFirestoreRepository.pauseAd(context, ad.id)
                                } else {
                                    AdFirestoreRepository.publishAd(context, ad)
                                }
                                onRefresh()
                            }
                        }) {
                            Icon(
                                imageVector = if (ad.status == "active") Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (ad.status == "active") "إيقاف" else "نشر"
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                AdFirestoreRepository.deleteAd(context, ad.id)
                                onRefresh()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "draft" -> "مسودة"
    "scheduled" -> "مجدول"
    "active" -> "منشور"
    "paused" -> "موقوف"
    else -> "منتهي"
}
