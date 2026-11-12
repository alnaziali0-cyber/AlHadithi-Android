package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.SupportChannels

/**
 * Support helpers shared by the settings hub.
 *
 * They live in their own file so the settings screen stays focused on the layout while the
 * platform interaction details (intents, safe link checks) remain isolated and testable.
 */

/** Opens the user's mail client with the support address pre-filled. */
internal fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts("mailto", SupportChannels.SUPPORT_EMAIL, null)
        putExtra(Intent.EXTRA_SUBJECT, "التواصل مع دعم تطبيق خزائن الهدى")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "لا يوجد تطبيق بريد إلكتروني مثبت على الجهاز", Toast.LENGTH_LONG).show()
    }
}

/**
 * Opens a social profile after verifying that the URL really points at the expected host and
 * path. The check prevents a tampered configuration from redirecting the user elsewhere.
 */
internal fun openSupportProfile(
    context: Context,
    url: String,
    expectedHost: String,
    expectedPath: String
) {
    if (!SupportChannels.isSafeHttpsProfile(url, expectedHost, expectedPath)) {
        Toast.makeText(context, "رابط الدعم غير صالح", Toast.LENGTH_LONG).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "تعذر فتح رابط التواصل على هذا الجهاز", Toast.LENGTH_LONG).show()
    }
}

/** Formats a byte count in Arabic-friendly units. */
internal fun formatBytes(bytes: Long): String = when {
    bytes < 1_024L -> "$bytes بايت"
    bytes < 1_048_576L -> "${"%.1f".format(java.util.Locale.US, bytes / 1_024.0)} كيلوبايت"
    else -> "${"%.1f".format(java.util.Locale.US, bytes / 1_048_576.0)} ميغابايت"
}

/** The kinds of message the user can send to the support team. */
internal enum class SupportTopic(val arabicTitle: String) {
    TECHNICAL("التواصل مع الدعم الفني"),
    BUG("الإبلاغ عن مشكلة"),
    SUGGESTION("اقتراح تطوير"),
    RELIGIOUS_CONTENT("الإبلاغ عن خطأ في محتوى إسلامي")
}

/** A titled paragraph used inside the permissions dialog. */
@Composable
internal fun PermissionRow(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A label/value line used inside the information cards. */
@Composable
internal fun InfoRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
